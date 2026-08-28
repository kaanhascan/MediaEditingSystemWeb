package com.veyzo.media_editing_system.Controller;

import com.veyzo.media_editing_system.Service.UserService;
import com.veyzo.media_editing_system.dto.request.LoginRequest;
import com.veyzo.media_editing_system.dto.request.RegisterRequest;
import com.veyzo.media_editing_system.dto.response.AuthResponse;
import com.veyzo.media_editing_system.dto.response.UserProfileDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest req){
        userService.register(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest req, HttpServletResponse response) {

        AuthResponse authResponse = userService.login(req);
        String token = authResponse.token();

        Cookie jwtCookie = new Cookie("jwt_token", token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(24 * 60 * 60); // 1 gün

        response.addCookie(jwtCookie);

        return ResponseEntity.ok("Giriş başarılı");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        Cookie jwtCookie = new Cookie("jwt_token", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);

        response.addCookie(jwtCookie);
        return ResponseEntity.ok("Çıkış yapıldı");
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getMyProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        UserProfileDto profileDto = userService.getMyProfile(email);
        return ResponseEntity.ok(profileDto);
    }
}