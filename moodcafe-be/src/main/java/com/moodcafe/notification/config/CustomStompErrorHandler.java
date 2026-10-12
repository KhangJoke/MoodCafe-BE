package com.moodcafe.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class CustomStompErrorHandler extends StompSubProtocolErrorHandler {

    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }

        String errorMessage = cause.getMessage() != null && !cause.getMessage().isBlank()
                ? cause.getMessage()
                : "WebSocket STOMP processing error";

        log.warn("STOMP processing error occurred: {}", errorMessage);

        boolean isAuth = errorMessage.toLowerCase().contains("jwt")
                || errorMessage.toLowerCase().contains("token")
                || errorMessage.toLowerCase().contains("auth")
                || errorMessage.toLowerCase().contains("expired");

        String formattedMessage = isAuth ? "AUTH_ERROR: " + errorMessage : errorMessage;

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(formattedMessage);
        accessor.setLeaveMutable(true);

        return MessageBuilder.createMessage(
                errorMessage.getBytes(StandardCharsets.UTF_8),
                accessor.getMessageHeaders()
        );
    }
}
