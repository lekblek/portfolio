package com.scalke.portfolio.backend.contact.application.usecase;

import com.scalke.portfolio.backend.contact.domain.model.ContactMessageReceived;
import com.scalke.portfolio.backend.contact.domain.port.ContactNotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notifie l'administrateur d'un message enregistré ({@code 02} §25, D14, D-CJ).
 * <p>
 * Exécuté seulement <em>après</em> la validation de la transaction qui a enregistré le message : un message annulé
 * n'est jamais notifié, et la notification ne peut plus annuler l'enregistrement. Un échec est journalisé, avec
 * l'identifiant du message seulement (aucune donnée personnelle dans les journaux), puis ignoré : le message reste
 * consultable dans l'administration. Pas de transaction (aucun accès à la base, D-BQ) ; envoi synchrone, dans le
 * fil de la requête, borné par les délais SMTP ({@code application.yaml}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotifyContactMessageReceivedUseCase {

    private final ContactNotificationSender notificationSender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void execute(ContactMessageReceived event) {
        try {
            notificationSender.messageReceived(event.message());
        } catch (RuntimeException exception) {
            log.error("Notification du message de contact {} impossible ; le message reste enregistré",
                event.message().id(), exception);
        }
    }
}
