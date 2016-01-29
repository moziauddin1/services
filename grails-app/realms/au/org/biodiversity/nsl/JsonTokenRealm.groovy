package au.org.biodiversity.nsl

/**
 * Created by ibis on 28/01/2016.
 */
class JsonTokenRealm {
    static authTokenClass = JsonToken

    String authenticate(JsonToken authToken) {
        // no checking whatsoever
        return authToken.principal
    }

    // do we actually need these methods? Or will shiro check all the realms
    // no matter which realm you logged on under?

    // it seems to check all the realms for the principal, which I thought would be wrong
    // the whole point of a security realm is that the objects in each realm are
    // nothing to do with one another, even if they happen to have the same id
}
