package au.org.biodiversity.nsl

import grails.converters.XML
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.converters.marshaller.NameAwareMarshaller
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller

/**
 * User: pmcneil
 * Date: 23/02/15
 *
 */
class XmlMapMarshaller implements ObjectMarshaller<XML>, NameAwareMarshaller {

    @Override
    String getElementName(Object o) {
        return 'data'
    }

    @Override
    boolean supports(Object object) {
        return object instanceof Map
    }

    @Override
    void marshalObject(Object object, XML converter) throws ConverterException {
        Map map = (Map) object

        for(entry in map) {
            converter.startNode(entry.key as String)
            if(entry.value instanceof String) {
                converter.chars(entry.value as String)
            } else if(entry.value instanceof Boolean) {
                converter.attribute('is', entry.value as String)
            } else {
                converter.convertAnother(entry.value)
            }
            converter.end()
        }
    }
}
