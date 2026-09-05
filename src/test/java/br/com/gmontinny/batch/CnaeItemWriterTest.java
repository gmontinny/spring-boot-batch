package br.com.gmontinny.batch;

import br.com.gmontinny.batch.writer.CnaeItemWriter;
import br.com.gmontinny.domain.entity.Cnae;
import br.com.gmontinny.domain.repository.CnaeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CnaeItemWriter — Testes Unitários")
class CnaeItemWriterTest {

    @Mock
    private CnaeRepository cnaeRepository;

    @InjectMocks
    private CnaeItemWriter writer;

    @Test
    @DisplayName("Deve persistir todos os itens do chunk via saveAll")
    void shouldSaveAllItemsInChunk() throws Exception {
        Cnae c1 = cnae("Cultivo de arroz");
        Cnae c2 = cnae("Criação de bovinos");
        Chunk<Cnae> chunk = new Chunk<>(List.of(c1, c2));

        writer.write(chunk);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Cnae>> captor = ArgumentCaptor.forClass(List.class);
        verify(cnaeRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()).extracting(Cnae::getDenominacao)
                .containsExactly("Cultivo de arroz", "Criação de bovinos");
    }

    @Test
    @DisplayName("Deve chamar saveAll mesmo com chunk vazio")
    void shouldCallSaveAllWithEmptyChunk() throws Exception {
        writer.write(new Chunk<>(List.of()));
        verify(cnaeRepository).saveAll(List.of());
    }

    private Cnae cnae(String denominacao) {
        Cnae c = new Cnae();
        c.setDenominacao(denominacao);
        return c;
    }
}
