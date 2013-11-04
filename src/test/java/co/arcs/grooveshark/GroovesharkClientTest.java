package co.arcs.grooveshark;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.BeforeClass;
import org.junit.Test;

import co.arcs.grooveshark.GroovesharkClient;
import co.arcs.grooveshark.GroovesharkException;

import com.google.common.io.ByteStreams;

public class GroovesharkClientTest extends GroovesharkApiTest {

	static GroovesharkClient client;

	@BeforeClass
	public static void beforeClass() {
		client = new GroovesharkClient();
	}

	@Test
	public void getUser() throws IOException, GroovesharkException {
		client.getUser(TestData.USER_NAME, TestData.USER_PASSWORD);
	}

	@Test
	public void getStreamUrl() throws IOException, GroovesharkException {
		URL url = client.getStreamUrl(TestData.SONG_1);
		System.out.println(url.toString());
		assertNotNull(url);
	}

	@Test
	public void retrieveStream() throws IOException, GroovesharkException {
		URL url = client.getStreamUrl(TestData.SONG_1);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		InputStream is = new BufferedInputStream(connection.getInputStream());
		ByteStreams.toByteArray(is);
	}
}
