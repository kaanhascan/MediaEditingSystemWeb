package com.veyzo.media_editing_system.Repository;

import com.veyzo.media_editing_system.Model.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VideoRepository extends JpaRepository<Video, UUID> {

    List<Video> findByUser_IdOrderByCreatedAtDesc(UUID userId);
}
