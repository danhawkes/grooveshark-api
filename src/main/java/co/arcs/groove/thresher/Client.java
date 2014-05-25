package co.arcs.groove.thresher;

import com.belladati.httpclientandroidlib.HttpHeaders;
import com.belladati.httpclientandroidlib.HttpResponse;
import com.belladati.httpclientandroidlib.client.HttpClient;
import com.belladati.httpclientandroidlib.client.config.RequestConfig;
import com.belladati.httpclientandroidlib.client.methods.HttpGet;
import com.belladati.httpclientandroidlib.client.methods.HttpPost;
import com.belladati.httpclientandroidlib.client.methods.HttpRequestBase;
import com.belladati.httpclientandroidlib.entity.BufferedHttpEntity;
import com.belladati.httpclientandroidlib.impl.client.HttpClients;
import com.belladati.httpclientandroidlib.impl.conn.PoolingHttpClientConnectionManager;
import com.belladati.httpclientandroidlib.message.BasicHeader;
import com.belladati.httpclientandroidlib.util.EntityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

public class Client {

    static final String DOMAIN = "grooveshark.com";
    private static final int TIMEOUT = 10000;

    private boolean debugLogging = false;
    private final HttpClient httpClient;
    private final ObjectMapper jsonMapper;
    private Session session;

    public Client() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(2);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(TIMEOUT)
                .setSocketTimeout(TIMEOUT)
                .build();

        List<BasicHeader> headers = Lists.newArrayList(new BasicHeader(HttpHeaders.ACCEPT_ENCODING,
                "gzip,deflate"));

        this.httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultHeaders(headers)
                .setDefaultRequestConfig(requestConfig)
                .setUserAgent("")
                .build();

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
    private JsonNode executeRequest(HttpPost request) throws IOException, GroovesharkException {

        HttpResponse response = httpClient.execute(request);

        if (debugLogging) {
            logRequest(request, response);
        }

        String responsePayload = CharStreams.toString(new InputStreamReader(response.getEntity()
                .getContent(), Charsets.UTF_8));

        // Parse response JSON
        try {
            return jsonMapper.readTree(new StringReader(responsePayload));
        } catch (JsonProcessingException e) {
            throw new GroovesharkException.ServerErrorException(
                    "Failed to parse response - received data was not valid JSON: " + responsePayload);
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

        if (debugLogging) {
            logRequest(request, response);
        }

        Session session = new Session(this, response);
        if (userFromOldSession != null) {
            // If old session was logged in, make sure the new one is too
            login(userFromOldSession.getEmail(), userFromOldSession.getPassword());
        } else {
            this.session = session;
        }
    }

    private void logRequest(HttpRequestBase request, HttpResponse response) throws IOException {
        try {
            ObjectWriter writer = jsonMapper.writer().withDefaultPrettyPrinter();

            System.out.println("=== REQUEST ===");
            System.out.println(request.getURI().toString());

            if (request instanceof HttpPost) {
                String requestPayload = CharStreams.toString(new InputStreamReader(((HttpPost) request)
                        .getEntity()
                        .getContent()));
                JsonNode requestNode = jsonMapper.readTree(requestPayload);
                System.out.println(writer.writeValueAsString(requestNode));
            } else {
                System.out.println("(No body)");
            }

            System.out.println("=== RESPONSE ===");
            if (response.getEntity() != null) {
                BufferedHttpEntity entity = new BufferedHttpEntity(response.getEntity());
                response.setEntity(entity);
                String responsePayload = CharStreams.toString(new InputStreamReader(entity.getContent()));

                try {
                    JsonNode responseNode = jsonMapper.readTree(responsePayload);
                    System.out.println(writer.writeValueAsString(responseNode));
                } catch (JsonProcessingException e) {
                    System.out.println(responsePayload);
                }
            } else {
                System.out.println("(No body)");
            }

            System.out.println();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public InputStream getStream(final Song song) throws IOException, GroovesharkException {
        return getStream(song.getId());
    }

    public InputStream getStream(final long songId) throws IOException, GroovesharkException {
        return getStreamResponse(songId).getEntity().getContent();
    }

    public HttpResponse getStreamResponse(final Song song) throws IOException, GroovesharkException {
        return getStreamResponse(song.getId());
    }

    public HttpResponse getStreamResponse(final long songId) throws IOException, GroovesharkException {
        HttpResponse response = httpClient.execute(new HttpGet(getStreamUrl(songId).toString()));
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            EntityUtils.consumeQuietly(response.getEntity());
            throw new IOException("API returned " + statusCode + " status code");
        }
        return response;
    }

    public URL getStreamUrl(final Song song) throws IOException, GroovesharkException {
        return getStreamUrl(song.getId());
    }

    public URL getStreamUrl(final long songId) throws IOException, GroovesharkException {
        JsonNode response = sendRequest(new RequestBuilder("getStreamKeyFromSongIDEx", false) {

            @Override
            void populateParameters(Session session, ObjectNode parameters) {
                parameters.put("type", 0);
                parameters.put("prefetch", false);
                parameters.put("songID", songId);
                parameters.put("country", session.getCountry());
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
    public User login(final String username,
            final String password) throws IOException, GroovesharkException {
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
        return session.getUser();
    }

    /**
     * Set whether debug information is written to standard output. Default
     * value is disabled.
     */
    public void setDebugLoggingEnabled(boolean enabled) {
        debugLogging = enabled;
    }

    HttpClient getHttpClient() {
        return httpClient;
    }

    ObjectMapper getJsonMapper() {
        return jsonMapper;
    }
}
