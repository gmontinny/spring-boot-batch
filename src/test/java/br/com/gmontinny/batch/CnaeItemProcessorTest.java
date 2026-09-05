package br.com.gmontinny.batch;

import br.com.gmontinny.batch.processor.CnaeItemProcessor;
import br.com.gmontinny.config.RabbitMQConfig;
import br.com.gmontinny.domain.entity.Cnae;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CnaeItemProcessor — Testes Unitários")
class CnaeItemProcessorTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private CnaeItemProcessor processor;

    private CnaeRow validRow;

    @BeforeEach
    void setUp() {
        validRow = new CnaeRow("A", "01", "01.1", "01.11", "0111-3/01",
                "Cultivo de arroz", "Inclui irrigação");
    }

    @Test
    @DisplayName("Deve mapear CnaeRow para Cnae e publicar no RabbitMQ")
    void shouldProcessValidRow() throws Exception {
        Cnae result = processor.process(validRow);

        assertThat(result).isNotNull();
        assertThat(result.getSecao()).isEqualTo("A");
        assertThat(result.getDivisao()).isEqualTo("01");
        assertThat(result.getGrupo()).isEqualTo("01.1");
        assertThat(result.getClasse()).isEqualTo("01.11");
        assertThat(result.getSubclasse()).isEqualTo("0111-3/01");
        assertThat(result.getDenominacao()).isEqualTo("Cultivo de arroz");
        assertThat(result.getObservacoes()).isEqualTo("Inclui irrigação");
        assertThat(result.getProcessedAt()).isNotNull();

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.BATCH_EXCHANGE),
                eq(RabbitMQConfig.BATCH_ROUTING_KEY),
                eq(validRow)
        );
    }

    @Test
    @DisplayName("Deve retornar null e não publicar quando denominação é nula")
    void shouldReturnNullWhenDenominacaoIsNull() throws Exception {
        CnaeRow row = new CnaeRow("A", "01", null, null, null, null, null);

        Cnae result = processor.process(row);

        assertThat(result).isNull();
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    @DisplayName("Deve retornar null e não publicar quando denominação é em branco")
    void shouldReturnNullWhenDenominacaoIsBlank() throws Exception {
        CnaeRow row = new CnaeRow("A", "01", null, null, null, "   ", null);

        Cnae result = processor.process(row);

        assertThat(result).isNull();
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    @DisplayName("Deve processar row com campos opcionais nulos")
    void shouldProcessRowWithNullOptionalFields() throws Exception {
        CnaeRow row = new CnaeRow(null, null, null, null, null, "Atividade genérica", null);

        Cnae result = processor.process(row);

        assertThat(result).isNotNull();
        assertThat(result.getDenominacao()).isEqualTo("Atividade genérica");
        assertThat(result.getSecao()).isNull();
        assertThat(result.getSubclasse()).isNull();
        verify(rabbitTemplate, times(1)).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }
}
