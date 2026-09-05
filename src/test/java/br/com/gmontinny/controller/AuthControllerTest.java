package br.com.gmontinny.controller;

import br.com.gmontinny.dto.request.LoginRequest;
import br.com.gmontinny.dto.request.RefreshTokenRequest;
import br.com.gmontinny.dto.response.AuthResponse;
import br.com.gmontinny.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController — Testes Unitários")
class AuthControllerTest {

    @Mock private AuthService authService;
    @InjectMocks private AuthController authController;

    @Test
    @DisplayName("login — deve retornar 200 com access e refresh token")
    void shouldReturn200WithBothTokens() {
        AuthResponse response = new AuthResponse("access-tok", "refresh-tok", "admin", 900000L);
        when(authService.authenticate(any())).thenReturn(response);

        ResponseEntity<AuthResponse> result = authController.login(new LoginRequest("admin", "Admin@2026"));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().token()).isEqualTo("access-tok");
        assertThat(result.getBody().refreshToken()).isEqualTo("refresh-tok");
        assertThat(result.getBody().type()).isEqualTo("Bearer");
        assertThat(result.getBody().expiresIn()).isEqualTo(900000L);
    }

    @Test
    @DisplayName("login — deve propagar BadCredentialsException com credenciais inválidas")
    void shouldThrowOnBadCredentials() {
        when(authService.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authController.login(new LoginRequest("admin", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("refresh — deve retornar 200 com novos tokens")
    void shouldReturn200OnRefresh() {
        AuthResponse response = new AuthResponse("new-access", "new-refresh", "admin", 900000L);
        when(authService.refresh(any())).thenReturn(response);

        ResponseEntity<AuthResponse> result = authController.refresh(new RefreshTokenRequest("old-refresh"));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().token()).isEqualTo("new-access");
        assertThat(result.getBody().refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    @DisplayName("refresh — deve propagar exceção para token inválido")
    void shouldThrowOnInvalidRefreshToken() {
        when(authService.refresh(any())).thenThrow(new RuntimeException("Token inválido"));

        assertThatThrownBy(() -> authController.refresh(new RefreshTokenRequest("bad-token")))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("logout — deve retornar 204 e chamar authService.logout")
    void shouldReturn204OnLogout() {
        var userDetails = mock(org.springframework.security.core.userdetails.UserDetails.class);
        when(userDetails.getUsername()).thenReturn("admin");
        doNothing().when(authService).logout("admin");

        ResponseEntity<Void> result = authController.logout(userDetails);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(authService).logout("admin");
    }

    @Test
    @DisplayName("logout — deve propagar exceção quando authService falha")
    void shouldThrowWhenLogoutFails() {
        var userDetails = mock(org.springframework.security.core.userdetails.UserDetails.class);
        when(userDetails.getUsername()).thenReturn("admin");
        doThrow(new RuntimeException("Erro")).when(authService).logout("admin");

        assertThatThrownBy(() -> authController.logout(userDetails))
                .isInstanceOf(RuntimeException.class);
    }
}
