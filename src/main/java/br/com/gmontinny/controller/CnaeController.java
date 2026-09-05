package br.com.gmontinny.controller;

import br.com.gmontinny.dto.response.CnaeResponse;
import br.com.gmontinny.service.CnaeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/v1/cnae")
@RequiredArgsConstructor
@Tag(name = "CNAE", description = "Consulta de atividades econômicas CNAE processadas via Spring Batch")
@SecurityRequirement(name = "bearerAuth")
public class CnaeController {

    private final CnaeService cnaeService;
    private final PagedResourcesAssembler<CnaeResponse> assembler;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "secao", "divisao", "grupo", "classe", "subclasse", "denominacao", "processedAt");

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Listar CNAEs", description = "Retorna lista paginada de todos os CNAEs importados.")
    public ResponseEntity<PagedModel<EntityModel<CnaeResponse>>> findAll(
            @Parameter(description = "Número da página (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Campo de ordenação", example = "id") @RequestParam(defaultValue = "id") String sort,
            @Parameter(description = "Direção: asc ou desc", example = "asc") @RequestParam(defaultValue = "asc") String direction) {
        PageRequest pageable = buildPageRequest(page, size, sort, direction, ALLOWED_SORT_FIELDS, "id");
        Page<CnaeResponse> result = cnaeService.findAll(pageable);
        result.forEach(this::addLinks);
        return ResponseEntity.ok(assembler.toModel(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(
            summary = "Buscar CNAE por ID",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CnaeResponse.class))),
                    @ApiResponse(responseCode = "404", description = "CNAE não encontrado", content = @Content)
            }
    )
    public ResponseEntity<CnaeResponse> findById(
            @Parameter(description = "ID do CNAE") @PathVariable Long id) {
        CnaeResponse response = cnaeService.findById(id);
        addLinks(response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Pesquisar CNAE por denominação", description = "Busca CNAEs cuja denominação contenha o termo informado (case-insensitive).")
    public ResponseEntity<PagedModel<EntityModel<CnaeResponse>>> search(
            @Parameter(description = "Termo de busca", example = "agricultura") @RequestParam String q,
            @Parameter(description = "Número da página", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Campo de ordenação", example = "id") @RequestParam(defaultValue = "id") String sort,
            @Parameter(description = "Direção: asc ou desc", example = "asc") @RequestParam(defaultValue = "asc") String direction) {
        PageRequest pageable = buildPageRequest(page, size, sort, direction, ALLOWED_SORT_FIELDS, "id");
        Page<CnaeResponse> result = cnaeService.search(q, pageable);
        result.forEach(this::addLinks);
        return ResponseEntity.ok(assembler.toModel(result));
    }

    private void addLinks(CnaeResponse response) {
        response.add(linkTo(methodOn(CnaeController.class).findById(response.getId())).withSelfRel());
        response.add(linkTo(methodOn(CnaeController.class).findAll(0, 20, "id", "asc")).withRel("cnae"));
    }

    private PageRequest buildPageRequest(int page, int size, String sort, String direction,
                                         Set<String> allowed, String fallback) {
        String field = allowed.contains(sort) ? sort : fallback;
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(dir, field));
    }
}
