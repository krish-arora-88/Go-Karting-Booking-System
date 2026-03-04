package com.gokarting.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(min = 3, max = 50)
        @Schema(example = "racer42")
        String username,

        @NotBlank @Size(min = 6, max = 100)
        @Schema(example = "securepass123")
        String password
) {}
