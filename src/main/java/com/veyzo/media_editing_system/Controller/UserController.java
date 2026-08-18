package com.veyzo.media_editing_system.Controller;



import com.veyzo.media_editing_system.Service.UserService;
import com.veyzo.media_editing_system.dto.request.LoginRequest;
import com.veyzo.media_editing_system.dto.request.RegisterRequest;
import com.veyzo.media_editing_system.dto.response.AuthResponse;
import com.veyzo.media_editing_system.dto.response.UserProfileDto;
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
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(userService.login(req));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getMyProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        UserProfileDto profileDto = userService.getMyProfile(email);

        return ResponseEntity.ok(profileDto);
    }


}
