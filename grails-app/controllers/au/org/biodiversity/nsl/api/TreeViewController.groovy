package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.Arrangement
import au.org.biodiversity.nsl.Name
import au.org.biodiversity.nsl.Instance
import au.org.biodiversity.nsl.TreeViewService
import grails.converters.JSON
import grails.transaction.Transactional
import grails.validation.Validateable

@Transactional
class TreeViewController {
	TreeViewService treeViewService

	def index(TreeViewParam p) {
		return [tree: p.tree, name: p.name]
	}

	def namePlacementBranch(TreeViewParam p) {
		render treeViewService.getBranchForName(p.tree, p.name) as JSON
	}

	def treeTopBranch(TreeViewParam p) {
		render treeViewService.getBranchForTree(p.tree) as JSON
	}

	def nameInTree(TreeViewParam p) {
		render treeViewService.getNameInTree(p.tree, p.name) as JSON
	}

	def namePlacementInTree(TreeViewParam p) {
		render treeViewService.getNamePlacementInTree(p.tree, p.name) as JSON
	}

	def instancePlacementInTree(TreeViewInstanceParam p) {
		render treeViewService.getInstancePlacementInTree(p.tree, p.instance) as JSON
	}
}

@Validateable
class TreeViewParam {
	String treeLabel
	String nameId

	String toString() {
		return [treeLabel: treeLabel, nameId:nameId].toString()
	}

	Name getName() {
		return nameId ? Name.get(nameId as Long) : null
	}

	Arrangement getTree() {
		return Arrangement.findByLabel(treeLabel)
	}

	static constraints = {
		nameId nullable: true
		name nullable: true
	}
}

@Validateable
class TreeViewInstanceParam {
	String treeLabel
	String instanceId

	String toString() {
		return [treeLabel: treeLabel, instanceId:instanceId].toString()
	}

	Instance getInstance() {
		return instanceId ? Instance.get(instanceId as Long) : null
	}

	Arrangement getTree() {
		return Arrangement.findByLabel(treeLabel)
	}

	static constraints = {
		instanceId nullable: true
		instance nullable: true
	}
}
