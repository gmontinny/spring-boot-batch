package br.com.gmontinny.messaging;

import br.com.gmontinny.batch.CnaeRow;
import br.com.gmontinny.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CnaeEventConsumer {

    /**
     * Consome eventos publicados pelo CnaeItemProcessor.
     *
     * Tolerância a Falhas: o RetryInterceptor (configurado em RabbitMQConfig)
     * reprocessa até 3 vezes com backoff exponencial antes de encaminhar para a DLQ.
     *
     * Redução de Sobrecarga: concurrency=2/max=5 consumers processam em paralelo
     * sem sobrecarregar o banco ou a aplicação.
     */
    @RabbitListener(queues = RabbitMQConfig.BATCH_QUEUE)
    public void onCnaeProcessed(CnaeRow row) {
        log.info("[MQ] CNAE recebido — subclasse={}, denominacao={}",
                row.getSubclasse(), row.getDenominacao());
        // Ponto de extensão: auditoria, notificações, integrações externas
    }

    /**
     * Consome mensagens que falharam após todas as tentativas de retry.
     * Capacidade de Reiniciar: mensagens na DLQ podem ser inspecionadas
     * e reprocessadas manualmente via RabbitMQ Management UI.
     */
    @RabbitListener(queues = RabbitMQConfig.DLQ_QUEUE)
    public void onCnaeDeadLetter(CnaeRow row) {
        log.error("[DLQ] Mensagem não processada após retries — subclasse={}, denominacao={}",
                row.getSubclasse(), row.getDenominacao());
        // Ponto de extensão: alertas, persistência de auditoria de falhas
    }
}
