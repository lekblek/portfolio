-- Couverture des séries (étape 27.4, D-BE, D-BY) : référence facultative vers le catalogue media.
-- Le module series ne stocke que l'identifiant du média (ADR 0002).

ALTER TABLE series
  ADD COLUMN cover_media_id BIGINT,
  -- Invariant 13 : un média référencé ne peut pas être supprimé.
  ADD CONSTRAINT series_cover_media_fk
    FOREIGN KEY (cover_media_id) REFERENCES media (id) ON DELETE RESTRICT;
