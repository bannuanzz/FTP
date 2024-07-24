
package org.example.ftp.controller;

import org.example.ftp.service.FtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@RestController
@RequestMapping("/ftp")
public class FtpController {

    @Autowired
    private FtpService ftpService;

    @PostMapping("/upload")
    // MultipartFile is a representation of an uploaded file received in a multipart request
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Save uploaded file to temporary location
            String tempFilePath = System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename();
            file.transferTo(new java.io.File(tempFilePath));

            // Upload file to FTP server
            ftpService.uploadFile(tempFilePath);
            return "Upload successful";
        } catch (IOException e) {
            e.printStackTrace();
            return "Upload failed: " + e.getMessage();
        }
    }

    @GetMapping("/download")
    public String downloadFile(@RequestParam("remoteFilePath") String remoteFilePath,
                               @RequestParam("localFilePath") String localFilePath) {
        try {
            // Download file from FTP server
            ftpService.downloadFile(remoteFilePath, localFilePath);
            return "Download successful";
        } catch (IOException e) {
            e.printStackTrace();
            return "Download failed: " + e.getMessage();
        }
    }
}
