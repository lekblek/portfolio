package com.scalke.portfolio.backend.contact.domain.port;

import com.scalke.portfolio.backend.contact.domain.model.ContactMessage;

import java.util.Optional;

/**
 * Port de persistance des messages de contact.
 * <p>
 * Ne contient que les méthodes utilisées par un appelant existant (ADR 0001) : {@code create} par
 * {@code SubmitContactMessageUseCase} et le seed de développement, {@code existsAny} par ce seed,
 * {@code findById} et {@code updateStatus} par {@code ChangeContactMessageStatusUseCase}. La liste des messages
 * arrivera avec l'administration (étape 36).
 */
public interface ContactMessageRepository {

    ContactMessage create(ContactMessage message);

    Optional<ContactMessage> findById(Long id);

    /**
     * Enregistre un changement de statut : seuls {@code status} et {@code updatedAt} sont écrits. Le port ne
     * revalide pas la transition : il fait confiance à son unique appelant (même règle que D-AX).
     */
    ContactMessage updateStatus(ContactMessage message);

    boolean existsAny();
}
