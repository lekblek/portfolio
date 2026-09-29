-- Étape 36.3 (D-CU, KI-31) : longueur maximale du contenu d'une publication, vérifiée aussi par le domaine.
-- Un tsvector est limité à 1 Mio : un contenu démesuré serait refusé par la colonne générée search_vector (V018)
-- au lieu d'une erreur de validation. 100 000 caractères : environ 15 000 mots, loin de cette limite.

ALTER TABLE publication
  ADD CONSTRAINT publication_content_length_check
    CHECK (char_length(content_markdown) <= 100000);
