package com.middleware.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RabbitMQConfig {

    // ── Exchange names ──────────────────────────────────────────────
    public static final String KOINS_EXCHANGE = "koins.exchange";

    // ── Queue names ─────────────────────────────────────────────────
    public static final String LOAN_NOTIFICATION_QUEUE   = "koins.loan.notifications";
    public static final String PAYMENT_CONFIRMATION_QUEUE = "koins.payment.confirmations";
    public static final String EMAIL_VERIFICATION_QUEUE = "koins.email.verification";
    public static final String PASSWORD_RESET_QUEUE = "koins.password.reset";

    // ── Routing keys ────────────────────────────────────────────────
    public static final String LOAN_NOTIFICATION_ROUTING_KEY    = "loan.notification";
    public static final String PAYMENT_CONFIRMATION_ROUTING_KEY = "payment.confirmation";
    public static final String EMAIL_VERIFICATION_ROUTING_KEY = "email.verification";
    public static final String PASSWORD_RESET_ROUTING_KEY = "password.reset";

    // ── Dead-letter queue names ──────────────────────────────────────
    public static final String LOAN_NOTIFICATION_DLQ    = "koins.loan.notifications.dlq";
    public static final String PAYMENT_CONFIRMATION_DLQ = "koins.payment.confirmations.dlq";
    public static final String EMAIL_VERIFICATION_DLQ = "koins.email.verification.dlq";
    public static final String PASSWORD_RESET_DLQ = "koins.password.reset.dlq";

    // ── Exchange ────────────────────────────────────────────────────
    @Bean
    public TopicExchange koinsExchange() {
        return new TopicExchange(KOINS_EXCHANGE, true, false);
    }

    // ── Loan notification queue + DLQ ───────────────────────────────
    @Bean
    public Queue loanNotificationQueue() {
        return QueueBuilder.durable(LOAN_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", LOAN_NOTIFICATION_DLQ)
                .build();
    }

    @Bean
    public Queue loanNotificationDlq() {
        return QueueBuilder.durable(LOAN_NOTIFICATION_DLQ).build();
    }

    @Bean
    public Binding loanNotificationBinding() {
        return BindingBuilder.bind(loanNotificationQueue())
                .to(koinsExchange())
                .with(LOAN_NOTIFICATION_ROUTING_KEY);
    }

    // ── Payment confirmation queue + DLQ ────────────────────────────
    @Bean
    public Queue paymentConfirmationQueue() {
        return QueueBuilder.durable(PAYMENT_CONFIRMATION_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", PAYMENT_CONFIRMATION_DLQ)
                .build();
    }

    @Bean
    public Queue paymentConfirmationDlq() {
        return QueueBuilder.durable(PAYMENT_CONFIRMATION_DLQ).build();
    }

    @Bean
    public Binding paymentConfirmationBinding() {
        return BindingBuilder.bind(paymentConfirmationQueue())
                .to(koinsExchange())
                .with(PAYMENT_CONFIRMATION_ROUTING_KEY);
    }

    //---- email
    @Bean
    public Queue emailVerificationQueue() {

        return QueueBuilder.durable(EMAIL_VERIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", EMAIL_VERIFICATION_DLQ)
                .build();
    }

    @Bean
    public Queue emailVerificationDlq() {
        return QueueBuilder.durable(EMAIL_VERIFICATION_DLQ).build();
    }

    @Bean
    public Binding emailVerificationBinding() {
        return BindingBuilder
                .bind(emailVerificationQueue())
                .to(koinsExchange())
                .with(EMAIL_VERIFICATION_ROUTING_KEY);
    }

    @Bean
    public Queue passwordResetQueue() {
        return QueueBuilder
                .durable(PASSWORD_RESET_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", PASSWORD_RESET_DLQ)
                .build();
    }
    @Bean
    public Queue passwordResetDlq() {
        return QueueBuilder
                .durable(PASSWORD_RESET_DLQ)
                .build();
    }


    @Bean
    public Binding passwordResetBinding() {
        return BindingBuilder
                .bind(passwordResetQueue())
                .to(koinsExchange())
                .with(PASSWORD_RESET_ROUTING_KEY);
    }

    // ── JSON serialisation ───────────────────────────────────────────

    @Bean
    public MessageConverter jsonMessageConverter() {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
