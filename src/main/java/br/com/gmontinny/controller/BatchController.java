package br.com.gmontinny.controller;

import br.com.gmontinny.service.BatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
@Tag(name = "Batch", description = "Controle do job Spring Batch para importação de CNAE — requer role ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class BatchController {

    private final BatchService batchService;

    @PostMapping("/cnae/run")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Iniciar importação CNAE",
            description = """
                    Dispara o job Spring Batch que lê o arquivo Excel `CNAE20_EstruturaDetalhada.xls`,
                    processa cada linha, publica eventos no RabbitMQ e persiste os dados no PostgreSQL.
                    
                    **Atenção:** Cada execução gera um novo JobInstance com timestamp único.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Job iniciado com sucesso"),
                    @ApiResponse(responseCode = "500", description = "Erro ao iniciar o job")
            }
    )
    public ResponseEntity<Map<String, String>> runCnaeImport() {
        String result = batchService.runCnaeImport();
        return ResponseEntity.ok(Map.of("message", result));
    }
}
