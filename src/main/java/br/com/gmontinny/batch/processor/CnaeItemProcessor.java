package br.com.gmontinny.batch.processor;

import br.com.gmontinny.batch.CnaeRow;
import br.com.gmontinny.config.RabbitMQConfig;
import br.com.gmontinny.domain.entity.Cnae;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CnaeItemProcessor implements ItemProcessor<CnaeRow, Cnae> {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public Cnae process(CnaeRow row) {
        if (row.getDenominacao() == null || row.getDenominacao().isBlank()) {
            return null; // filtra registros inválidos
        }

        Cnae cnae = new Cnae();
        cnae.setSecao(row.getSecao());
        cnae.setDivisao(row.getDivisao());
        cnae.setGrupo(row.getGrupo());
        cnae.setClasse(row.getClasse());
        cnae.setSubclasse(row.getSubclasse());
        cnae.setDenominacao(row.getDenominacao());
        cnae.setObservacoes(row.getObservacoes());
        cnae.setProcessedAt(LocalDateTime.now());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.BATCH_EXCHANGE,
                RabbitMQConfig.BATCH_ROUTING_KEY,
                row
        );

        log.debug("Processado CNAE: {}", row.getDenominacao());
        return cnae;
    }
}
