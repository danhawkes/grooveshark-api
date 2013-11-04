package co.arcs.grooveshark;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import co.arcs.grooveshark.GroovesharkException.ServerErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

public class GroovesharkClient {

	protected static final String DOMAIN = "grooveshark.com";

	private static final boolean DEBUG = true;
	final HttpClient httpClient;
	final ObjectMapper jsonMapper;
	private Session session;

	public GroovesharkClient() {
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(5000)
				.setSocketTimeout(5000).setConnectionRequestTimeout(5000).build();
		httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig)
				.setMaxConnPerRoute(1000).setMaxConnTotal(1000).build();
		jsonMapper = new ObjectMapper();
	}

	/**
	 * Immutable holder for information about an API request. Instances of this
	 * class will be created by an {@link ApiRequest.Builder} instance.
	 */
	private static class ApiRequest {

		public ApiRequest(String method, JsonNode parameters, boolean secure) {
			this.method = method;
			this.parameters = parameters;
			this.secure = secure;
		}

		final String method;
		final JsonNode parameters;
		final boolean secure;

		/**
		 * Builder for {@link ApiRequest} instances. This class is used to allow
		 * the {@link GroovesharkClient#sendRequest(Builder)} method to rebuild
		 * requests with a new or modified {@link Session} object.
		 */
		private static abstract class Builder {
			abstract ApiRequest buildRequest(Session session);
		}
	}

	/**
	 * Sends a request.
	 * <p>
	 * This method checks the validity of the underlying {@link Session} before
	 * constructing the request payload, and constructs a new one if necessary.
	 * If the request subsequently fails because the session was invalid (i.e.
	 * it has expired unexpectedly), the method will attempt to resend the
	 * request with a new session. If this fails, the attempt is aborted and an
	 * {@link ServerErrorException} returned.
	 * </p>
	 * 
	 * @param requestBuilder
	 * @return JSON node containing the response body.
	 * @throws IOException
	 * @throws GroovesharkException
	 */
	private JsonNode sendRequest(ApiRequest.Builder requestBuilder) throws IOException,
			GroovesharkException {

		createOrRenewSessionAsRequired();
		session.createOrRenewCommsTokenAsRequired();

		ApiRequest request = requestBuilder.buildRequest(session);

		// Build JSON payload
		JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
		ObjectNode rootNode = nodeFactory.objectNode();
		{
			// Header
			ObjectNode header = nodeFactory.objectNode();
			header.put("client", "mobileshark");
			header.put("clientRevision", "20120830");
			header.put("country", session.country);
			header.put("privacy", 0);
			header.put("session", session.phpSession);
			if (session.commsToken != null) {
				header.put("token", signRequest(request.method, session.commsToken));
			} else {
				throw new RuntimeException();
			}
			header.put("uuid", session.uuid);
			rootNode.put("header", header);

			// Method + parameters
			rootNode.put("method", request.method);
			rootNode.put("parameters", request.parameters);
		}

		// Build request object
		String url = (request.secure ? "https" : "http") + "://" + GroovesharkClient.DOMAIN
				+ "/more.php#" + request.method;
		HttpPost httpRequest = new HttpPost(url);
		httpRequest.setEntity(new StringEntity(rootNode.toString()));

		boolean sessionAlreadyRenewed = false;
		boolean commsTokenAlreadyRenewed = false;
		while (true) {

			// Renew out of date session/token
			if (!sessionAlreadyRenewed) {
				createOrRenewSessionAsRequired();
			}
			if (!commsTokenAlreadyRenewed) {
				session.createOrRenewCommsTokenAsRequired();
			}

			try {
				// Attempt initial request
				HttpResponse response = httpClient.execute(httpRequest);
				String payload = CharStreams.toString(new InputStreamReader(response.getEntity()
						.getContent(), Charsets.UTF_8));

				if (DEBUG) {
					System.out.println("Request:  " + rootNode.toString());
					System.out.println("Response: " + payload);
				}

				// Parse response JSON
				JsonNode jsonNode;
				try {
					jsonNode = jsonMapper.readTree(new StringReader(payload));
				} catch (JsonProcessingException e) {
					throw new GroovesharkException.ServerErrorException(
							"Failed to parse response - was not valid JSON: " + payload);
				}

				// Check for API errors
				if (jsonNode.has("fault")) {
					JsonNode faultNode = jsonNode.get("fault");
					int faultCode = faultNode.get("code").asInt();
					switch (faultCode) {
						case GroovesharkInternalApiExceptionCodes.INVALID_SESSION:
							// Attempt to renew session and retry
							if (sessionAlreadyRenewed) {
								throw new GroovesharkException.ServerErrorException(
										"Failed with invalid session. Renewed session still invalid.");
							} else {
								createOrRenewSession();
								sessionAlreadyRenewed = true;
								continue;
							}
						case GroovesharkInternalApiExceptionCodes.INVALID_TOKEN:
							// Attempt to renew token and retry
							if (commsTokenAlreadyRenewed) {
								throw new GroovesharkException.ServerErrorException(
										"Failed with invalid comms token. Renewed token also invalid.");
							} else {
								session.createOrRenewCommsToken();
								commsTokenAlreadyRenewed = true;
								continue;
							}
						case GroovesharkInternalApiExceptionCodes.RATE_LIMITED:
							// Pass this error back to the caller
							throw new GroovesharkException.RateLimitedException();
						case GroovesharkInternalApiExceptionCodes.INVALID_CLIENT:
						case GroovesharkInternalApiExceptionCodes.HTTP_ERROR:
						case GroovesharkInternalApiExceptionCodes.HTTP_TIMEOUT:
						case GroovesharkInternalApiExceptionCodes.MAINTENANCE:
						case GroovesharkInternalApiExceptionCodes.MUST_BE_LOGGED_IN:
						case GroovesharkInternalApiExceptionCodes.PARSE_ERROR:
						default:
							// Something has gone unrecoverably wrong, so just
							// return a generic exception with debugging info.
							throw new GroovesharkException.ServerErrorException(faultNode);
					}
				}

				return jsonNode;
			} finally {
				// Finished with connection at this point, so make it reuseable
				httpRequest.reset();
			}
		}
	}

	private void createOrRenewSessionAsRequired() throws IOException, GroovesharkException {
		if ((session == null) || session.isExpired()) {
			createOrRenewSession();
		}
	}

	private void createOrRenewSession() throws IOException, GroovesharkException {
		HttpGet request = new HttpGet("http://" + DOMAIN + "/preload.php?getCommunicationToken");
		HttpResponse response = httpClient.execute(request);
		this.session = new Session(this, response);
	}

	public static String signRequest(String method, String commsToken) {
		String salt = "gooeyFlubber";
		String rand = Utils.randHexChars(6);
		String s = Joiner.on(':').join(method, commsToken, salt, rand);
		return rand + DigestUtils.shaHex(s);
	}

	public User getUser(final String username, final String password) throws IOException,
			GroovesharkException {
		JsonNode node = sendRequest(new ApiRequest.Builder() {

			@Override
			ApiRequest buildRequest(Session session) {
				ObjectNode parameters = JsonNodeFactory.instance.objectNode();
				parameters.put("username", username);
				parameters.put("password", password);
				return new ApiRequest("authenticateUser", parameters, true);
			}
		});
		return new User(node);
	}

	public URL getStreamUrl(final Song song) throws IOException, GroovesharkException {
		JsonNode response = sendRequest(new ApiRequest.Builder() {
			@Override
			ApiRequest buildRequest(Session session) {
				ObjectNode parameters = JsonNodeFactory.instance.objectNode();
				parameters.put("type", 0);
				parameters.put("prefetch", false);
				parameters.put("songID", song.id);
				parameters.put("country", session.country);
				parameters.put("mobile", false);
				return new ApiRequest("getStreamKeyFromSongIDEx", parameters, false);
			}
		});
		JsonNode result = response.get("result");
		String ip = result.get("ip").asText();
		String streamKey = result.get("streamKey").asText();
		return new URL("http://" + ip + "/stream.php?streamKey=" + streamKey);
	}

	public List<Song> getLibrary(final User user) throws IOException, GroovesharkException {
		// TODO if (hasMorePages) ...
		JsonNode response = sendRequest(new ApiRequest.Builder() {
			@Override
			ApiRequest buildRequest(Session session) {
				ObjectNode parameters = JsonNodeFactory.instance.objectNode();
				parameters.put("userID", user.id);
				parameters.put("page", 0);
				return new ApiRequest("userGetSongsInLibrary", parameters, false);
			}
		});
		ArrayList<Song> songs = Lists.newArrayList();
		Iterator<JsonNode> elements = response.get("result").get("Songs").elements();
		while (elements.hasNext()) {
			songs.add(new Song(elements.next()));
		}
		return songs;
	}

	// TODO: Work out how this method works without a 'user' parameter
	public boolean addToLibrary(final Song... songs) throws IOException, GroovesharkException {
		JsonNode response = sendRequest(new ApiRequest.Builder() {
			@Override
			ApiRequest buildRequest(Session session) {
				ObjectNode parameters = JsonNodeFactory.instance.objectNode();
				ArrayNode songsNode = parameters.putArray("songs");
				for (Song s : songs) {
					ObjectNode songNode = songsNode.objectNode();
					songNode.put("songID", s.id);
					songNode.put("artistID", s.artistId);
					songNode.put("albumID", s.albumId);
					songsNode.add(songNode);
				}
				return new ApiRequest("userAddSongsToLibrary", parameters, false);
			}
		});
		return response.get("result").get("Timestamps").has("newTSModified");
	}

	public boolean removeFromLibrary(final User user, final Song... songs) throws IOException,
			GroovesharkException {
		JsonNode response = sendRequest(new ApiRequest.Builder() {
			@Override
			ApiRequest buildRequest(Session session) {
				ObjectNode parameters = JsonNodeFactory.instance.objectNode();
				parameters.put("userID", user.id);
				ArrayNode albumsNode = parameters.putArray("albumIDs");
				ArrayNode artistsNode = parameters.putArray("artistIDs");
				ArrayNode songsNode = parameters.putArray("songIDs");
				for (Song s : songs) {
					songsNode.add(s.id);
					artistsNode.add(s.artistId);
					albumsNode.add(s.albumId);
				}
				return new ApiRequest("userRemoveSongsFromLibrary", parameters, false);
			}
		});
		return response.get("result").get("Timestamps").has("newTSModified");
	}

	// TODO: Work out how this method works without a 'user' parameter
	public boolean addFavorite(final Song song) throws IOException, GroovesharkException {
		JsonNode response = sendRequest(new ApiRequest.Builder() {
			@Override
			ApiRequest buildRequest(Session session) {
				ObjectNode parameters = JsonNodeFactory.instance.objectNode();
				parameters.put("what", "Song");
				parameters.put("ID", song.id);
				return new ApiRequest("favorite", parameters, false);
			}
		});
		return response.get("result").get("success").asBoolean();
	}

	// TODO: Work out how this method works without a 'user' parameter
	public boolean removeFavorite(final Song song) throws IOException, GroovesharkException {
		JsonNode response = sendRequest(new ApiRequest.Builder() {
			@Override
			ApiRequest buildRequest(Session session) {
				ObjectNode parameters = JsonNodeFactory.instance.objectNode();
				parameters.put("what", "Song");
				parameters.put("ID", song.id);
				return new ApiRequest("unfavorite", parameters, false);
			}
		});
		return response.get("result").get("success").asBoolean();
	}

	public List<Song> getFavorites(final User user) throws IOException, GroovesharkException {
		JsonNode response = sendRequest(new ApiRequest.Builder() {
			@Override
			ApiRequest buildRequest(Session session) {
				ObjectNode parameters = JsonNodeFactory.instance.objectNode();
				parameters.put("ofWhat", "Songs");
				parameters.put("userID", user.id);
				return new ApiRequest("getFavorites", parameters, false);
			}
		});
		ArrayList<Song> songs = Lists.newArrayList();
		Iterator<JsonNode> elements = response.get("result").elements();
		while (elements.hasNext()) {
			songs.add(new Song(elements.next()));
		}
		return songs;
	}
}