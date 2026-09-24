-- Classement des publications (étape 20) : au plus une catégorie (D20), des tags.
-- Le module publication ne stocke que les identifiants ; les termes appartiennent à taxonomy (D-AN).

ALTER TABLE publication
  ADD COLUMN category_id BIGINT,
  -- Invariant 25 : une catégorie utilisée ne peut pas être supprimée.
  ADD CONSTRAINT publication_category_fk
    FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE RESTRICT;

CREATE TABLE publication_tag (
                               publication_id BIGINT NOT NULL,
                               tag_id         BIGINT NOT NULL,

                               CONSTRAINT publication_tag_pk
                                 PRIMARY KEY (publication_id, tag_id),

                               CONSTRAINT publication_tag_publication_fk
                                 FOREIGN KEY (publication_id) REFERENCES publication (id) ON DELETE CASCADE,

                               -- Invariant 25 : un tag utilisé ne peut pas être supprimé.
                               CONSTRAINT publication_tag_tag_fk
                                 FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE RESTRICT
);
