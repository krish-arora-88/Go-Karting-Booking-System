package com.gokarting.application;

import com.gokarting.domain.exception.UserAlreadyExistsException;
import com.gokarting.domain.model.User;
import com.gokarting.domain.model.UserRole;
import com.gokarting.domain.port.in.RegisterUserUseCase;
import com.gokarting.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegisterUserService implements RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @Override
    public User register(RegisterCommand command) {
        if (userRepository.existsByUsername(command.username())) {
            throw new UserAlreadyExistsException(command.username());
        }

        var user = new User(
                UUID.randomUUID(),
                command.username(),
                command.email(),
                passwordEncoder.encode(command.rawPassword()),
                UserRole.USER,
                Instant.now()
        );

        User saved = userRepository.save(user);
        log.info("New user registered: username={}", saved.getUsername());
        return saved;
    }
}
