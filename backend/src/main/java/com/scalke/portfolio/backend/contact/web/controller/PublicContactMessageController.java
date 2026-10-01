package com.scalke.portfolio.backend.contact.web.controller;

import com.scalke.portfolio.backend.contact.application.usecase.SubmitContactMessageUseCase;
import com.scalke.portfolio.backend.contact.web.dto.SubmitContactMessageRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Formulaire de contact public ({@code 01} §13, D-EJ) : validation, piège à robots, limitation de débit, sauvegarde,
 * puis notification de l'administrateur (D-CJ).
 * <p>
 * 202 Accepted sans corps : le message est reçu, sans ressource publique à désigner (pas de {@code Location}, aucun
 * identifiant exposé). Un piège rempli reçoit exactement la même réponse, sans rien enregistrer.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/public/contact-messages")
@Slf4j
public class PublicContactMessageController {

    private final SubmitContactMessageUseCase submitContactMessageUseCase;

    /**
     * {@code getRemoteAddr()} rend l'adresse transmise par {@code X-Forwarded-For} depuis un réseau de confiance (D-DC).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    void submit(@Valid @RequestBody SubmitContactMessageRequest body, HttpServletRequest request) {
        if (body.trapped()) {
            log.info("Message de contact écarté depuis {} : piège à robots rempli", request.getRemoteAddr());
        } else {
            submitContactMessageUseCase.execute(body.toSubmission(), request.getRemoteAddr());
        }
    }
}
