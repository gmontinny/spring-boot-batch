package br.com.gmontinny.controller;

import br.com.gmontinny.dto.request.LoginRequest;
import br.com.gmontinny.dto.request.RefreshTokenRequest;
import br.com.gmontinny.dto.response.AuthResponse;
import br.com.gmontinny.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Login, renovação de token e logout")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
            summary = "Autenticar usuário",
            description = """
                    Autentica com username e password.
                    Retorna um **access token** (válido por 15 minutos) e um **refresh token** (válido por 7 dias).
                    Use o access token no header `Authorization: Bearer <token>` para acessar os demais endpoints.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Autenticado com sucesso",
                            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Credenciais inválidas", content = @Content),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
                    @ApiResponse(responseCode = "429", description = "Muitas tentativas — rate limit atingido", content = @Content)
            }
    )
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Renovar access token",
            description = """
                    Renova o access token usando um refresh token válido.
                    O refresh token atual é **revogado** e um novo par de tokens é emitido (**token rotation**).
                    Isso previne reutilização de refresh tokens comprometidos.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tokens renovados com sucesso",
                            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Refresh token inválido, expirado ou revogado", content = @Content),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content)
            }
    )
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Logout do usuário",
            description = """
                    Revoga **todos** os refresh tokens do usuário autenticado.
                    O access token atual continua válido até expirar (15 min) — para invalidação imediata,
                    adicione o token a uma blacklist no Redis.
                    """,
            responses = {
                    @ApiResponse(responseCode = "204", description = "Logout realizado com sucesso"),
                    @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
            }
    )
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {
        authService.logout(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
