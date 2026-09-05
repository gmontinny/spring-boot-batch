package br.com.gmontinny.service;

import br.com.gmontinny.domain.entity.Role;
import br.com.gmontinny.domain.entity.User;
import br.com.gmontinny.domain.repository.RoleRepository;
import br.com.gmontinny.domain.repository.UserRepository;
import br.com.gmontinny.dto.request.UserRequest;
import br.com.gmontinny.dto.response.UserResponse;
import br.com.gmontinny.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService — Testes Unitários")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Deve criar usuário com role informada")
    void shouldCreateUser() {
        UserRequest request = new UserRequest("joao", "joao@email.com", "Senha@123", Set.of("ROLE_USER"));
        Role role = new Role("ROLE_USER");
        User saved = new User();
        saved.setUsername("joao");
        UserResponse response = new UserResponse();
        response.setId(1L);

        when(userRepository.existsByUsername("joao")).thenReturn(false);
        when(userRepository.existsByEmail("joao@email.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("Senha@123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(userMapper.toResponse(saved)).thenReturn(response);

        UserResponse result = userService.create(request);

        assertThat(result.getId()).isEqualTo(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Deve lançar 409 quando username já existe")
    void shouldThrowConflictOnDuplicateUsername() {
        UserRequest request = new UserRequest("admin", "x@x.com", "Senha@123", Set.of());
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("Deve lançar 409 quando email já existe")
    void shouldThrowConflictOnDuplicateEmail() {
        UserRequest request = new UserRequest("novo", "admin@gmontinny.com.br", "Senha@123", Set.of());
        when(userRepository.existsByUsername("novo")).thenReturn(false);
        when(userRepository.existsByEmail("admin@gmontinny.com.br")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("Deve lançar 404 ao buscar usuário inexistente")
    void shouldThrowNotFoundWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("Deve deletar usuário existente")
    void shouldDeleteExistingUser() {
        User user = new User();
        user.setUsername("joao");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.delete(1L);

        verify(userRepository).delete(user);
    }
}
