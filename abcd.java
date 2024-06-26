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

            // Recursively retrieve and process files and directories
            retrieveAndProcessDirectory(sp, dateReceived, auditTopic, remoteDirectory, localDirectory, channelSftp);
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

    private void retrieveAndProcessDirectory(SpaceCollector sp, String dateReceived, String auditTopic,
                                             String remoteDirectory, String localDirectory, ChannelSftp channelSftp) throws Exception {
        Vector<ChannelSftp.LsEntry> files = channelSftp.ls(remoteDirectory);

        Map<String, String> auditStatusMap = getAuditStatusMap(files, remoteDirectory);

        for (ChannelSftp.LsEntry file : files) {
            if (!file.getAttrs().isDir()) {
                String filePath = remoteDirectory + "/" + file.getFilename(); // Use absolute file path

                String status = auditStatusMap.get(filePath);
                if (status == null || "COLLECTION_FAILED".equals(status)) {
                    try {
                        String outputFile = localDirectory + "/" + file.getFilename();
                        LOGGER.info("in filePath : " + filePath + " out filePath : " + outputFile);

                        FileUtil.downloadFile(outputStreamCreator.create(outputFile), channelSftp.get(filePath),
                                filePath, outputFile, sp.getOutputFilePath());

                        processDownloadedFile(sp, dateReceived, auditTopic, outputFile, localDirectory);
                    } catch (Exception e) {
                        CollectionAudit audit = createAuditObject(sp,
                                filePath, JobStatus.COLLECTION_FAILED.toString(), e.getMessage());
                        kafkaProducerService.writeMessage(collectorUtil.buildAuditQueueJSON(audit), "",
                                auditTopic);
                        LOGGER.error("Error on retrieveData {}", e);
                    }
                }
            } else {
                if (!".".equals(file.getFilename()) && !"..".equals(file.getFilename())) {
                    LOGGER.info("DIR found : " + file.getFilename());
                    String subRemoteDirectory = remoteDirectory + "/" + file.getFilename();
                    String subLocalDirectory = localDirectory + "/" + file.getFilename();
                    FileUtil.createDirectoryIfNotExists(subLocalDirectory);
                    retrieveAndProcessDirectory(sp, dateReceived, auditTopic, subRemoteDirectory, subLocalDirectory, channelSftp);
                }
            }
        }
    }

    private void processDownloadedFile(SpaceCollector sp, String dateReceived, String auditTopic,
                                       String outputFile, String localDirectory) throws Exception {
        List<String> filesList = new ArrayList<>();
        if (ZipUtil.isZipFile(outputFile)) {
            String fileNameWithoutExtn = FileUtil.getFileNameWithoutExtn(outputFile);
            FileUtil.createDirectoryIfNotExists(localDirectory + "/" + fileNameWithoutExtn);

            ZipUtil.extract(outputFile, localDirectory + "/" + fileNameWithoutExtn);
            filesList.addAll(FileUtil.readFilesInDirectory(localDirectory + "/" + fileNameWithoutExtn));
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
                    sp.getUrl(),
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

            // Check if the file is an archive and process it further
            if (ZipUtil.isZipFile(fileName)) {
                String nestedFileNameWithoutExtn = FileUtil.getFileNameWithoutExtn(fileName);
                FileUtil.createDirectoryIfNotExists(localDirectory + "/" + nestedFileNameWithoutExtn);

                ZipUtil.extract(fileName, localDirectory + "/" + nestedFileNameWithoutExtn);
                List<String> nestedFilesList = FileUtil.readFilesInDirectory(localDirectory + "/" + nestedFileNameWithoutExtn);

                for (String nestedFile : nestedFilesList) {
                    processDownloadedFile(sp, dateReceived, auditTopic, nestedFile, localDirectory);
                }
            }
        }
    }
