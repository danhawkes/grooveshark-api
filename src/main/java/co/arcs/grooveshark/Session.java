package co.arcs.grooveshark;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

/**
 * Represents a grant to communicate with the grooveshark API a limited period
 * of time.
 * <p>
 * Sessions start off as 'anonymous': unassociated with a user.
 * </p>
 */
class Session {

	private final Client client;
	final String phpSession;
	final long phpSessionCreatedTime;
	final String secretKey;
	final String uuid;
	final JsonNode country;
	@Nullable
	String commsToken;
	User user;

	private static final long PHP_SESSION_LIFETIME = 60 * 60 * 24 * 7;

	Session(Client api, HttpResponse response) throws JsonProcessingException, IOException,
			GroovesharkException {
		this.client = api;
		this.phpSession = extractPhpSession(response);
		checkNotNull(phpSession);
		phpSessionCreatedTime = new Date().getTime();
		this.country = extractCountry(response);
		checkNotNull(country);
		this.secretKey = generateSecretKey(phpSession);
		this.uuid = generateUuid();

	}

	Session(Client api, String phpSession, JsonNode country, String secretKey, String uuid,
			String commsToken) {
		this.client = api;
		this.phpSession = phpSession;
		this.phpSessionCreatedTime = new Date().getTime();
		this.country = country;
		this.secretKey = secretKey;
		this.uuid = uuid;
		this.commsToken = commsToken;
	}

	@Nullable
	private JsonNode extractCountry(HttpResponse response) throws IOException {
		String responseBody = CharStreams.toString(new InputStreamReader(response.getEntity()
				.getContent(), Charsets.UTF_8));
		Matcher matcher = Pattern.compile("window.tokenData = (.*);").matcher(responseBody);
		if (matcher.find()) {
			String json = matcher.group(1);
			JsonNode rootNode = client.jsonMapper.readTree(new StringReader(json));
			JsonNode gsConfigNode = rootNode.get("getGSConfig");
			if (gsConfigNode != null) {
				return gsConfigNode.get("country");
			}
		}
		return null;
	}

	@Nullable
	private static String extractPhpSession(HttpResponse response) throws IOException {
		Header[] headers = response.getHeaders("Set-Cookie");
		for (Header header : headers) {
			HeaderElement[] elements = header.getElements();
			for (HeaderElement element : elements) {
				if ("PHPSESSID".equals(element.getName())) {
					return element.getValue();
				}
			}
		}
		return null;
	}

	private static String generateSecretKey(String phpSession) {
		return DigestUtils.md5Hex(phpSession);
	}

	private static String generateUuid() {
		return UUID.randomUUID().toString().toUpperCase(Locale.US);
	}

	void createCommsToken() throws IOException, GroovesharkException {
		String method = "getCommunicationToken";

		// Build JSON payload
		JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
		ObjectNode rootNode = nodeFactory.objectNode();
		{
			// Header
			ObjectNode header = nodeFactory.objectNode();
			header.put("client", "mobileshark");
			header.put("clientRevision", "20120830");
			header.put("country", country);
			header.put("privacy", 0);
			header.put("session", phpSession);
			header.put("uuid", uuid);
			rootNode.put("header", header);

			// Method
			rootNode.put("method", method);

			// Parameters
			ObjectNode parameters = JsonNodeFactory.instance.objectNode();
			parameters.put("secretKey", Session.this.secretKey);
			rootNode.put("parameters", parameters);
		}

		// Build and send request
		String url = "https://" + Client.DOMAIN + "/more.php#" + method;
		HttpPost httpRequest = new HttpPost(url);
		httpRequest.setEntity(new StringEntity(rootNode.toString()));
		HttpResponse httpResponse = client.httpClient.execute(httpRequest);
		String payload = CharStreams.toString(new InputStreamReader(httpResponse.getEntity()
				.getContent(), Charsets.UTF_8));

		// Parse response JSON
		JsonNode jsonNode;
		try {
			jsonNode = client.jsonMapper.readTree(new StringReader(payload));
		} catch (JsonProcessingException e) {
			throw new GroovesharkException.ServerErrorException(
					"Failed to parse response - was not valid JSON: " + payload);
		}

		// TODO parse API errors
		this.commsToken = jsonNode.get("result").asText();
	}

	void createCommsTokenAsRequired() throws IOException, GroovesharkException {
		if (commsToken == null) {
			createCommsToken();
		}
	}

	boolean isExpired() {
		return (phpSessionCreatedTime + PHP_SESSION_LIFETIME) < new Date().getTime();
	}

	/**
	 * Marks this session as being authenticated and associated with a user.
	 */
	void setAuthenticated(User user) {
		this.user = user;
	}

	public User getUser() {
		return user;
	}

	boolean hasUser() {
		return (user != null);
	}
}