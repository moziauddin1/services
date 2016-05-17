package au.org.biodiversity.nsl;

import grails.converters.JSON;
import org.apache.log4j.Logger;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.subject.Subject;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.json.JSONElement;
import org.codehaus.groovy.grails.web.json.JSONObject;

import java.io.IOException;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
     * @todo implement the rest of JWT
     */

    public static JsonToken buildUsingCredentials(String credentials) {
        try {
            if (credentials == null) return null;

            int dot1 = credentials.indexOf('.');
            if (dot1 == -1) return null;
            int dot2 = dot1 + 1 + credentials.substring(dot1 + 1).indexOf('.');
            if (dot2 == dot1) return null;

            String header = Base64.decodeToString(credentials.substring(0, dot1));
            String payload = Base64.decodeToString(credentials.substring(dot1 + 1, dot2));
            String signature = credentials.substring(dot2 + 1);

            JSONObject headerJ = (JSONObject) JSON.parse(header);
            JSONObject payloadJ = (JSONObject) JSON.parse(payload);

            String principal = payloadJ.getString("sub"); // JWT subject

            return new JsonToken(credentials, principal);
        }
        catch(RuntimeException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Build a token for a subject. This builder method will require more parameters - timeouts, certificates and so on.
     * These things are owned by the JsonTokenRealm class. Perhaps the code in this builder really belongs there.
     * @todo implement the rest of JWT
     */

    public static JsonToken buildUsingSubject(Subject subject, String secret) {
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

        JSONObject header = new JSONObject() ;
        JSONObject payload = new JSONObject() ;

        header.put("type", "JWT");
        // the algorithm indicates to other systems that they cannot be expected to understand this
        header.put("alg", "nsl-simple-check");

        payload.put("sub", principal);

        String header64 = Base64.encodeToString(header.toString().getBytes());
        String payload64 = Base64.encodeToString(payload.toString().getBytes());
        String signature64 = Base64.encodeToString(buildSignature(principal, secret)); // signature is an empty string

        return new JsonToken(header64 + "." + payload64 + "." + signature64, principal);
    }


    private JsonToken(String credentials, String principal) {
        this.credentials = credentials;
        this.principal = principal;
    }

    public static byte[] buildSignature(String principal, String secret)  {
        String foo = principal + '/' + secret;

        MessageDigest sha ;
        try {
            sha = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("THIS NEVER HAPPENS", e);
        }

        return  sha.digest(foo.getBytes());
    }

    public byte[] getSignature() {
        return Base64.decode(credentials.substring(credentials.lastIndexOf('.')+1));
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
