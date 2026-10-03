package com.scalke.portfolio.backend.contact.domain.port;

import com.scalke.portfolio.backend.contact.domain.model.ContactMessage;
import com.scalke.portfolio.backend.contact.domain.model.ContactStatus;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;

import java.util.Optional;

/**
 * Port de persistance des messages de contact.
 * <p>
 * Ne contient que les méthodes utilisées par un appelant existant (ADR 0001) : {@code create} par
 * {@code SubmitContactMessageUseCase} et le seed de développement, {@code existsAny} par ce seed,
 * {@code findById} et {@code updateStatus} par {@code ChangeContactMessageStatusUseCase}, {@code findById} aussi par
 * {@code GetContactMessageUseCase}, {@code findPage} par {@code ListContactMessagesUseCase} (D-CZ).
 */
public interface ContactMessageRepository {

    ContactMessage create(ContactMessage message);

    Optional<ContactMessage> findById(Long id);

    /**
     * Enregistre un changement de statut : seuls {@code status} et {@code updatedAt} sont écrits. Le port ne
     * revalide pas la transition : il fait confiance à son unique appelant (même règle que D-AX).
     */
    ContactMessage updateStatus(ContactMessage message);

    /**
     * Efface le message, immédiatement (F30, D-EX).
     */
    void delete(ContactMessage message);

    boolean existsAny();

    /**
     * Messages du statut donné ({@code null} : tous), les plus récents d'abord (date de réception puis identifiant
     * décroissants : tri total).
     */
    PageResult<ContactMessage> findPage(ContactStatus status, PageQuery query);
}
