/*
    Copyright 2015 Australian National Botanic Gardens

    This file is part of NSL services project.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy
    of the License at http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

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

	def namePlacementPath(TreeViewParam p) {
		render treeViewService.getPathForName(p.tree, p.name) as JSON
	}

	def treeTopPath(TreeViewParam p) {
		render treeViewService.getPathForTree(p.tree) as JSON
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
