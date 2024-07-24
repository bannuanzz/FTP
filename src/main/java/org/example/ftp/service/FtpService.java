
package org.example.ftp.service;

import org.example.ftp.config.FtpProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// @Service annotation is used with classes that provide some business functionalities.
@Service
public class FtpService {

    private static final int BUFFER_SIZE = 1024;

    @Autowired
    private FtpProperties ftpProperties;

    // Read response from FTP server
    private String readResponse(BufferedReader reader) throws IOException {
        StringBuilder response = new StringBuilder();
        String line;
        // Read until the response code is received
        while ((line = reader.readLine()) != null) {
            response.append(line).append("\n");
            if (line.matches("\\d{3} .*")) {
                break;
            }
        }
        return response.toString();
    }

    private int extractPassivePort(String response) {
        Pattern pattern = Pattern.compile("\\((\\d+,\\d+,\\d+,\\d+),(\\d+),(\\d+)\\)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            int port1 = Integer.parseInt(matcher.group(2));
            int port2 = Integer.parseInt(matcher.group(3));
            return port1 * 256 + port2;
        }
        return -1;
    }

    public void uploadFile(String filePath) throws IOException {
        File file = new File(filePath);
        long localFileSize = file.length();

        // Connect to FTP server
        try (Socket cmdSocket = new Socket(ftpProperties.getServer(), ftpProperties.getPort());
             PrintWriter writer = new PrintWriter(cmdSocket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(cmdSocket.getInputStream()))) {

            // Read initial response
            System.out.println(readResponse(reader));

            // Login to FTP server
            writer.println("USER " + ftpProperties.getUser());
            System.out.println(readResponse(reader));
            writer.println("PASS " + ftpProperties.getPassword());
            System.out.println(readResponse(reader));

            // Switch to binary mode
            writer.println("TYPE I");
            System.out.println(readResponse(reader));

            // Enter passive mode
            writer.println("PASV");
            String response = readResponse(reader);
            System.out.println(response);

            // Extract data port from response
            int dataPort = extractPassivePort(response);

            // Get remote file size
            writer.println("SIZE " + file.getName());
            response = readResponse(reader);
            long remoteFileSize = 0;
            if (response.startsWith("213 ")) {
                remoteFileSize = Long.parseLong(response.substring(4).trim());
            }

            // Connect to data port
            try (Socket dataSocket = new Socket(ftpProperties.getServer(), dataPort);
                 OutputStream dataOut = dataSocket.getOutputStream();
                 InputStream fileIn = new FileInputStream(file)) {

                if (remoteFileSize > 0 && remoteFileSize < localFileSize) {
                    // Restart upload from remoteFileSize
                    writer.println("REST " + remoteFileSize);
                    System.out.println(readResponse(reader));
                    fileIn.skip(remoteFileSize);
                } else {
                    remoteFileSize = 0;
                }

                // Start file upload
                writer.println("STOR " + file.getName());
                System.out.println(readResponse(reader));

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    dataOut.write(buffer, 0, bytesRead);
                }
                System.out.println(readResponse(reader));
            }

            writer.println("QUIT");
            System.out.println(readResponse(reader));
        }
    }

    public void downloadFile(String remoteFilePath, String localFilePath) throws IOException {
        File localFile = new File(localFilePath);
        long localFileSize = 0;
        if (localFile.exists()) {
            localFileSize = localFile.length();
        }

        // Connect to FTP server
        try (Socket cmdSocket = new Socket(ftpProperties.getServer(), ftpProperties.getPort());
             PrintWriter writer = new PrintWriter(cmdSocket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(cmdSocket.getInputStream()))) {

            // Read initial response
            System.out.println(readResponse(reader));

            // Login to FTP server
            writer.println("USER " + ftpProperties.getUser());
            System.out.println(readResponse(reader));
            writer.println("PASS " + ftpProperties.getPassword());
            System.out.println(readResponse(reader));

            // Switch to binary mode
            writer.println("TYPE I");
            System.out.println(readResponse(reader));

            // Enter passive mode
            writer.println("PASV");
            String response = readResponse(reader);
            System.out.println(response);

            // Extract data port from response
            int dataPort = extractPassivePort(response);

            // Connect to data port
            try (Socket dataSocket = new Socket(ftpProperties.getServer(), dataPort);
                 InputStream dataIn = dataSocket.getInputStream();
                 OutputStream fileOut = new FileOutputStream(localFile, true)) {

                if (localFileSize > 0) {
                    // Restart download from localFileSize
                    writer.println("REST " + localFileSize);
                    System.out.println(readResponse(reader));
                }

                // Start file download
                writer.println("RETR " + remoteFilePath);
                System.out.println(readResponse(reader));

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = dataIn.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                }
                System.out.println(readResponse(reader));
            }

            writer.println("QUIT");
            System.out.println(readResponse(reader));
        }
    }
}
