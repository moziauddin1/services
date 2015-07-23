package au.org.biodiversity.nsl.api

import org.hibernate.SessionFactory

import grails.converters.JSON
import grails.transaction.Transactional
import grails.validation.Validateable
import au.org.biodiversity.nsl.*
import au.org.biodiversity.nsl.tree.*
import static au.org.biodiversity.nsl.tree.DomainUtils.*

@Transactional
class TreeEditController {
	AsRdfRenderableService asRdfRenderableService
	SessionFactory sessionFactory_nsl
	TreeViewService treeViewService
	TreeOperationsService treeOperationsService
	QueryService queryService

    /** @deprecated */

	def placeApniName(PlaceApniNameParam p) {
		// this should invoke the classification service
		
		return render ([success: false, validationErrors: TMP_RDF_TO_MAP.asMap(asRdfRenderableService.springErrorsAsRenderable(p.errors))])  as JSON
	}

	def placeApcInstance(PlaceApcInstanceParam p) {
		// most of this code belongs in the classification service
		
		log.debug "placeApcInstance(${p})"
		if (!p.validate()) {
			log.debug "!p.validate()"
			RdfRenderable err = asRdfRenderableService.springErrorsAsRenderable(p.errors)
			Map<?,?> result = [success: false, validationErrors: TMP_RDF_TO_MAP.asMap(err)]
			return render(result as JSON)
		}

		Arrangement apc = Arrangement.findByLabel('APC')

//		Uri nameUri = uri('nsl-name', p.instance.name.id)
//		Uri supernameUri = p.supername ? uri('nsl-name', p.supername.id) : null
//		Uri taxonUri = uri('nsl-instance', p.instance.id)

		Uri nodeTypeUri
		Uri linkTypeUri

		if(p.placementType == 'DeclaredBt') {
			nodeTypeUri = uri('apc-voc', 'DeclaredBt')
			linkTypeUri = uri('apc-voc', 'btOf') // this always gets changed below in normal use
		}
		else
		if(p.placementType == 'ApcExcluded') {
			nodeTypeUri = uri('apc-voc', 'ApcExcluded')
			linkTypeUri = uri('apc-voc', 'hasExcludedName')
		}
		else {
			nodeTypeUri = uri('apc-voc', 'ApcConcept')
			linkTypeUri = uri('apc-voc', 'btOf')
		}

		if(p.supername == null) {
			linkTypeUri = uri('apc-voc', 'topNode')
		}
		else {
			Link supernameLink = null
			List<Link> supernameLinks = queryService.findCurrentNslNamePlacement(apc, p.instance.name) // Should return a unique current node for the name. Should.
			if (supernameLinks) {
				supernameLink = supernameLinks.first()
				if(supernameLink.supernode.typeUriIdPart == 'DeclaredBt') {
					linkTypeUri = uri('apc-voc', 'declaredBtOf')
				}
			}
		}

		boolean nameExists = !!queryService.findCurrentNslNamePlacement(apc, p.instance.name)

		if(p.supername) {
			List<Link> supernameLinks = queryService.findCurrentNslNamePlacement(apc, p.supername) // Should return a unique current node for the name. Should.
			if (supernameLinks) {
				Link supernameLink = supernameLinks.first()
				if(supernameLink.supernode.typeUriIdPart == 'DeclaredBt') {
					linkTypeUri = uri('apc-voc', 'declaredBtOf')
				}
			}
		}

		try {
			log.debug "perform update/add"
			
			def profileData = [:]

			if(nameExists)
				treeOperationsService.updateNslName(apc, p.instance.name, p.supername, p.instance, nodeTypeUri: nodeTypeUri, linkTypeUri: linkTypeUri, profileData)
			else
				treeOperationsService.addNslName(apc, p.instance.name, p.supername, p.instance, nodeTypeUri: nodeTypeUri, linkTypeUri: linkTypeUri, profileData)
								
			apc = refetchArrangement(apc)
			p.refetch()
		}
		catch (ServiceException ex) {
			RdfRenderable err = asRdfRenderableService.serviceExceptionAsRenderable(ex, message_param_detangler)
			Map<?,?> result = [success: false, serviceException: TMP_RDF_TO_MAP.asMap(err)]
			log.debug "ServiceException"
			log.warn ex
			return render(result as JSON)
		}

		//		sessionFactory_nsl.getCurrentSession().clear()

		def result = [success:true]
		log.debug "treeViewService.getInstancePlacementInTree"
		Map<?,?> npt = treeViewService.getInstancePlacementInTree(apc, p.instance)
		result << npt

		log.debug "render(result as JSON)"
		return render(result as JSON)
	}

	private Closure message_param_detangler = {
		if(it instanceof Node) {
			Node n = (Node) it;

			// we prefer just using the name
			if(DomainUtils.getNameUri(n)?.nsPart?.label == 'nsl-name') {
				Name name = Name.get(DomainUtils.getNameUri(n).idPart as Integer)
				name ? message_param_detangler(name) : DomainUtils.getNodeUri(n);
			}
			else
			if(DomainUtils.getTaxonUri(n)?.nsPart?.label == 'nsl-instance') {
				Instance inst = Instance.get(DomainUtils.getTaxonUri(n).idPart as Integer)
				inst?.name ? message_param_detangler(inst.name) : DomainUtils.getNodeUri(n);
			}
			else {
				DomainUtils.getNodeUri(n);
			}
		}
		else if(it instanceof Link) {
			Link l = (Link) it;
			"${message_param_detangler(l.supernode)}[${l.linkSeq}]->${message_param_detangler(l.subnode)}";
		}
		else if(it instanceof Arrangement) {
			((Arrangement)it).label ?: DomainUtils.getArrangementUri(it)
		}
		else if(it instanceof Name) {
			Name name = (Name) it;
			name.fullName;
		}
		else if(it instanceof Instance) {
			Instance inst = (Instance) it;
			if(inst.reference?.citation) {
				"${message_param_detangler(inst.name)} s. ${inst.reference.citation}"
			}
			else {
				message_param_detangler(inst.name)
			}
		}
		else {
			it;
		}
	}

	def removeApcInstance(RemoveApcInstanceParam p) {
		// most of this code belongs in the classification service

		log.debug "removeAPCInstance(${p})"
		if (!p.validate()) {
			log.debug "!p.validate()"
			RdfRenderable err = asRdfRenderableService.springErrorsAsRenderable(p.errors)
			Map<?,?> result = [success: false, validationErrors: TMP_RDF_TO_MAP.asMap(err)]
			return render(result as JSON)
		}

		Arrangement apc = Arrangement.findByLabel('APC')

		try {
			log.debug "perform remove"
			treeOperationsService.deleteNslName(apc, p.instance.name, p.replacementName)
			apc = refetchArrangement(apc)
			p.refetch()
		}
		catch (ServiceException ex) {
			RdfRenderable err = asRdfRenderableService.serviceExceptionAsRenderable(ex)
			Map<?,?> result = [success: false, serviceException: TMP_RDF_TO_MAP.asMap(err)]
			log.debug "ServiceException"
			log.warn ex
			return render(result as JSON)
		}

		//		sessionFactory_nsl.getCurrentSession().clear()

		def result = [success:true]
		log.debug "treeViewService.getInstancePlacementInTree"
		Map<?,?> npt = treeViewService.getInstancePlacementInTree(apc, p.instance)
		result << npt

		log.debug "render(result as JSON)"
		return render(result as JSON)
	}
}

/** This class does not belong here. */
class TMP_RDF_TO_MAP {
	static Object asMap(RdfRenderable r) {
		if(r==null) return null
		if(r instanceof RdfRenderable.Obj) return objAsMap((RdfRenderable.Obj)r)
		if(r instanceof RdfRenderable.Literal) return literalAsMap((RdfRenderable.Literal)r)
		if(r instanceof RdfRenderable.Coll) return collAsMap((RdfRenderable.Coll)r)
		if(r instanceof RdfRenderable.Primitive) return ((RdfRenderable.Primitive)r).o
		if(r instanceof RdfRenderable.Resource) return resourceAsMap((RdfRenderable.Resource)r)
		return r.getClass().getName()
	}

	static Object objAsMap(RdfRenderable.Obj r) {
		def o = [:]
		for(Map.Entry<Uri, RdfRenderable.Obj.Value> e: r.entrySet()) {
			String k = e.getKey().asQNameDIfOk()

			if(e.getValue().isSingleValue()) {
				o.put(k, asMap(e.getValue().asSingleValue().v))
			}
			else {
				def oo = []
				o.put(k, oo)
				for(RdfRenderable rr: e.getValue().asMultiValue()) {
					oo << asMap(rr)
				}
			}
		}
		return o
	}

	static Object collAsMap(RdfRenderable.Coll r) {
		def o = []
		boolean ignoreFirst = true
		for(RdfRenderable i: r) {
			if(ignoreFirst)
				ignoreFirst = false
			else
				o << asMap(i)
		}
		return o
	}

	static Object literalAsMap(RdfRenderable.Literal r) {
		return [
			type: r.uri.asQNameDIfOk(),
			value: r.literal
		]
	}

	static Object resourceAsMap(RdfRenderable.Resource r) {
		return r.uri
	}


}

@Validateable
class PlaceApniNameParam {
	long nameId
	long supernameId

	String toString() {
		return [nameId:nameId, superNameId:supernameId].toString()
	}

	Name getName() {
		return nameId ? Name.get(nameId) : null
	}

	Name getSupername() {
		return supernameId ? Name.get(supernameId) : null
	}

	static constraints = {
		name nullable: false
		nameId nullable: false
		supername nullable: true
		supernameId nullable: true
	}
}

@Validateable
class PlaceApcInstanceParam {
	Instance instance
	Name  supername
	String placementType

	String toString() {
		return [instance:instance, supername:supername,placementType:placementType].toString()
	}

	void refetch() {
		if(instance != null) instance = Instance.get(instance.id)
		if(supername != null) supername = Name.get(supername.id)
	}

	static constraints = {
		instance nullable: false
		supername nullable: true
		placementType nullable: true
	}
}

@Validateable
class RemoveApcInstanceParam {
	Instance instance
	Name  replacementName

	String toString() {
		return [instance:instance, replacementName: replacementName].toString()
	}

	void refetch() {
		if(instance != null) instance = Instance.get(instance.id)
		if(replacementName != null) replacementName = Name.get(replacementName.id)
	}

	static constraints = {
		instance nullable: false
		replacementName nullable: true
	}
}
