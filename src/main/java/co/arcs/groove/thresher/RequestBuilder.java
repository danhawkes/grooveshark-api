package co.arcs.groove.thresher;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;

import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.entity.StringEntity;

/**
 * Builder class for API requests that depend on a valid {@link Session}.
 */
abstract class RequestBuilder {

    final String method;
    final boolean secure;

    RequestBuilder(String method, boolean secure) {
        this.method = method;
        this.secure = secure;
    }

    HttpPost build(Session session) {
        // Build JSON payload
        JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        ObjectNode rootNode = nodeFactory.objectNode();
        {
            // Header
            ObjectNode header = nodeFactory.objectNode();
            header.put("client", "mobileshark");
            header.put("clientRevision", "20120830");
            header.put("country", session.getCountry());
            header.put("privacy", 0);
            header.put("session", session.getPhpSession());
            header.put("token", signRequest(method, session.getCommsToken()));
            header.put("uuid", session.getUuid());
            rootNode.put("header", header);

            // Method
            rootNode.put("method", method);

            // Parameters
            ObjectNode parameters = nodeFactory.objectNode();
            populateParameters(session, parameters);
            if (parameters.size() > 0) {
                rootNode.put("parameters", parameters);
            }
        }

        // Build request object
        String url = (secure ? "https" : "http") + "://" + Client.DOMAIN + "/more.php#" + method;
        HttpPost httpRequest = new HttpPost(url);
        httpRequest.setEntity(new StringEntity(rootNode.toString(), Charsets.UTF_8));
        return httpRequest;
    }

    static String signRequest(String method, String commsToken) {
        String salt = "gooeyFlubber";
        String rand = Utils.randHexChars(6);
        String s = Joiner.on(':').join(method, commsToken, salt, rand);
        return rand + DigestUtils.shaHex(s);
    }

    abstract void populateParameters(Session session, ObjectNode parameters);
}
