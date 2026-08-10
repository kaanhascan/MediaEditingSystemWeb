package com.veyzo.media_editing_system.Service;

import com.veyzo.media_editing_system.Model.User;
import com.veyzo.media_editing_system.Model.Video;
import com.veyzo.media_editing_system.Model.VideoStatus;
import com.veyzo.media_editing_system.Repository.UserRepository;
import com.veyzo.media_editing_system.Repository.VideoRepository;
import com.veyzo.media_editing_system.dto.response.UploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final FfmpegService ffmpegService;


    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/raw/";

    public UploadResponse uploadVideo(MultipartFile file, String title) throws IOException {

        File directory = new File(UPLOAD_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }


        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        Path filePath = Paths.get(UPLOAD_DIR, uniqueFileName);


        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);


        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));


        Video video = Video.builder()
                .title(title)
                .originalFileName(uniqueFileName)
                .originalFilePath(filePath.toString())
                .status(VideoStatus.PENDING)
                .user(currentUser)
                .build();

        Video savedVideo = videoRepository.save(video);

        ffmpegService.processVideoTrimming(savedVideo.getId(), "00:00:05", "10");


        return new UploadResponse(
                savedVideo.getId(),
                savedVideo.getTitle(),
                savedVideo.getStatus(),
                "Video başarıyla yüklendi ve işleme sırasına alındı."
        );
    }
}