package au.org.biodiversity.nsl

/**
 * Created by ibis on 28/01/2016.
 */
class JsonTokenRealm {
//    LdapRealm ldapRealm

    static authTokenClass = JsonToken

    String authenticate(JsonToken authToken) {
        // no checking whatsoever
        return authToken.principal
    }

    // do we actually need these methods? Or will shiro check all the realms
    // no matter which realm you logged on under?

    def hasRole(principal, roleName) {
        return false // ldapRealm.hasRole(principal, roleName)
    }

    def hasAllRoles(principal, roles) {
        return false // ldapRealm.hasAllRoles(principal, roles)
    }

    def isPermitted(principal, requiredPermission) {
        return false // ldapRealm.isPermitted(principal, requiredPermission)
    }
}
