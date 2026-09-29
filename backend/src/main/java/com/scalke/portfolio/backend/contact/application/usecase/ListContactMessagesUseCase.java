package com.scalke.portfolio.backend.contact.application.usecase;

import com.scalke.portfolio.backend.contact.domain.model.ContactMessage;
import com.scalke.portfolio.backend.contact.domain.model.ContactStatus;
import com.scalke.portfolio.backend.contact.domain.port.ContactMessageRepository;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Boîte de réception de l'administration (D-CZ) : messages d'un statut ou tous, les plus récents d'abord.
 */
@Service
@RequiredArgsConstructor
public class ListContactMessagesUseCase {

    private final ContactMessageRepository contactMessageRepository;

    /**
     * @param status statut recherché ; {@code null} : tous les messages
     */
    @Transactional(readOnly = true)
    public PageResult<ContactMessage> execute(ContactStatus status, PageQuery query) {
        return contactMessageRepository.findPage(status, query);
    }
}
