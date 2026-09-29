package com.scalke.portfolio.backend.contact.web.controller;

import com.scalke.portfolio.backend.contact.application.usecase.ChangeContactMessageStatusUseCase;
import com.scalke.portfolio.backend.contact.application.usecase.GetContactMessageUseCase;
import com.scalke.portfolio.backend.contact.application.usecase.ListContactMessagesUseCase;
import com.scalke.portfolio.backend.contact.domain.model.ContactStatus;
import com.scalke.portfolio.backend.contact.web.dto.AdminContactMessageResponse;
import com.scalke.portfolio.backend.contact.web.dto.AdminContactMessageSummaryResponse;
import com.scalke.portfolio.backend.contact.web.dto.ChangeContactMessageStatusRequest;
import com.scalke.portfolio.backend.shared.api.ApiPaging;
import com.scalke.portfolio.backend.shared.api.PageResponse;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Boîte de réception des messages de contact (D-CZ), derrière la session de l'administrateur : liste filtrable par
 * statut, lecture, avancement du statut par sa route propre (invariant 29). Pas de suppression : l'archivage range un
 * message traité.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/contact-messages")
public class AdminContactMessageController {

    private final ListContactMessagesUseCase listContactMessagesUseCase;
    private final GetContactMessageUseCase getContactMessageUseCase;
    private final ChangeContactMessageStatusUseCase changeContactMessageStatusUseCase;

    /**
     * {@code ?status=NEW} : les messages de ce statut seulement ; valeur inconnue → 400 {@code MALFORMED_REQUEST}.
     */
    @GetMapping
    PageResponse<AdminContactMessageSummaryResponse> list(
        @RequestParam(required = false) ContactStatus status,
        @PageableDefault(size = ApiPaging.ADMIN_PAGE_SIZE) Pageable pageable) {
        PageQuery query = new PageQuery(pageable.getPageNumber(), pageable.getPageSize());
        return PageResponse.from(listContactMessagesUseCase.execute(status, query)
            .map(AdminContactMessageSummaryResponse::from));
    }

    @GetMapping("/{id}")
    AdminContactMessageResponse get(@PathVariable Long id) {
        return AdminContactMessageResponse.from(getContactMessageUseCase.execute(id));
    }

    /**
     * 409 {@code INVALID_CONTACT_MESSAGE_TRANSITION} si le statut visé n'est pas après le statut actuel.
     */
    @PostMapping("/{id}/status")
    AdminContactMessageResponse changeStatus(@PathVariable Long id,
                                             @Valid @RequestBody ChangeContactMessageStatusRequest body) {
        return AdminContactMessageResponse.from(changeContactMessageStatusUseCase.execute(id, body.status()));
    }
}
