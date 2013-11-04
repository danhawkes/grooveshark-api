package co.arcs.grooveshark;

import static com.google.common.base.Preconditions.checkNotNull;
import co.arcs.grooveshark.GroovesharkException.InvalidCredentialsException;

import com.fasterxml.jackson.databind.JsonNode;

public class User {

	public final long id;
	public final String email;

	User(long id, String email) {
		this.id = id;
		this.email = email;
	}

	User(JsonNode node) throws InvalidCredentialsException {
		JsonNode result = node.get("result");
		this.id = result.get("userID").asLong();
		if (id == 0) {
			throw new InvalidCredentialsException();
		}
		this.email = result.get("Email").asText();
		checkNotNull(email);
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", email=" + email + "]";
	}

}
