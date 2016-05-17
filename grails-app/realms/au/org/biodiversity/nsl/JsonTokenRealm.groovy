package au.org.biodiversity.nsl

import org.apache.shiro.authc.AuthenticationException
import org.codehaus.groovy.grails.commons.GrailsApplication

/**
 * Created by ibis on 28/01/2016.
 */
class JsonTokenRealm {
    static authTokenClass = JsonToken

    GrailsApplication grailsApplication;

    /* todo: implement the 'real' JWT algorithm */

    String authenticate(JsonToken authToken) {
        byte[] check = JsonToken.buildSignature(authToken.principal, grailsApplication.config.nslServices.jwt.secret);

        if(Arrays.equals(authToken.getSignature(), check)) {
            return authToken.principal
        }
        else {
            throw new AuthenticationException("JWT not recognised");
        }
    }
}
