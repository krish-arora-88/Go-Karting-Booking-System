package com.gokarting.domain.port.in;

import com.gokarting.domain.model.User;

public interface RegisterUserUseCase {

    record RegisterCommand(String username, String email, String rawPassword) {}

    User register(RegisterCommand command);
}
