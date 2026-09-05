package br.com.gmontinny.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import br.com.gmontinny.domain.repository.CnaeRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatchService — Testes Unitários")
class BatchServiceTest {

    @Mock private JobOperator jobOperator;
    @Mock private Job cnaeImportJob;
    @Mock private JobRepository jobRepository;
    @Mock private CnaeRepository cnaeRepository;

    @InjectMocks
    private BatchService batchService;

    @Test
    @DisplayName("Deve disparar job e retornar mensagem com id e status")
    void shouldRunJobAndReturnMessage() throws Exception {
        JobExecution execution = mock(JobExecution.class);
        when(execution.getId()).thenReturn(42L);
        when(execution.getStatus()).thenReturn(BatchStatus.STARTED);
        when(jobOperator.start(any(Job.class), any(JobParameters.class))).thenReturn(execution);

        String result = batchService.runCnaeImport();

        assertThat(result).contains("42").contains("STARTED");
        verify(jobOperator).start(eq(cnaeImportJob), any(JobParameters.class));
    }

    @Test
    @DisplayName("Deve lançar RuntimeException quando JobOperator falha")
    void shouldThrowWhenJobOperatorFails() throws Exception {
        when(jobOperator.start(any(Job.class), any(JobParameters.class))).thenThrow(new RuntimeException("Conexão recusada"));

        assertThatThrownBy(() -> batchService.runCnaeImport())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha ao iniciar o job");
    }
}
