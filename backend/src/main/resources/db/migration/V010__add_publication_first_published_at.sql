-- Étape 22 : mémoire durable de la première publication (D-AZ), base de la stabilité des slugs (D11).
-- published_at reste la date affichée ; une replanification la remplace. first_published_at ne change
-- plus dès qu'elle est passée : elle survit à tout enchaînement de transitions (constat A01 de l'audit
-- du 2026-09-25 : ARCHIVED → DRAFT → SCHEDULED → DRAFT effaçait toute trace d'une publication passée).

ALTER TABLE publication
  ADD COLUMN first_published_at TIMESTAMPTZ;

-- Reprise des lignes existantes : la date de publication présente est la meilleure trace disponible.
UPDATE publication
SET first_published_at = published_at
WHERE published_at IS NOT NULL;

ALTER TABLE publication
  -- Planifiée, publiée ou archivée : une première date existe (provisoire tant qu'elle est future).
  ADD CONSTRAINT publication_first_published_at_check
    CHECK (status NOT IN ('SCHEDULED', 'PUBLISHED', 'ARCHIVED') OR first_published_at IS NOT NULL),
  ADD CONSTRAINT publication_first_published_before_published_check
    CHECK (first_published_at IS NULL OR published_at IS NULL OR first_published_at <= published_at);
