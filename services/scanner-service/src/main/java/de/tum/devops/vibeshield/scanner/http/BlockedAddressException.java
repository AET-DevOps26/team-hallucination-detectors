package de.tum.devops.vibeshield.scanner.http;

/**
 * Thrown by the {@link SsrfGuard} when a scan target resolves to an address the
 * scanner must never reach (loopback, private, link-local, etc.). Carries a
 * user-facing reason that the scan result surfaces as its error message.
 */
public class BlockedAddressException extends RuntimeException {

    public BlockedAddressException(String message) {
        super(message);
    }
}
