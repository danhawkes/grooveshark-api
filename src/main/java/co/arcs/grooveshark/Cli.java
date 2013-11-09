package co.arcs.grooveshark;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.google.common.base.Joiner;

class Cli {

	public static void main(String[] args) throws IOException, GroovesharkException {

		if (args.length != 2) {
			printError("Too " + (args.length > 2 ? "many" : "few") + " arguments");
			return;
		}

		Client client = new Client();
		String command = args[0];
		String query = args[1];

		try {
			if (command.equals("search")) {
				print(client.searchSongs(query));
			} else if (command.equals("url")) {
				try {
					// Treat query as song ID
					long songId = Long.parseLong(query);
					print(client.getStream(songId));
				} catch (NumberFormatException e) {
					// Treat query as search string, return first match
					List<Song> songs = client.searchSongs(query);
					if (songs.size() > 0) {
						print(client.getStream(songs.get(0)));
					}
				}
			} else {
				printError("Command \"" + args[0] + "\" not recognised");
			}
		} catch (IOException e) {
			printError(e);
		} catch (GroovesharkException e) {
			printError(e);
		}
	}

	private static void print(List<Song> songs) {
		for (Song s : songs) {
			System.out.println(print(s));
		}
	}

	private static String print(Song song) {
		return Joiner.on(" - ").join(song.id, song.name, song.artistName);
	}

	private static void print(URL url) {
		System.out.println(url.toString());
	}

	private static void printError(String message) {
		System.out.println(message);
		System.exit(1);
	}

	private static void printError(Throwable t) {
		System.err.println(t);
		System.exit(1);
	}
}
