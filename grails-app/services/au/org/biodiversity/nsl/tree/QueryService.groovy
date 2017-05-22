/*
    Copyright 2015 Australian National Botanic Gardens

    This file is part of NSL tree services plugin project.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy
    of the License at http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package au.org.biodiversity.nsl.tree

import au.org.biodiversity.nsl.*
import grails.transaction.Transactional
import org.hibernate.SessionFactory

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp

import static au.org.biodiversity.nsl.tree.HibernateSessionUtils.*

@Transactional
class QueryService {
    static datasource = 'nsl'

    SessionFactory sessionFactory_nsl

    static class Statistics {
        public int nodesCt
        public int currentNodesCt
        public int typesCt
        public int currentTypesCt
        public int namesCt
        public int currentNamesCt
        public int taxaCt
        public int currentTaxaCt
        public Set<Arrangement> dependsOn = new HashSet<Arrangement>()
        public Set<Arrangement> dependants = new HashSet<Arrangement>()
    }


    Statistics getStatistics(Arrangement r) {
        return doWork(sessionFactory_nsl, { Connection cnct ->
            final Statistics s = new Statistics()

            s.nodesCt = withQResult(cnct, '''
					select count(*) from tree_node where tree_arrangement_id = ?
					''') { PreparedStatement stmt ->
                stmt.setLong(1, r.id)
            } as Integer
            s.currentNodesCt = withQResult(cnct, '''
					select count(*) from tree_node where tree_arrangement_id = ?
					and next_node_id is null
					''') { PreparedStatement stmt ->
                stmt.setLong(1, r.id)
            } as Integer
            s.typesCt = withQResult(cnct, '''
					select count(*) from (
						select distinct type_uri_ns_part_id, type_uri_id_part
						from tree_node where tree_arrangement_id = ?
					) subq
					''') { PreparedStatement stmt ->
                stmt.setLong(1, r.id)
            } as Integer
            s.currentTypesCt = withQResult(cnct, '''
					select count(*) from (
						select distinct type_uri_ns_part_id, type_uri_id_part
						from tree_node where tree_arrangement_id = ?
						and next_node_id is null
					) subq
					''') { PreparedStatement stmt ->
                stmt.setLong(1, r.id)
            } as Integer
            s.namesCt = withQResult(cnct, '''
					select count(*) from (
						select distinct name_uri_ns_part_id, name_uri_id_part
						from tree_node where tree_arrangement_id = ?
					) subq
					''') { PreparedStatement stmt ->
                stmt.setLong(1, r.id)
            } as Integer
            s.currentNamesCt = withQResult(cnct, '''
					select count(*) from (
						select distinct name_uri_ns_part_id, name_uri_id_part
						from tree_node where tree_arrangement_id = ?
						and next_node_id is null
					) subq
					''') { PreparedStatement stmt ->
                stmt.setLong(1, r.id)
            } as Integer
            s.taxaCt = withQResult(cnct, '''
					select count(*) from (
						select distinct taxon_uri_ns_part_id, taxon_uri_id_part
						from tree_node where tree_arrangement_id = ?
					) subq
					''') { PreparedStatement stmt ->
                stmt.setLong(1, r.id)
            } as Integer
            s.currentTaxaCt = withQResult(cnct, '''
					select count(*) from (
						select distinct taxon_uri_ns_part_id, taxon_uri_id_part
						from tree_node where tree_arrangement_id = ?
						and next_node_id is null
					) subq
					''') { PreparedStatement stmt ->
                stmt.setLong(1, r.id)
            } as Integer

            get_dependencies r, s, cnct

            return s
        }) as Statistics
    }

    Statistics getDependencies(Arrangement r) {
        return doWork(sessionFactory_nsl, { Connection cnct ->
            final Statistics s = new Statistics()
            get_dependencies r, s, cnct
            return s
        }) as Statistics
    }

    @SuppressWarnings("ChangeToOperator")
    private static void get_dependencies(final Arrangement r, final Statistics s, final Connection cnct) {
        withQ(cnct, '''
SELECT DISTINCT n2.tree_arrangement_id
FROM tree_node n1
  JOIN tree_link l ON n1.id = l.supernode_id
  JOIN tree_node n2 ON l.subnode_id = n2.id
WHERE n1.tree_arrangement_id = ?
      AND n1.tree_arrangement_id <> n2.tree_arrangement_id
				''') { PreparedStatement stmt ->
            stmt.setLong(1, r.id)
            ResultSet rs = stmt.executeQuery()
            while (rs.next()) {
                s.dependsOn.add(Arrangement.get(rs.getLong(1)))
            }
            rs.close()
        }

        withQ(cnct, '''
SELECT DISTINCT n1.tree_arrangement_id
FROM tree_node n2
  JOIN tree_link l ON l.subnode_id = n2.id
  JOIN tree_node n1 ON n1.id = l.supernode_id
WHERE
  n2.tree_arrangement_id = ?
  AND n1.tree_arrangement_id <> n2.tree_arrangement_id''') { PreparedStatement stmt ->
            stmt.setLong(1, r.id)
            ResultSet rs = stmt.executeQuery()
            while (rs.next()) {
                s.dependants.add(Arrangement.get(rs.getLong(1)))
            }
            rs.close()
        }
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    void dumpTrees(Node tree1, Node tree2) {
        log.info dumpNodes([tree1, tree2])
    }

    @SuppressWarnings("ChangeToOperator")
    String dumpNodes(Collection<Node> topNodes) {

        StringWriter out = new StringWriter()
        doWork(sessionFactory_nsl) { Connection cnct ->
            Set<Node> nodes = new HashSet<Node>()

            withQ(cnct, '''
WITH RECURSIVE n(id) AS (
  SELECT tree_node.id
  FROM tree_node
  WHERE id = ?
  UNION ALL
  SELECT subnode_id
  FROM n
    JOIN tree_link ON n.id = supernode_id
)
SELECT DISTINCT id
FROM n''') { PreparedStatement stmt ->
                topNodes.each { Node topNode ->
                    if (topNode) {
                        topNode.refresh()
                        stmt.setLong(1, topNode.id)
                        ResultSet rs = stmt.executeQuery()
                        try {
                            while (rs.next()) {
                                nodes.add Node.get(rs.getLong(1))
                            }
                        }
                        finally {
                            rs.close()
                        }
                    }
                }
            }

            nodes.each { Node n ->
                n.refresh()
                n.root.refresh()
                n.subLink.each { Link l ->
                    l.refresh()
                }
                n.supLink.each { Link l ->
                    l.refresh()
                }
            }


            Map<Node, Node> nodeEquivalenceClass = new HashMap<Node, Node>()
            Map<Node, Collection<Node>> equivalenceClassNodes = new HashMap<Node, Collection<Node>>()

            Closure makeEquivalent = { Node a, Node b ->
                if (nodes.contains(a) && nodes.contains(b) && nodeEquivalenceClass.get(a) != nodeEquivalenceClass.get(b)) {
                    Collection<Node> aclass = equivalenceClassNodes.get(nodeEquivalenceClass.get(a))
                    Collection<Node> bclass = equivalenceClassNodes.get(nodeEquivalenceClass.get(b))
                    aclass.each { Node aa -> nodeEquivalenceClass.put(aa, b) }
                    bclass.addAll(aclass)
                    aclass.clear()
                }
            }

            nodes.each { Node n ->
                nodeEquivalenceClass.put(n, n)
                equivalenceClassNodes.put(n, new HashSet<Node>())
                equivalenceClassNodes.get(n).add(n)
            }

            nodes.each { Node n ->
                if (n.next && !DomainUtils.isEndNode(n.next)) makeEquivalent(n, n.next)
                if (n.prev) makeEquivalent(n, n.prev)
            }


            Closure dumpnode = { Node n ->
                String lbl
                String style = n.next ? DomainUtils.isEndNode(n.next) ? 'dotted' : 'dashed' : 'normal'
                lbl = "${DomainUtils.getNodeUri(n).asQNameD()}"
                if (DomainUtils.getRawNodeTypeUri(n)) lbl = "${lbl}\\n${DomainUtils.getRawNodeTypeUri(n).asQNameD()}"
                if (DomainUtils.hasName(n)) lbl = "${lbl}\\n${DomainUtils.getNameUri(n).asQNameD()}"
                if (DomainUtils.hasTaxon(n)) lbl = "${lbl}\\n${DomainUtils.getTaxonUri(n).asQNameD()}"
                if (DomainUtils.hasResource(n)) lbl = "${lbl}\\n${DomainUtils.getResourceUri(n).asQNameD()}"
                if (n.literal) lbl = "${lbl}\\n<${n.literal}>"

                out.println("""${n.id} [shape=\"${DomainUtils.isCheckedIn(n) ? 'rectangle' : 'oval'}\", label=\"${
                    lbl
                }\", style=\"${style}\"];""")
            }


            out.println "digraph {"

            equivalenceClassNodes.each { Node n, Collection<Node> v ->
                if (!v.isEmpty()) {
                    if (v.size() == 1) {
                        dumpnode(n)
                    } else {
                        out.println "subgraph cluster_${n.id} {"
                        out.println 'rankdir = \"LR\";'
                        v.each { nn -> dumpnode(nn) }
                        out.println "}"
                    }
                }
            }

            nodes.each { Node n ->
                n.subLink.each { Link l ->
                    String colour
                    colour = "black"


                    String label = "${l.id}"
                    String taillabel = "${l.linkSeq}"

                    if (l.typeUriIdPart) {
                        label = "${label}\\n${l.typeUriIdPart}"
                    }

                    out.println """${l.supernode.id} -> ${l.subnode.id} [taillabel=\"${taillabel}\", label=\"${
                        label
                    }\", color=\"${colour}\"];"""
                }
                n.supLink.findAll { !nodes.contains(it.supernode) }.each { Link l ->
                    String colour
                    colour = "black"


                    String label = "${l.id}"
                    String taillabel = "${l.linkSeq}"

                    if (l.typeUriIdPart) {
                        label = "${label}\\n${l.typeUriIdPart}"
                    }

                    out.println """${l.supernode.id} -> ${l.subnode.id} [taillabel=\"${taillabel}\", label=\"${
                        label
                    }\", color=\"${colour}\"];"""
                }
            }
            nodes.each { Node n ->
                if (n.next && nodes.contains(n.next)) {
                    if (n.next.prev == n) {
                        out.println """${n.id} -> ${n.next.id} [constraint=\"true\", color=\"blue\"];"""
                    } else {
                        out.println """${n.id} -> ${
                            n.next.id
                        } [style=\"dashed\", constraint=\"true\", color=\"blue\"];"""
                    }
                }
                if (n.prev && nodes.contains(n.prev)) {
                    if (n.prev.next == n) {
                        // already done
                    } else {
                        out.println """${n.id} -> ${
                            n.prev.id
                        } [style=\"dashed\", constraint=\"true\", color=\"red\"];"""
                    }
                }
            }

            out.println "}"
        }
        out.toString()
    }

    @SuppressWarnings(["ChangeToOperator"])
    List<Link> getPathForNode(Arrangement a, Node n) {
        if (!a) throw new IllegalArgumentException("Arrangement not specified")
        if (!n) throw new IllegalArgumentException("Node not specified")

        Collection<Link> l = new ArrayList<Link>()
        doWork(sessionFactory_nsl) { Connection cnct ->
            withQ(cnct, '''
WITH RECURSIVE search_up(link_id, supernode_id, subnode_id) AS (
  SELECT
    l.id,
    l.supernode_id,
    l.subnode_id
  FROM tree_link l
  WHERE l.subnode_id = ?
  UNION ALL
  SELECT
    l.id,
    l.supernode_id,
    l.subnode_id
  FROM search_up
    JOIN tree_link l ON search_up.supernode_id = l.subnode_id
),
    search_down(link_id, subnode_id, depth) AS
  (
    SELECT
      search_up.link_id,
      search_up.subnode_id,
      0
    FROM tree_arrangement a
      JOIN search_up ON a.node_id = search_up.supernode_id
    WHERE a.id = ?
    UNION ALL
    SELECT
      search_up.link_id,
      search_up.subnode_id,
      search_down.depth + 1
    FROM search_down
      JOIN search_up ON search_down.subnode_id = search_up.supernode_id
  )
SELECT link_id
FROM search_down
ORDER BY depth''') { PreparedStatement sql ->
                sql.setLong(1, n.id)
                sql.setLong(2, a.id)
                ResultSet rs = sql.executeQuery()
                try {
                    while (rs.next()) {
                        l.add(Link.get(rs.getLong(1)))
                    }
                }
                finally {
                    rs.close()
                }
            }
        }

        return l
    }

    // find the latest parent node for the given node up to the root of
    // the tree that is belongs to

    @SuppressWarnings("GroovyUnusedDeclaration")
    List<Link> getLatestPathForNode(Node n) {
        Collection<Link> path = new ArrayList<Link>()
        for (; ;) {
            def links = n.supLink.findAll { it.supernode.root == n.root && it.supernode.checkedInAt != null }

            if (links) {
                Link mostrecent = links.first()
                links.each { Link it ->
                    if (it.supernode.checkedInAt.timeStamp > mostrecent.supernode.checkedInAt.timeStamp) {
                        mostrecent = it
                    }
                }

                path.add(mostrecent)
                n = mostrecent.supernode
            } else {
                break
            }
        }
        return path
    }

    Long getNextval() {
        return doWork(sessionFactory_nsl) { Connection cnct ->
            return withQResult(cnct, "select nextval('nsl_global_seq') as v") {}
        } as Long
    }

    @SuppressWarnings("ChangeToOperator")
    Timestamp getTimestamp() {
        return (Timestamp) doWork(sessionFactory_nsl, { Connection cnct ->
            return withQ(cnct, '''select localtimestamp as ts''', { PreparedStatement stmt ->
                ResultSet rs = stmt.executeQuery()
                rs.next()
                Timestamp ts = rs.getTimestamp(1)
                rs.close()
                return ts
            })
        })
    }

    /**
     * Find a List of Current Nodes that match the nameUri in the classification. In a correctly configured tree there
     * should only ever be one Current Node that matches the nameUri
     *
     * @param classification
     * @param nameUri
     * @return Collection of Node
     */
    Collection<Node> findCurrentNslName(Arrangement classification, Name name) {
        Node.executeQuery('''SELECT n
FROM Node n
WHERE root = :arrangement
      AND name = :name
      AND checkedInAt IS NOT NULL
      AND next IS NULL
''', [arrangement: classification, name: name])
    }

    /**
     * Find a List of Current Nodes that match the nameUri in the classification. In a correctly configured tree there
     * should only ever be one Current Node that matches the nameUri
     *
     * @param classification
     * @param nameUri
     * @return Collection of Node
     */
    Collection<Node> findCurrentName(Arrangement classification, Uri nameUri) {
        Node.executeQuery('''SELECT n
FROM Node n
WHERE root = :arrangement
      AND nameUriIdPart = :idPart
      AND nameUriNsPart = :namespace
      AND checkedInAt IS NOT NULL
      AND next IS NULL
''', [arrangement: classification, idPart: nameUri.idPart, namespace: nameUri.nsPart])
    }

    /**
     * Find a List of Current Nodes that match the taxonUri in the classification. In a correctly configured tree there
     * should only ever be one Current Node that matches the taxonUri
     *
     * @param classification
     * @param taxonUri
     * @return Collection of Node
     */
    Collection<Node> findCurrentNslInstance(Arrangement classification, Instance instance) {
        Node.executeQuery('''SELECT n
FROM Node n
WHERE root = :arrangement
      AND instance = :instance
      AND checkedInAt IS NOT NULL
      AND next IS NULL
''', [arrangement: classification, instance: instance])
    }

    /**
     * Find a List of Current Nodes that match the taxonUri in the classification. In a correctly configured tree there
     * should only ever be one Current Node that matches the taxonUri
     *
     * @param classification
     * @param taxonUri
     * @return Collection of Node
     */
    Collection<Node> findCurrentTaxon(Arrangement classification, Uri taxonUri) {
        Node.executeQuery('''SELECT n
FROM Node n
WHERE root = :arrangement
      AND taxonUriIdPart = :idPart
      AND taxonUriNsPart = :namespace
      AND checkedInAt IS NOT NULL
      AND next IS NULL
''', [arrangement: classification, idPart: taxonUri.idPart, namespace: taxonUri.nsPart])
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Collection<Link> findCurrentNamePlacement(Arrangement classification, Uri nameUri) {
        Link.executeQuery('''SELECT l
FROM Link l
WHERE
  supernode.root = :arrangement
  AND supernode.checkedInAt IS NOT NULL
  AND supernode.next IS NULL
  AND subnode.root = :arrangement
  AND subnode.nameUriIdPart = :idPart
  AND subnode.nameUriNsPart = :namespace
  AND subnode.checkedInAt IS NOT NULL
  AND subnode.next IS NULL
''', [arrangement: classification, idPart: nameUri.idPart, namespace: nameUri.nsPart])
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Collection<Link> findCurrentTaxonPlacement(Arrangement classification, Uri taxonUri) {
        Link.executeQuery('''SELECT l
FROM Link l
WHERE
  supernode.root = :arrangement
  AND supernode.checkedInAt IS NOT NULL
  AND supernode.next IS NULL
  AND subnode.root = :arrangement
  AND subnode.taxonUriIdPart = :idPart
  AND subnode.taxonUriNsPart = :namespace
  AND subnode.checkedInAt IS NOT NULL
  AND subnode.next IS NULL
''', [arrangement: classification, idPart: taxonUri.idPart, namespace: taxonUri.nsPart])
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Collection<Link> findCurrentNslNamePlacement(Arrangement classification, Name nslName) {
        Link.executeQuery('''SELECT l
FROM Link l
WHERE
  supernode.root = :arrangement
  AND supernode.checkedInAt IS NOT NULL
  AND supernode.next IS NULL
  AND subnode.root = :arrangement
  AND subnode.name = :nslName
  AND subnode.checkedInAt IS NOT NULL
  AND subnode.next IS NULL
''', [arrangement: classification, nslName: nslName])
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Collection<Link> findCurrentNslInstancePlacement(Arrangement classification, Instance nslInstance) {
        Link.executeQuery('''SELECT l
FROM Link l
WHERE
  supernode.root = :arrangement
  AND supernode.checkedInAt IS NOT NULL
  AND supernode.next IS NULL
  AND subnode.root = :arrangement
  AND subnode.instance = :nslInstance
  AND subnode.checkedInAt IS NOT NULL
  AND subnode.next IS NULL
''', [arrangement: classification, nslInstance: nslInstance])
    }

    @SuppressWarnings("ChangeToOperator")
    Link findCurrentNslNameInTreeOrBaseTree(Arrangement tree, Name name) {
        Link l = null

        doWork(sessionFactory_nsl) { Connection cnct ->
            withQ(cnct, "select find_name_in_tree(?, ?) as link_id") { PreparedStatement qry ->
                qry.setLong(1, name.id)
                qry.setLong(2, tree.id)
                ResultSet rs = qry.executeQuery()
                rs.next()
                l = Link.get(rs.getLong('link_id'))
                rs.close()
            }
        }

        return l
    }

    // this query returns the relationship instances where instance.hasSynonym foo
    @SuppressWarnings("ChangeToOperator")
    List<Instance> findSynonymsOfInstanceInTree(Arrangement tree, Instance instance) {
        List<Instance> l = new ArrayList<Instance>()

        //find instances that cite this instance and check if they are on this tree or the base tree


        doWork(sessionFactory_nsl) { Connection cnct ->
            withQ(cnct, '''
WITH RECURSIVE
    walk AS (
    SELECT
      instance.id  relationship_instance_id,
      tree_node.id node_id,
      tree_node.tree_arrangement_id
    FROM instance
      JOIN tree_node ON instance.cites_id = tree_node.instance_id
    WHERE instance.cited_by_id = ?
          AND tree_node.internal_type = 'T'
          AND tree_node.tree_arrangement_id IN (?, ?)
          AND tree_node.next_node_id IS NULL
    UNION ALL
    SELECT
      walk.relationship_instance_id,
      tree_node.id node_id,
      tree_node.tree_arrangement_id
    FROM walk
      JOIN tree_link ON tree_link.subnode_id = walk.node_id
      JOIN tree_node ON tree_link.supernode_id = tree_node.id
    WHERE tree_node.next_node_id IS NULL
          AND tree_node.tree_arrangement_id IN (?, ?)
          AND walk.tree_arrangement_id <> ?
  )
SELECT DISTINCT relationship_instance_id
FROM walk
WHERE tree_arrangement_id = ?''') { PreparedStatement qry ->
                qry.setLong(1, instance.id)
                qry.setLong(2, tree.id)
                qry.setLong(3, tree.baseArrangement == null ? tree.id : tree.baseArrangement.id)
                qry.setLong(4, tree.id)
                qry.setLong(5, tree.baseArrangement == null ? tree.id : tree.baseArrangement.id)
                qry.setLong(6, tree.id)
                qry.setLong(7, tree.id)


                ResultSet rs = qry.executeQuery()
                while (rs.next())
                    l.add(Instance.get(rs.getLong('relationship_instance_id')))
                rs.close()
            }
        }

        return l
    }

    @SuppressWarnings("ChangeToOperator")
    List<Instance> findInstancesHavingSynonymInTree(Arrangement tree, Instance instance) {
//        List<Instance> l = new ArrayList<Instance>()
        List<Instance> potential = Instance.executeQuery('select i.citedBy from Instance i where i.cites = :instance', [instance: instance])
        List<Instance> synonyms = Instance.executeQuery('''
select n.instance 
from Node n 
where n.next is null and n.root = :tree 
and n.instance in (:potential)''', [tree: tree, potential: potential])
//        doWork(sessionFactory_nsl) { Connection cnct ->
//            withQ(cnct, '''
//WITH RECURSIVE
//    walk AS (
//    SELECT
//      instance.id AS relationship_instance_id,
//      tree_node.id   node_id,
//      tree_node.tree_arrangement_id
//    FROM instance
//      JOIN tree_node ON tree_node.instance_id = instance.cited_by_id
//    WHERE instance.cites_id = ?
//          AND tree_node.internal_type = 'T'
//          AND tree_node.tree_arrangement_id IN (?, ?)
//          AND tree_node.next_node_id IS NULL
//    UNION ALL
//    SELECT
//      walk.relationship_instance_id,
//      tree_node.id node_id,
//      tree_node.tree_arrangement_id
//    FROM walk
//      JOIN tree_link ON tree_link.subnode_id = walk.node_id
//      JOIN tree_node ON tree_link.supernode_id = tree_node.id
//    WHERE tree_node.next_node_id IS NULL
//          AND tree_node.tree_arrangement_id IN (?, ?)
//          AND walk.tree_arrangement_id <> ?
//  )
//SELECT DISTINCT relationship_instance_id
//FROM walk
//WHERE tree_arrangement_id = ? ''') { PreparedStatement qry ->
//                qry.setLong(1, instance.id)
//                qry.setLong(2, tree.id)
//                qry.setLong(3, tree.baseArrangement == null ? tree.id : tree.baseArrangement.id)
//                qry.setLong(4, tree.id)
//                qry.setLong(5, tree.baseArrangement == null ? tree.id : tree.baseArrangement.id)
//                qry.setLong(6, tree.id)
//                qry.setLong(7, tree.id)
//
//
//                ResultSet rs = qry.executeQuery()
//                while (rs.next())
//                    l.add(Instance.get(rs.getLong('relationship_instance_id')))
//                rs.close()
//            }
//        }

        return synonyms
    }


    @SuppressWarnings(["ChangeToOperator", "GroovyUnusedDeclaration"])
    List findNamesInSubtree(Node node, String nameLike) {
        List l = []

        doWork sessionFactory_nsl, { Connection cnct ->

// well, this is rather nasty!
// find the names. find any current nodes whose instances have the name, or whose instances have
// a synonym with the name. then run up the current tree, until we stop at the node we are looking for.


            withQ cnct, '''
with recursive names as (
select id from name where LOWER(simple_name) like LOWER(?)
),
matching_nodes as (
  select tree_node.id tree_node_id, instance.id as instance_id
  from tree_node join instance on tree_node.instance_id = instance.id
  where instance.name_id in (select id from names)
  and tree_node.internal_type = 'T'
  and tree_node.replaced_at_id is null
  and tree_node.tree_arrangement_id ''' + (node.root.baseArrangement ? ' in (?,?)' : ' = ?') + '''
union all
  select tree_node.id tree_node_id, syn.id as instance_id
  from tree_node join instance on tree_node.instance_id = instance.id
    join instance as syn on instance.id = syn.cited_by_id
  where syn.name_id in (select id from names)
  and tree_node.internal_type = 'T'
  and tree_node.replaced_at_id is null
  and tree_node.tree_arrangement_id ''' + (node.root.baseArrangement ? ' in (?,?)' : ' = ?') + '''
)
,
tree_runner as (
  select matching_nodes.tree_node_id as running_node_id, matching_nodes.*
  from matching_nodes
union all
  select tree_link.supernode_id as running_node_id, tree_runner.tree_node_id, tree_runner.instance_id
  from tree_runner join tree_link on tree_link.subnode_id = tree_runner.running_node_id
  join tree_node on tree_link.supernode_id = tree_node.id
  where
  tree_runner.running_node_id <> ? -- clip the search
  and tree_node.replaced_at_id is null
)
select tree_runner.*
from tree_runner
where tree_runner.running_node_id = ?
			''', { PreparedStatement sql ->
                if (node.root.baseArrangement) {
                    sql.setString(1, nameLike)
                    sql.setLong(2, node.root.id)
                    sql.setLong(3, node.root.baseArrangement.id)
                    sql.setLong(4, node.root.id)
                    sql.setLong(5, node.root.baseArrangement.id)
                    sql.setLong(6, node.id)
                    sql.setLong(7, node.id)
                } else {
                    sql.setString(1, nameLike)
                    sql.setLong(2, node.root.id)
                    sql.setLong(3, node.root.id)
                    sql.setLong(4, node.id)
                    sql.setLong(5, node.id)
                }
                ResultSet rs = sql.executeQuery()
                try {
                    while (rs.next()) {
                        l.add([
                                node           : Node.get(rs.getLong('tree_node_id')),
                                matchedInstance: Instance.get(rs.getLong('instance_id'))
                        ])
                    }
                }
                finally {
                    rs.close()
                }
            }
        }

        return l

    }

    @SuppressWarnings(["ChangeToOperator", "GroovyUnusedDeclaration"])
    List findNamesDirectlyInSubtree(Node node, String nameLike) {

        /**
         * For some reason, simply removing second clause from the union in the findNamesInSubtree
         * query slows down this search. Postgres optimises something it shouldn't, and
         * I don't know what.
         * So instead, I have that nasty 'foo' column which seems to trick postgres
         * into doing it the right way.
         */

        List l = []

        doWork sessionFactory_nsl, { Connection cnct ->

// well, this is rather nasty!
// find the names. find any current nodes whose instances have the name, or whose instances have
// a synonym with the name. then run up the current tree, until we stop at the node we are looking for.


            withQ cnct, '''
with recursive names as (
select id from name where LOWER(simple_name) like LOWER(?)
),
matching_nodes as (
  select 'x' as foo, tree_node.id tree_node_id, instance.id as instance_id
  from tree_node join instance on tree_node.instance_id = instance.id
  where instance.name_id in (select id from names)
  and tree_node.internal_type = 'T'
  and tree_node.replaced_at_id is null
  and tree_node.tree_arrangement_id ''' + (node.root.baseArrangement ? ' in (?,?)' : ' = ?') + '''
union all
  select 'y' as foo, tree_node.id tree_node_id, syn.id as instance_id
  from tree_node join instance on tree_node.instance_id = instance.id
    join instance as syn on instance.id = syn.cited_by_id
  where syn.name_id in (select id from names)
  and tree_node.internal_type = 'T'
  and tree_node.replaced_at_id is null
  and tree_node.tree_arrangement_id ''' + (node.root.baseArrangement ? ' in (?,?)' : ' = ?') + '''
)
,
tree_runner as (
  select matching_nodes.tree_node_id as running_node_id, matching_nodes.*
  from matching_nodes where foo = 'x'
union all
  select tree_link.supernode_id as running_node_id, 'z' as foo, tree_runner.tree_node_id, tree_runner.instance_id
  from tree_runner join tree_link on tree_link.subnode_id = tree_runner.running_node_id
  join tree_node on tree_link.supernode_id = tree_node.id
  where
  tree_runner.running_node_id <> ? -- clip the search
  and tree_node.replaced_at_id is null
)
select tree_runner.*
from tree_runner
where tree_runner.running_node_id = ?
			''', { PreparedStatement sql ->
                if (node.root.baseArrangement) {
                    sql.setString(1, nameLike)
                    sql.setLong(2, node.root.id)
                    sql.setLong(3, node.root.baseArrangement.id)
                    sql.setLong(4, node.root.id)
                    sql.setLong(5, node.root.baseArrangement.id)
                    sql.setLong(6, node.id)
                    sql.setLong(7, node.id)
                } else {
                    sql.setString(1, nameLike)
                    sql.setLong(2, node.root.id)
                    sql.setLong(3, node.root.id)
                    sql.setLong(4, node.id)
                    sql.setLong(5, node.id)
                }
                ResultSet rs = sql.executeQuery()
                try {
                    while (rs.next()) {
                        l.add([
                                node           : Node.get(rs.getLong('tree_node_id')),
                                matchedInstance: Instance.get(rs.getLong('instance_id'))
                        ])
                    }
                }
                finally {
                    rs.close()
                }
            }
        }

        return l

    }

/**
 * Looks for a node in a tree, finding either the node itself or a checked-out version of the node. This is a common
 * thing needing to be done.
 */

    @SuppressWarnings("ChangeToOperator")
    Link findNodeCurrentOrCheckedout(Node supernode, Node findNode) {
        if (!supernode) throw new IllegalArgumentException("supernode is null")
        if (!findNode) throw new IllegalArgumentException("findNode is null")

        doWork(sessionFactory_nsl) { Connection cnct ->
            withQ cnct, '''
with recursive
start_nodes(id) as (
    select ?
    union
    select id from tree_node n where n.checked_in_at_id is null and n.prev_node_id = ?
),
ll(start_id, supernode_id) as (
    select tree_link.id as start_id, tree_link.supernode_id
    from start_nodes join tree_link on start_nodes.id = tree_link.subnode_id
union all
    select ll.start_id, tree_link.supernode_id
    from ll join tree_link on ll.supernode_id = tree_link.subnode_id
)
select distinct start_id from ll where supernode_id = ?
				''',
                    { PreparedStatement qry ->
                        qry.setLong(1, findNode.id)
                        qry.setLong(2, findNode.id)
                        qry.setLong(3, supernode.id)
                        ResultSet rs = qry.executeQuery()

                        try {
                            if (!rs.next()) return null

                            long linkId = rs.getLong(1)

                            if (!rs.next()) {
                                // unique result found
                                return Link.get(linkId)
                            } else {
                                // drat - more than one link found
                                Message multipleLinksFound = ServiceException.makeMsg(Msg.NODE_APPEARS_IN_MULTIPLE_LOCATIONS_IN, [findNode, supernode])

                                multipleLinksFound.nested.add Link.get(linkId)
                                multipleLinksFound.nested.add Link.get(rs.getLong(1))
                                while (rs.next()) {
                                    multipleLinksFound.nested.add Link.get(rs.getLong(1))
                                }

                                ServiceException.raise ServiceException.makeMsg(Msg.findNodeCurrentOrCheckedout, [supernode, findNode, multipleLinksFound])
                            }

                        }
                        finally {
                            rs.close()
                        }

                    }
        } as Link
    }

    @SuppressWarnings("ChangeToOperator")
    int countPaths(Node root, Node focus) {
        try {
            if (root == null || focus == null) return 0
            if (root == focus) return 1

            int ct = 0

            doWork(sessionFactory_nsl) { Connection cnct ->
                withQ cnct, '''
        with recursive scan_up as (
                select supernode_id, subnode_id from tree_link where subnode_id = ?
                union all
                select l.supernode_id, l.subnode_id from tree_link l, scan_up where l.subnode_id = scan_up.supernode_id and scan_up.subnode_id <> ?
        )
        select count(*) as ct from scan_up where supernode_id = ?
				''',
                        { PreparedStatement qry ->
                            qry.setLong(1, focus.id)
                            qry.setLong(2, root.id)
                            qry.setLong(3, root.id)
                            ResultSet rs = qry.executeQuery()
                            try {
                                rs.next()
                                ct = rs.getInt(1)
                            }
                            finally {
                                rs.close()
                            }

                        }
            }
            return ct
        }
        catch (Exception e) {
            e.printStackTrace()
            throw e
        }
    }

    @SuppressWarnings(["ChangeToOperator", "GroovyUnusedDeclaration"])
    int countImmediateSubtaxa(Node n) {
        if (n == null) return 0
        long ct = 0

        doWork(sessionFactory_nsl) { Connection cnct ->
            withQ cnct, '''
        select count(*) ct
        from tree_link l
        join tree_node n on l.subnode_id = n.id
        where l.supernode_id = ?
        and n.internal_type in ('S','T')
				''',
                    { PreparedStatement qry ->
                        qry.setLong(1, n.id)
                        ResultSet rs = qry.executeQuery()
                        try {
                            rs.next()
                            ct = rs.getLong('ct')
                        }
                        finally {
                            rs.close()
                        }

                    }
        }

        return ct
    }

    @SuppressWarnings(["ChangeToOperator", "GroovyUnusedDeclaration"])
    Map<Long, Integer> getSubtaxaCountForAllSubtaxa(Node n) {
        Map m = new HashMap<Long, Integer>()

        doWork(sessionFactory_nsl) { Connection cnct ->
            withQ cnct, '''
        select l1.subnode_id as node_id, count(l2.subnode_id) as subnodes
        from tree_link l1
        join tree_node n1 on l1.subnode_id = n1.id
        join tree_link l2 on l1.subnode_id = l2.supernode_id
        join tree_node n2 on l2.subnode_id = n2.id
        where l1.supernode_id = ?
        and n1.internal_type in ('S','T')
        and n2.internal_type in ('S','T')
        group by l1.subnode_id
				''',
                    { PreparedStatement qry ->
                        qry.setLong(1, n.id)
                        ResultSet rs = qry.executeQuery()
                        try {
                            while (rs.next()) {
                                m.put(rs.getLong('node_id'), rs.getInt('subnodes'))
                            }
                        }
                        finally {
                            rs.close()
                        }

                    }
        }

        return m
    }

    @SuppressWarnings("ChangeToOperator")
    List<Node> findPath(Node root, Node focus) {
        List<Node> l = new ArrayList<Node>()

        doWork(sessionFactory_nsl) { Connection cnct ->
            withQ cnct, '''
        with recursive scan_up as (
                select id as supernode_id, id as subnode_id from tree_node where id = ?
                union all
                select l.supernode_id, l.subnode_id from tree_link l, scan_up where l.subnode_id = scan_up.supernode_id and scan_up.supernode_id <> ?
        ),
        scan_down as (
                select scan_up.* from scan_up where scan_up.supernode_id = ?
        union all
        select scan_up.* from scan_up, scan_down where scan_up.supernode_id = scan_down.subnode_id and scan_down.supernode_id <> ?
        )
        select * from scan_down
				''',
                    { PreparedStatement qry ->
                        qry.setLong(1, focus.id)
                        qry.setLong(2, root.id)
                        qry.setLong(3, root.id)
                        qry.setLong(4, focus.id)
                        ResultSet rs = qry.executeQuery()

                        try {
                            while (rs.next()) {

                                l.add(Node.get(rs.getLong(1)))
                            }
                        }
                        finally {
                            rs.close()
                        }

                    }
        }

        return l
    }

    @SuppressWarnings("ChangeToOperator")
    List<Link> findPathLinks(Node root, Node focus) {
        if (!root) throw new IllegalArgumentException("root is null")
        if (!focus) throw new IllegalArgumentException("focus is null")

        List<Link> links = new ArrayList<Link>()

        doWork(sessionFactory_nsl) { Connection cnct ->
            withQ cnct, '''
                with recursive scan_up as (
                  select l.id, l.supernode_id, l.subnode_id from tree_link l where l.subnode_id = ?
                union all
                  select l.id, l.supernode_id, l.subnode_id from tree_link l, scan_up where l.subnode_id = scan_up.supernode_id and scan_up.supernode_id <> ?
                ),
                scan_down as (
                  select 1 as depth, scan_up.* from scan_up where scan_up.supernode_id = ?
                union all
                  select depth+1 as depth, scan_up.* from scan_up, scan_down where scan_up.supernode_id = scan_down.subnode_id and scan_down.supernode_id <> ?
                )
                select * from scan_down order by depth
        ''',
                    { PreparedStatement qry ->
                        qry.setLong(1, focus.id)
                        qry.setLong(2, root.id)
                        qry.setLong(3, root.id)
                        qry.setLong(4, focus.id)
                        ResultSet rs = qry.executeQuery()

                        try {
                            while (rs.next()) {
                                links.add(Link.get(rs.getLong('id')))
                            }
                        }
                        finally {
                            rs.close()
                        }
                    }
        }

        return links
    }

    @SuppressWarnings(["ChangeToOperator", "GroovyUnusedDeclaration"])
    int countDraftNodes(Node focus) {
        try {
            if (focus == null) return 0

            int ct = 0

            doWork(sessionFactory_nsl) { Connection cnct ->
                withQ cnct, '''
        with recursive scan as (
                select tree_node.id from tree_node where tree_node.id = ? and tree_node.checked_in_at_id is null
                union all
                select tree_node.id from scan join tree_link on scan.id = tree_link.supernode_id
                join tree_node on tree_link.subnode_id = tree_node.id
                where tree_node.checked_in_at_id is null
        )
        select count(*) as ct from scan
				''',
                        { PreparedStatement qry ->
                            qry.setLong(1, focus.id)
                            ResultSet rs = qry.executeQuery()
                            try {
                                rs.next()
                                ct = rs.getInt(1)
                            }
                            finally {
                                rs.close()
                            }

                        }
            }
            return ct
        }
        catch (Exception e) {
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Find all names in n1 or n2, and work out changes. Do this by link id, because we want to report by
     * placement rather than by node composition.
     * @param n1
     * @param n2
     * @return
     */

    List<Map> findDifferences(Node n1, Node n2) {
        log.debug("differences between ${n1} and ${n2}")

        List<Map> changesByName = []

        doWork(sessionFactory_nsl) { Connection cnct ->
            withQ cnct, '''
WITH RECURSIVE tree_1 AS (
  SELECT
    cast(NULL AS BIGINT) AS link_id,
    n.id                 AS subnode_id,
    n.name_id            AS name_id
  FROM tree_node n
  WHERE n.id = ?
  UNION ALL
  SELECT
    l.id         AS link_id,
    l.subnode_id AS subnode_id,
    n.name_id    AS name_id
  FROM tree_1
    JOIN tree_link l ON tree_1.subnode_id = l.supernode_id
    JOIN tree_node n ON l.subnode_id = n.id
  WHERE n.internal_type = 'T\'
),
    tree_2 AS (
    SELECT
      cast(NULL AS BIGINT) AS link_id,
      n.id                 AS subnode_id,
      n.name_id            AS name_id
    FROM tree_node n
    WHERE n.id = ?
    UNION ALL
    SELECT
      l.id         AS link_id,
      l.subnode_id AS subnode_id,
      n.name_id    AS name_id
    FROM tree_2
      JOIN tree_link l ON tree_2.subnode_id = l.supernode_id
      JOIN tree_node n ON l.subnode_id = n.id
    WHERE n.internal_type = 'T\'
  ),
    names AS ( SELECT name_id
               FROM tree_1
               UNION SELECT name_id
                     FROM tree_2)
SELECT
  names.name_id,
  tree_1.link_id       AS link_1,
  tree_2.link_id       AS link_2,
  tree_1.subnode_id    AS subnode_1,
  tree_2.subnode_id    AS subnode_2,
  super_node_1.name_id AS super_name_1,
  super_node_2.name_id AS super_name_2
FROM
  names
  JOIN name ON names.name_id = name.id
  LEFT OUTER JOIN tree_1 ON names.name_id = tree_1.name_id
  LEFT OUTER JOIN tree_2 ON names.name_id = tree_2.name_id
  LEFT OUTER JOIN tree_link l1 ON l1.id = tree_1.link_id
  LEFT OUTER JOIN tree_link l2 ON l2.id = tree_2.link_id
  LEFT OUTER JOIN tree_node super_node_1 ON l1.supernode_id = super_node_1.id
  LEFT OUTER JOIN tree_node super_node_2 ON l2.supernode_id = super_node_2.id
WHERE tree_1.link_id ISNULL
      OR tree_2.link_id ISNULL
      OR tree_1.link_id <> tree_2.link_id
      OR tree_1.subnode_id <> tree_2.subnode_id
      OR super_node_1.name_id <> super_node_2.name_id
ORDER BY name.full_name
				''',
                    { PreparedStatement qry ->
                        qry.setLong(1, n1.id)
                        qry.setLong(2, n2.id)
                        log.debug("about to execute the big query")
                        ResultSet rs = qry.executeQuery()
                        log.debug("got the resultset")
                        try {
                            //noinspection ChangeToOperator
                            while (rs.next()) {
                                Long nameId = rs.getLong('name_id') ?: null
                                Long subNode1Id = rs.getLong('subnode_1') ?: null
                                Long subNode2Id = rs.getLong('subnode_2') ?: null
                                Long link1Id = rs.getLong('link_1') ?: null
                                Long link2Id = rs.getLong('link_2') ?: null
                                Long superName1 = rs.getLong('super_name_1') ?: null
                                Long superName2 = rs.getLong('super_name_2') ?: null

                                log.debug("${nameId} (${subNode1Id} ${link1Id}) (${subNode2Id} ${link2Id}) $superName1, $superName2")

                                if (link1Id && link2Id && link1Id == link2Id) {
                                    log.debug("* just part of a common subtree")
                                }

                                assert subNode1Id || subNode2Id

                                boolean placementChanged =
                                        ((subNode1Id == null) != (subNode2Id == null)) ||
                                                ((link1Id == null) != (link2Id == null)) ||
                                                (link1Id != null && link2Id != null && superName1 != superName2)

                                log.debug "Placement changed: $placementChanged"

                                if (!placementChanged && subNode1Id == subNode2Id) {
                                    log.debug("* same node under same name. Continuing.")
                                    continue
                                }

                                Name name = Name.get(nameId)
                                Node subNode1 = Node.get(subNode1Id)
                                Node subNode2 = Node.get(subNode2Id)
                                Link link1 = Link.get(link1Id)
                                Link link2 = Link.get(link2Id)

                                log.debug("${name.fullName} ${subNode1} ${link1} ${subNode2} ${link2}")

                                List<String> changes = new ArrayList<String>()

                                if (placementChanged) {
                                    changes.add(decodePlacementChange(subNode1, subNode2, link2, link1))
                                }

                                if (subNode1 != null && subNode2 != null && subNode1.id != subNode2.id) {
                                    if (subNode1.instance?.id != subNode2.instance?.id) {
                                        changes.add("reference changed from ${subNode1.instance?.reference?.citation} to ${subNode2.instance?.reference?.citation}")
                                    }

                                    if (subNode1.typeUriIdPart != subNode2.typeUriIdPart) {
                                        changes.add("type changed from ${subNode1.typeUriIdPart} to ${subNode2.typeUriIdPart}")
                                    }

                                    Map<Uri, Link> pp1 = DomainUtils.getProfileItemsAsMap(subNode1)
                                    Map<Uri, Link> pp2 = DomainUtils.getProfileItemsAsMap(subNode2)

                                    Set<Uri> items = new HashSet<Uri>()
                                    items.addAll(pp1.keySet())
                                    items.addAll(pp2.keySet())

                                    for (Uri u : items) {
                                        if (pp1.containsKey(u) && pp2.containsKey(u)) {
                                            if (pp1.get(u).subnode.literal != pp2.get(u).subnode.literal) {
                                                changes.add("Profile item ${DomainUtils.vnuForItem(pp1.get(u))?.title ?: u} changed")
                                            }
                                        } else if (!pp1.containsKey(u)) {
                                            changes.add("Profile item ${DomainUtils.vnuForItem(pp2.get(u))?.title ?: u} added")
                                        } else if (!pp2.containsKey(u)) {
                                            changes.add("Profile item ${(DomainUtils.vnuForItem(pp2.get(u))?.title) ?: u} removed")
                                        }
                                    }
                                }

                                // no filtering yet

                                if (!changes.isEmpty()) {
                                    changesByName.add([name: name.fullName, changes: changes])
                                }
                            }
                            log.debug("done finding changes.")

                        }
                        finally {
                            rs.close()
                        }

                    }
        }

        return changesByName
    }

    private static String decodePlacementChange(Node subNode1, Node subNode2, Link link2, Link link1) {
        if (subNode1 == null && subNode2 != null && link2 == null) {
            return "New placement"
        }
        if (subNode1 == null && subNode2 != null && link2 != null) {
            return "New placement under ${link2.supernode.name.fullName}"
        }

        if (subNode1 != null && subNode2 == null && link2 == null) {
            return "Name removed"
        }
        if (subNode1 != null && subNode2 == null && link2 != null) {
            return "Name removed from ${link1.supernode.name.fullName}"
        }

        if (link1 == null && link2 != null) {
            return "Name moved from root to ${link2.supernode.name.fullName}"
        }

        if (link1 != null && link2 == null) {
            return "Name moved from ${link1.supernode.name.fullName} to root"
        }
        return "Name moved from ${link1.supernode.name.fullName} to ${link2.supernode.name.fullName} "
    }

    static Name resolveName(Node node) {
        if (!node) return null
        if (node.name) return node.name
        if (!node.nameUriNsPart || !node.nameUriIdPart) return null
        if (node.nameUriNsPart.label == 'nsl-name') return Name.get(node.nameUriIdPart as Long)
        if (node.nameUriNsPart.label == 'apni-name') return Name.findByNamespaceAndSourceSystemAndSourceId(node.root.namespace, 'PLANT_NAME', node.nameUriIdPart as Long)
        return null
    }

    static Instance resolveInstance(Node node) {
        if (!node) return null
        if (node.instance) return node.instance
        if (!node.taxonUriNsPart || !node.taxonUriIdPart) return null
        if (node.taxonUriNsPart.label == 'nsl-instance') return Instance.get(node.taxonUriIdPart as Long)
        if (node.taxonUriNsPart.label == 'apni-taxon') return Instance.findByNamespaceAndSourceSystemAndSourceId(node.root.namespace, 'PLANT_NAME_REFERENCE', node.taxonUriIdPart as Long)
        return null
    }

    static class PlacementSpan {
        Link from
        Link to

        PlacementSpan(Link from, Link to) {
            this.from = from
            this.to = to
        }

        String toString() {
            return "${from.supernode.id}-${to.supernode.id}->${from.subnode.id}-${to.subnode.id}"
        }
    }

    // this is a rather nasty recursive routine whose job it is to boil down
    // the potentially thousands of placements of a node (owing to the way our tree works) into their tarry essence
    // we need to ignore placements that are the result of one of our sibling nodes causing a
    // synthetic move.

    static Collection<ArrayList<PlacementSpan>> getPlacementPaths(Node n) {

        Collection<ArrayList<PlacementSpan>> paths = new HashSet<ArrayList<PlacementSpan>>()

        // TODO: arrange something more formal with respect to the fact that we don't go higher than the root
        if (n == null || n.supLink.isEmpty() || n.internalType == NodeInternalType.S || n.typeUriIdPart == 'classification-root') {
            return paths
        }

        for (Link l : n.supLink) {
            Collection<ArrayList<PlacementSpan>> linkspans = getPlacementPaths(l.supernode)

            if (linkspans.isEmpty()) {
                linkspans.add(new ArrayList<PlacementSpan>())
            }

            for (ArrayList<PlacementSpan> linkspan_path : linkspans) {
                linkspan_path.add(new PlacementSpan(l, l))
            }
            paths.addAll(linkspans)
        }

        mergePaths(paths)

        return paths
    }

    static void mergePaths(Collection<ArrayList<PlacementSpan>> paths) {
        look_for_more_merges:
        for (; ;) {
            for (ArrayList<PlacementSpan> a : paths) {
                look_for_next_pair:
                for (ArrayList<PlacementSpan> b : paths) {
                    if (a == b) continue
                    if (a.size() != b.size()) continue
                    for (int i = 0; i < a.size(); i++) {
                        PlacementSpan aa = a.get(i)
                        PlacementSpan bb = b.get(i)

                        /* We only look at the supernode. This is ok, because the subnode of the final link of all paths will always be node n */

                        if (aa.to.supernode.id != bb.from.supernode.prev?.id) continue look_for_next_pair
                        if (!nodesLookTheSame(aa.to.supernode, bb.from.supernode)) continue look_for_next_pair
                        if (!linksLookTheSame(aa.to, bb.from)) continue look_for_next_pair
                    }
                    // right! we need to do a merge of b into a.

                    for (int i = 0; i < a.size(); i++) {
                        PlacementSpan aa = a.get(i)
                        PlacementSpan bb = b.get(i)
                        aa.to = bb.to
                    }
                    paths.remove(b)

                    /* and this will mess up the iterators, so restart the whole thing
                    yes, it's a load. but getting it right is a lot of code. Consider what happens when
                    we merge paths in the middle of a big sequence of paths needing to be merged.
                    */
                    continue look_for_more_merges
                }
            }

            break look_for_more_merges
        }
    }

    private static boolean nodesLookTheSame(Node n1, Node n2) {
        if (n1 == null && n2 == null) return true
        if (n1 == null || n2 == null) return false

        return uriSame(n1.typeUriNsPart, n1.typeUriIdPart, n2.typeUriNsPart, n2.typeUriIdPart) &&
                uriSame(n1.nameUriNsPart, n1.nameUriIdPart, n2.nameUriNsPart, n2.nameUriIdPart) &&
                uriSame(n1.taxonUriNsPart, n1.taxonUriIdPart, n2.taxonUriNsPart, n2.taxonUriIdPart) &&
                uriSame(n1.resourceUriNsPart, n1.resourceUriIdPart, n2.resourceUriNsPart, n2.resourceUriIdPart)
    }

    private static boolean linksLookTheSame(Link l1, Link l2) {
        if (l1 == null && l2 == null) return true
        if (l1 == null || l2 == null) return false

        return /* l1.linkSeq == l2.linkSeq && */ uriSame(l1.typeUriNsPart, l1.typeUriIdPart, l2.typeUriNsPart, l2.typeUriIdPart)
    }

    private static boolean uriSame(UriNs nsPart1, String idPart1, UriNs nsPart2, String idPart2) {
        return (nsPart1 == nsPart2) && ((idPart1 ?: '') == (idPart2 ?: ''))
    }
}
