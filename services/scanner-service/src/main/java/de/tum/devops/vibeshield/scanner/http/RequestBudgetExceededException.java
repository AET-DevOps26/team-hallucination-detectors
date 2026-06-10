package de.tum.devops.vibeshield.scanner.http;

/** Raised when a scan tries to send more requests than its configured budget allows. */
public class RequestBudgetExceededException extends RuntimeException {

    public RequestBudgetExceededException(int budget) {
        super("Scan exceeded its request budget of " + budget + " requests.");
    }
}
