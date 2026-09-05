package br.com.gmontinny.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "Payload para criação de usuário")
public record UserRequest(

        @Schema(description = "Nome de usuário único", example = "joao.silva")
        @NotBlank @Size(min = 3, max = 100)
        String username,

        @Schema(description = "E-mail do usuário", example = "joao@email.com")
        @NotBlank @Email
        String email,

        @Schema(description = "Senha (mínimo 8 caracteres)", example = "Senha@123")
        @NotBlank @Size(min = 8)
        String password,

        @Schema(description = "Roles atribuídas ao usuário", example = "[\"ROLE_USER\"]")
        Set<String> roles
) {}
