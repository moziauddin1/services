package au.org.biodiversity.nsl;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;

/**
 * At present, a JsonToken is nothing but the principal - no password, no nothing, not even any actual JSON.
 *
 * Ultimately, we would implement JWT protocol or something else. The "job" of this class would be to
 * encode and decode a slab of base64 encoded JSON. The checking as to whether the decoded token
 * is legit belongs in JsonTokenRealm. This class purely concernes itself with whether or not
 * the token is parseable.
 *
 *
 * @todo Implement Java Web Token (JWT) protocol
 * @author Created by ibis on 28/01/2016.
 */
public class JsonToken implements AuthenticationToken {
    final String credentials;
    final String principal;

    /**
     * Rehydrate a credentials string as given by the getCredentials method.
     * @todo this method will need to parse JSON
     */

    public static JsonToken buildUsingCredentials(String credentials) {
        return new JsonToken(credentials, credentials);
    }

    /**
     * Build a token for a subject. This builder method will require more parameters - timeouts, certificates and so on.
     * @todo implement the rest of JWT
     */

    public static JsonToken buildUsingSubject(Subject subject) {
        String principal;

        if(subject == null) {
            principal = null;
        }
        else if(subject.getPrincipal() == null) {
            principal = null;
        }
        else {
            principal = subject.getPrincipal().toString();
        }

        return new JsonToken(principal, principal);

    }


    private JsonToken(String credentials, String principal) {
        this.credentials = credentials;
        this.principal = principal;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }
}
