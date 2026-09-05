package br.com.gmontinny.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta de autenticação com access token e refresh token")
public record AuthResponse(

        @Schema(description = "Access token JWT (expira em 15 minutos)")
        String token,

        @Schema(description = "Tipo do token", example = "Bearer")
        String type,

        @Schema(description = "Refresh token para renovar o access token (expira em 7 dias)")
        String refreshToken,

        @Schema(description = "Nome de usuário autenticado")
        String username,

        @Schema(description = "Tempo de expiração do access token em milissegundos")
        long expiresIn
) {
    public AuthResponse(String token, String refreshToken, String username, long expiresIn) {
        this(token, "Bearer", refreshToken, username, expiresIn);
    }
}
