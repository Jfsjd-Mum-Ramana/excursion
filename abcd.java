package org.vdsi.space.collections.customsshcollector.services;

import com.jcraft.jsch.*;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.vdsi.space.collections.customsshcollector.config.ProfileCheckConfig;
import org.vdsi.space.collections.customsshcollector.repository.LuceneCollectionAuditRepository;
import org.vdsi.space.collections.customsshcollector.util.CollectorUtil;
import org.vdsi.space.collections.customsshcollector.util.DateUtil;
import org.vdsi.space.collections.customsshcollector.util.FileUtil;
import org.vdsi.space.collections.customsshcollector.util.ZipUtil;
import org.vdsi.space.collections.lucene.enums.JobStatus;
import org.vdsi.space.collections.lucene.enums.ProcessType;
import org.vdsi.space.collections.lucene.model.CollectionAudit;
import org.vdsi.space.collections.lucene.model.SpaceCollector;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SSHService {
    public static final String SSH_KNOWN_HOSTS = "/.ssh/known_hosts";
    private static final Logger LOGGER = LoggerFactory.getLogger(SSHService.class);
    @Autowired
    CollectorUtil collectorUtil;
    @Autowired
    DateUtil dateUtil;
    @Autowired
    private LuceneCollectionAuditRepository lucenceColletionAuditRepo;
    private JSchFactory jschFactory;
    private OutputStreamCreator outputStreamCreator;
    @Autowired
    KafkaProducerService kafkaProducerService;
    @Autowired
    private S3Service s3Service;

    private JSch jsch = new JSch();

    public SSHService(LuceneCollectionAuditRepository lucenceColletionAuditRepo,
                      JSchFactory jschFactory, OutputStreamCreator outputStreamCreator) {
        this.lucenceColletionAuditRepo = lucenceColletionAuditRepo;
        this.jschFactory = jschFactory;
        this.outputStreamCreator = outputStreamCreator;
        //this.s3Service = s3Service;
    }

    public boolean retrieveData(SpaceCollector sp, String dateReceived, String auditTopic) throws Exception {
        LOGGER.info("Entered SSH service");
        String sshUsername = sp.getUserName();
        String sshHost = sp.getUrl();
        String sshPassword = sp.getPassword();
        int port = sp.getPort().intValue();

        JSch jsch = getJsch(sshHost, sshPassword);

        Session session = getSession(sshUsername, sshHost, sshPassword, port, jsch);
        session.connect();

        ChannelSftp channelSftp = null;
        try {
            String remoteDirectory = FileUtil.getDirectory(sp.getInputFilePath());
            String localDirectory = FileUtil.getDirectory(sp.getOutputFilePath());
            LOGGER.info("remoteDirectory : " + remoteDirectory + " localDirectory : " + localDirectory);

            channelSftp = getChannelSftp(session, remoteDirectory);
            Vector<ChannelSftp.LsEntry> files = channelSftp.ls(".");

            Map<String, String> auditStatusMap = getAuditStatusMap(files, remoteDirectory);

            // Download each file to the local directory
            for (ChannelSftp.LsEntry file : files) {
                if (!file.getAttrs().isDir()) {
                    String filePath = remoteDirectory + file.getFilename(); // Use absolute file path
                    retrieveAttributesOfRemoteFile(channelSftp, file, remoteDirectory, sp.getInputFilePath());

                    String status = auditStatusMap.get(filePath);
                    if (status == null || "COLLECTION_FAILED".equals(status)) {
                        try {
                            String outputFile = localDirectory + file.getFilename();
                            LOGGER.info("in filePath : " + filePath + " out filePath : " + outputFile);

                            FileUtil.downloadFile(outputStreamCreator.create(outputFile), channelSftp.get(file.getFilename()),
                                    filePath, outputFile, sp.getOutputFilePath());

                            List<String> filesList = new ArrayList<>();
                            if (ZipUtil.isZipFile(outputFile)) {
                                String fileNameWithoutExtn = FileUtil.getFileNameWithoutExtn(filePath);
                                FileUtil.createDirectoryIfNotExists(sp.getOutputFilePath() + "/" + fileNameWithoutExtn);

                                ZipUtil.extract(outputFile, sp.getOutputFilePath() + "/" + fileNameWithoutExtn);
                                filesList.addAll(FileUtil.readFilesInDirectory(sp.getOutputFilePath() + "/" + fileNameWithoutExtn));
                            } else {
                                filesList.add(outputFile);
                            }

                            for (String fileName : filesList) {
                                Path insideFilePath = Paths.get(fileName);
                                Path folderPath = Paths.get(sp.getOutputFilePath()).relativize(insideFilePath.getParent());

                                String unixBasedPath = folderPath.toString().replace("\\", "/");

                                String key = String.format("%s/%s/%s/%s/%s",
                                        ProfileCheckConfig.activeProfile,
                                        sp.getFileType(),
                                        sshHost,
                                        dateUtil.getDateInMMddyyyy(),
                                        !StringUtils.isBlank(unixBasedPath)
                                                ? (unixBasedPath.toString() + "/" + insideFilePath.getFileName())
                                                : insideFilePath.getFileName());
                                System.out.println("**************************");
                                System.out.println("Filename: " + fileName);
                                System.out.println("Key: " + key);
                                System.out.println("folderPath: " + folderPath);
                                System.out.println("Inside File Path: " + insideFilePath);
                                System.out.println("**************************");

                                s3Service.pushToS3(fileName, key);

                                CollectionAudit audit = createAuditObject(sp,
                                        key, JobStatus.COLLECTION_SUCCESSFUL.toString(), "");
                                kafkaProducerService.writeMessage(collectorUtil.buildAuditQueueJSON(audit),
                                        "", auditTopic);
                            }
                        } catch (Exception e) {
                            CollectionAudit audit = createAuditObject(sp,
                                    filePath, JobStatus.COLLECTION_FAILED.toString(), e.getMessage());
                            kafkaProducerService.writeMessage(collectorUtil.buildAuditQueueJSON(audit), "",
                                    auditTopic);
                            LOGGER.error("Error on retrieveData {}", e);
                        }
                    }
                } else {
                    LOGGER.info("DIR found : " + file.getFilename());
                }
            }
        } finally {
            if (channelSftp != null) {
                channelSftp.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
        return true;
    }

    private CollectionAudit createAuditObject(SpaceCollector sp,
                                              String auditInputFilePath,
                                              String auditJobStatus,
                                              String auditExceptions) {
        CollectionAudit audit = getCollectionAudit(sp);
        audit.setInputFilePath(auditInputFilePath);
        audit.setJobStatus(auditJobStatus);
        audit.setExceptions(auditExceptions);
        return audit;
    }

    public void retrieveAttributesOfRemoteFile(ChannelSftp channelSftp,
                                               ChannelSftp.LsEntry file,
                                               String remoteDirectory,
                                               String inputFilePath)
            throws Exception {
        String filePath = file.getFilename(); // Use relative file path to fetch the actual file because we have already moved inside the remote directory
        try {
            channelSftp.stat(filePath);
            LOGGER.info("in filePath : " + filePath + " exists");
        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                LOGGER.error("Remote file does not exist: " + inputFilePath);
            }
            throw e;
        }
    }

    private Map<String, String> getAuditStatusMap(Vector<ChannelSftp.LsEntry> files, String remoteDirectory) {
        List<String> filePaths = files.stream()
                .filter(file -> !file.getAttrs().isDir())
                .map(file -> remoteDirectory + file.getFilename()) // Use absolute file path
                .collect(Collectors.toList());

        // Get latest status for each file path
        List<CollectionAudit> audits = lucenceColletionAuditRepo.findLatestByFilePaths(filePaths);

        // Create a map for quick lookup
        Map<String, String> auditStatusMap = audits.stream().filter(audit -> audit
                != null).collect(Collectors.toMap(audit -> audit.getInputFilePath(), audit ->
                audit.getJobStatus()));
        return auditStatusMap;
    }

    private static Session getSession(String sshUsername, String sshHost, String sshPassword, int port, JSch jsch)
            throws JSchException {
        Session session = jsch.getSession(sshUsername, sshHost, port);
        session.setConfig("StrictHostKeyChecking", "no");
        if (!sshPassword.isEmpty()) {
            session.setPassword(sshPassword);
        }
        return session;
    }

    public JSch getJsch(String sshHost, String sshPassword) throws JSchException {
        jschFactory.createJSch();
        if (sshPassword.equals("")) {
            jsch.addIdentity("/prod/eclapp/lib/id_rsa_decoded");
        }
        //sk changes
        if (sshHost.equals("localhost")) {
            String knownHostsFile = System.getProperty("user.home") + SSH_KNOWN_HOSTS;
            jsch.setKnownHosts(knownHostsFile);
        }
        return jsch;
    }

    private CollectionAudit getCollectionAudit(SpaceCollector sp) {
        CollectionAudit audit = new CollectionAudit();
        audit.setId(UUID.randomUUID().toString());
        audit.setDateProcessed(collectorUtil.localDateToString(LocalDateTime.now()));
        audit.setFileType(sp.getFileType());
        audit.setDelimiters(sp.getDelimiter());
        audit.setUrl(sp.getUrl());
        audit.setPort(Integer.valueOf(sp.getPort().toString()));
        audit.setCollectorId(sp.getId());
        audit.setProcessType(ProcessType.Collector);
        return audit;
    }

    private ChannelSftp getChannelSftp(Session session, String remoteDirectory) throws JSchException, SftpException {
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();
        // Navigate to the remote directory
        channelSftp.cd(remoteDirectory);

        return channelSftp;
    }
}






package org.vdsi.space.collections.customsshcollector.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

    public static List<String> readFilesInDirectory(String directoryPath) throws NoSuchFieldException {
        try {
            List<String> fileList = new ArrayList<>();
            Files.walkFileTree(Paths.get(directoryPath), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (Files.isRegularFile(file)) {
                        fileList.add(file.toString());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            return fileList;
        } catch (IOException e) {
            throw new NoSuchFieldException(directoryPath);
        }
    }

    public static void transferFile(InputStream inputStream, OutputStream outputStream, String outputFile)
            throws IOException, IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.close();
        inputStream.close();
    }
    public static String getDirectory(String sp) {
        String remoteDirectory = sp;
        if (!remoteDirectory.endsWith("/")) {
            remoteDirectory += "/";
        }
        return remoteDirectory;
    }

    public static void downloadFile(OutputStream outputStream, InputStream inputStream,
                                    String filePath, String outputFile, String outputFilePath) throws IOException {
        File localFile = new File(outputFilePath);
        if (!localFile.exists()) {
            LOGGER.error("Local file does not exist: " + outputFilePath);
            localFile.getParentFile().mkdirs();
            localFile.createNewFile();
        } else {
            LOGGER.info("Output file path: " + outputFilePath + " exists");
        }

        transferFile(inputStream, outputStream, outputFile);
    }

    public static String getFileName(String filePath) {
        Path path = Paths.get(filePath);
        return path.getFileName().toString();
    }

    public static String stripFileExt(String fileName) {
        return fileName.replaceFirst("[.][^.]+$", "");
    }

    public static String stripFileExtMultipleTimes(String fileName) {
        while (fileName.contains(".")) {
            fileName = fileName.substring(0, fileName.lastIndexOf('.'));
        }
        return fileName;
    }

    public static String getFileNameWithoutExtn(String filePath) {
        String fileName = FileUtil.getFileName(filePath);
        LOGGER.info("File name: " + fileName);
        String fileNameWithoutExtn = FileUtil.stripFileExtMultipleTimes(fileName);
        LOGGER.info("File name without extension: " + fileNameWithoutExtn);
        return fileNameWithoutExtn;
    }

    public static void createDirectoryIfNotExists(String directoryPath) {
        Path path = Paths.get(directoryPath);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                LOGGER.error("Error creating directory: " + directoryPath, e);
            }
        }
    }
}







package org.vdsi.space.collections.customsshcollector.util;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;


public class ZipUtil {

    public static boolean isZipFile(String filePath) {
        String extn = ZipUtil.getFileExtension(filePath);

        String[] array = {"zip", "tar", "tar.gz", "gz"};
        List<String> list = Arrays.asList(array);

        return list.contains(extn);
    }

    public static void extract(String filePath, String destDir) throws IOException, NoSuchFieldException {
        String fileExtension = getFileExtension(filePath);
        if ("tar".equalsIgnoreCase(fileExtension)) {
            untar(filePath, destDir);
        } else if ("tar.gz".equalsIgnoreCase(fileExtension)) {
            unGzipAndUntar(filePath, destDir);
        } else if ("gz".equalsIgnoreCase(fileExtension)) {
            unzipGZ(filePath, destDir);
        } else {
            System.err.println("Unsupported file type: " + fileExtension);
            throw new IOException("Unsupported file type: " + fileExtension);
        }

        // Recursively extract nested archives
        List<String> files = FileUtil.readFilesInDirectory(destDir);
        for (String file : files) {
            if (isZipFile(file)) {
                extract(file, destDir);
            }
        }
    }

    private static String getFileExtension(String filePath) {
        if (filePath.endsWith(".tar.gz")) {
            return "tar.gz";
        } else if (filePath.endsWith(".gz")) {
            return "gz";
        } else {
            return filePath.substring(filePath.lastIndexOf('.') + 1);
        }
    }

    public static void untar(String tarFilePath, String destDir) throws IOException {
        try (TarArchiveInputStream tarInput = new TarArchiveInputStream(new FileInputStream(tarFilePath))) {
            TarArchiveEntry entry;
            while ((entry = tarInput.getNextTarEntry()) != null) {
                File destPath = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    destPath.mkdirs();
                } else {
                    destPath.getParentFile().mkdirs();
                    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destPath))) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = tarInput.read(buffer)) != -1) {
                            bos.write(buffer, 0, len);
                        }
                    }
                }
            }
        }
    }

    public static void unGzipAndUntar(String tarGzFilePath, String destDir) throws IOException {
        try (TarArchiveInputStream tarInput = new TarArchiveInputStream(
                new GzipCompressorInputStream(new FileInputStream(tarGzFilePath)))) {
            TarArchiveEntry entry;
            while ((entry = tarInput.getNextTarEntry()) != null) {
                File destPath = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    destPath.mkdirs();
                } else {
                    destPath.getParentFile().mkdirs();
                    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destPath))) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = tarInput.read(buffer)) != -1) {
                            bos.write(buffer, 0, len);
                        }
                    }
                }
            }
        }
    }

    public static void unzipGZ(String gzFilePath, String outputDir) throws IOException {
        File file = new File(gzFilePath);
        try (GZIPInputStream in = new GZIPInputStream(new FileInputStream(file))) {
            File target = new File(outputDir, FileUtil.stripFileExt(file.getName()));
            try (OutputStream os = new FileOutputStream(target)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    os.write(buf, 0, len);
                }
            }
        }
    }
}



As a developer,
I want to enable folder traversal in the custom-ssh-collector,
So that when our collector connects to a host and tries to find files inside it,
we can find the file from the nested directory structure and upload the individual files to S3.

 

folder1 -> S-folder-1
                    -> file1
                    -> file2
                    -> file3
                    -> file4.gz
        -> S-folder-2
                    -> file1
                    -> file2
                    -> file3
                    -> file4.gz
        -> S-folder-3
                    -> file1
                    -> file2
                    -> file3
                    -> file4.gz
        -> S-folder-4
                    -> file1
                    -> file2
                    -> file3
                    -> file4.gz
                    ->file5.tar.gz 

Enable Folder Traversal in custom-ssh-collector
