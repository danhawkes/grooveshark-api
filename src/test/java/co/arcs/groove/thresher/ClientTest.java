package co.arcs.groove.thresher;

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

import co.arcs.groove.thresher.Client;
import co.arcs.groove.thresher.GroovesharkException;
import co.arcs.groove.thresher.Song;
import co.arcs.groove.thresher.GroovesharkException.InvalidCredentialsException;

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

	@Test(expected = InvalidCredentialsException.class)
	public void getUserWithBadCredentials() throws IOException, GroovesharkException {
		client.login(TestData.BAD_USER_NAME, TestData.BAD_USER_PASSWORD);
	}

	@Test
	public void getStreamUrl() throws IOException, GroovesharkException {
		URL url = client.getStreamUrl(TestData.SONG_1);
		assertNotNull(url);
	}

	@Test
	public void retrieveStream() throws IOException, GroovesharkException {
		URL url = client.getStreamUrl(TestData.SONG_1);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		InputStream is = new BufferedInputStream(connection.getInputStream());
		ByteStreams.toByteArray(is);
	}

	@Test
	public void searchSongs() throws IOException, GroovesharkException {
		List<Song> songs = client.searchSongs("indigo jam unit 5am");
		assertTrue(songs.size() > 0);
	}

	@Test
	public void getPopularSongs() throws IOException, GroovesharkException {
		List<Song> songs = client.searchPopularSongs();
		assertTrue(songs.size() > 0);
	}
}
