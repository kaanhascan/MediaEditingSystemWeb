package com.veyzo.media_editing_system.Service;

import com.veyzo.media_editing_system.Model.User;
import com.veyzo.media_editing_system.Model.Video;
import com.veyzo.media_editing_system.Model.VideoStatus;
import com.veyzo.media_editing_system.Repository.UserRepository;
import com.veyzo.media_editing_system.Repository.VideoRepository;
import com.veyzo.media_editing_system.dto.response.UploadResponse;
import com.veyzo.media_editing_system.dto.response.VideoListResponse;
import com.veyzo.media_editing_system.dto.response.VideoStatusResponse;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final FfmpegService ffmpegService;


    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/raw/";

    public UploadResponse uploadVideo(MultipartFile file, String title,String startTime,String duration) throws IOException {

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

        ffmpegService.processVideoTrimming(savedVideo.getId(), startTime, duration);


        return new UploadResponse(
                savedVideo.getId(),
                savedVideo.getTitle(),
                savedVideo.getStatus(),
                "Video başarıyla yüklendi ve işleme sırasına alındı."
        );
    }

    public VideoStatusResponse getVideoStatus(UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video bulunamadı"));

        return new VideoStatusResponse(video.getStatus(), video.getProcessedFileName());
    }

    public List<VideoListResponse> getUserVideos() {

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));


        return videoRepository.findByUser_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(video -> new VideoListResponse(
                        video.getId(),
                        video.getTitle(),
                        video.getStatus(),
                        video.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
}