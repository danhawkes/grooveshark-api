package co.arcs.grooveshark;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import co.arcs.grooveshark.GroovesharkClient;
import co.arcs.grooveshark.GroovesharkException;
import co.arcs.grooveshark.Song;
import co.arcs.grooveshark.User;

public class UserTest extends GroovesharkApiTest {

	static GroovesharkClient client;
	static User user;

	@BeforeClass
	public static void beforeClass() throws IOException, GroovesharkException {
		client = new GroovesharkClient();
		user = client.getUser(TestData.USER_NAME, TestData.USER_PASSWORD);
	}

	@Test
	public void getLibrary() throws IOException, GroovesharkException {
		List<Song> songs = client.getLibrary(user);
		assertTrue(songs.size() != 0);
	}

	@Test
	public void addToLibraryOne() throws IOException, GroovesharkException {
		assertTrue(client.addToLibrary(TestData.SONG_1));
	}

	@Test
	public void addToLibraryMany() throws IOException, GroovesharkException {
		assertTrue(client.addToLibrary(TestData.SONG_1, TestData.SONG_2));
	}

	@Test
	public void removeFromLibraryOne() throws IOException, GroovesharkException {
		assertTrue(client.removeFromLibrary(user, TestData.SONG_1));
	}

	@Test
	public void removeFromLibraryMany() throws IOException, GroovesharkException {
		assertTrue(client.removeFromLibrary(user, TestData.SONG_1, TestData.SONG_2));
	}

	@Test
	public void getFavorites() throws IOException, GroovesharkException {
		List<Song> songs = client.getFavorites(user);
		assertTrue(songs.size() != 0);
	}

	@Test
	public void addFavorite() throws IOException, GroovesharkException {
		assertTrue(client.addFavorite(TestData.SONG_1));
	}

	@Test
	public void removeFavorite() throws IOException, GroovesharkException {
		assertTrue(client.removeFavorite(TestData.SONG_1));
	}
}
