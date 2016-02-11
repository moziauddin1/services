package au.org.biodiversity.nsl

/**
 * Created by ibis on 28/01/2016.
 */
class JsonTokenRealm {
    static authTokenClass = JsonToken

    /*
    todo: certificates and whatnot
    This method may be the correct place to do timeouts, certificate stamping and whatnot.
     */

    String authenticate(JsonToken authToken) {
        // no checking whatsoever
        return authToken.principal
    }

    void foo() {

    }
}
