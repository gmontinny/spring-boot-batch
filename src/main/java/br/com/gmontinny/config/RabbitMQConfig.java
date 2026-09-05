package br.com.gmontinny.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.config.StatelessRetryOperationsInterceptor;

@Configuration
public class RabbitMQConfig {

    public static final String BATCH_EXCHANGE    = "batch.exchange";
    public static final String BATCH_QUEUE       = "batch.cnae.queue";
    public static final String BATCH_ROUTING_KEY = "batch.cnae";
    public static final String DLQ_QUEUE         = "batch.cnae.dlq";
    public static final String DLQ_ROUTING_KEY   = "batch.cnae.dead";

    @Value("${spring.rabbitmq.listener.simple.concurrency:2}")
    private int concurrency;

    @Value("${spring.rabbitmq.listener.simple.max-concurrency:5}")
    private int maxConcurrency;

    // -------------------------------------------------------------------------
    // Topologia: Exchange → Queue → DLQ
    // -------------------------------------------------------------------------
    @Bean
    public DirectExchange batchExchange() {
        return ExchangeBuilder.directExchange(BATCH_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue batchQueue() {
        return QueueBuilder.durable(BATCH_QUEUE)
                .withArgument("x-dead-letter-exchange", BATCH_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public Binding batchBinding() {
        return BindingBuilder.bind(batchQueue()).to(batchExchange()).with(BATCH_ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(batchExchange()).with(DLQ_ROUTING_KEY);
    }

    // -------------------------------------------------------------------------
    // Serialização JSON
    // -------------------------------------------------------------------------
    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    // -------------------------------------------------------------------------
    // Retry com backoff exponencial — encaminha para DLQ após esgotar tentativas
    // (Tolerância a Falhas / Redução de Sobrecarga)
    // -------------------------------------------------------------------------
    @Bean
    public StatelessRetryOperationsInterceptor retryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxRetries(3)
                .backOffOptions(2000, 2.0, 10000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setConcurrentConsumers(concurrency);
        factory.setMaxConcurrentConsumers(maxConcurrency);
        factory.setAdviceChain(retryInterceptor());
        return factory;
    }
}
