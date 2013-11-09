package co.arcs.grooveshark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import co.arcs.grooveshark.GroovesharkException.InvalidCredentialsException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

public class User {

	private final Client client;
	public final Library library = new Library();
	public final Favorites favorites = new Favorites();

	public final long id;
	public final String email;
	public final String password;

	User(Client client, long id, String email, String password) {
		this.client = client;
		this.id = id;
		this.email = email;
		this.password = password;
	}

	User(Client client, String email, String password, JsonNode node)
			throws InvalidCredentialsException {
		this.client = client;
		JsonNode result = node.get("result");
		this.id = result.get("userID").asLong();
		if (id == 0) {
			throw new InvalidCredentialsException();
		}
		this.email = email;
		this.password = password;
	}

	public class Library {

		public List<Song> get() throws IOException, GroovesharkException {
			// TODO if (hasMorePages) ...
			JsonNode response = client.sendRequest(new RequestBuilder("userGetSongsInLibrary",
					false) {
				@Override
				void populateParameters(Session session, ObjectNode parameters) {
					parameters.put("userID", id);
					parameters.put("page", 0);
				}
			});
			ArrayList<Song> songs = Lists.newArrayList();
			Iterator<JsonNode> elements = response.get("result").get("Songs").elements();
			while (elements.hasNext()) {
				songs.add(new Song(elements.next()));
			}
			return songs;
		}

		public boolean add(final Song... songs) throws IOException, GroovesharkException {
			JsonNode response = client.sendRequest(new RequestBuilder("userAddSongsToLibrary",
					false) {
				@Override
				void populateParameters(Session session, ObjectNode parameters) {
					ArrayNode songsNode = parameters.putArray("songs");
					for (Song s : songs) {
						ObjectNode songNode = songsNode.objectNode();
						songNode.put("songID", s.id);
						songNode.put("artistID", s.artistId);
						songNode.put("albumID", s.albumId);
						songsNode.add(songNode);
					}
				}
			});
			return response.get("result").get("Timestamps").has("newTSModified");
		}

		public boolean remove(final Song... songs) throws IOException, GroovesharkException {
			JsonNode response = client.sendRequest(new RequestBuilder("userRemoveSongsFromLibrary",
					false) {
				@Override
				void populateParameters(Session session, ObjectNode parameters) {
					parameters.put("userID", id);
					ArrayNode albumsNode = parameters.putArray("albumIDs");
					ArrayNode artistsNode = parameters.putArray("artistIDs");
					ArrayNode songsNode = parameters.putArray("songIDs");
					for (Song s : songs) {
						songsNode.add(s.id);
						artistsNode.add(s.artistId);
						albumsNode.add(s.albumId);
					}
				}
			});
			return response.get("result").get("Timestamps").has("newTSModified");
		}
	}

	public class Favorites {

		public boolean add(final Song song) throws IOException, GroovesharkException {
			JsonNode response = client.sendRequest(new RequestBuilder("favorite", false) {

				@Override
				void populateParameters(Session session, ObjectNode parameters) {
					parameters.put("what", "Song");
					parameters.put("ID", song.id);
				}
			});
			return response.get("result").get("success").asBoolean();
		}

		public boolean remove(final Song song) throws IOException, GroovesharkException {
			JsonNode response = client.sendRequest(new RequestBuilder("unfavorite", false) {

				@Override
				void populateParameters(Session session, ObjectNode parameters) {
					parameters.put("what", "Song");
					parameters.put("ID", song.id);

				}
			});
			return response.get("result").get("success").asBoolean();
		}

		public List<Song> get() throws IOException, GroovesharkException {
			JsonNode response = client.sendRequest(new RequestBuilder("getFavorites", false) {

				@Override
				void populateParameters(Session session, ObjectNode parameters) {
					parameters.put("ofWhat", "Songs");
					parameters.put("userID", id);
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
}
