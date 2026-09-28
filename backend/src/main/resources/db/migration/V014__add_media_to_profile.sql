-- Médias du profil (étape 27.3, D-B, D-BX) : avatar (image) et CV (PDF, D06), références vers le catalogue media.
-- Le module profile ne stocke que des identifiants de médias (ADR 0002). Les deux restent facultatifs : un
-- profil peut être publié avant l'envoi de ses fichiers.

ALTER TABLE profile
  ADD COLUMN avatar_media_id BIGINT,
  ADD COLUMN cv_media_id BIGINT,
  -- Invariant 13 : un média référencé ne peut pas être supprimé.
  ADD CONSTRAINT profile_avatar_media_fk
    FOREIGN KEY (avatar_media_id) REFERENCES media (id) ON DELETE RESTRICT,
  ADD CONSTRAINT profile_cv_media_fk
    FOREIGN KEY (cv_media_id) REFERENCES media (id) ON DELETE RESTRICT;
