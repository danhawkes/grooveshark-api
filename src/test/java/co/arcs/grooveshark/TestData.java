package co.arcs.grooveshark;

import java.io.IOException;

import co.arcs.grooveshark.GroovesharkClient;
import co.arcs.grooveshark.Session;
import co.arcs.grooveshark.Song;
import co.arcs.grooveshark.User;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestData {

	public static final User USER = new User(25461291,
			"a8da7cd573d12b14a0af9b11252de9d8@mailinator.com");
	public static final String USER_NAME = "a8da7cd573d12b14a0af9b11252de9d8@mailinator.com";
	public static final String USER_PASSWORD = "a8da7cd573d12b14a0af9b11252de9d8";
	public static final Song SONG_1 = new Song(35951687, 490, 100204);
	public static final Song SONG_2 = new Song(35354896, 846, 140485);

	public static final Session SESSION_WITH_INVALID_PHP_SESSION;

	static {
		try {
			SESSION_WITH_INVALID_PHP_SESSION = new Session(
					new GroovesharkClient(),
					"eb089e7241484dd421a379ba1c6190a2",
					new ObjectMapper()
							.readTree("{\"ID\":221,\"CC1\":0,\"CC2\":0,\"CC3\":0,\"CC4\":268435456,\"DMA\":0,\"IPR\":0}"),
					"1f8ff78911df69f5cff211e7b82393ae", "8650BA4A-BCAD-41C4-A288-EFAD95139C90",
					"665e9c8f02885aa00b8e61effdcc4661d40b081ee1ba44");
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
