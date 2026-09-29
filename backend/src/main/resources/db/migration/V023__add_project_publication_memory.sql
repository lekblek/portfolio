-- Étape 36.5 (D-CX) : mémoire de la publication d'un projet, base de la stabilité de son slug (D11, D-BC).
-- Un projet déjà publié garde son slug, même archivé ou repassé en brouillon.

ALTER TABLE project
  ADD COLUMN ever_published BOOLEAN NOT NULL DEFAULT FALSE;

-- Reprise des lignes existantes : un projet publié ou archivé est tenu pour avoir été public.
UPDATE project
SET ever_published = TRUE
WHERE visibility IN ('PUBLISHED', 'ARCHIVED');

ALTER TABLE project
  ADD CONSTRAINT project_ever_published_check
    CHECK (visibility <> 'PUBLISHED' OR ever_published),
  -- KI-31 appliqué aux projets : le document de recherche (V019) reste loin de la limite d'un tsvector.
  ADD CONSTRAINT project_description_length_check
    CHECK (char_length(description_markdown) <= 100000);
