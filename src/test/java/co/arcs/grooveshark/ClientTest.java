package co.arcs.grooveshark;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.ByteStreams;

public class ClientTest extends GroovesharkApiTest {

	static Client client;

	@BeforeClass
	public static void beforeClass() {
		client = new Client();
		client.setDebugLoggingEnabled(true);
	}

	@Test
	public void getUser() throws IOException, GroovesharkException {
		client.login(TestData.USER_NAME, TestData.USER_PASSWORD);
	}

	@Test
	public void getStreamUrl() throws IOException, GroovesharkException {
		URL url = client.getStream(TestData.SONG_1);
		assertNotNull(url);
	}

	@Test
	public void retrieveStream() throws IOException, GroovesharkException {
		URL url = client.getStream(TestData.SONG_1);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		InputStream is = new BufferedInputStream(connection.getInputStream());
		ByteStreams.toByteArray(is);
	}

	@Test
	public void searchSongs() throws IOException, GroovesharkException {
		List<Song> songs = client.searchSongs("clair de lune");
		assertTrue(songs.size() > 0);
	}

	@Test
	public void getPopularSongs() throws IOException, GroovesharkException {
		List<Song> songs = client.searchPopularSongs();
		assertTrue(songs.size() > 0);
	}
}
