-- Document de recherche des publications (étape 28, D-CA, D-CB), configuration french_unaccent (V017).
-- Pondération (01 §11) : titre et tags A (forte), résumé B (moyenne), contenu D (normale, poids par défaut).
-- Chaque publication a son document, quel que soit son statut : la visibilité est évaluée à la lecture
-- (D-AH), si bien qu'une publication planifiée devient trouvable à sa date sans aucune écriture.

-- Noms des tags de la publication, dans l'ordre alphabétique. Une colonne générée ne lisant que sa ligne,
-- ils sont recopiés ici par PostgreSQL (déclencheurs ci-dessous), jamais par l'application ; l'entité JPA
-- ne mappe ni cette colonne ni le document.
ALTER TABLE publication
  ADD COLUMN tag_names TEXT NOT NULL DEFAULT '';

-- Invariant 28 : le document suit toujours le texte de la publication, calculé par PostgreSQL.
ALTER TABLE publication
  ADD COLUMN search_vector TSVECTOR NOT NULL GENERATED ALWAYS AS (
       setweight(to_tsvector('french_unaccent', title), 'A')
    || setweight(to_tsvector('french_unaccent', tag_names), 'A')
    || setweight(to_tsvector('french_unaccent', summary), 'B')
    || setweight(to_tsvector('french_unaccent', content_markdown), 'D')
  ) STORED;

CREATE INDEX publication_search_vector_idx
  ON publication USING GIN (search_vector);

-- Corps SQL standard (et non plpgsql) : PostgreSQL enregistre la dépendance à tag.name, si bien qu'une
-- migration du module taxonomy ne peut pas supprimer cette colonne sans traiter ce document.
CREATE FUNCTION publication_tag_names(target_id BIGINT) RETURNS TEXT
  LANGUAGE sql
  STABLE
  RETURN (SELECT coalesce(string_agg(t.name, ' ' ORDER BY t.name), '')
            FROM publication_tag pt
            JOIN tag t ON t.id = pt.tag_id
           WHERE pt.publication_id = target_id);

-- Tag ajouté, retiré ou remplacé : les noms des publications concernées sont recalculés. Un déclencheur
-- AFTER ... FOR EACH ROW s'exécute à la fin de l'instruction : il voit toutes les lignes qu'elle a modifiées.
CREATE FUNCTION publication_tag_changed() RETURNS TRIGGER
  LANGUAGE plpgsql
AS $$
BEGIN
  IF TG_OP IN ('INSERT', 'UPDATE') THEN
    UPDATE publication SET tag_names = publication_tag_names(id) WHERE id = NEW.publication_id;
  END IF;
  IF TG_OP IN ('DELETE', 'UPDATE') THEN
    UPDATE publication SET tag_names = publication_tag_names(id) WHERE id = OLD.publication_id;
  END IF;
  RETURN NULL;
END;
$$;

CREATE TRIGGER publication_tag_search_trigger
  AFTER INSERT OR UPDATE OR DELETE ON publication_tag
  FOR EACH ROW
  EXECUTE FUNCTION publication_tag_changed();

-- Tag renommé : posé par le module publication sur la table tag du module taxonomy, dans le sens du graphe
-- des modules (publication → taxonomy, comme publication_tag_tag_fk) ; taxonomy l'ignore (D-CB). Un tag
-- utilisé ne pouvant pas être supprimé (invariant 25), le renommage est le seul changement à propager.
CREATE FUNCTION publication_tag_renamed() RETURNS TRIGGER
  LANGUAGE plpgsql
AS $$
BEGIN
  UPDATE publication SET tag_names = publication_tag_names(id)
   WHERE id IN (SELECT publication_id FROM publication_tag WHERE tag_id = NEW.id);
  RETURN NULL;
END;
$$;

CREATE TRIGGER tag_publication_search_trigger
  AFTER UPDATE OF name ON tag
  FOR EACH ROW
  WHEN (OLD.name IS DISTINCT FROM NEW.name)
  EXECUTE FUNCTION publication_tag_renamed();

-- Publications déjà enregistrées (bases de développement).
UPDATE publication SET tag_names = publication_tag_names(id);
