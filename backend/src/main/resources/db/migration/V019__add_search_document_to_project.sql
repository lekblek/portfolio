-- Document de recherche des projets (étape 28, D-CA, D-CB), configuration french_unaccent (V017).
-- Même pondération que les publications (01 §11) : titre et technologies A (les technologies jouent pour un
-- projet le rôle des tags), description courte B, description D. Chaque projet a son document, quelle que
-- soit sa visibilité : seul PUBLISHED est lu par la recherche (invariant 11).

-- Noms des technologies du projet, dans l'ordre alphabétique, recopiés par PostgreSQL (déclencheurs
-- ci-dessous), jamais par l'application ; l'entité JPA ne mappe ni cette colonne ni le document.
ALTER TABLE project
  ADD COLUMN technology_names TEXT NOT NULL DEFAULT '';

-- Invariant 28 : le document suit toujours le texte du projet, calculé par PostgreSQL.
ALTER TABLE project
  ADD COLUMN search_vector TSVECTOR NOT NULL GENERATED ALWAYS AS (
       setweight(to_tsvector('french_unaccent', title), 'A')
    || setweight(to_tsvector('french_unaccent', technology_names), 'A')
    || setweight(to_tsvector('french_unaccent', short_description), 'B')
    || setweight(to_tsvector('french_unaccent', description_markdown), 'D')
  ) STORED;

CREATE INDEX project_search_vector_idx
  ON project USING GIN (search_vector);

-- Corps SQL standard : PostgreSQL enregistre la dépendance à technology.name.
CREATE FUNCTION project_technology_names(target_id BIGINT) RETURNS TEXT
  LANGUAGE sql
  STABLE
  RETURN (SELECT coalesce(string_agg(t.name, ' ' ORDER BY t.name), '')
            FROM project_technology pt
            JOIN technology t ON t.id = pt.technology_id
           WHERE pt.project_id = target_id);

-- Technologie ajoutée, retirée ou remplacée (exécuté à la fin de l'instruction, comme pour les tags).
CREATE FUNCTION project_technology_changed() RETURNS TRIGGER
  LANGUAGE plpgsql
AS $$
BEGIN
  IF TG_OP IN ('INSERT', 'UPDATE') THEN
    UPDATE project SET technology_names = project_technology_names(id) WHERE id = NEW.project_id;
  END IF;
  IF TG_OP IN ('DELETE', 'UPDATE') THEN
    UPDATE project SET technology_names = project_technology_names(id) WHERE id = OLD.project_id;
  END IF;
  RETURN NULL;
END;
$$;

CREATE TRIGGER project_technology_search_trigger
  AFTER INSERT OR UPDATE OR DELETE ON project_technology
  FOR EACH ROW
  EXECUTE FUNCTION project_technology_changed();

-- Technologie renommée. Une technologie utilisée ne pouvant pas être supprimée (invariant 22), le
-- renommage est le seul changement à propager.
CREATE FUNCTION project_technology_renamed() RETURNS TRIGGER
  LANGUAGE plpgsql
AS $$
BEGIN
  UPDATE project SET technology_names = project_technology_names(id)
   WHERE id IN (SELECT project_id FROM project_technology WHERE technology_id = NEW.id);
  RETURN NULL;
END;
$$;

CREATE TRIGGER technology_project_search_trigger
  AFTER UPDATE OF name ON technology
  FOR EACH ROW
  WHEN (OLD.name IS DISTINCT FROM NEW.name)
  EXECUTE FUNCTION project_technology_renamed();

-- Projets déjà enregistrés (bases de développement).
UPDATE project SET technology_names = project_technology_names(id);
