package au.org.biodiversity.nsl

import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.AuthenticationException
import org.codehaus.groovy.grails.commons.GrailsApplication

import javax.crypto.spec.SecretKeySpec

/**
 * Created by ibis on 28/01/2016.
 */
class JsonTokenRealm {
    static authTokenClass = JsonToken

    GrailsApplication grailsApplication;

    String authenticate(JsonToken authToken) {
        SecretKeySpec signingKey = new SecretKeySpec((grailsApplication.config.nslServices.jwt.secret as String).getBytes('UTF-8'), 'plain text');
        authToken.verify(signingKey);
        return authToken.principal;
    }
}
