package co.arcs.groove.thresher;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

public class Client {

	static final String DOMAIN = "grooveshark.com";
	private static final int TIMEOUT = 10000;

	private boolean debugLogging = false;
	final HttpClient httpClient;
	final ObjectMapper jsonMapper;
	private Session session;

	public Client() {
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setDefaultMaxPerRoute(2);

		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(TIMEOUT)
				.setSocketTimeout(TIMEOUT).setConnectionRequestTimeout(TIMEOUT).build();

		List<BasicHeader> headers = Lists.newArrayList(new BasicHeader(HttpHeaders.CONNECTION,
				"close"), new BasicHeader(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate"));

		this.httpClient = HttpClients.custom().setConnectionManager(connectionManager)
				.setDefaultHeaders(headers).setDefaultRequestConfig(requestConfig).build();

		this.jsonMapper = new ObjectMapper();
	}

	/**
	 * Sends a request, and keeps the connection open for re-use.
	 * 
	 * @param requestBuilder
	 * @return JSON node containing the response body.
	 * @throws IOException
	 * @throws GroovesharkException
	 */
	JsonNode sendRequest(RequestBuilder requestBuilder) throws IOException, GroovesharkException {

		createSessionIfRequired();
		session.createCommsTokenAsRequired();

		boolean sessionAlreadyRenewed = false;
		boolean commsTokenAlreadyRenewed = false;
		while (true) {

			// Renew out of date session/token
			if (!sessionAlreadyRenewed) {
				createSessionIfRequired();
			}
			if (!commsTokenAlreadyRenewed) {
				session.createCommsTokenAsRequired();
			}

			HttpPost httpRequest = requestBuilder.build(session);
			httpRequest.setHeader(HttpHeaders.CONNECTION, "keep-alive");
			try {
				JsonNode jsonNode = executeRequest(httpRequest);

				// Handle fault codes from the API
				GroovesharkException exception = mapGroovesharkFaultCodeToException(jsonNode);
				if (exception != null) {
					if (exception instanceof GroovesharkException.InvalidSessionException) {
						// Attempt to renew session and retry
						if (sessionAlreadyRenewed) {
							throw new GroovesharkException.ServerErrorException(
									"Failed with invalid session. Renewed session still invalid.");
						} else {
							createSession();
							sessionAlreadyRenewed = true;
							continue;
						}
					} else if (exception instanceof GroovesharkException.InvalidCommsTokenException) {
						// Attempt to renew token and retry
						if (commsTokenAlreadyRenewed) {
							throw new GroovesharkException.ServerErrorException(
									"Failed with invalid comms token. Renewed token also invalid.");
						} else {
							session.createCommsToken();
							commsTokenAlreadyRenewed = true;
							continue;
						}
					} else {
						// The exception can't be handled internally
						throw exception;
					}
				}

				return jsonNode;
			} finally {
				// Finished with connection at this point, so make it reuseable
				httpRequest.reset();
			}
		}
	}

	/**
	 * Boilerplate to send the request and parse the response payload as JSON.
	 */
	private JsonNode executeRequest(HttpPost httpRequest) throws IOException, GroovesharkException {

		HttpResponse response = httpClient.execute(httpRequest);
		String payload = CharStreams.toString(new InputStreamReader(response.getEntity()
				.getContent(), Charsets.UTF_8));

		if (debugLogging) {
			ObjectWriter writer = jsonMapper.writer().withDefaultPrettyPrinter();
			JsonNode requestNode = jsonMapper.readTree(CharStreams.toString(new InputStreamReader(
					httpRequest.getEntity().getContent())));
			JsonNode responseNode = jsonMapper.readTree(new StringReader(payload));
			System.out.println(httpRequest.getURI().toString());
			System.out.println(writer.writeValueAsString(requestNode));
			System.out.println(writer.writeValueAsString(responseNode));
			System.out.println();
		}

		// Parse response JSON
		try {
			return jsonMapper.readTree(new StringReader(payload));
		} catch (JsonProcessingException e) {
			throw new GroovesharkException.ServerErrorException(
					"Failed to parse response - received data was not valid JSON: " + payload);
		}
	}

	private static GroovesharkException mapGroovesharkFaultCodeToException(JsonNode jsonNode) {
		if (jsonNode.has("fault")) {
			JsonNode faultNode = jsonNode.get("fault");
			int faultCode = faultNode.get("code").asInt();
			switch (faultCode) {
				case FaultCodes.INVALID_SESSION:
					return new GroovesharkException.InvalidSessionException(faultNode);
				case FaultCodes.INVALID_TOKEN:
					return new GroovesharkException.InvalidCommsTokenException(faultNode);
				case FaultCodes.RATE_LIMITED:
					return new GroovesharkException.RateLimitedException(faultNode);
				case FaultCodes.INVALID_CLIENT:
				case FaultCodes.HTTP_ERROR:
				case FaultCodes.HTTP_TIMEOUT:
				case FaultCodes.MAINTENANCE:
				case FaultCodes.MUST_BE_LOGGED_IN:
				case FaultCodes.PARSE_ERROR:
				default:
					// Something has gone unrecoverably wrong, so just
					// return a generic exception with debugging info.
					return new GroovesharkException.ServerErrorException(faultNode);
			}
		}
		return null;
	}

	private void createSessionIfRequired() throws IOException, GroovesharkException {
		if ((session == null) || session.isExpired()) {
			createSession();
		}
	}

	private void createSession() throws IOException, GroovesharkException {
		User userFromOldSession = (session == null) ? null : session.getUser();
		HttpGet request = new HttpGet("http://" + DOMAIN + "/preload.php?getCommunicationToken");
		HttpResponse response = httpClient.execute(request);
		Session session = new Session(this, response);
		if (userFromOldSession != null) {
			// If old session was logged in, make sure the new one is too
			login(userFromOldSession.email, userFromOldSession.password);
		} else {
			this.session = session;
		}
	}

	public InputStream getStream(final Song song) throws IOException, GroovesharkException {
		return getStream(song.id);
	}

	public InputStream getStream(final long songId) throws IOException, GroovesharkException {
		return getStreamResponse(songId).getEntity().getContent();
	}

	public HttpResponse getStreamResponse(final Song song) throws IOException, GroovesharkException {
		return getStreamResponse(song.id);
	}

	public HttpResponse getStreamResponse(final long songId) throws IOException,
			GroovesharkException {
		HttpResponse response = httpClient.execute(new HttpGet(getStreamUrl(songId).toString()));
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != 200) {
			EntityUtils.consumeQuietly(response.getEntity());
			throw new IOException("API returned " + statusCode + " status code");
		}
		return response;
	}

	public URL getStreamUrl(final Song song) throws IOException, GroovesharkException {
		return getStreamUrl(song.id);
	}

	public URL getStreamUrl(final long songId) throws IOException, GroovesharkException {
		JsonNode response = sendRequest(new RequestBuilder("getStreamKeyFromSongIDEx", false) {
			@Override
			void populateParameters(Session session, ObjectNode parameters) {
				parameters.put("type", 0);
				parameters.put("prefetch", false);
				parameters.put("songID", songId);
				parameters.put("country", session.country);
				parameters.put("mobile", false);
			}
		});
		JsonNode result = response.get("result");
		if (result.size() == 0) {
			throw new GroovesharkException.ServerErrorException("Received empty response");
		}
		String ip = result.get("ip").asText();
		String streamKey = result.get("streamKey").asText();
		return new URL("http://" + ip + "/stream.php?streamKey=" + streamKey);
	}

	public List<Song> searchSongs(final String query) throws IOException, GroovesharkException {
		JsonNode response = sendRequest(new RequestBuilder("getResultsFromSearch", false) {

			@Override
			void populateParameters(Session session, ObjectNode parameters) {
				parameters.put("type", "Songs");
				parameters.put("query", query);
			}
		});
		ArrayList<Song> songs = Lists.newArrayList();
		Iterator<JsonNode> elements = response.get("result").get("result").elements();
		while (elements.hasNext()) {
			songs.add(new Song(elements.next()));
		}
		return songs;
	}

	public List<Song> searchPopularSongs() throws IOException, GroovesharkException {
		JsonNode response = sendRequest(new RequestBuilder("popularGetSongs", false) {

			@Override
			void populateParameters(Session session, ObjectNode parameters) {
				parameters.put("type", "daily");
			}
		});
		ArrayList<Song> songs = Lists.newArrayList();
		Iterator<JsonNode> elements = response.get("result").get("Songs").elements();
		while (elements.hasNext()) {
			songs.add(new Song(elements.next()));
		}
		return songs;
	}

	/**
	 * Logs in as the given user, allowing access to various
	 * authentication-requiring methods in the {@link User} class.
	 * 
	 * @param username
	 * @param password
	 * @return The logged in user. Also available via {@link #getUser()};
	 * @throws IOException
	 * @throws GroovesharkException
	 */
	public User login(final String username, final String password) throws IOException,
			GroovesharkException {
		JsonNode node = sendRequest(new RequestBuilder("authenticateUser", true) {

			@Override
			void populateParameters(Session session, ObjectNode parameters) {
				parameters.put("username", username);
				parameters.put("password", password);
			}
		});
		// Success: the session that created this request is now authenticated,
		// so we can mark is as such.
		User user = new User(this, username, password, node);
		session.setAuthenticated(user);
		return user;
	}

	/**
	 * @return The user that logged in via this client, or null.
	 */
	@Nullable
	public User getUser() {
		return session.user;
	}

	/**
	 * Set whether debug information is written to standard output. Default
	 * value is disabled.
	 */
	public void setDebugLoggingEnabled(boolean enabled) {
		debugLogging = enabled;
	}
}