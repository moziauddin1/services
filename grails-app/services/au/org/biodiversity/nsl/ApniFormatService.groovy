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

package au.org.biodiversity.nsl

import grails.transaction.Transactional

@Transactional
class ApniFormatService {

    def classificationService

    Map getNameModel(Name name) {
        Name familyName = RankUtils.getParentOfRank(name, 'Familia')
        Node apc = classificationService.isNameInAPC(name)
        Map model = [name: name, apc: apc, familyName: familyName]
        model.putAll(nameReferenceInstanceMap(name) as Map)
        return model
    }

    List<Instance> sortInstances(Name name) {
        name?.instances?.sort { a, b ->
            Integer yearA = a.reference.year ?: 4000
            Integer yearB = b.reference.year ?: 4000
            if (yearA == yearB) {
                a.reference.citation <=> b.reference.citation
            } else {
                yearA <=> yearB
            }
        }

    }

    Map nameReferenceInstanceMap(Name name) {
        Map<Reference, List<Instance>> refGroups = name.instances.groupBy { instance -> instance.reference }
        List<Reference> references = new ArrayList(refGroups.keySet())
        references.sort { a, b ->
            //NSL-1119 protolog references must come first so if a reference contains a protologue instance make it win
            Integer aProto = refGroups[a].find { Instance i -> i.instanceType.protologue } ? 1 : 0
            Integer bProto = refGroups[b].find { Instance i -> i.instanceType.protologue } ? 1 : 0
            //NSL-1119 primary references must come next
            Integer aPrimary = refGroups[a].find { Instance i -> i.instanceType.primaryInstance } ? 1 : 0
            Integer bPrimary = refGroups[b].find { Instance i -> i.instanceType.primaryInstance } ? 1 : 0
            if (a.year == b.year) {
                if (aProto == bProto) {
                    if (aPrimary == bPrimary) {
                        if (a == b) {
                            if (a.pages == b.pages) {
                                return a.id <=> b.id  //highest Id first
                            }
                            return a.pages <=> b.pages //reverse string sort 1s, then 2s
                        }
                        return a.citation <=> b.citation //alpha sort by reference citation
                    }
                    return bPrimary <=> aPrimary // primary reference first (1)
                }
                return bProto <=> aProto // proto reference first (1)
            }
            return (a.year) <=> (b.year) //lowest year first
        }
        return [references: references, instancesByRef: refGroups]
    }

    static String transformXics(String text) {
        xicsMap.each { k, v ->
            text = text.replaceAll(k, v)
        }
        return HTMLSanitiser.encodeInvalidMarkup(text).trim()
    }

    /**
     * This is a sequential list of regular expressions and replacements. The sequence is meaningful as replacements are
     * re-used to get some sequences to work. Yes it's ugly but works. The result needs to be parsed by a HTML parser to
     * make sure valid HTML exists since the input may contain invalid html, so there may not be an opening or closing tag.
     */
    static Map<String, String> xicsMap = ['~J'                : '&Aacute;',
                                          '~c'                : '&Agrave;',
                                          '~T'                : '&Acirc;',
                                          '~p'                : '&Auml;',
                                          '<AOU>'             : '&Aring;',
                                          '~K'                : '&Eacute;',
                                          '~d'                : '&Egrave;',
                                          '~U'                : '&Ecirc;',
                                          '~q'                : '&Euml;',
                                          '~L'                : '&Iacute;',
                                          '~3'                : '&Igrave;',
                                          '~V'                : '&Icirc;',
                                          '~r'                : '&Iuml;',
                                          '~M'                : '&Oacute;',
                                          '~e'                : '&Ograve;',
                                          '~W'                : '&Ocirc;',
                                          '~s'                : '&Ouml;',
                                          '~N'                : '&Uacute;',
                                          '~f'                : '&Ugrave;',
                                          '\\\\'              : '&Ucirc;',
                                          '~t'                : '&Uuml;',
                                          '~z'                : '&Ccedil;',
                                          '~1'                : '&Ntilde;',
                                          '~O'                : '&aacute;',
                                          '~g'                : '&agrave;',
                                          '~X'                : '&acirc;',
                                          '~u'                : '&auml;',
                                          '<AOL>'             : '&aring;',
                                          '<ATL>'             : '&atilde;',
                                          '~P'                : '&eacute;',
                                          '~h'                : '&egrave;',
                                          '~Y'                : '&ecirc;',
                                          '~v'                : '&euml;',
                                          '~Q'                : '&iacute;',
                                          '~i'                : '&igrave;',
                                          '~Z'                : '&icirc;',
                                          '~w'                : '&iuml;',
                                          '~R'                : '&oacute;',
                                          '~D'                : '&ograve;',
                                          '~a'                : '&ocirc;',
                                          '~x'                : '&ouml;',
                                          '<OTL>'             : '&otilde;',
                                          '~S'                : '&uacute;',
                                          '~E'                : '&ugrave;',
                                          '~b'                : '&ucirc;',
                                          '~y'                : '&uuml;',
                                          '~0'                : '&ccedil;',
                                          '~2'                : '&ntilde;',
                                          '<PM>'              : '&plusmn;',
                                          '<MR>'              : '&#151;',
                                          '<NR>'              : '&#150;',
                                          '<BY>'              : '&times;',
                                          '<DEG>'             : '&deg;',
                                          '<MIN>'             : '&prime;',
                                          '\\^I'              : '&#134;',
                                          '\\^K'              : '&#138;',
                                          '\\^k'              : '1/2',
                                          '\\^l'              : '<13>',
                                          '\\^m'              : '<23>',
                                          '\\^n'              : '1/4',
                                          '\\^o'              : '3/4',
                                          '\\^u'              : '&alpha;',
                                          '\\^v'              : '&beta;',
                                          '\\^w'              : '&gamma;',
                                          '\\^x'              : '&delta;',
                                          '\\^t'              : '=',
                                          '<13>'              : '1/3',
                                          '<23>'              : '2/3',
                                          '<IT>'              : '<i>',
                                          '<i>'               : '<i>',
                                          '</i>'              : '</i>',
                                          '<RO>'              : '</i>',
                                          '<RRO>'             : '</i>',
                                          '<MALE>'            : '&#9794;',
                                          '<M>'               : '&#9794;',
                                          '<FM>'              : '&#9792;',
                                          '<F,8>&uacute;<F,4>': '&Delta;',
                                          '<F,17>&auml;<F,4>' : '&#10218;',
                                          '<F,17>&auml; <F,4>': '&#10219;',
                                          '<F,17>&alpha;<F,4>': '&#10219;'
    ]
}
