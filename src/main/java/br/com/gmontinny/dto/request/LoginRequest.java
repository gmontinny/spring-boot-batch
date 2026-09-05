package br.com.gmontinny.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload de autenticação")
public record LoginRequest(

        @Schema(description = "Nome de usuário", example = "admin")
        @NotBlank(message = "Username é obrigatório")
        String username,

        @Schema(description = "Senha do usuário", example = "Admin@2026")
        @NotBlank(message = "Password é obrigatório")
        String password
) {}
