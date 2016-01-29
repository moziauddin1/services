package au.org.biodiversity.nsl;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;

/**
 * Created by ibis on 28/01/2016.
 */
public class JsonToken implements AuthenticationToken {
    String principal;

    // not using any security at all just yet

    /**
     * Build a JSON token from an HTTP header supplied by a client
     * @param tok token supplied by an HTTP request
     */
    public JsonToken(String tok) {
        principal = tok;
    }

    /**
     * Build a JSON token from the currently logged-in user
     * @param tok the current shiro security user
     */
    public JsonToken(Subject tok) {
        // just some bulletproofing. this was causing issues
        if(tok == null) {
            principal = null;
            new Throwable("tok is null").printStackTrace();
        }
        else if(tok.getPrincipal() == null) {
            principal = null;
            new Throwable("principal is null").printStackTrace();
        }
        else {
            principal = tok.getPrincipal().toString();
        }
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public Object getCredentials() {
        return principal;
    }
}
