-- Compte administrateur unique (étape 33, D-CM, D-CN), initialisé depuis la configuration (D18).

CREATE TABLE admin_account (
                             id            BIGINT       PRIMARY KEY DEFAULT 1,
                             login         VARCHAR(100) NOT NULL,
                             password_hash VARCHAR(100) NOT NULL,
                             enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
                             last_login_at TIMESTAMPTZ,
                             created_at    TIMESTAMPTZ  NOT NULL,
                             updated_at    TIMESTAMPTZ  NOT NULL,

                             -- Invariant 30 : il existe au plus un compte administrateur (même principe que le profil).
                             CONSTRAINT admin_account_single_row
                               CHECK (id = 1),

                             CONSTRAINT admin_account_login_check
                               CHECK (login ~ '^[A-Za-z0-9._@+-]{3,100}$'),

                             -- Invariant 15 : seule une empreinte bcrypt au format de DelegatingPasswordEncoder est acceptée,
                             -- jamais un mot de passe en clair. Changer d'algorithme exigera une migration élargissant ce CHECK.
                             CONSTRAINT admin_account_password_hash_check
                               CHECK (password_hash ~ '^\{bcrypt\}\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$'),

                             CONSTRAINT admin_account_dates_check
                               CHECK (updated_at >= created_at)
);
