package br.com.gmontinny.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload para renovação do access token")
public record RefreshTokenRequest(

        @Schema(description = "Refresh token obtido no login", example = "eyJhbGci...")
        @NotBlank(message = "Refresh token é obrigatório")
        String refreshToken
) {}
