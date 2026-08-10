package com.veyzo.media_editing_system.Service;

import com.veyzo.media_editing_system.Model.Video;
import com.veyzo.media_editing_system.Model.VideoStatus;
import com.veyzo.media_editing_system.Repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FfmpegService {

    private final VideoRepository videoRepository;
    private final String PROCESSED_DIR = System.getProperty("user.dir") + "/uploads/processed/";

    @Async
    public void processVideoTrimming(UUID videoId, String startTime, String duration) {


        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video bulunamadı"));

        try {
            video.setStatus(VideoStatus.PROCESSING);
            videoRepository.save(video);

            File directory = new File(PROCESSED_DIR);
            if (!directory.exists()) directory.mkdirs();

            String outputFileName = "trimmed_" + video.getOriginalFileName();
            String outputFilePath = PROCESSED_DIR + outputFileName;


            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffmpeg", "-y",
                    "-i", video.getOriginalFilePath(),
                    "-ss", startTime,
                    "-t", duration,
                    "-c", "copy",
                    outputFilePath
            );


            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();


            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[FFMPEG]: " + line);
            }


            int exitCode = process.waitFor();

            if (exitCode == 0) {
                video.setStatus(VideoStatus.COMPLETED);
                video.setProcessedFileName(outputFileName);
                video.setProcessedFilePath(outputFilePath);
            } else {
                video.setStatus(VideoStatus.FAILED);
            }

        } catch (Exception e) {
            e.printStackTrace();
            video.setStatus(VideoStatus.FAILED);
        } finally {
            videoRepository.save(video);
        }
    }
}
