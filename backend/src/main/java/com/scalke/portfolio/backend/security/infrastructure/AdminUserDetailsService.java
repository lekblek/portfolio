package com.scalke.portfolio.backend.security.infrastructure;

import com.scalke.portfolio.backend.security.domain.model.AdminAccount;
import com.scalke.portfolio.backend.security.domain.port.AdminAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Le compte administrateur unique vu par Spring Security (D-CO) : rôle {@code ADMIN}, désactivé si
 * {@code enabled} est faux. Un identifiant inconnu lève {@link UsernameNotFoundException}, que
 * {@code DaoAuthenticationProvider} transforme en mauvais identifiants, après un calcul bcrypt factice pour que
 * la durée de réponse ne révèle pas l'identifiant.
 */
@Component
@RequiredArgsConstructor
class AdminUserDetailsService implements UserDetailsService {

    private final AdminAccountRepository adminAccountRepository;

    @Override
    public UserDetails loadUserByUsername(String login) {
        AdminAccount account = adminAccountRepository.find()
            .filter(candidate -> candidate.login().equals(login))
            .orElseThrow(() -> new UsernameNotFoundException("unknown administrator"));
        return User.withUsername(account.login())
            .password(account.passwordHash())
            .roles("ADMIN")
            .disabled(!account.enabled())
            .build();
    }
}
