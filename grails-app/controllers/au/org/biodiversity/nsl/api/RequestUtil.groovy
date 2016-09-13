package au.org.biodiversity.nsl.api

import javax.servlet.http.HttpServletRequest

/**
 * User: pmcneil
 * Date: 13/09/16
 *
 */
trait RequestUtil {

    private final ArrayList<String> headers = ["X-Forwarded-For","Proxy-Client-IP","WL-Proxy-Client-IP","HTTP_CLIENT_IP","HTTP_X_FORWARDED_FOR"]

    public remoteAddress(HttpServletRequest request) {
        String ip = headers.find { String header ->
            String value = request.getHeader(header)
            if (value && !"unknown".equalsIgnoreCase(value)) {
                return value
            }
            return null
        }
        if (!ip) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}