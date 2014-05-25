package co.arcs.groove.thresher;

/**
 * Fault codes returned by the Grooveshark API. These are not exposed in the
 * public API as some are handled internally.
 */
class FaultCodes {

    public static final int EMPTY_RESULTS = -256;
    public static final int INVALID_CLIENT = 1024;
    public static final int RATE_LIMITED = 512;
    public static final int INVALID_TOKEN = 256;
    public static final int INVALID_SESSION = 16;
    public static final int MAINTENANCE = 10;
    public static final int MUST_BE_LOGGED_IN = 8;
    public static final int HTTP_TIMEOUT = 6;
    public static final int PARSE_ERROR = 4;
    public static final int HTTP_ERROR = 2;
}
