package br.com.gmontinny.controller;

import br.com.gmontinny.dto.request.UserRequest;
import br.com.gmontinny.dto.response.UserResponse;
import br.com.gmontinny.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Gerenciamento de usuários — requer role ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final PagedResourcesAssembler<UserResponse> assembler;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar usuários", description = "Retorna lista paginada de usuários. Requer ADMIN.")
    public ResponseEntity<PagedModel<EntityModel<UserResponse>>> findAll(Pageable pageable) {
        Page<UserResponse> page = userService.findAll(pageable);
        page.forEach(this::addLinks);
        return ResponseEntity.ok(assembler.toModel(page));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Buscar usuário por ID",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = UserResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content)
            }
    )
    public ResponseEntity<UserResponse> findById(
            @Parameter(description = "ID do usuário") @PathVariable Long id) {
        UserResponse response = userService.findById(id);
        addLinks(response);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar usuário", description = "Cria um novo usuário com roles definidas.")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.create(request);
        addLinks(response);
        URI location = linkTo(methodOn(UserController.class).findById(response.getId())).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remover usuário")
    @ApiResponse(responseCode = "204", description = "Usuário removido")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void addLinks(UserResponse response) {
        response.add(linkTo(methodOn(UserController.class).findById(response.getId())).withSelfRel());
        response.add(linkTo(methodOn(UserController.class).findAll(Pageable.unpaged())).withRel("users"));
    }
}
