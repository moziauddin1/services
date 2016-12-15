<div class="panel panel-default">
    <div class="panel-heading">
        <g:render template="/search/common-search-heading"/>
        <g:if test="${query.sparql}">
            <span id="QUERY-FORM-CLOSE" onclick="hide_query_form();" style="cursor: pointer;"><i class="fa fa-caret-up"></i> Hide search form</span>
            <span id="QUERY-FORM-OPEN" onclick="show_query_form();" style="cursor: pointer; display: none"><i class="fa fa-caret-down"></i> Show search form</span>

            <script type="text/javascript">

                function hide_query_form() {
                    $('#QUERY-FORM-OPEN').show();
                    $('#QUERY-FORM-CLOSE').hide();
                    $('#QUERY-FORM').hide();
                }

                function show_query_form() {
                    $('#QUERY-FORM-CLOSE').show();
                    $('#QUERY-FORM-OPEN').hide();
                    $('#QUERY-FORM').show();
                }

            </script>
        </g:if>
    </div>



    <div class="panel-body" id="QUERY-FORM">
        <div class="container-fluid">
            <div class="col-md-8">
                <g:if test="${query.sparql}">
                <%--  we are in sparql mode - executing the client-side functionality --%>
                    <div class="form-group">
                        <label style="width: 100%;">Query
                            <help><i class="fa fa-info-circle"></i>

                                <div>
                                    <ul>
                                        <li>This is a <a href=http://www.w3.org/TR/rdf-sparql-query/">SPARQL</a> query.</li>
                                        <li>Sample queries are listed to the right.</li>
                                        <li><strong>Please use a LIMIT clause in your query.</strong></li>
                                    </ul>
                                </div>
                            </help>
                            <br/>
                            <textarea id="SPARQLQUERY" name="query" placeholder="SPARQL Query" style="width: 100%; font-family: monospace; font-weight: normal;" rows="10"
                                      class="form-control ">${query.query}</textarea>
                        </label>
                    </div>

                    <div class="form-group"><div class="btn-group">
                        <g:set var="formName" value="sparql"/>
                        <button class="btn btn-primary" onclick="send_sparql_query();">Search</button>
                    </div>
                    </div>
                </g:if>


                <g:else>
                <%--  need to refetch from the server to get the sparql-mode controls --%>
                    <g:form name="search" role="form" controller="search" action="search" method="POST">
                        <div class="form-group">
                            <label style="width: 100%;">Query
                                <help><i class="fa fa-info-circle"></i>

                                    <div>
                                        <ul>
                                            <li>This is a <a href=http://www.w3.org/TR/rdf-sparql-query/">SPARQL</a> query.</li>
                                            <li>Sample queries are listed to the right.</li>
                                            <li><strong>Please use a LIMIT clause in your query.</strong></li>
                                        </ul>
                                    </div>
                                </help>
                                <br/>
                                <textarea id="SPARQLQUERY" name="query" placeholder="SPARQL Query" style="width: 100%; font-family: monospace; font-weight: normal;"
                                          class="form-control ">${query.query}</textarea>
                            </label>
                        </div>

                        <div class="form-group">
                            <g:set var="formName" value="sparql"/>
                            <div class="btn-group">
                                <button type="submit" name="sparql" value="true" class="btn btn-primary">Search</button>
                            </div>
                        </div>
                    </g:form>

                </g:else>
            </div>

            <div class="col-md-4">
                <div class="panel-title">Sample Queries</div>


                <div class="panel-group" id="sample-queries-accordion">
                    <div class="panel panel-default">
                        <div class="panel-heading panel-title">
                            <a data-toggle="collapse" data-parent="#sample-queries-accordion" href="#q-ping">"Hello, World!"</a>
                        </div>

                        <div id="q-ping" class="panel-collapse collapse panel-body">
                            <p>
                                A simple test query. This query should return one row, with one column named 'greeting' containing the text 'Hello, World!'.
                            </p>
                            <pre style="overflow-x: scroll; white-space: pre; height:5em;">
select *
where {
  bind('Hello, World!' as ?greeting)
}
LIMIT 50
                            </pre>
                            <button class="btn btn-default pull-right">Copy</button>
                        </div>
                    </div>

                    <div class="panel panel-default">
                        <div class="panel-heading panel-title">
                            <a data-toggle="collapse" data-parent="#sample-queries-accordion" href="#query-find-string">Simple string search</a>
                        </div>

                        <div id="query-find-string" class="panel-collapse collapse panel-body">
                            <p>
                                This query finds 'Doodia aspera' or 'MONOTREMATA' anywhere in the data, and displays the subject and predicate. This type of
                                query can be a useful starting point for exploring the data.
                            </p>
                            <p>
                                The <tt>?match</tt> parameter is bound both as a typed and as an untyped literal in order to find the data however it appears.
                                Additionally, this type of search is case-sensitive. Higher taxa in AFD have ALLCAPS names.
                            </p>
                            <pre style="overflow-x: scroll; white-space: pre; height:5em;">
prefix g: &lt;http://biodiversity.org.au/voc/graph/GRAPH#&gt;
prefix xs: &lt;http://www.w3.org/2001/XMLSchema#&gt;

select *
where {
  {
    { bind('Doodia aspera' as ?match) }
    union { bind('MONOTREMATA' as ?match) }
    union { bind('Doodia aspera'^^xs:string as ?match) }
    union { bind('MONOTREMATA'^^xs:string as ?match) }
  }

  graph g:all {
    ?item ?predicate ?match
  }
}
ORDER BY ?item ?predicate
LIMIT 50
                                   </pre>
                            <button class="btn btn-default pull-right">Copy</button>
                        </div>
                    </div>


                    <div class="panel panel-default">
                        <div class="panel-heading panel-title">
                            <a data-toggle="collapse" data-parent="#sample-queries-accordion" href="#MetadataSampleQueries">Service Metadata</a>
                        </div>

                        <div id="MetadataSampleQueries" class="panel-collapse collapse panel-body container-fluid panel-group" id="metadata-queries-accordion">
                            <div class="panel panel-default">
                                <div class="panel-heading panel-title">
                                    <a data-toggle="collapse" data-parent="#metadata-queries-accordion" href="#sampleQueryListAllGraphs">List all graphs</a>
                                </div>

                                <div id="sampleQueryListAllGraphs" class="panel-collapse collapse panel-body container-fluid">
                                    <p>
                                        Data on our SPARQL service is organised into a set of named RDF graphs. This query asks the service
                                        for the list of graphs that it contains.
                                    </p>
                                    <p>
                                        This query does not itself specify a graph, and so takes data from the default graph. On our server,
                                        the default graph contains only this list of graphs.
                                    </p>
                                    <pre style="overflow-x: scroll; white-space: pre; height:5em;">
prefix rdf: &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;
prefix rdfs: &lt;http://www.w3.org/2000/01/rdf-schema#&gt;
prefix dcterms: &lt;http://purl.org/dc/terms/&gt;
prefix g: &lt;http://biodiversity.org.au/voc/graph/GRAPH#&gt;

select *
where {
  ?uri rdf:type g:GraphURI .
  OPTIONAL { ?uri rdfs:label ?label  } .
  OPTIONAL { ?uri dcterms:title ?title  } .
  OPTIONAL { ?uri dcterms:description ?desc  } .
}
ORDER BY ?uri
LIMIT 50
                                    </pre>
                                    <button class="btn btn-default pull-right">Copy</button>
                                </div>
                            </div>

                            <div class="panel panel-default">
                                <div class="panel-heading panel-title">
                                    <a data-toggle="collapse" data-parent="#metadata-queries-accordion" href="#sampleQueryAllVoc">List all vocabularies</a>
                                </div>

                                <div id="sampleQueryAllVoc" class="panel-collapse collapse panel-body container-fluid">
                                    <p>
                                        All of our vocabularies are available in the <tt>ibis_voc</tt> graph. This query fetches their URIs.
                                    </p>
                                    <pre style="overflow-x: scroll; white-space: pre; height:5em;">
prefix rdf: &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;
prefix rdfs: &lt;http://www.w3.org/2000/01/rdf-schema#&gt;
prefix dcterms: &lt;http://purl.org/dc/terms/&gt;
prefix g: &lt;http://biodiversity.org.au/voc/graph/GRAPH#&gt;
prefix owl: &lt;http://www.w3.org/2002/07/owl#&gt;

select *
where {
  graph g:ibis_voc {
    ?ont a owl:Ontology .
    optional { ?ont rdfs:label ?label }
    optional { ?ont dcterms:title ?title }
    optional { ?ont dcterms:description ?description }
  }
}
ORDER BY ?ont
LIMIT 500
                                    </pre>
                                    <button class="btn btn-default pull-right">Copy</button>
                                </div>
                            </div>



                            <div class="panel panel-default">
                                <div class="panel-heading panel-title">
                                        <a data-toggle="collapse" data-parent="#metadata-queries-accordion" href="#sampleQueryListAllClasses">List all classes</a>
                                </div>

                                <div id="sampleQueryListAllClasses" class="panel-collapse collapse panel-body container-fluid">
                                    <p>
                                         This query fetches all classes defined in any vocabulary. Some vocabularies use <tt>rdfs:Class</tt>, and some use <tt>owl:Class</tt>, so we take a union.
                                    </p>
                                    <pre style="overflow-x: scroll; white-space: pre; height:5em;">
prefix rdf: &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;
prefix rdfs: &lt;http://www.w3.org/2000/01/rdf-schema#&gt;
prefix dcterms: &lt;http://purl.org/dc/terms/&gt;
prefix g: &lt;http://biodiversity.org.au/voc/graph/GRAPH#&gt;
prefix owl: &lt;http://www.w3.org/2002/07/owl#&gt;

select *
where {
  graph g:ibis_voc {
    {
     { ?class a rdfs:Class . }
     union
     { ?class a owl:Class . }
    }
    optional { ?class rdfs:label ?label }
    optional { ?class dcterms:description ?description }
    optional { ?class rdfs:isDefinedBy ?definedBy }
  }
}
ORDER BY ?class
LIMIT 500
                                    </pre>
                                    <button class="btn btn-default pull-right">Copy</button>
                                </div>
                            </div>


                            <div class="panel panel-default">
                                <div class="panel-heading panel-title">
                                    <a data-toggle="collapse" data-parent="#metadata-queries-accordion" href="#sampleQueryNameVoc">List BOA Name vocabulary</a>
                                </div>

                                <div id="sampleQueryNameVoc" class="panel-collapse collapse panel-body container-fluid">
                                    <p>
                                        This query fetches all vocabulary items defined as part of the BOA 'name' vocabulary and shows their types.
                                    </p>
                                    <pre style="overflow-x: scroll; white-space: pre; height:5em;">
prefix rdf: &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;
prefix rdfs: &lt;http://www.w3.org/2000/01/rdf-schema#&gt;
prefix dcterms: &lt;http://purl.org/dc/terms/&gt;
prefix g: &lt;http://biodiversity.org.au/voc/graph/GRAPH#&gt;
prefix owl: &lt;http://www.w3.org/2002/07/owl#&gt;

select *
where {
  graph g:ibis_voc {
    ?item rdfs:isDefinedBy &lt;http://biodiversity.org.au/voc/boa/Name#ONTOLOGY&gt;
    optional { ?item rdfs:label ?label }
    optional { ?item dcterms:title ?title }
    optional { ?item dcterms:description ?description }
    optional { ?item a ?class }
  }
}
ORDER BY ?class ?item
LIMIT 500
                                    </pre>
                                    <button class="btn btn-default pull-right">Copy</button>
                                </div>
                            </div>
                        </div>
                    </div>


                    <div class="panel panel-default">
                        <div class="panel-heading panel-title">
                            <a data-toggle="collapse" data-parent="#sample-queries-accordion" href="#NSLSampleQueries">NSL Sample queries</a>
                        </div>

                        <div id="NSLSampleQueries" class="panel-collapse collapse panel-body container-fluid panel-group" id="nsl-queries-accordion">
                            <div class="panel panel-default">
                                <div class="panel-heading panel-title">
                                    <a data-toggle="collapse" data-parent="#nsl-queries-accordion" href="#sampleQueryNSLName">Find a name in NSL</a>
                                </div>

                                <div id="sampleQueryNSLName" class="panel-collapse collapse panel-body container-fluid">
                                    <p>
                                        Find 'Doodia aspera' in NSL using the boa 'nameComplete' property, and display all properties.
                                    </p>
                                    <pre style="overflow-x: scroll; white-space: pre; height:5em;">
prefix g: &lt;http://biodiversity.org.au/voc/graph/GRAPH#&gt;
prefix xs: &lt;http://www.w3.org/2001/XMLSchema#&gt;
prefix boa_name: &lt;http://biodiversity.org.au/voc/boa/Name#&gt;

select *
where {
  graph g:nsl {
    ?name boa_name:simpleName 'Doodia aspera' .
    ?name ?p ?o .
  }
}
ORDER BY ?name ?p ?o
LIMIT 50
                                   </pre>
                                    <button class="btn btn-default pull-right">Copy</button>
                                </div>
                            </div>


                            <div class="panel panel-default">
                                <div class="panel-heading panel-title">
                                    <a data-toggle="collapse" data-parent="#nsl-queries-accordion" href="#sampleQueryNSLInstance">Find name instances in NSL</a>
                                </div>

                                <div id="sampleQueryNSLInstance" class="panel-collapse collapse panel-body container-fluid">
                                    <p>
                                        Find 'Doodia aspera' in NSL using the boa 'simpleName' property. Then find all instances, and display the reference and instance type, and the related name if any.
                                    </p>
                                    <p>
                                        This example uses optional clauses to ensure that the identifiers are fetched.
                                    </p>
                                    <pre style="overflow-x: scroll; white-space: pre; height:5em;">
prefix xs: &lt;http://www.w3.org/2001/XMLSchema#&gt;
prefix rdfs: &lt;http://www.w3.org/2000/01/rdf-schema#&gt;
prefix g: &lt;http://biodiversity.org.au/voc/graph/GRAPH#&gt;
prefix boa_name: &lt;http://biodiversity.org.au/voc/boa/Name#&gt;
prefix boa_inst: &lt;http://biodiversity.org.au/voc/boa/Instance#&gt;
prefix boa_ref: &lt;http://biodiversity.org.au/voc/boa/Reference#&gt;

select ?inst ?fullName ?ref ?page ?inst_type ?of
where {
  graph g:nsl {
    ?name boa_name:simpleName 'Doodia aspera' .
    optional { ?name boa_name:fullNameHtml ?fullName }
    ?inst boa_inst:name ?name .
    optional { ?inst boa_inst:type/rdfs:label ?inst_type }
    optional { ?inst boa_inst:page ?page }
    optional { ?inst boa_inst:reference/boa_ref:citationHtml ?ref . }
    optional { ?inst boa_inst:citedBy/boa_inst:name/boa_name:fullNameHtml ?of . }
  }
}
ORDER BY ?inst ?ref
LIMIT 50
                                   </pre>
                                    <button class="btn btn-default pull-right">Copy</button>
                                </div>
                            </div>

                            <div class="panel panel-default">
                                <div class="panel-heading panel-title">
                                    <a data-toggle="collapse" data-parent="#nsl-queries-accordion" href="#sampleQueryAPCAccepted">APC Accepted Name</a>
                                </div>

                                <div id="sampleQueryAPCAccepted" class="panel-collapse collapse panel-body container-fluid">
                                    <p>
                                        Find the APC accepted name for a series of simple names.
                                    </p>
                                    <p>
                                        Notice that we use a URI to specify the APC tree. This URI is the persistent semantic web identifier for the APC tree at biodiversity.org.au .
                                    </p>
                                    <p>
                                        This is a complex query that can take several seconds to execute. The <a href="http://biodiversity.org.au/dataexport/html/tnrs.html">taxon name matching service</a> executes more quickly
                                        by taking advantage of certain predicates specific to the National Species List application. This query uses only the BOA vocabulary.
                                    </p>
                                    <pre style="overflow-x: scroll; white-space: pre; height:5em;">
prefix xs: &lt;http://www.w3.org/2001/XMLSchema#&gt;
prefix rdfs: &lt;http://www.w3.org/2000/01/rdf-schema#&gt;
prefix g: &lt;http://biodiversity.org.au/voc/graph/GRAPH#&gt;
prefix boa_name: &lt;http://biodiversity.org.au/voc/boa/Name#&gt;
prefix boa_inst: &lt;http://biodiversity.org.au/voc/boa/Instance#&gt;
prefix boa_ref: &lt;http://biodiversity.org.au/voc/boa/Reference#&gt;
prefix boa_tree: &lt;http://biodiversity.org.au/voc/boa/Tree#&gt;

select ?match ?matchedName ?matchedIn ?type ?acceptedName ?acceptedRef
where {
  {
    { bind('Onoclea nuda' as ?match) }
    union { bind('Salpichlaena orientalis' as ?match) }
    union { bind('Stegania lanceolata' as ?match) }
    union { bind('Blechnum' as ?match) }
  }

  graph g:nsl {
    ?nameURI boa_name:simpleName ?match .
    OPTIONAL {
      ?taxonURI boa_inst:name ?nameURI .
      {
        {
          ?taxonURI boa_inst:citedBy ?acceptedTaxonURI .
          ?nameURI boa_name:fullNameHtml ?matchedName .
          optional { ?taxonURI boa_inst:cites/boa_inst:reference/boa_ref:citationHtml ?matchedIn . }
        }
        UNION
        {
          BIND ( ?taxonURI as ?acceptedTaxonURI ) .
        }
      }
      ?node boa_tree:taxon ?acceptedTaxonURI .
      ?node a boa_tree:CurrentNode .
      ?node boa_tree:tree &lt;http://biodiversity.org.au/boa/tree/APC&gt; .
      ?taxonURI boa_inst:type/rdfs:label ?type .
      OPTIONAL { ?node boa_tree:name/boa_name:fullNameHtml ?acceptedName }
      OPTIONAL { ?acceptedTaxonURI boa_inst:reference/boa_ref:citationHtml ?acceptedRef . }
    }
  }
}
LIMIT 50
                                    </pre>
                                    <button class="btn btn-default pull-right">Copy</button>
                                </div>
                            </div>




                            <div class="panel panel-default">
                                <div class="panel-heading panel-title">
                                    <a data-toggle="collapse" data-parent="#nsl-queries-accordion" href="#sampleQueryAPCPlacement">APC Name Placement</a>
                                </div>

                                <div id="sampleQueryAPCPlacement" class="panel-collapse collapse panel-body container-fluid">
                                    <p>
                                        Find all placements of 'Malvaceae' in APC and sort them by date.
                                    </p>
                                    <p>
                                        Notice that we use a URI to specify the APC tree. This URI is the persistent semantic web identifier for the APC tree at biodiversity.org.au .
                                    </p>
                                    <pre style="overflow-x: scroll; white-space: pre; height:5em;">
prefix xs: &lt;http://www.w3.org/2001/XMLSchema#&gt;
prefix rdfs: &lt;http://www.w3.org/2000/01/rdf-schema#&gt;
prefix g: &lt;http://biodiversity.org.au/voc/graph/GRAPH#&gt;
prefix boa_name: &lt;http://biodiversity.org.au/voc/boa/Name#&gt;
prefix boa_inst: &lt;http://biodiversity.org.au/voc/boa/Instance#&gt;
prefix boa_tree: &lt;http://biodiversity.org.au/voc/boa/Tree#&gt;

select ?fullName ?node ?from_ts ?to_ts
where {
  graph g:nsl {
    ?name boa_name:simpleName 'Malvaceae' .
    optional { ?name boa_name:fullNameHtml ?fullName . }

    ?node boa_tree:name ?name .
    ?node boa_tree:tree &lt;http://biodiversity.org.au/boa/tree/APC&gt; .
    optional { ?node boa_tree:checkedInAt/boa_tree:eventTimeStamp ?from_ts . }
    optional { ?node boa_tree:replacedAt/boa_tree:eventTimeStamp ?to_ts . }
  }
}
ORDER BY ?name ?from_ts
LIMIT 50
                                    </pre>
                                    <button class="btn btn-default pull-right">Copy</button>
                                </div>
                            </div>


                            <div class="panel panel-default">
                                <div class="panel-heading panel-title">
                                    <a data-toggle="collapse" data-parent="#nsl-queries-accordion" href="#sampleQueryAPCPlacementSynonyms">Synonyms and APC Subnames</a>
                                </div>

                                <div id="sampleQueryAPCPlacementSynonyms" class="panel-collapse collapse panel-body container-fluid">
                                    <p>
                                        Find 'Blechnum' in APC, find its immediate child nodes, then get all synonymns for all of these names.
                                    </p>
                                    <p>
                                        Notice that we use a URI to specify the APC tree. This URI is the persistent semantic web identifier for the APC tree at biodiversity.org.au .
                                    </p>
                                    <p>
                                        This is a complex query and can take several seconds to execute.
                                    </p>
                                    <pre style="overflow-x: scroll; white-space: pre; height:5em;">
prefix xs: &lt;http://www.w3.org/2001/XMLSchema#&gt;
prefix rdfs: &lt;http://www.w3.org/2000/01/rdf-schema#&gt;
prefix g: &lt;http://biodiversity.org.au/voc/graph/GRAPH#&gt;
prefix boa_name: &lt;http://biodiversity.org.au/voc/boa/Name#&gt;
prefix boa_inst: &lt;http://biodiversity.org.au/voc/boa/Instance#&gt;
prefix boa_ref: &lt;http://biodiversity.org.au/voc/boa/Reference#&gt;
prefix boa_tree: &lt;http://biodiversity.org.au/voc/boa/Tree#&gt;

select ?nameHtml ?nameUri ?in ?instType ?of ?placement
where {
  graph g:nsl {
    ?name boa_name:simpleName 'Blechnum' .

    ?node boa_tree:name ?name .
    ?node boa_tree:tree &lt;http://biodiversity.org.au/boa/tree/APC&gt; .
    ?node a boa_tree:CurrentNode .

    {
      {
        ?node boa_tree:taxon ?apc_inst .
      }
      union {
        ?link boa_tree:linkSup ?node .
        ?link boa_tree:linkSub/boa_tree:taxon ?apc_inst .
        optional { ?link boa_tree:linkType ?placement . }
      }
    }

    {
      { 
        bind(?apc_inst as ?inst) .
      }
      union {
        ?inst boa_inst:citedBy ?apc_inst .
        ?apc_inst boa_inst:name/boa_name:fullNameHtml ?of .
      }
    }
    
    ?inst boa_inst:type/rdfs:label ?instType .
    ?inst boa_inst:name ?nameUri .
    optional { ?inst boa_inst:cites/boa_inst:reference/boa_ref:citationHtml ?in . }
    ?nameUri boa_name:fullNameHtml ?nameHtml .
    ?nameUri boa_name:simpleName ?simpleName .

  }
}
ORDER BY ?simpleName
LIMIT 50                                        
                                    </pre>
                                    <button class="btn btn-default pull-right">Copy</button>
                                </div>
                            </div>





                        </div>
                    </div>





                    <div class="panel panel-default">
                        <div class="panel-heading panel-title">
                            <a data-toggle="collapse" data-parent="#sample-queries-accordion" href="#AFDSampleQueries">AFD Sample Queries</a>
                        </div>

                        <div id="AFDSampleQueries" class="panel-collapse collapse panel-body container-fluid panel-group" id="afd-queries-accordion">
                            <div class="panel panel-default">
                                <div class="panel-heading panel-title">
                                    <a data-toggle="collapse" data-parent="#afd-queries-accordion" href="#sampleQueryAFDName">Find a name in AFD</a>
                                </div>

                                <div id="sampleQueryAFDName" class="panel-collapse collapse panel-body container-fluid">
                                    <p>
                                        Find MONOTREMATA in AFD based on the TDWG 'nameComplete' property, and display all properties.
                                    </p>
                                    <pre style="overflow-x: scroll; white-space: pre; height:5em;">
prefix xs: &lt;http://www.w3.org/2001/XMLSchema#&gt;
prefix rdf: &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;
prefix rdfs: &lt;http://www.w3.org/2000/01/rdf-schema#&gt;
prefix owl: &lt;http://www.w3.org/2002/07/owl#&gt;
prefix dc: &lt;http://purl.org/dc/elements/1.1/&gt;
prefix dcterms: &lt;http://purl.org/dc/terms/&gt;
prefix tn: &lt;http://rs.tdwg.org/ontology/voc/TaxonName#&gt;
prefix tc: &lt;http://rs.tdwg.org/ontology/voc/TaxonConcept#&gt;
prefix pc: &lt;http://rs.tdwg.org/ontology/voc/PublicationCitation#&gt;
prefix tcomm: &lt;http://rs.tdwg.org/ontology/voc/Common#&gt;
prefix ibis: &lt;http://biodiversity.org.au/voc/ibis/IBIS#&gt;
prefix afd: &lt;http://biodiversity.org.au/voc/afd/AFD#&gt;
prefix apni: &lt;http://biodiversity.org.au/voc/apni/APNI#&gt;
prefix apc: &lt;http://biodiversity.org.au/voc/apc/APC#&gt;
prefix afdp: &lt;http://biodiversity.org.au/voc/afd/profile#&gt;
prefix apnip: &lt;http://biodiversity.org.au/voc/apni/profile#&gt;
prefix g: &lt;http://biodiversity.org.au/voc/graph/GRAPH#&gt;

select *
where {
  graph g:afd {
    ?monotremata_n tn:nameComplete "MONOTREMATA"^^xs:string .
    ?monotremata_n ?p ?o
  }
}
ORDER BY ?p
LIMIT 50
                                    </pre>
                                    <button class="btn btn-default pull-right">Copy</button>
                                </div>
                            </div>

                            <div class="panel panel-default">
                                <div class="panel-heading panel-title">
                                    <a data-toggle="collapse" data-parent="#afd-queries-accordion" href="#sampleQueryAFDTaxon">Taxon species in AFD</a>
                                </div>

                                <div id="sampleQueryAFDTaxon" class="panel-collapse collapse panel-body container-fluid">
                                    <p>
                                        Find species of MACROPODIDAE in AFD using the ibis:isPartOfTaxon predicate.
                                    </p>
                                    <pre style="overflow-x: scroll; white-space: pre; height:5em;">
prefix xs: &lt;http://www.w3.org/2001/XMLSchema#&gt;
prefix rdf: &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;
prefix rdfs: &lt;http://www.w3.org/2000/01/rdf-schema#&gt;
prefix owl: &lt;http://www.w3.org/2002/07/owl#&gt;
prefix dc: &lt;http://purl.org/dc/elements/1.1/&gt;
prefix dcterms: &lt;http://purl.org/dc/terms/&gt;
prefix tn: &lt;http://rs.tdwg.org/ontology/voc/TaxonName#&gt;
prefix tc: &lt;http://rs.tdwg.org/ontology/voc/TaxonConcept#&gt;
prefix pc: &lt;http://rs.tdwg.org/ontology/voc/PublicationCitation#&gt;
prefix tcomm: &lt;http://rs.tdwg.org/ontology/voc/Common#&gt;
prefix ibis: &lt;http://biodiversity.org.au/voc/ibis/IBIS#&gt;
prefix afd: &lt;http://biodiversity.org.au/voc/afd/AFD#&gt;
prefix apni: &lt;http://biodiversity.org.au/voc/apni/APNI#&gt;
prefix apc: &lt;http://biodiversity.org.au/voc/apc/APC#&gt;
prefix afdp: &lt;http://biodiversity.org.au/voc/afd/profile#&gt;
prefix apnip: &lt;http://biodiversity.org.au/voc/apni/profile#&gt;
prefix g: &lt;http://biodiversity.org.au/voc/graph/GRAPH#&gt;

select ?genusPart ?specificEpithet ?authorship ?sp_n
where {
  graph g:afd {
     ?macropodidae_n tn:nameComplete "MACROPODIDAE"^^xs:string .
     ?macropodidae_t tc:hasName ?macropodidae_n .
     ?sp_t ibis:isPartOfTaxon ?macropodidae_t .
     ?sp_t tc:hasName ?sp_n .
     ?sp_n tn:rank &lt;http://rs.tdwg.org/ontology/voc/TaxonRank#Species&gt; .
     ?sp_n tn:genusPart ?genusPart .
     ?sp_n tn:specificEpithet ?specificEpithet .
     ?sp_n tn:authorship ?authorship .
  }
}
ORDER BY ?genusPart ?specificEpithet
LIMIT 50
                                    </pre>
                                    <button class="btn btn-default pull-right">Copy</button>
                                </div>
                            </div>


                            <div class="panel panel-default">
                                <div class="panel-heading panel-title">
                                    <a data-toggle="collapse" data-parent="#afd-queries-accordion" href="#sampleQueryAFDProfile">AFD Profile data</a>
                                </div>

                                <div id="sampleQueryAFDProfile" class="panel-collapse collapse panel-body container-fluid">
                                    <p>
                                        Find macropods known to occur in Victora, and any extra distribution text.
                                    </p>
                                    <pre style="overflow-x: scroll; white-space: pre; height:5em;">
prefix xs: &lt;http://www.w3.org/2001/XMLSchema#&gt;
prefix rdf: &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;
prefix rdfs: &lt;http://www.w3.org/2000/01/rdf-schema#&gt;
prefix owl: &lt;http://www.w3.org/2002/07/owl#&gt;
prefix dc: &lt;http://purl.org/dc/elements/1.1/&gt;
prefix dcterms: &lt;http://purl.org/dc/terms/&gt;
prefix tn: &lt;http://rs.tdwg.org/ontology/voc/TaxonName#&gt;
prefix tc: &lt;http://rs.tdwg.org/ontology/voc/TaxonConcept#&gt;
prefix pc: &lt;http://rs.tdwg.org/ontology/voc/PublicationCitation#&gt;
prefix tcomm: &lt;http://rs.tdwg.org/ontology/voc/Common#&gt;
prefix ibis: &lt;http://biodiversity.org.au/voc/ibis/IBIS#&gt;
prefix afd: &lt;http://biodiversity.org.au/voc/afd/AFD#&gt;
prefix apni: &lt;http://biodiversity.org.au/voc/apni/APNI#&gt;
prefix apc: &lt;http://biodiversity.org.au/voc/apc/APC#&gt;
prefix afdp: &lt;http://biodiversity.org.au/voc/afd/profile#&gt;
prefix apnip: &lt;http://biodiversity.org.au/voc/apni/profile#&gt;
prefix g: &lt;http://biodiversity.org.au/voc/graph/GRAPH#&gt;

select ?sp_n_title ?extraDistInfo
where {
  graph g:afd {
     ?macropodidae_n tn:nameComplete "MACROPODIDAE"^^xs:string .
     ?macropodidae_t tc:hasName ?macropodidae_n .
     ?sp_t ibis:isPartOfTaxon ?macropodidae_t .
     ?sp_t tc:hasName ?sp_n .
     ?sp_n dcterms:title  ?sp_n_title .
     ?sp_t afdp:hasDistribution ?distributionRecord .
     ?distributionRecord afdp:hasAusstateRegion
             &lt;http://biodiversity.org.au/voc/afd/Term#region.ausstate.Vic&gt; .
     OPTIONAL { ?distributionRecord afdp:distExtra ?extraDistInfo . } .
  }
}
ORDER BY ?genusPart ?specificEpithet
LIMIT 50
                                    </pre>
                                    <button class="btn btn-default pull-right">Copy</button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script type="text/javascript">
    //<![CDATA[

    $(function() {
        $("#sample-queries-accordion").find('button').click(function(event) {
            $("#SPARQLQUERY").text($(event.target).parent().find('> pre').text())
        });
    });

    //]]>
</script>


<g:if test="${query.sparql}">
    <script type="text/javascript">
        //<![CDATA[

        // I will jam all the javascript in here for a quick win.

        $(function () {
            send_sparql_query();
        });

        function send_sparql_query() {
            var query = document.getElementById("SPARQLQUERY").value;

            clear_sparql_result();
            sparql_spinner_on();

            $.ajax("/sparql/", {
                data: {
                    output: "json",
                    query: query // encodeURIComponent(
                },
                dataType: 'json',
                cache: 'false',
                success: function (data, textStatus, jqXHR) {
                    display_sparql_result(data);
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    display_sparql_error(jqXHR.responseText);
                }
            });

        }


        //]]>
    </script>
</g:if>


