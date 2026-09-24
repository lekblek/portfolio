-- Cycle de vie éditorial (étape 21) : une publication n'est archivée qu'après avoir été publiée ;
-- elle garde sa date de publication. Invariant 24 étendu à ARCHIVED (D-AV).

ALTER TABLE publication
  DROP CONSTRAINT publication_published_at_check,
  ADD CONSTRAINT publication_published_at_check
    CHECK (status NOT IN ('SCHEDULED', 'PUBLISHED', 'ARCHIVED') OR published_at IS NOT NULL);
