package au.org.biodiversity.nsl

import grails.transaction.Transactional
import org.apache.shiro.grails.annotations.RoleRequired
import org.springframework.transaction.TransactionStatus

@Transactional
class InstanceService {

    def classificationService
    def linkService

    /**
     * Delete the instance
     *
     * check we can delete this instance
     * remove references to the instance from tree nodes
     * mark mapper links to this instance as deleted
     *
     * @param instance
     * @param reason
     * @return
     */
    @RoleRequired('admin')
    Map deleteInstance(Instance instance, String reason) {
        Map canWeDelete = canDelete(instance, reason)
        if (canWeDelete.ok) {
            try {
                Name.withTransaction { TransactionStatus t ->
                    Map response = linkService.deleteInstanceLinks(instance, reason)
                    if (!response.success) {
                        List<String> errors = ["Error deleting link from the mapper"]
                        errors.addAll(response.errors)
                        return [ok: false, errors: errors]
                    }
                    removeInstanceFromTrees(instance)
                    instance.refresh()
                    instance.delete()
                    t.flush()
                }
            } catch (e) {
                List<String> errors = [e.message]
                while (e.cause) {
                    e = e.cause
                    errors << e.message
                }
                return [ok: false, errors: errors]
            }
        }
        return canWeDelete
    }

    /**
     * Can we delete this instance.
     *
     * Check:
     * 1. no other instances cite this instance
     * 2. no other instances say they are citedBy this instance
     * 3. there are no Instance Notes for this instance
     * 4. this instance is not the parent of any other instance
     * 5. there are no external references for this reference
     * 6. there are no comments on this instance
     * 7. check this instance is not currently in APC
     *
     * @param instance
     * @return a map with ok and a list of error Strings
     */
    public Map canDelete(Instance instance, String reason) {
        List<String> errors = []
        if (!reason) {
            errors << 'You need to supply a reason for deleting this instance.'
        }
        if (classificationService.isInstanceInAPC(instance)) {
            errors << "This instance is in APC."
        }
        if (NslSimpleName.findByApcInstance(instance)) {
            errors << "This instance is an APC Instance in NslSimpleName."
        }
        if(instance.instancesForCites) {
            errors << "There are ${instance.instancesForCites.size()} instances that cite this."
        }
        if(instance.instancesForCitedBy) {
            errors << "There are ${instance.instancesForCitedBy.size()} instances that say this cites it."
        }
        if(instance.instancesForParent) {
            errors << "This is a parent for ${instance.instancesForParent.size()} instances."
        }
        if(instance.instanceNotes) {
            errors << "There are ${instance.instanceNotes.size()} instances notes on this."
        }
        if(instance.externalRefs) {
            errors << "There are ${instance.externalRefs.size()} external references."
        }
        if(instance.comments) {
            errors << "There are ${instance.comments.size()} comments for this instance."
        }

        if (errors.size() > 0) {
            return [ok: false, errors: errors]
        }
        return [ok: true]
    }

    /**
     * * Removing from the tree may mean:
     *
     * 1. make sure it's a leaf node (no children)
     * 2. set any FK references to the name to null
     *
     * @param name
     */
    void removeInstanceFromTrees(Instance instance) {
        //set *all* the node, instance references to null
        List<Node> nodeReferences = Node.findAllByInstance(instance)
        nodeReferences.each { Node node ->
            log.info "Setting node $node.id instance ids to null from $instance.id"
            node.instance = null //set the instance references to null
            node.save()
        }
    }

    public List<Instance> findPrimaryInstance(Name name){
        Instance.executeQuery("select i from Instance i where i.name = :name and i.instanceType.primaryInstance = true",[name: name])
    }

    public List<Instance> sortInstances(List<Instance> instances) {
        instances.sort { a, b ->
            if (a.citedBy == b.citedBy) {
                if (a.instanceType.sortOrder == b.instanceType.sortOrder) {
                    if (a.cites?.reference?.year == b.cites?.reference?.year) {
                        if (a.reference == b.reference) {
                            if (a.page == b.page) {
                                return b.id <=> a.id
                            }
                            return b.page <=> a.page
                        }
                        return a.reference.citation <=> b.reference.citation
                    }
                    return (a.cites?.reference?.year) <=> (b.cites?.reference?.year)
                }
                return a.instanceType.sortOrder <=> b.instanceType.sortOrder
            }
            return compareReferences(a, b, 'citedBy')
        }
    }

    public static Integer compareReferences(Instance a1, Instance b1, String sortOn = null) {
        Instance a = (sortOn ? a1[sortOn] : a1) as Instance
        Instance b = (sortOn ? b1[sortOn] : b1) as Instance
        if (a && b) {
            if (a.reference.year == b.reference.year) {
                if (a.reference == b.reference) {
                    if (a.page == b.page) {
                        return b.id <=> a.id
                    }
                    return b.page <=> a.page
                }
                return a.reference.citation <=> b.reference.citation
            }
            return (a.reference.year) <=> (b.reference.year)
        }
        return a <=> b

    }

}
