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

SELECT
  *,
  (year || '-' || month || '-' || day) :: TIMESTAMP
FROM daily_top_nodes('APC', '2016-01-01');

-- get tree element data from a tree

DROP FUNCTION IF EXISTS tree_element_data_from_node( BIGINT );
CREATE FUNCTION tree_element_data_from_node(root_node BIGINT)
  RETURNS TABLE(tree_id     BIGINT, parent_id BIGINT, node_id BIGINT, instance_id BIGINT, name_id BIGINT,
                simple_name TEXT, display TEXT, prev_node_id BIGINT, tree_path TEXT, name_path TEXT, rank_path JSONB)
LANGUAGE SQL
AS $$
WITH RECURSIVE treewalk (tree_id, parent_id, node_id, instance_id, name_id, simple_name, display, prev_node_id, tree_path, name_path, rank_path) AS (
  SELECT
    tree.id        AS tree_id,
    NULL :: BIGINT AS parent_id,
    node.id        AS node_id,
    node.instance_id,
    node.name_id,
    ''             AS simple_name,
    ''             AS display,
    node.prev_node_id,
    '' :: TEXT     AS tree_path,
    '' :: TEXT     AS name_path,
    '{}' :: JSONB  AS rank_path,
    0              AS depth
  FROM tree_node node
    JOIN tree_arrangement tree ON node.tree_arrangement_id = tree.id
  WHERE node.id = root_node
  UNION ALL
  SELECT
    treewalk.tree_id,
    treewalk.node_id                                                                                   AS parent_id,
    node.id                                                                                            AS node_id,
    node.instance_id                                                                                   AS instance_id,
    node.name_id                                                                                       AS name_id,
    name.simple_name :: TEXT                                                                           AS simple_name,
    '<indent class="' || depth + 1 || '"></indent>' || name.full_name_html || ' ' || ref.citation_html AS display,
    node.prev_node_id                                                                                  AS prev_node_id,
    treewalk.tree_path || '/' || node_id                                                               AS tree_path,
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

SELECT *
FROM tree_element_data_from_node(5451502)
WHERE simple_name LIKE 'Asplenium %';

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

SELECT *
FROM tree_version;

UPDATE tree t
SET current_tree_version_id = (SELECT max(id)
                               FROM tree_version v
                               WHERE v.tree_id = t.id)
WHERE name = 'APC';

-- create elements

INSERT INTO tree_element
(tree_version_id,
 tree_element_id,
 lock_version,
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

UPDATE tree_element ce
SET previous_element_id = pe.tree_element_id,
  previous_version_id   = pe.tree_version_id
FROM tree_element pe
WHERE ce.parent_version_id - 1 = pe.tree_version_id AND ce.name_id = pe.name_id;


SELECT *
FROM tree_element, tree t
WHERE t.name = 'APC'
      AND tree_version_id = 136
ORDER BY name_path;

-- look at changes to the tree structure over time
SELECT
  te.tree_version_id,
  pe.name_path,
  '->',
  te.name_path
FROM tree_element te
  JOIN tree_element pe ON te.previous_version_id = pe.tree_version_id AND te.previous_element_id = pe.tree_element_id
WHERE te.name_path <> pe.name_path
ORDER BY te.tree_version_id, te.name_path;

-- what doesn't have a previous version
SELECT
  tree_version_id,
  tree_element_id,
  name_path,
  simple_name
FROM tree_element
WHERE previous_version_id IS NULL AND tree_version_id > 1
ORDER BY tree_version_id, name_path;

-- validate that the tree matches
WITH RECURSIVE treewalk AS (
  SELECT
    tree_version_id,
    tree_element_id,
    '/' || name.name_element :: TEXT AS path,
    name_path
  FROM tree_element top
    JOIN name ON top.name_id = name.id
  WHERE tree_version_id = 137 AND parent_element_id IS NULL
  UNION ALL
  SELECT
    next_el.tree_version_id,
    next_el.tree_element_id,
    treewalk.path || '/' || name.name_element,
    next_el.name_path
  FROM treewalk
    JOIN tree_element next_el
      ON next_el.tree_version_id = treewalk.tree_version_id AND next_el.parent_element_id = treewalk.tree_element_id
    JOIN name ON next_el.name_id = name.id
)
SELECT *
FROM treewalk
WHERE path <> name_path;

SELECT
  name_path,
  simple_name
FROM tree_element
WHERE tree_version_id = 137 AND name_path LIKE '%/Calytrix%'
ORDER BY name_path;

SELECT
  name_path,
  simple_name
FROM tree_element
WHERE tree_version_id = 137 AND tree_element_id = 7845073;