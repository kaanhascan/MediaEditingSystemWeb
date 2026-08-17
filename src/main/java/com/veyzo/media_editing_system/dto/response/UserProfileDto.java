package com.veyzo.media_editing_system.dto.response;

import java.time.LocalDateTime;

public record UserProfileDto(
        String username,
        String email,
        String plan,
        LocalDateTime joinDate
) {
}
