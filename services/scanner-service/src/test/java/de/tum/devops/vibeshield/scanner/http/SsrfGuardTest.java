package de.tum.devops.vibeshield.scanner.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The SSRF guard's blocklist. Inputs are literal IPs so {@code InetAddress} resolves
 * them without touching the network — the suite stays hermetic and deterministic.
 */
class SsrfGuardTest {

    private final SsrfGuard strict = new SsrfGuard(false);

    @ParameterizedTest
    @ValueSource(strings = {
            "127.0.0.1",          // loopback
            "0.0.0.0",            // wildcard
            "169.254.169.254",    // link-local — cloud metadata endpoint
            "10.0.0.1",           // private 10/8
            "172.16.5.4",         // private 172.16/12
            "192.168.1.10",       // private 192.168/16
            "::1",                // IPv6 loopback
            "fc00::1",            // IPv6 unique-local
            "fe80::1",            // IPv6 link-local
    })
    void blocksNonPublicAddresses(String ip) throws Exception {
        assertThat(strict.blockedCategory(InetAddress.getByName(ip))).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "8.8.8.8",            // public
            "1.1.1.1",            // public
            "93.184.216.34",      // public (example.com)
            "192.0.2.1",          // public TEST-NET (RFC 5737) — unroutable but not private
    })
    void allowsPublicAddresses(String ip) throws Exception {
        assertThat(strict.blockedCategory(InetAddress.getByName(ip))).isNull();
    }

    @Test
    void allowLoopback_relaxesOnlyLoopback() throws Exception {
        SsrfGuard relaxed = new SsrfGuard(true);
        assertThat(relaxed.blockedCategory(InetAddress.getByName("127.0.0.1"))).isNull();
        // Everything else still blocked.
        assertThat(relaxed.blockedCategory(InetAddress.getByName("10.0.0.1"))).isEqualTo("private");
        assertThat(relaxed.blockedCategory(InetAddress.getByName("169.254.169.254")))
                .isEqualTo("link-local");
    }

    @Test
    void assertAllowed_throwsForPrivateTarget() {
        assertThatThrownBy(() -> strict.assertAllowed(URI.create("http://10.255.255.1/admin")))
                .isInstanceOf(BlockedAddressException.class)
                .hasMessageContaining("not allowed to reach");
    }

    @Test
    void assertAllowed_passesForPublicTarget() {
        assertThatNoException()
                .isThrownBy(() -> strict.assertAllowed(URI.create("http://192.0.2.1/")));
    }

    @Test
    void assertAllowed_rejectsHostlessTarget() {
        assertThatThrownBy(() -> strict.assertAllowed(URI.create("file:///etc/passwd")))
                .isInstanceOf(BlockedAddressException.class);
    }
}
