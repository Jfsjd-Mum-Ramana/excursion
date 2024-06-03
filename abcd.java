import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.mockito.Mockito;
import com.jcraft.jsch.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class SSHServiceSteps {

    @Autowired
    SSHService sshService;

    @Autowired
    S3Service s3Service;

    private SpaceCollector spaceCollector;
    private ChannelSftp channelSftp;
    private Session session;
    private Vector<ChannelSftp.LsEntry> remoteFiles;
    private Map<String, ByteArrayInputStream> remoteFileStreams;

    @Given("a SpaceCollector with SSH connection details")
    public void givenSpaceCollectorWithSSHDetails() throws JSchException {
        spaceCollector = new SpaceCollector();
        spaceCollector.setUserName("testUser");
        spaceCollector.setUrl("testHost");
        spaceCollector.setPassword("testPassword");
        spaceCollector.setPort(22);
        spaceCollector.setInputFilePath("/remote/dir1");
        spaceCollector.setOutputFilePath("/local/dir1");

        // Mocking JSch and Session
        JSch jsch = Mockito.mock(JSch.class);
        session = Mockito.mock(Session.class);
        channelSftp = Mockito.mock(ChannelSftp.class);

        sshService = new SSHService(null, new JSchFactory() {
            @Override
            public JSch createJSch() {
                return jsch;
            }
        }, new OutputStreamCreator() {
            @Override
            public OutputStream create(String path) throws IOException {
                return new FileOutputStream(path);
            }
        });

        Mockito.when(jsch.getSession(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt())).thenReturn(session);
        Mockito.when(session.openChannel("sftp")).thenReturn(channelSftp);
        Mockito.when(session.isConnected()).thenReturn(true);
        Mockito.when(channelSftp.getSession()).thenReturn(session);

        remoteFiles = new Vector<>();
        remoteFileStreams = new HashMap<>();
    }

    @Given("a remote directory structure with nested directories, files, and ZIP files")
    public void givenRemoteDirectoryStructureWithNestedDirectoriesAndFiles(Map<String, String> directories) throws SftpException {
        for (Map.Entry<String, String> entry : directories.entrySet()) {
            String remoteDir = entry.getKey();
            String localDir = entry.getValue();

            // Mock directory entries
            ChannelSftp.LsEntry dirEntry = Mockito.mock(ChannelSftp.LsEntry.class);
            SftpATTRS attrs = Mockito.mock(SftpATTRS.class);
            Mockito.when(attrs.isDir()).thenReturn(true);
            Mockito.when(dirEntry.getAttrs()).thenReturn(attrs);
            Mockito.when(dirEntry.getFilename()).thenReturn(remoteDir.substring(remoteDir.lastIndexOf("/") + 1));
            remoteFiles.add(dirEntry);
        }
        Mockito.when(channelSftp.ls(Mockito.anyString())).thenAnswer(new Answer<Vector<ChannelSftp.LsEntry>>() {
            @Override
            public Vector<ChannelSftp.LsEntry> answer(InvocationOnMock invocation) throws Throwable {
                String path = (String) invocation.getArguments()[0];
                Vector<ChannelSftp.LsEntry> filesInDir = new Vector<>();
                for (ChannelSftp.LsEntry entry : remoteFiles) {
                    if (path.endsWith(entry.getFilename())) {
                        filesInDir.add(entry);
                    }
                }
                return filesInDir;
            }
        });
    }

    @Given("files and ZIP files in the remote directories")
    public void givenFilesAndZIPFilesInTheRemoteDirectories(List<String> files) throws SftpException, IOException {
        for (String file : files) {
            // Mock file entries
            ChannelSftp.LsEntry fileEntry = Mockito.mock(ChannelSftp.LsEntry.class);
            SftpATTRS attrs = Mockito.mock(SftpATTRS.class);
            Mockito.when(attrs.isDir()).thenReturn(false);
            Mockito.when(fileEntry.getAttrs()).thenReturn(attrs);
            Mockito.when(fileEntry.getFilename()).thenReturn(file.substring(file.lastIndexOf("/") + 1));
            remoteFiles.add(fileEntry);

            // Mock file content
            ByteArrayInputStream fileStream = new ByteArrayInputStream(("Content of " + file).getBytes());
            remoteFileStreams.put(file, fileStream);
        }
        Mockito.when(channelSftp.get(Mockito.anyString())).thenAnswer(new Answer<InputStream>() {
            @Override
            public InputStream answer(InvocationOnMock invocation) throws Throwable {
                String path = (String) invocation.getArguments()[0];
                return remoteFileStreams.get(path);
            }
        });
    }

    @When("the retrieveData method is called")
    public void whenRetrieveDataMethodIsCalled() throws Exception {
        Mockito.doNothing().when(channelSftp).connect();
        Mockito.doNothing().when(channelSftp).cd(Mockito.anyString());

        sshService.retrieveData(spaceCollector, "2024-05-30", "auditTopic");
    }

    @Then("all files from the remote directories should be downloaded to the corresponding local directories")
    public void thenAllFilesFromRemoteDirectoriesShouldBeDownloaded() {
        File localDir = new File(spaceCollector.getOutputFilePath());
        assertTrue(localDir.exists());
        // Verify downloaded files
        for (String remoteFile : remoteFileStreams.keySet()) {
            String localFilePath = spaceCollector.getOutputFilePath() + remoteFile.substring(remoteFile.lastIndexOf("/"));
            File localFile = new File(localFilePath);
            assertTrue(localFile.exists());
        }
    }

    @Then("all ZIP files should be extracted to the corresponding local directories")
    public void thenAllZIPFilesShouldBeExtracted() {
        File localDir = new File(spaceCollector.getOutputFilePath());
        assertTrue(localDir.exists());
        // Verify extracted files
        File[] files = localDir.listFiles();
        assertNotNull(files);
        for (File file : files) {
            if (ZipUtil.isZipFile(file.getName())) {
                // Assuming ZipUtil.extract works correctly and extracts files
                String extractedDirPath = spaceCollector.getOutputFilePath() + "/" + FileUtil.getFileNameWithoutExtn(file.getName());
                File extractedDir = new File(extractedDirPath);
                assertTrue(extractedDir.exists());
                assertTrue(extractedDir.isDirectory());
            }
        }
    }

    @Then("the files should be pushed to S3 with the correct keys")
    public void thenFilesShouldBePushedToS3WithCorrectKeys() {
        // Verify files pushed to S3 (mock S3 service calls)
        for (String remoteFile : remoteFileStreams.keySet()) {
            String key = generateS3Key(remoteFile);
            Mockito.verify(s3Service, Mockito.times(1)).pushToS3(Mockito.eq(remoteFile), Mockito.eq(key));
        }
    }

    private String generateS3Key(String filePath) {
        // Generate the S3 key based on the file path and other parameters
        String unixBasedPath = filePath.replace("\\", "/");
        return String.format("%s/%s/%s/%s/%s",
                ProfileCheckConfig.activeProfile,
                spaceCollector.getFileType(),
                spaceCollector.getUrl(),
                "2024-05-30",
                unixBasedPath);
    }
}






Feature: Retrieve and Process Nested Directories and ZIP Files

  Scenario: Retrieve files from nested directories and extract ZIP files
    Given a SpaceCollector with SSH connection details
    And a remote directory structure with nested directories, files, and ZIP files
      | remoteDirectory             | localDirectory              |
      | /remote/dir1                | /local/dir1                 |
      | /remote/dir1/subdir1        | /local/dir1/subdir1         |
    And files and ZIP files in the remote directories
      | /remote/dir1/file1.txt      |
      | /remote/dir1/subdir1/file2.zip |
    And files inside the ZIP files
      | /remote/dir1/subdir1/file2.zip/subfile1.txt |
    When the retrieveData method is called
    Then all files from the remote directories should be downloaded to the corresponding local directories
    And all ZIP files should be extracted to the corresponding local directories
    And the files should be pushed to S3 with the correct keys
