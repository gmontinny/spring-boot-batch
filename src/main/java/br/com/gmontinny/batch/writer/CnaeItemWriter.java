package br.com.gmontinny.batch.writer;

import br.com.gmontinny.domain.entity.Cnae;
import br.com.gmontinny.domain.repository.CnaeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CnaeItemWriter implements ItemWriter<Cnae> {

    private final CnaeRepository cnaeRepository;

    @Override
    public void write(Chunk<? extends Cnae> chunk) {
        cnaeRepository.saveAll(chunk.getItems());
        log.info("Gravados {} registros CNAE no banco", chunk.size());
    }
}
