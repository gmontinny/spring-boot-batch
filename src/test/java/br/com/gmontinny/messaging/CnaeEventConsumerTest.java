package br.com.gmontinny.messaging;

import br.com.gmontinny.batch.CnaeRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("CnaeEventConsumer — Testes Unitários")
class CnaeEventConsumerTest {

    @Spy
    private CnaeEventConsumer consumer;

    @Test
    @DisplayName("Deve processar mensagem válida sem lançar exceção")
    void shouldProcessValidMessage() {
        CnaeRow row = new CnaeRow("A", "01", "01.1", "01.11", "0111-3/01",
                "Cultivo de arroz", null);

        assertDoesNotThrow(() -> consumer.onCnaeProcessed(row));
    }

    @Test
    @DisplayName("Deve processar mensagem da DLQ sem lançar exceção")
    void shouldProcessDeadLetterMessage() {
        CnaeRow row = new CnaeRow(null, null, null, null, null, "Falhou 3x", null);

        assertDoesNotThrow(() -> consumer.onCnaeDeadLetter(row));
    }

    @Test
    @DisplayName("Deve processar mensagem com campos nulos sem lançar exceção")
    void shouldHandleNullFields() {
        CnaeRow row = new CnaeRow(null, null, null, null, null, null, null);

        assertDoesNotThrow(() -> consumer.onCnaeProcessed(row));
    }
}
