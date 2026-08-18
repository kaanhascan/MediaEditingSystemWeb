package com.veyzo.media_editing_system.Controller;

import com.veyzo.media_editing_system.Model.Video;
import com.veyzo.media_editing_system.Model.VideoStatus;

import com.veyzo.media_editing_system.Repository.VideoRepository;
import com.veyzo.media_editing_system.Service.VideoService;
import com.veyzo.media_editing_system.dto.response.VideoListResponse;
import com.veyzo.media_editing_system.dto.response.VideoStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;
    private final VideoRepository videoRepository;

    @PostMapping(value = "/upload")
    public ResponseEntity<?> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("startTime") String startTime,
            @RequestParam("duration") String duration,
            @RequestParam(value = "batchId", required = false) String batchId
    ) {
        try {
            videoService.uploadVideo(file, title, startTime, duration, batchId);
            return ResponseEntity.ok("Video başarıyla yüklendi");

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Dosya yüklenirken bir hata oluştu: " + e.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Beklenmeyen bir hata oluştu: " + e.getMessage());
        }
    }

    @PostMapping(value = "/extract-audio")
    public ResponseEntity<?> extractAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "batchId", required = false) String batchId
    ) {
        try {
            videoService.uploadForAudio(file, title, batchId);
            return ResponseEntity.ok("Video başarıyla yüklendi, ses ayırma işlemi başladı");

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Dosya yüklenirken bir hata oluştu: " + e.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Beklenmeyen bir hata oluştu: " + e.getMessage());
        }
    }

    @GetMapping("/{videoId}/status")
    public ResponseEntity<VideoStatusResponse> getVideoStatus(@PathVariable UUID videoId) {
        return ResponseEntity.ok(videoService.getVideoStatus(videoId));
    }

    @GetMapping(value = "/download/batch/{batchId}", produces = "application/zip")
    public ResponseEntity<StreamingResponseBody> downloadBatchAsZip(@PathVariable String batchId) {

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        List<Video> videos = videoService.getVideosForBatch(batchId, userEmail);

        StreamingResponseBody stream = outputStream -> {
            videoService.createZipForVideos(videos, outputStream);
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"veyzo_klasor_" + batchId + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(stream);
    }

    @GetMapping("/download/{videoId}")
    public ResponseEntity<Resource> downloadVideo(@PathVariable UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video bulunamadı"));

        if (video.getStatus() != VideoStatus.COMPLETED) {
            throw new RuntimeException("Video henüz hazır değil.");
        }

        File file = new File(video.getProcessedFilePath());
        Resource resource = new FileSystemResource(file);

        String processedPath = video.getProcessedFilePath();
        String extension = processedPath != null && processedPath.contains(".")
                ? processedPath.substring(processedPath.lastIndexOf("."))
                : ".mp4"; // Fallback

        String downloadName = video.getTitle() + extension;

        MediaType mediaType = extension.equalsIgnoreCase(".mp3")
                ? MediaType.parseMediaType("audio/mpeg")
                : MediaType.parseMediaType("video/mp4");

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                .body(resource);
    }

    @GetMapping("/my-videos")
    public ResponseEntity<List<VideoListResponse>> getMyVideos() {
        return ResponseEntity.ok(videoService.getUserVideos());
    }

    @DeleteMapping("/{videoId}")
    public ResponseEntity<String> deletevideo(@PathVariable UUID videoId) {
        try{
            videoService.deleteVideo(videoId);
            return ResponseEntity.ok("Video başarıyla silindi");
        }
        catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(value = "/merge", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> mergeVideos(
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "existingVideoIds", required = false) List<String> existingVideoIds,
            @RequestParam("title") String title,
            @RequestParam(value = "batchId", required = false) String batchId
    ) {
        try {
            List<UUID> uuidList = null;
            if (existingVideoIds != null && !existingVideoIds.isEmpty()) {
                uuidList = existingVideoIds.stream()
                        .map(UUID::fromString)
                        .toList();
            }

            videoService.mergeVideos(files, uuidList, title, batchId);
            return ResponseEntity.ok("Video birleştirme işlemi başarıyla kuyruğa alındı.");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Birleştirme hatası: " + e.getMessage());
        }
    }
}