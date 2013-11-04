package co.arcs.grooveshark;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Base class for exceptions from Grooveshark.
 * <p>
 * This class and its subclasses represent a sub-set of the exceptions returned
 * from the internal API. Some internal exceptions are handled silently (eg.
 * invalid tokens are automatically renewed), while others are mapped to the
 * exceptions listed here.
 * </p>
 */
@SuppressWarnings("serial")
public class GroovesharkException extends Exception {

	GroovesharkException() {
		super();
	}

	GroovesharkException(String message) {
		super(message);
	}

	GroovesharkException(JsonNode faultNode) {
		super(faultNode.toString());
	}

	/**
	 * The supplied login credentials were invalid.
	 */
	public static class InvalidCredentialsException extends GroovesharkException {
	}

	public static class RateLimitedException extends GroovesharkException {
	}

	/**
	 * Something has gone wrong with the API: it's either changed in a way that
	 * breaks compatibility with this library, of the Grooveshark servers are
	 * down.
	 * <p>
	 * This is generally an unrecoverable error as far as the client is
	 * concerned.
	 * </p>
	 */
	public static class ServerErrorException extends GroovesharkException {

		ServerErrorException() {
			super();
		}

		ServerErrorException(String message) {
			super(message);
		}

		ServerErrorException(JsonNode faultNode) {
			super(faultNode);
		}
	}
}
