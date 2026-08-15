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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final FfmpegService ffmpegService;


    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/raw/";

    public UploadResponse uploadVideo(MultipartFile file, String title,String startTime,String duration,String batchId) throws IOException {

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
                .batchId(batchId)
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
                        video.getCreatedAt(),
                        video.getBatchId()
                ))
                .collect(Collectors.toList());
    }
    @Transactional
    public void deleteVideo(UUID videoId) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video Bulunamadı"));

        String currentUserIdStr = currentUser.getId().toString();
        String videoOwnerIdStr = video.getUser().getId().toString();

        System.out.println("Giriş Yapan (String): " + currentUserIdStr);
        System.out.println("Videonun Sahibi (String): " + videoOwnerIdStr);

        if(!videoOwnerIdStr.equals(currentUserIdStr)) {
            throw new RuntimeException("Bu video başka bir kullancıya ait!");
        }

        try{
            if(video.getOriginalFilePath() != null){
                Files.deleteIfExists(Paths.get(video.getOriginalFilePath()));
            }
            if(video.getProcessedFilePath() != null){
                Files.deleteIfExists(Paths.get(video.getProcessedFilePath()));
            }
        }
        catch(Exception e){
            System.err.println("Dosya fiziksel olarak silinirken bir hata ile karşılaşıldı." + e.getMessage());
        }
        videoRepository.delete(video);
    }

    public List<Video> getVideosForBatch(String batchId, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        return videoRepository.findByBatchIdAndUser(batchId, currentUser);
    }
    public void createZipForVideos(List<Video> videos, OutputStream outputStream) {
        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            boolean hasFiles = false;

            for (Video video : videos) {
                String statusStr = video.getStatus() != null ? String.valueOf(video.getStatus()) : "";

                System.out.println("--- VİDEO İŞLENİYOR: " + video.getTitle() + " ---");

                if (!"COMPLETED".equalsIgnoreCase(statusStr) || video.getProcessedFilePath() == null) {
                    System.out.println("-> Atlandı: Statü uygun değil.");
                    continue;
                }

                Path filePath = Paths.get(video.getProcessedFilePath());

                if (Files.exists(filePath)) {
                    System.out.println("-> ZIP'e ekleniyor: " + video.getTitle());
                    ZipEntry zipEntry = new ZipEntry(video.getTitle() + ".mp4");
                    zos.putNextEntry(zipEntry);
                    Files.copy(filePath, zos);
                    zos.closeEntry();
                    hasFiles = true;
                } else {
                    System.out.println("-> Dosya fiziksel olarak bulunamadı: " + filePath.toAbsolutePath());
                }
            }

            if (!hasFiles) {
                ZipEntry errorEntry = new ZipEntry("bilgi.txt");
                zos.putNextEntry(errorEntry);
                zos.write("Bu klasördeki videolar henuz islenmemis, basarisiz olmus veya silinmis olabilir.".getBytes());
                zos.closeEntry();
            }

        } catch (Exception e) {
            System.err.println("ZIP oluşturulurken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }
}