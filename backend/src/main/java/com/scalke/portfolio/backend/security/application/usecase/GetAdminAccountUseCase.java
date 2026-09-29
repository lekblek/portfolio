package com.scalke.portfolio.backend.security.application.usecase;

import com.scalke.portfolio.backend.security.domain.model.AdminAccount;
import com.scalke.portfolio.backend.security.domain.port.AdminAccountRepository;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Le compte de l'administrateur connecté (D-CO). Appelé seulement derrière l'authentification : le compte existe
 * forcément, sauf s'il a été supprimé de la base pendant la session.
 */
@Service
@RequiredArgsConstructor
public class GetAdminAccountUseCase {

    private final AdminAccountRepository adminAccountRepository;

    @Transactional(readOnly = true)
    public AdminAccount execute() {
        return adminAccountRepository.find()
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Compte introuvable."));
    }
}
