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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatchController — Testes Unitários")
class BatchControllerTest {

    @Mock private BatchService batchService;
    @InjectMocks private BatchController batchController;

    @Test
    @DisplayName("runCnaeImport — deve retornar 200 com mensagem do job")
    void shouldReturn200WithJobMessage() {
        when(batchService.runCnaeImport()).thenReturn("Job iniciado com id: 1 | Status: STARTED");

        ResponseEntity<Map<String, String>> result = batchController.runCnaeImport();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsEntry("message", "Job iniciado com id: 1 | Status: STARTED");
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
        when(batchService.runCnaeImport()).thenReturn("Job iniciado com id: 2 | Status: STARTED");

        batchController.runCnaeImport();

        org.mockito.Mockito.verify(batchService).runCnaeImport();
    }
}
