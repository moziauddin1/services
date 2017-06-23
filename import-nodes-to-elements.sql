-- get current classification-root and follow the prev links back to find versions back to jan 2015
DROP FUNCTION IF EXISTS daily_top_nodes( TEXT, TIMESTAMP );
CREATE FUNCTION daily_top_nodes(tree_label TEXT, since TIMESTAMP)
  RETURNS TABLE(latest_node_id BIGINT, year DOUBLE PRECISION, month DOUBLE PRECISION, day DOUBLE PRECISION)
LANGUAGE SQL
AS $$

WITH RECURSIVE treewalk AS (
  SELECT class_root.*
  FROM tree_node class_node
    JOIN tree_arrangement a ON class_node.id = a.node_id AND a.label = tree_label
    JOIN tree_link sublink ON class_node.id = sublink.supernode_id
    JOIN tree_node class_root ON sublink.subnode_id = class_root.id
  UNION ALL
  SELECT node.*
  FROM treewalk
    JOIN tree_node node ON treewalk.prev_node_id = node.id
)
SELECT
  max(tw.id) AS latest_node_id,
  year,
  month,
  day
FROM treewalk tw
  JOIN tree_event event ON tw.checked_in_at_id = event.id
  ,
      extract(YEAR FROM event.time_stamp) AS year,
      extract(MONTH FROM event.time_stamp) AS month,
      extract(DAY FROM event.time_stamp) AS day
WHERE event.time_stamp > since
GROUP BY year, month, day
ORDER BY latest_node_id ASC
$$;

-- SELECT
--   *,
--   (year || '-' || month || '-' || day) :: TIMESTAMP
-- FROM daily_top_nodes('APC', '2016-01-01');

-- get tree element data from a tree

DROP FUNCTION IF EXISTS tree_element_data_from_node( BIGINT );
CREATE FUNCTION tree_element_data_from_node(root_node BIGINT)
  RETURNS TABLE(tree_id     BIGINT, parent_id BIGINT, node_id BIGINT, excluded BOOLEAN, instance_id BIGINT, name_id BIGINT,
                simple_name TEXT, display TEXT, prev_node_id BIGINT, tree_path TEXT, name_path TEXT, rank_path JSONB)
LANGUAGE SQL
AS $$
WITH RECURSIVE treewalk (tree_id, parent_id, node_id, excluded, instance_id, name_id, simple_name, display, prev_node_id, tree_path, name_path, rank_path) AS (
  SELECT
    tree.id                                                                                    AS tree_id,
    NULL :: BIGINT                                                                             AS parent_id,
    node.id                                                                                    AS node_id,
    (node.type_uri_id_part <> 'ApcConcept') :: BOOLEAN                                         AS excluded,
    node.instance_id                                                                           AS instance_id,
    node.name_id                                                                               AS name_id,
    name.simple_name :: TEXT                                                                   AS simple_name,
    '<indent class="' || 0 || '"></indent>' || name.full_name_html || ' ' || ref.citation_html AS display,
    node.prev_node_id                                                                          AS prev_node_id,
    node.id :: TEXT                                                                            AS tree_path,
    coalesce(name.name_element, '?') :: TEXT                                                   AS name_path,
    jsonb_build_object(rank.name, name.name_element) :: JSONB                                  AS rank_path,
    0                                                                                          AS depth
  FROM tree_link link
    JOIN tree_node node ON link.subnode_id = node.id
    JOIN tree_arrangement tree ON node.tree_arrangement_id = tree.id
    JOIN name ON node.name_id = name.id
    JOIN name_rank rank ON name.name_rank_id = rank.id
    JOIN instance inst ON node.instance_id = inst.id
    JOIN reference ref ON inst.reference_id = ref.id
  WHERE link.supernode_id = root_node
        AND node.internal_type = 'T'
  UNION ALL
  SELECT
    treewalk.tree_id                                                                                   AS tree_id,
    treewalk.node_id                                                                                   AS parent_id,
    node.id                                                                                            AS node_id,
    (node.type_uri_id_part <> 'ApcConcept') :: BOOLEAN                                                 AS excluded,
    node.instance_id                                                                                   AS instance_id,
    node.name_id                                                                                       AS name_id,
    name.simple_name :: TEXT                                                                           AS simple_name,
    '<indent class="' || depth + 1 || '"></indent>' || name.full_name_html || ' ' || ref.citation_html AS display,
    node.prev_node_id                                                                                  AS prev_node_id,
    treewalk.tree_path || '/' || node.id                                                               AS tree_path,
    treewalk.name_path || '/' || coalesce(name.name_element, '?')                                      AS name_path,
    treewalk.rank_path || jsonb_build_object(rank.name, name.name_element)                             AS rank_path,
    treewalk.depth + 1                                                                                 AS depth
  FROM treewalk
    JOIN tree_link link ON link.supernode_id = treewalk.node_id
    JOIN tree_node node ON link.subnode_id = node.id
    JOIN name ON node.name_id = name.id
    JOIN name_rank rank ON name.name_rank_id = rank.id
    JOIN instance inst ON node.instance_id = inst.id
    JOIN reference ref ON inst.reference_id = ref.id
  WHERE node.internal_type = 'T'
        AND node.tree_arrangement_id = treewalk.tree_id
)
SELECT
  tree_id,
  parent_id,
  node_id,
  excluded,
  instance_id,
  name_id,
  simple_name,
  display,
  prev_node_id,
  tree_path,
  name_path,
  rank_path
FROM treewalk
$$;

-- SELECT
--   *
-- FROM tree_element_data_from_node(9061740) ted
--   JOIN tree_node node ON node.id = ted.node_id
-- ORDER BY name_path;

VACUUM ANALYSE;

DROP SEQUENCE IF EXISTS test_seq;
CREATE SEQUENCE test_seq;

INSERT INTO tree (group_name, name) VALUES ('treebuilder', 'APC');

-- create versions

INSERT INTO tree_version
(id,
 lock_version,
 draft_name,
 log_entry,
 previous_version_id,
 published,
 published_at,
 published_by,
 tree_id)
  (SELECT
     nextval('test_seq' :: REGCLASS)                   AS id,
     0                                                 AS lock_version,
     'import'                                          AS draft_name,
     'import'                                          AS log_entry,
     NULL                                              AS previous_version_id,
     TRUE                                              AS published,
     (year || '-' || month || '-' || day) :: TIMESTAMP AS published_at,
     'import'                                          AS published_by,
     a.id                                              AS tree_id
   FROM daily_top_nodes('APC', '2016-01-01'), tree a
   WHERE a.name = 'APC');

UPDATE tree_version
SET previous_version_id = id - 1
WHERE id > (SELECT min(id)
            FROM tree_version);

-- SELECT *
-- FROM tree_version;

UPDATE tree t
SET current_tree_version_id = (SELECT max(id)
                               FROM tree_version v
                               WHERE v.tree_id = t.id)
WHERE name = 'APC';

VACUUM ANALYSE;

-- create elements
ALTER TABLE IF EXISTS tree_element
  DROP CONSTRAINT IF EXISTS FK_tb2tweovvy36a4bgym73jhbbk;

ALTER TABLE IF EXISTS tree_element
  DROP CONSTRAINT IF EXISTS FK_slpx4w0673tudgw4fcodauilv;

ALTER TABLE IF EXISTS tree_element
  DROP CONSTRAINT IF EXISTS FK_89rcrnlb8ed10mgp22d3cj646;

ALTER TABLE IF EXISTS tree_element
  DROP CONSTRAINT IF EXISTS FK_964uyddp8ju1ya5v2px9wx5tf;

ALTER TABLE IF EXISTS tree_element
  DROP CONSTRAINT IF EXISTS FK_kaotdsllnfojld6pdxb8c9gml;

INSERT INTO tree_element
(tree_version_id,
 tree_element_id,
 lock_version,
 excluded,
 display_string,
 element_link,
 instance_id,
 instance_link,
 name_id,
 name_link,
 parent_version_id,
 parent_element_id,
 previous_version_id,
 previous_element_id,
 profile,
 rank_path,
 simple_name,
 tree_path,
 name_path,
 updated_at,
 updated_by)
  (SELECT
     v.id                          AS tree_version_id,
     el_data.node_id               AS tree_element_id,
     0 :: BIGINT                   AS lock_version,
     el_data.excluded              AS excluded,
     el_data.display               AS display_string,
     ''                            AS element_link,
     el_data.instance_id :: BIGINT AS instance_id,
     ''                            AS instance_link,
     el_data.name_id :: BIGINT     AS name_id,
     ''                            AS name_link,
     CASE WHEN el_data.parent_id IS NOT NULL AND el_data.parent_id != dtn.latest_node_id
       THEN
         v.id
     ELSE NULL :: BIGINT
     END                           AS parentversionid,
     CASE WHEN el_data.parent_id IS NOT NULL AND el_data.parent_id != dtn.latest_node_id
       THEN
         el_data.parent_id :: BIGINT
     ELSE NULL :: BIGINT
     END                           AS parentelementid,
     NULL                          AS previousversionid,
     NULL                          AS previouselementid,
     ('{}' :: JSONB)               AS profile,
     el_data.rank_path :: JSONB    AS rank_path,
     el_data.simple_name           AS simple_name,
     el_data.tree_path             AS tree_path,
     el_data.name_path             AS name_path,
     v.published_at                AS updated_at,
     v.published_by                AS updated_by
   FROM daily_top_nodes('APC', '2016-01-01') AS dtn,
     tree_version v,
         tree_element_data_from_node(dtn.latest_node_id) AS el_data
   WHERE v.published_at = (dtn.year || '-' || dtn.month || '-' || dtn.day) :: TIMESTAMP
         AND instance_id IS NOT NULL);

VACUUM ANALYSE;

UPDATE tree_element ce
SET previous_element_id = pe.tree_element_id,
  previous_version_id   = pe.tree_version_id
FROM tree_element pe
WHERE ce.parent_version_id - 1 = pe.tree_version_id AND ce.name_id = pe.name_id;

VACUUM ANALYSE;

-- SELECT *
-- FROM tree_element, tree t
-- WHERE t.name = 'APC'
--       AND tree_version_id = (SELECT max(id)
--                              FROM tree_version
--                              WHERE tree_id = t.id)
-- ORDER BY name_path;

-- look at changes to the tree structure over time
-- SELECT
--   te.tree_version_id,
--   pe.name_path,
--   '->',
--   te.name_path
-- FROM tree_element te
--   JOIN tree_element pe ON te.previous_version_id = pe.tree_version_id AND te.previous_element_id = pe.tree_element_id
-- WHERE te.name_path <> pe.name_path
-- ORDER BY te.tree_version_id, te.name_path;

-- what doesn't have a previous version
-- SELECT
--   tree_version_id,
--   tree_element_id,
--   name_path,
--   simple_name
-- FROM tree_element
-- WHERE previous_version_id IS NULL AND tree_version_id > 1
-- ORDER BY tree_version_id, name_path;

-- validate that the tree matches
-- WITH RECURSIVE treewalk AS (
--   SELECT
--     tree_version_id,
--     tree_element_id,
--     '/' || name.name_element :: TEXT AS path,
--     name_path
--   FROM tree_element top
--     JOIN name ON top.name_id = name.id
--   WHERE tree_version_id = 137 AND parent_element_id IS NULL
--   UNION ALL
--   SELECT
--     next_el.tree_version_id,
--     next_el.tree_element_id,
--     treewalk.path || '/' || name.name_element,
--     next_el.name_path
--   FROM treewalk
--     JOIN tree_element next_el
--       ON next_el.tree_version_id = treewalk.tree_version_id AND next_el.parent_element_id = treewalk.tree_element_id
--     JOIN name ON next_el.name_id = name.id
-- )
-- SELECT *
-- FROM treewalk
-- WHERE path <> name_path;
--
-- SELECT
--   name_path,
--   simple_name
-- FROM tree_element
-- WHERE tree_version_id = 137 AND name_path LIKE '%/Calytrix%'
-- ORDER BY name_path;
--
-- SELECT
--   name_path,
--   simple_name
-- FROM tree_element
-- WHERE tree_version_id = 137 AND tree_element_id = 7845073;

-- count BTs by version in tree_elements
-- SELECT
--   v.id,
--   v.published_at,
--   count(tree_element_id) AS BTs
-- FROM tree_element element
--   JOIN tree_node node ON node.id = element.tree_element_id
--   JOIN tree_version v ON element.tree_version_id = v.id
-- WHERE node.type_uri_id_part = 'DeclaredBt'
-- GROUP BY v.id
-- ORDER BY v.id;

-- update all names with BT to point to the BTs parent
UPDATE tree_element element
SET parent_element_id = bt_element.parent_element_id,
  parent_version_id   = bt_element.parent_version_id
FROM tree_node node, tree_element bt_element
WHERE node.id = element.parent_element_id
      AND node.type_uri_id_part = 'DeclaredBt'
      AND bt_element.tree_element_id = element.parent_element_id
      AND bt_element.tree_version_id = element.tree_version_id;

-- count elements with bt parents - it should be zero
SELECT count(*) AS elements_with_bt_parents
FROM tree_element element
  JOIN tree_node node ON node.id = element.parent_element_id
  JOIN tree_version v ON element.tree_version_id = v.id
WHERE node.type_uri_id_part = 'DeclaredBt';

-- * delete BT elements *
--- clean up foreign keys
UPDATE tree_element
SET parent_element_id = NULL, parent_version_id = NULL, previous_version_id = NULL, previous_element_id = NULL
WHERE tree_element_id IN (SELECT id
                          FROM tree_node node
                          WHERE node.type_uri_id_part = 'DeclaredBt');

UPDATE tree_element
SET previous_version_id = NULL, previous_element_id = NULL
WHERE previous_element_id IN (SELECT id
                              FROM tree_node node
                              WHERE node.type_uri_id_part = 'DeclaredBt');


DELETE FROM tree_element
WHERE tree_element_id IN (SELECT id
                          FROM tree_node node
                          WHERE node.type_uri_id_part = 'DeclaredBt');

ALTER TABLE IF EXISTS tree_element
  ADD CONSTRAINT FK_tb2tweovvy36a4bgym73jhbbk
FOREIGN KEY (tree_version_id)
REFERENCES tree_version;

ALTER TABLE IF EXISTS tree_element
  ADD CONSTRAINT FK_slpx4w0673tudgw4fcodauilv
FOREIGN KEY (instance_id)
REFERENCES instance;

ALTER TABLE IF EXISTS tree_element
  ADD CONSTRAINT FK_89rcrnlb8ed10mgp22d3cj646
FOREIGN KEY (name_id)
REFERENCES name;

ALTER TABLE IF EXISTS tree_element
  ADD CONSTRAINT FK_964uyddp8ju1ya5v2px9wx5tf
FOREIGN KEY (parent_Version_Id, parent_Element_Id)
REFERENCES tree_element;

ALTER TABLE IF EXISTS tree_element
  ADD CONSTRAINT FK_kaotdsllnfojld6pdxb8c9gml
FOREIGN KEY (previous_Version_Id, previous_Element_Id)
REFERENCES tree_element;

VACUUM ANALYSE;

-- count of bt tree_elements should be zero
SELECT count(*)
FROM tree_element
WHERE tree_element_id IN (SELECT id
                          FROM tree_node node
                          WHERE node.type_uri_id_part = 'DeclaredBt');

-- shortcut set all the links
UPDATE tree_element el
SET element_link = 'http://' || host.host_name || '/node/apni/' || tree_element_id,
  name_link      = 'http://' || host.host_name || '/name/apni/' || name_id,
  instance_link  = 'http://' || host.host_name || '/instance/apni/' || instance_id
FROM mapper.host host
WHERE host.preferred;

-- temporarily set the element link
-- UPDATE tree_element el
-- SET element_link = 'http://' || host.host_name || '/' || url.uri
-- FROM mapper.host host, mapper.identifier ident
--   JOIN mapper.match url ON ident.preferred_uri_id = url.id
-- WHERE host.preferred
--       AND ident.id_number = el.tree_element_id
--       AND ident.object_type = 'node'
--       AND ident.name_space = 'apni';

-- set name link
-- UPDATE tree_element el
-- SET name_link = 'http://' || host.host_name || '/' || url.uri
-- FROM mapper.host host, mapper.identifier ident
--   JOIN mapper.match url ON ident.preferred_uri_id = url.id
-- WHERE host.preferred
--       AND ident.id_number = el.name_id
--       AND ident.object_type = 'name'
--       AND ident.name_space = 'apni';
--
-- EXPLAIN
-- UPDATE tree_element el
-- SET name_link = 'http://' || host.host_name || '/' || url.uri
-- FROM mapper.host host, mapper.identifier ident
--   JOIN mapper.match url ON ident.preferred_uri_id = url.id
-- WHERE host.preferred
--       AND ident.id_number = el.instance_id
--       AND ident.object_type = 'instance'
--       AND ident.name_space = 'apni';

VACUUM ANALYSE;

SELECT *
FROM tree_element
WHERE tree_version_id = 137
ORDER BY name_path;
