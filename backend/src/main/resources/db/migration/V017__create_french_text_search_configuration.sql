-- Configuration linguistique de la recherche plein texte (étape 28, D-BZ).
-- Objet de niveau base, partagé par les modules qui indexent du texte (publication, project), comme
-- l'extension unaccent de V000. Toujours nommée explicitement : default_text_search_config dépend de
-- l'installation (english dans l'image postgres).

-- Mots vides français (liste french.stop du serveur, celle de french_stem), retirés AVANT les accents :
-- sinon « été », « à » et « où » deviendraient « ete », « a » et « ou », qui ne sont plus des mots vides.
-- ACCEPT = false : un mot qui n'est pas vide passe, inchangé, au dictionnaire suivant.
CREATE TEXT SEARCH DICTIONARY french_stopwords (
  TEMPLATE = pg_catalog.simple,
  STOPWORDS = french,
  ACCEPT = false
);

-- Copie de french. Les mots qui contiennent une lettre non ASCII sont filtrés, désaccentués, puis
-- racinisés : « Développement », « developpement » et « développer » donnent le même lexème. Les mots
-- ASCII gardent french_stem, qui écarte lui-même les mots vides.
CREATE TEXT SEARCH CONFIGURATION french_unaccent (COPY = pg_catalog.french);

ALTER TEXT SEARCH CONFIGURATION french_unaccent
  ALTER MAPPING FOR word, hword, hword_part
  WITH french_stopwords, unaccent, french_stem;
