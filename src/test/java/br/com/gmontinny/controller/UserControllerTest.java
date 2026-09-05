package br.com.gmontinny.controller;

import br.com.gmontinny.dto.request.UserRequest;
import br.com.gmontinny.dto.response.UserResponse;
import br.com.gmontinny.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController — Testes Unitários")
class UserControllerTest {

    @Mock private UserService userService;
    @Mock private PagedResourcesAssembler<UserResponse> assembler;
    @InjectMocks private UserController userController;

    @Test
    @DisplayName("findAll — deve usar sort=id quando campo inválido é informado")
    void shouldFallbackToIdWhenSortFieldInvalid() {
        UserResponse response = userResponse(1L);
        var page = new PageImpl<>(List.of(response));
        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);

        when(userService.findAll(captor.capture())).thenReturn(page);
        when(assembler.toModel(any())).thenReturn(PagedModel.empty());

        userController.findAll(0, 10, "[\"username\"]", "asc");

        PageRequest used = captor.getValue();
        assertThat(used.getSort().getOrderFor("id")).isNotNull();
        assertThat(used.getSort().getOrderFor("[\"username\"]")).isNull();
    }

    @Test
    @DisplayName("findAll — deve usar sort=username quando campo válido é informado")
    void shouldUseValidSortField() {
        UserResponse response = userResponse(1L);
        var page = new PageImpl<>(List.of(response));
        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);

        when(userService.findAll(captor.capture())).thenReturn(page);
        when(assembler.toModel(any())).thenReturn(PagedModel.empty());

        userController.findAll(0, 10, "username", "asc");

        PageRequest used = captor.getValue();
        assertThat(used.getSort().getOrderFor("username")).isNotNull();
        assertThat(used.getSort().getOrderFor("username").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("findAll — deve usar direction=DESC quando informado")
    void shouldUseDescDirection() {
        var page = new PageImpl<>(List.of(userResponse(1L)));
        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);

        when(userService.findAll(captor.capture())).thenReturn(page);
        when(assembler.toModel(any())).thenReturn(PagedModel.empty());

        userController.findAll(0, 10, "username", "desc");

        assertThat(captor.getValue().getSort().getOrderFor("username").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("findAll — deve retornar 200")
    void shouldReturn200() {
        when(userService.findAll(any())).thenReturn(new PageImpl<>(List.of()));
        when(assembler.toModel(any())).thenReturn(PagedModel.empty());

        ResponseEntity<?> result = userController.findAll(0, 10, "id", "asc");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("create — deve retornar 201")
    void shouldReturn201OnCreate() {
        UserResponse response = userResponse(2L);
        when(userService.create(any())).thenReturn(response);

        ResponseEntity<UserResponse> result = userController.create(
                new UserRequest("novo", "novo@email.com", "Senha@123", Set.of("ROLE_USER")));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("delete — deve retornar 204")
    void shouldReturn204OnDelete() {
        doNothing().when(userService).delete(1L);

        ResponseEntity<Void> result = userController.delete(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private UserResponse userResponse(Long id) {
        UserResponse r = new UserResponse();
        r.setId(id);
        r.setUsername("user" + id);
        return r;
    }
}
