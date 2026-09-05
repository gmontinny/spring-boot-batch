package br.com.gmontinny.controller;

import br.com.gmontinny.service.BatchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatchController — Testes Unitários")
class BatchControllerTest {

    @Mock private BatchService batchService;
    @InjectMocks private BatchController batchController;

    @Test
    @DisplayName("runCnaeImport — deve retornar 200 com mensagem do job")
    void shouldReturn200WithJobMessage() {
        when(batchService.runCnaeImport()).thenReturn("Job iniciado com id: 1 | Status: STARTING");

        ResponseEntity<Map<String, String>> result = batchController.runCnaeImport();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsEntry("message", "Job iniciado com id: 1 | Status: STARTING");
    }

    @Test
    @DisplayName("runCnaeImport — deve propagar exceção quando BatchService falha")
    void shouldThrowWhenBatchServiceFails() {
        when(batchService.runCnaeImport()).thenThrow(new RuntimeException("Falha ao iniciar o job"));

        assertThatThrownBy(() -> batchController.runCnaeImport())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha ao iniciar o job");
    }

    @Test
    @DisplayName("runCnaeImport — deve chamar batchService.runCnaeImport uma vez")
    void shouldCallBatchServiceOnce() {
        when(batchService.runCnaeImport()).thenReturn("Job iniciado com id: 2 | Status: STARTING");

        batchController.runCnaeImport();

        verify(batchService).runCnaeImport();
    }

    @Test
    @DisplayName("getStatus — deve retornar 200 com dados do job COMPLETED")
    void shouldReturn200WithCompletedStatus() {
        Map<String, Object> status = Map.of(
                "jobExecutionId", 1L,
                "status", "COMPLETED",
                "exitCode", "COMPLETED",
                "lidos", 1318,
                "gravados", 1318,
                "pulados", 0
        );
        when(batchService.getStatus(1L)).thenReturn(status);

        ResponseEntity<Map<String, Object>> result = batchController.getStatus(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsEntry("status", "COMPLETED");
        assertThat(result.getBody()).containsEntry("gravados", 1318);
    }

    @Test
    @DisplayName("getStatus — deve retornar 200 com dados do job FAILED")
    void shouldReturn200WithFailedStatus() {
        Map<String, Object> status = Map.of(
                "jobExecutionId", 2L,
                "status", "FAILED",
                "exitCode", "FAILED",
                "erro", "Erro ao processar chunk"
        );
        when(batchService.getStatus(2L)).thenReturn(status);

        ResponseEntity<Map<String, Object>> result = batchController.getStatus(2L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsEntry("status", "FAILED");
        assertThat(result.getBody()).containsKey("erro");
    }

    @Test
    @DisplayName("getStatus — deve propagar 404 quando job não encontrado")
    void shouldThrow404WhenJobNotFound() {
        when(batchService.getStatus(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Job execution não encontrado: 99"));

        assertThatThrownBy(() -> batchController.getStatus(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
