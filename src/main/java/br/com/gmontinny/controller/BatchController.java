package br.com.gmontinny.controller;

import br.com.gmontinny.service.BatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
                    Dispara o job Spring Batch de forma assíncrona.
                    Retorna imediatamente com o `jobExecutionId` — use GET `/cnae/status/{jobExecutionId}` para acompanhar.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Job iniciado"),
                    @ApiResponse(responseCode = "500", description = "Erro ao iniciar o job")
            }
    )
    public ResponseEntity<Map<String, String>> runCnaeImport() {
        String result = batchService.runCnaeImport();
        return ResponseEntity.ok(Map.of("message", result));
    }

    @GetMapping("/cnae/status/{jobExecutionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Consultar status do job",
            description = """
                    Retorna o status atual de uma execução do job pelo `jobExecutionId`.
                    
                    **Status possíveis:** `STARTING`, `STARTED`, `COMPLETED`, `FAILED`, `STOPPED`
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Status retornado"),
                    @ApiResponse(responseCode = "404", description = "Job execution não encontrado")
            }
    )
    public ResponseEntity<Map<String, Object>> getStatus(
            @Parameter(description = "ID retornado pelo endpoint /run", example = "1")
            @PathVariable Long jobExecutionId) {
        return ResponseEntity.ok(batchService.getStatus(jobExecutionId));
    }
}
