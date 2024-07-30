package org.example.ftp.controller;

import org.example.ftp.service.FtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.io.File;

@RestController
@RequestMapping("/ftp")
public class FtpController {

    @Autowired
    private FtpService ftpService;

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                             @RequestParam(value = "remoteDir", required = false) String remoteDir) {

        String tempDir = System.getProperty("java.io.tmpdir"); // 使用系统临时目录
        File dir = new File(tempDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String tempFilePath = tempDir + File.separator + "upload_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(tempFilePath);

        try {
            // 检查目录是否可写
            if (!Files.isWritable(filePath.getParent())) {
                throw new AccessDeniedException("No write access to directory: " + tempDir);
            }

            // 保存临时文件并获取文件锁
            try (FileOutputStream fos = new FileOutputStream(tempFilePath);
                 FileChannel channel = fos.getChannel()) {
                FileLock lock = channel.tryLock();
                if (lock != null) {
                    fos.write(file.getBytes());
                    lock.release();
                } else {
                    throw new IOException("Unable to lock file: " + tempFilePath);
                }
            }

            System.out.println("Directory writable: " + Files.isWritable(filePath.getParent()));
            System.out.println("File path writable: " + Files.isWritable(filePath));

            // 上传文件到FTP服务器
            ftpService.uploadFile(tempFilePath, remoteDir);
            return "Upload successful";
        } catch (AccessDeniedException e) {
            e.printStackTrace();
            return "Upload failed: Access denied - " + e.getMessage();
        } catch (IOException e) {
            e.printStackTrace();
            return "Upload failed: " + e.getMessage();
        }
    }

    @GetMapping("/download")
    public String downloadFile(@RequestParam("remoteFilePath") String remoteFilePath,
                               @RequestParam("localFilePath") String localFilePath) {
        try {
            ftpService.downloadFile(remoteFilePath, localFilePath);
            return "Download successful";
        } catch (IOException e) {
            e.printStackTrace();
            return "Download failed: " + e.getMessage();
        }
    }
}
