package br.com.gmontinny.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatchService — Testes Unitários")
class BatchServiceTest {

    @Mock private JobLauncher asyncJobLauncher;
    @Mock private Job cnaeImportJob;

    @InjectMocks
    private BatchService batchService;

    @Test
    @DisplayName("Deve disparar job e retornar mensagem com id e status")
    void shouldRunJobAndReturnMessage() throws Exception {
        JobExecution execution = mock(JobExecution.class);
        when(execution.getId()).thenReturn(42L);
        when(execution.getStatus()).thenReturn(BatchStatus.STARTED);
        when(asyncJobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(execution);

        String result = batchService.runCnaeImport();

        assertThat(result).contains("42").contains("STARTED");
        verify(asyncJobLauncher).run(eq(cnaeImportJob), any(JobParameters.class));
    }

    @Test
    @DisplayName("Deve lançar RuntimeException quando JobLauncher falha")
    void shouldThrowWhenJobLauncherFails() throws Exception {
        when(asyncJobLauncher.run(any(), any())).thenThrow(new RuntimeException("Conexão recusada"));

        assertThatThrownBy(() -> batchService.runCnaeImport())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha ao iniciar o job");
    }
}
