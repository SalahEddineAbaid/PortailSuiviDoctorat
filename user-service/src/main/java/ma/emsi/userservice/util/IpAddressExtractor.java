package ma.emsi.userservice.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Utility component for extracting client IP addresses from HTTP requests.
 * Handles X-Forwarded-For header for proxied requests and falls back to
 * RemoteAddr.
 */
@Component
public class IpAddressExtractor {

    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";

    /**
     * Extracts the client IP address from the HTTP request.
     * First checks the X-Forwarded-For header (for proxied requests),
     * then falls back to the remote address.
     *
     * @param request the HTTP servlet request
     * @return the client IP address, or null if request is null
     */
    public String extractIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        // Check X-Forwarded-For header first (for requests through proxies/load
        // balancers)
        String xForwardedFor = request.getHeader(X_FORWARDED_FOR_HEADER);
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs (client, proxy1, proxy2, ...)
            // The first IP is the original client IP
            return xForwardedFor.split(",")[0].trim();
        }

        // Fallback to RemoteAddr
        return request.getRemoteAddr();
    }
}
