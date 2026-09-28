-- Médias des projets (étape 27.2, D-AD, D-BV) : couverture et captures, références vers le catalogue media.
-- Le module project ne stocke que des identifiants de médias (ADR 0002).

ALTER TABLE project
  ADD COLUMN cover_media_id BIGINT,
  -- Invariant 13 : un média référencé ne peut pas être supprimé.
  ADD CONSTRAINT project_cover_media_fk
    FOREIGN KEY (cover_media_id) REFERENCES media (id) ON DELETE RESTRICT;

CREATE TABLE project_screenshot (
                                  project_id    BIGINT       NOT NULL,
                                  media_id      BIGINT       NOT NULL,
                                  caption       VARCHAR(300),
                                  display_order INTEGER      NOT NULL,

                                  -- Invariant 27 : une capture apparaît au plus une fois dans un projet.
                                  CONSTRAINT project_screenshot_pk
                                    PRIMARY KEY (project_id, media_id),

                                  CONSTRAINT project_screenshot_display_order_check
                                    CHECK (display_order >= 0),

                                  -- Composant du projet : supprimé avec lui.
                                  CONSTRAINT project_screenshot_project_fk
                                    FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE,

                                  -- Invariant 13.
                                  CONSTRAINT project_screenshot_media_fk
                                    FOREIGN KEY (media_id) REFERENCES media (id) ON DELETE RESTRICT
);
