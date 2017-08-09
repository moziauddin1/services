-- add all tree_element identifiers
INSERT INTO mapper.identifier (id, id_number, version_number, name_space, object_type, deleted, reason_deleted, updated_at, updated_by, preferred_uri_id)
  SELECT
    nextval('mapper.mapper_sequence'),
    tree_element_id,
    tree_version_id,
    'apni',
    'tree',
    FALSE,
    NULL,
    now(),
    'pmcneil',
    NULL
  FROM tree_element;

-- create default match links
INSERT INTO mapper.match (id, uri, deprecated, updated_at, updated_by)
  SELECT
    nextval('mapper.mapper_sequence'),
    (object_type || '/' || version_number || '/' || id_number :: TEXT),
    FALSE,
    now(),
    'pmcneil'
  FROM mapper.identifier
  WHERE object_type = 'tree' AND version_number IS NOT NULL;

-- or make the insert statements from a separate DB
-- SELECT
--   'INSERT INTO mapper.identifier (id, id_number, version_number, name_space, object_type, deleted, reason_deleted, updated_at, updated_by, preferred_uri_id) values (nextval(''mapper.mapper_sequence''),'
--   ||
--   tree_element_id || ',' ||
--   tree_version_id || ', ''apni'', ''tree'', FALSE, NULL, now(),''pmcneil'', NULL);'
-- FROM tree_element;
--
-- SELECT
--   'INSERT INTO mapper.match (id, uri, deprecated, updated_at, updated_by) values (nextval(''mapper.mapper_sequence''), '''
--   ||
--   (object_type || '/' || version_number || '/' || id_number :: TEXT) ||
--   ''', FALSE, now(), ''pmcneil'');'
-- FROM mapper.identifier
-- WHERE object_type = 'tree' AND version_number IS NOT NULL;

-------------8<--------

-- set the identifier preferred uris
UPDATE mapper.identifier i
SET preferred_uri_id = m.id
FROM mapper.match m
WHERE i.object_type = 'tree' AND i.version_number IS NOT NULL
      AND m.uri = (i.object_type || '/' || i.version_number || '/' || i.id_number);

-- map identifiers to match uris
INSERT INTO mapper.identifier_identities (match_id, identifier_id)
  SELECT
    m.id,
    i.id
  FROM mapper.identifier i
    JOIN mapper.match m ON i.preferred_uri_id = m.id
  WHERE i.object_type = 'tree'
        AND NOT exists(SELECT 1
                       FROM mapper.identifier_identities ii
                       WHERE ii.identifier_id = i.id AND ii.match_id = m.id);

-- add the default host to all matches that don't have it.
INSERT INTO mapper.match_host (match_hosts_id, host_id)
  SELECT
    m.id,
    (SELECT h.id
     FROM mapper.host h
     WHERE h.preferred)
  FROM mapper.match m
  WHERE
    NOT exists(SELECT 1
               FROM mapper.match_host mh
               WHERE mh.match_hosts_id = m.id);
