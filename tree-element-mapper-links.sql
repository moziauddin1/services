DROP INDEX IF EXISTS identifier_id_version_object_index;
CREATE INDEX identifier_id_version_object_index
  ON mapper.identifier (id_number, object_type, version_number);

DROP INDEX IF EXISTS identifier_object_type_index;
CREATE INDEX identifier_object_type_index
  ON mapper.identifier (object_type);

-- add all tree_element identifiers
INSERT INTO mapper.identifier (id, id_number, version_number, name_space, object_type, deleted, reason_deleted, updated_at, updated_by, preferred_uri_id)
  SELECT
    nextval('mapper.mapper_sequence'),
    tree_element_id,
    tree_version_id,
    'apni',
    'treeElement',
    FALSE,
    NULL,
    now(),
    'pmcneil',
    NULL
  FROM tree_version_element;

-- create default match preferred uris /tree/treeVersionId/ElementId
INSERT INTO mapper.match (id, uri, deprecated, updated_at, updated_by)
  SELECT
    nextval('mapper.mapper_sequence'),
    ('tree/' || version_number || '/' || id_number :: TEXT),
    FALSE,
    now(),
    'pmcneil'
  FROM mapper.identifier
  WHERE object_type = 'treeElement';

UPDATE mapper.identifier i
SET preferred_uri_id = m.id
FROM mapper.match m
WHERE i.object_type = 'treeElement'
      AND m.uri = ('tree/' || i.version_number || '/' || i.id_number);

-- Add tree version links

INSERT INTO mapper.identifier (id, id_number, version_number, name_space, object_type, deleted, reason_deleted, updated_at, updated_by, preferred_uri_id)
  SELECT
    nextval('mapper.mapper_sequence'),
    id,
    NULL,
    'apni',
    'treeVersion',
    FALSE,
    NULL,
    now(),
    'pmcneil',
    NULL
  FROM tree_version;

-- create default match uri /tree/versionId
INSERT INTO mapper.match (id, uri, deprecated, updated_at, updated_by)
  SELECT
    nextval('mapper.mapper_sequence'),
    ('tree/' || id_number),
    FALSE,
    now(),
    'pmcneil'
  FROM mapper.identifier
  WHERE object_type = 'treeVersion';

UPDATE mapper.identifier i
SET preferred_uri_id = m.id
FROM mapper.match m
WHERE i.object_type = 'treeVersion'
      AND m.uri = ('tree/' || i.id_number);

-- Add tree links

INSERT INTO mapper.identifier (id, id_number, version_number, name_space, object_type, deleted, reason_deleted, updated_at, updated_by, preferred_uri_id)
  SELECT
    nextval('mapper.mapper_sequence'),
    id,
    NULL,
    'apni',
    'tree',
    FALSE,
    NULL,
    now(),
    'pmcneil',
    NULL
  FROM tree;

-- create default match uri /tree/namespace/tree.name
INSERT INTO mapper.match (id, uri, deprecated, updated_at, updated_by)
  SELECT
    nextval('mapper.mapper_sequence'),
    ('tree/' || i.name_space || '/' || t.name),
    FALSE,
    now(),
    'pmcneil'
  FROM tree t
    JOIN mapper.identifier i ON t.id = i.id_number AND i.object_type = 'tree';

UPDATE mapper.identifier i
SET preferred_uri_id = m.id
FROM tree t, mapper.match m
WHERE i.object_type = 'tree'
      AND i.id_number = t.id
      AND m.uri = ('tree/' || i.name_space || '/' || t.name);

--
-- map old node ids to new tree ids
DROP FUNCTION IF EXISTS map_nodes_element_identifiers();
CREATE FUNCTION map_nodes_element_identifiers()
  RETURNS TABLE(tree_element_id BIGINT, node_mid_id BIGINT, elem_mid_id BIGINT, tree_version BIGINT)
LANGUAGE SQL
AS $$
SELECT
  ipath.id,
  node_mid.id AS node_id,
  elem_mid.id AS elem_id,
  max(tvte.tree_version_id)
FROM instance_paths ipath
  JOIN tree_version_element tvte ON tvte.tree_element_id = ipath.id
  ,
      jsonb_array_elements(ipath.nodes) AS node,
  mapper.identifier node_mid,
  mapper.identifier elem_mid
WHERE to_jsonb(node_mid.id_number) = node
      AND node_mid.object_type = 'node'
      AND elem_mid.id_number = ipath.id
      AND elem_mid.version_number = tvte.tree_version_id
      AND elem_mid.object_type = 'treeElement'
GROUP BY ipath.id, node_mid.id, elem_mid.id, node;
$$;

-- UPDATE mapper.identifier_identities ii
-- SET identifier_id = mids.elem_mid_id
-- FROM map_nodes_element_identifiers() mids
-- WHERE ii.identifier_id = mids.node_mid_id;

-- global updates
-- join matches to identifiers
INSERT INTO mapper.identifier_identities (match_id, identifier_id)
  SELECT
    m.id,
    i.id
  FROM mapper.identifier i
    JOIN mapper.match m ON i.preferred_uri_id = m.id
  WHERE i.object_type LIKE 'tree%'
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