package co.arcs.grooveshark;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

public class UserTest extends GroovesharkApiTest {

	static Client client;
	static User user;

	@BeforeClass
	public static void beforeClass() throws IOException, GroovesharkException {
		client = new Client();
		client.setDebugLoggingEnabled(true);
		user = client.login(TestData.USER_NAME, TestData.USER_PASSWORD);
	}

	@Test
	public void getLibrary() throws IOException, GroovesharkException {
		List<Song> songs = user.library.getSongs();
		assertTrue(songs.size() != 0);
	}

	@Test
	public void addToLibraryOne() throws IOException, GroovesharkException {
		assertTrue(user.library.addSong(TestData.SONG_1));
	}

	@Test
	public void addToLibraryMany() throws IOException, GroovesharkException {
		assertTrue(user.library.addSong(TestData.SONG_1, TestData.SONG_2));
	}

	@Test
	public void removeFromLibraryOne() throws IOException, GroovesharkException {
		assertTrue(user.library.removeSong(TestData.SONG_1));
	}

	@Test
	public void removeFromLibraryMany() throws IOException, GroovesharkException {
		assertTrue(user.library.removeSong(TestData.SONG_1, TestData.SONG_2));
	}

	@Test
	public void getFavorites() throws IOException, GroovesharkException {
		List<Song> songs = user.favorites.getSongs();
		assertTrue(songs.size() != 0);
	}

	@Test
	public void addFavorite() throws IOException, GroovesharkException {
		assertTrue(user.favorites.addSong(TestData.SONG_1));
	}

	@Test
	public void removeFavorite() throws IOException, GroovesharkException {
		assertTrue(user.favorites.removeSong(TestData.SONG_1));
	}
}