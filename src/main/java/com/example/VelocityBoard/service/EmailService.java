package com.example.VelocityBoard.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class EmailService {

    private final Resend resend;

    public EmailService(@Value("${RESEND_API_KEY}") String apiKey) {
        this.resend = new Resend(apiKey);
    }

    public Mono<CreateEmailResponse> sendHtmlEmail(String to, String subject, String htmlBody) {
        return Mono.fromCallable(() -> {
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("VelocityBoard <no-reply@velocity-board.com>")
                    .to(to)
                    .subject(subject)
                    .html(htmlBody)
                    .build();
            try {
                return resend.emails().send(params);
            } catch (ResendException e) {
                throw new RuntimeException("Error sending email via Resend", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
