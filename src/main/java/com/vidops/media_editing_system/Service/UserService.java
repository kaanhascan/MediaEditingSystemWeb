package com.vidops.media_editing_system.Service;

import com.vidops.media_editing_system.Model.User;
import com.vidops.media_editing_system.Repository.UserRepository;
import com.vidops.media_editing_system.dto.request.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    public User register(RegisterRequest req){
        if(userRepository.existsByEmail(req.email()))
            throw new RuntimeException("Bu e-posta zaten kayıtlı.");
        if(userRepository.existsByUsername(req.username()))
            throw new RuntimeException("Bu kullanıcı adı zaten kayıtlı.");

        return userRepository.save(
                User.builder()
                        .username(req.username())
                        .email(req.email())
                        .password(passwordEncoder.encode(req.password()))
                        .build()
        );
    }
}
