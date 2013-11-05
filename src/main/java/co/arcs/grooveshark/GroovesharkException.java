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
 * 
 * @see InvalidCredentialsException
 * @see RateLimitedException
 * @see ServerErrorException
 */
@SuppressWarnings("serial")
public class GroovesharkException extends Exception {

	GroovesharkException(String message) {
		super(message);
	}

	GroovesharkException(JsonNode faultNode) {
		super(faultNode.toString());
	}

	/**
	 * Exception generated when the supplied login credentials were invalid.
	 */
	public static class InvalidCredentialsException extends GroovesharkException {

		public InvalidCredentialsException(JsonNode faultNode) {
			super(faultNode);
		}

		public InvalidCredentialsException() {
			super("Client is not logged in!");
		}
	}

	/**
	 * Exception generated when something has gone wrong with the API: it's
	 * either changed in a way that breaks compatibility with this library, or
	 * the Grooveshark servers are down. The detail message should include
	 * additional information.
	 * <p>
	 * This is generally an unrecoverable error as far as the client is
	 * concerned.
	 * </p>
	 */
	public static class ServerErrorException extends GroovesharkException {

		ServerErrorException(String message) {
			super(message);
		}

		ServerErrorException(JsonNode faultNode) {
			super(faultNode);
		}
	}

	/**
	 * Exception generated when the client has made too many requests in a short
	 * period of time.
	 */
	public static class RateLimitedException extends GroovesharkException {

		public RateLimitedException(JsonNode faultNode) {
			super(faultNode);
		}
	}

	static class InvalidSessionException extends GroovesharkException {

		InvalidSessionException(JsonNode faultNode) {
			super(faultNode);
		}
	}

	static class InvalidCommsTokenException extends GroovesharkException {

		InvalidCommsTokenException(JsonNode faultNode) {
			super(faultNode);
		}
	}
}
