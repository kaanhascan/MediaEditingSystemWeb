package com.veyzo.media_editing_system.Controller;

import com.veyzo.media_editing_system.Service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title
    ) {
        try {
            return ResponseEntity.ok(videoService.uploadVideo(file, title));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}