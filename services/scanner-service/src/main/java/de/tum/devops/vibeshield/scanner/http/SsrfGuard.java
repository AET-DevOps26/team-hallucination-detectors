package de.tum.devops.vibeshield.scanner.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * SSRF guard. The registered-URL validation only checks scheme + host, so a user
 * could otherwise point the scanner at our own infrastructure — {@code 127.0.0.1},
 * the {@code 169.254.169.254} cloud-metadata endpoint, {@code 10.x} ranges, or
 * internal cluster hostnames. This refuses any target that resolves to a non-public
 * address.
 *
 * <p>Resolution happens here, at fetch time, not at registration: a hostname cannot
 * dodge the check by resolving to a public IP when it is registered and a private one
 * when the scan actually runs (DNS rebinding).
 *
 * <p>{@code scanner.ssrf.allow-loopback} relaxes only the loopback rule, for
 * integration tests that scan an embedded server on {@code 127.0.0.1}. It stays off
 * in every deployed environment.
 */
@Component
public class SsrfGuard {

    private static final Logger log = LoggerFactory.getLogger(SsrfGuard.class);

    private final boolean allowLoopback;

    public SsrfGuard(@Value("${scanner.ssrf.allow-loopback:false}") boolean allowLoopback) {
        this.allowLoopback = allowLoopback;
    }

    /**
     * Asserts that {@code uri}'s host resolves only to addresses the scanner is
     * allowed to reach. Unresolvable hosts are left alone — the normal fetch path
     * reports those as unreachable, which is the honest outcome.
     *
     * @throws BlockedAddressException if any resolved address is off-limits
     */
    public void assertAllowed(URI uri) {
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new BlockedAddressException("Refused to scan a target without a host.");
        }
        InetAddress[] resolved;
        try {
            resolved = InetAddress.getAllByName(host);
        } catch (UnknownHostException unresolvable) {
            return;
        }
        for (InetAddress address : resolved) {
            String category = blockedCategory(address);
            if (category != null) {
                log.warn("Refusing SSRF-risk target {} -> {} ({})",
                        host, address.getHostAddress(), category);
                throw new BlockedAddressException("Refused to scan " + host
                        + ": it resolves to a " + category
                        + " address that the scanner is not allowed to reach.");
            }
        }
    }

    /** The off-limits category of an address, or {@code null} when it is public/routable. */
    String blockedCategory(InetAddress address) {
        if (address.isLoopbackAddress()) {
            return allowLoopback ? null : "loopback";
        }
        if (address.isAnyLocalAddress()) {
            return "wildcard";
        }
        if (address.isLinkLocalAddress()) {
            // Includes 169.254.169.254, the cloud-metadata endpoint.
            return "link-local";
        }
        if (address.isSiteLocalAddress()) {
            // 10/8, 172.16/12, 192.168/16.
            return "private";
        }
        if (address.isMulticastAddress()) {
            return "multicast";
        }
        if (isUniqueLocalIpv6(address)) {
            return "unique-local";
        }
        return null;
    }

    /** IPv6 unique-local (fc00::/7) — not covered by {@link InetAddress#isSiteLocalAddress()}. */
    private static boolean isUniqueLocalIpv6(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC;
    }
}
