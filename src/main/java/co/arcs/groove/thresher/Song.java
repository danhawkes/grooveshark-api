package co.arcs.groove.thresher;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;

public class Song {

	public static class UserData {

		/**
		 * Time the song was added to the current user's collection, or null.
		 */
		@Nullable
		public final String timeAdded;
		
		/**
		 * Time the song was added to the current user's favorites, or null.
		 */
		@Nullable
		public final String timeFavorited;
		
		public final boolean favorited;
		public final boolean collected;

		public UserData(String timeAdded, String timeFavorited) {
			this.timeFavorited = timeFavorited;
			this.timeAdded = (timeAdded != null) ? timeAdded : timeFavorited;
			this.favorited = timeFavorited != null;
			this.collected = timeAdded != null;
		}
	}

	public final int id;
	public final String name;
	public final int track;
	public final int duration;
	public final String coverArtFilename;
	public final boolean lowBitrateAvailable;
	public final int popularity;
	public final int year;
	public final int albumId;
	public final String albumName;
	public final int artistId;
	public final String artistName;

	@Nullable
	public final UserData userData;

	Song(JsonNode node) {
		id = node.get("SongID").asInt();
		name = node.has("Name") ? node.get("Name").asText() : node.get("SongName").asText();
		track = node.get("TrackNum").asInt();
		duration = node.get("EstimateDuration").asInt();
		coverArtFilename = node.get("CoverArtFilename").asText();
		lowBitrateAvailable = node.get("IsLowBitrateAvailable").asBoolean();
		popularity = node.get("Popularity").asInt();
		year = node.has("Year") ? node.get("Year").asInt() : 0;
		albumId = node.get("AlbumID").asInt();
		albumName = node.get("AlbumName").asText();
		artistId = node.get("ArtistID").asInt();
		artistName = node.get("ArtistName").asText();

		String timeFavorited = node.has("TSFavorited") ? node.get("TSFavorited").asText() : null;
		String timeAdded = node.has("TSAdded") ? node.get("TSAdded").asText() : timeFavorited;
		userData = (timeAdded != null || timeFavorited != null) ? new UserData(timeAdded,
				timeFavorited) : null;
	}

	Song(int id, int artistId, int albumId) {
		this.id = id;
		name = null;
		track = 0;
		duration = 0;
		coverArtFilename = null;
		lowBitrateAvailable = false;
		popularity = 0;
		year = 0;
		this.albumId = albumId;
		albumName = null;
		this.artistId = artistId;
		artistName = null;
		userData = null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Song other = (Song) obj;
		if (id != other.id)
			return false;
		return true;
	}
}
