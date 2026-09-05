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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/v1/cnae")
@RequiredArgsConstructor
@Tag(name = "CNAE", description = "Consulta de atividades econômicas CNAE processadas via Spring Batch")
@SecurityRequirement(name = "bearerAuth")
public class CnaeController {

    private final CnaeService cnaeService;
    private final PagedResourcesAssembler<CnaeResponse> assembler;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(
            summary = "Listar CNAEs",
            description = "Retorna lista paginada de todos os CNAEs importados. Suporta parâmetros de paginação: `page`, `size`, `sort`."
    )
    public ResponseEntity<PagedModel<EntityModel<CnaeResponse>>> findAll(Pageable pageable) {
        Page<CnaeResponse> page = cnaeService.findAll(pageable);
        page.forEach(this::addLinks);
        return ResponseEntity.ok(assembler.toModel(page));
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
    @Operation(
            summary = "Pesquisar CNAE por denominação",
            description = "Busca CNAEs cuja denominação contenha o termo informado (case-insensitive)."
    )
    public ResponseEntity<PagedModel<EntityModel<CnaeResponse>>> search(
            @Parameter(description = "Termo de busca na denominação", example = "agricultura")
            @RequestParam String q,
            Pageable pageable) {
        Page<CnaeResponse> page = cnaeService.search(q, pageable);
        page.forEach(this::addLinks);
        return ResponseEntity.ok(assembler.toModel(page));
    }

    private void addLinks(CnaeResponse response) {
        response.add(linkTo(methodOn(CnaeController.class).findById(response.getId())).withSelfRel());
        response.add(linkTo(methodOn(CnaeController.class).findAll(Pageable.unpaged())).withRel("cnae"));
    }
}
