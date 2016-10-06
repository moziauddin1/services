package au.org.biodiversity.nsl;

import grails.converters.JSON;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.codec.CodecException;
import org.apache.shiro.subject.Subject;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.json.JSONObject;

import javax.crypto.Mac;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class JsonToken implements AuthenticationToken {
    final String signable;
    final byte[] signature;
    final String credentials;
    final String principal;

    /**
     * Rehydrate a credentials string as given by the getCredentials method.
     */

    public static JsonToken buildUsingCredentials(String credentials) throws AuthenticationException {
        try {
            if (credentials == null) return null;

            int dot1 = credentials.indexOf('.');
            if (dot1 == -1) throw new AuthenticationException("Credentials are not a JWT");
            int dot2 = dot1 + 1 + credentials.substring(dot1 + 1).indexOf('.');
            if (dot2 == dot1) throw new AuthenticationException("Credentials are not a JWT");

            String header = base64UrlDecode(credentials.substring(0, dot1));
            String payload = base64UrlDecode(credentials.substring(dot1 + 1, dot2));
            byte[] signature = Base64.decode(credentials.substring(dot2 + 1));

            JSONObject headerJ = (JSONObject) JSON.parse(header);
            JSONObject payloadJ = (JSONObject) JSON.parse(payload);

            if (!"JWT".equals(headerJ.getString("type")))
                throw new AuthenticationException("Credentials are not a JWT");
            if (!"HS256".equals(headerJ.getString("alg")))
                throw new AuthenticationException("Unrecognised JWT algorithm " + headerJ.getString("alg"));

            String principal = payloadJ.getString("sub"); // JWT subject. may be null.

            return new JsonToken(credentials.substring(0, dot2), signature, credentials, principal);
        } catch (CodecException | ConverterException ex) {
            throw new AuthenticationException("Credentials are not a JWT", ex);
        }
    }

    /**
     * Build a token for a subject. This builder method will require more parameters - timeouts, certificates and so on.
     * These things are "known about" by JsonTokenRealm (which verifies tokens) and by AuthController (which builds them).
     */

    public static JsonToken buildUsingSubject(Subject subject, Key secret) throws InvalidKeyException {
        String principal;

        if (subject == null) {
            principal = null;
        } else if (subject.getPrincipal() == null) {
            principal = null;
        } else {
            principal = subject.getPrincipal().toString();
        }

        return buildUsingPrincipal(principal, secret);
    }

    public static JsonToken buildUsingPrincipal(String principal, Key secret) throws InvalidKeyException {
        JSONObject header = new JSONObject();
        JSONObject payload = new JSONObject();

        header.put("type", "JWT");
        header.put("alg", "HS256");

        payload.put("sub", principal);

        String signable = base64UrlEncode(header.toString()) + "." + base64UrlEncode(payload.toString());
        byte[] signature = buildSignature(secret, signable);
        String credentials = signable + "." + Base64.encodeToString(signature);

        return new JsonToken(signable, signature, credentials, principal);
    }

    private static String base64UrlEncode(String plain) {
        try {
            return Base64.encodeToString(URLEncoder.encode(plain, "UTF-8").getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("this never happens", ex);
        }
    }

    private static String base64UrlDecode(String encoded) {
        try {
            return URLDecoder.decode(new String(Base64.decode(encoded), "UTF-8"), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("this never happens", ex);
        }
    }

    private JsonToken(String signable, byte[] signature, String credentials, String principal) {
        this.signable = signable;
        this.signature = signature;
        this.credentials = credentials;
        this.principal = principal;
    }

    public void verify(Key secret) throws AuthenticationException, InvalidKeyException {
        if (!Arrays.equals(signature, buildSignature(secret, signable))) {
            throw new AuthenticationException("JWT not verified");
        }
    }

    public static byte[] buildSignature(Key secret, String message) throws InvalidKeyException {
        Mac sha256_HMAC;
        try {
            sha256_HMAC = Mac.getInstance("HmacSHA256");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("this never happens", ex);
        }

        sha256_HMAC.init(secret);
        return sha256_HMAC.doFinal(message.getBytes());
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
