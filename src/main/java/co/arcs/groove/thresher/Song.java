package co.arcs.groove.thresher;

import com.fasterxml.jackson.databind.JsonNode;

public class Song {

	public final long id;
	public final String name;
	public final int track;
	public final int duration;
	public final String coverArtFilename;
	public final int flags;
	public final boolean lowBitrateAvailable;
	public final boolean verified;
	public final long popularity;
	public final String timeAdded;
	public final String timeFavorited;
	public final int year;
	public final long albumId;
	public final String albumName;
	public final long artistId;
	public final String artistName;

	Song(JsonNode node) {
		id = node.get("SongID").asLong();
		name = node.has("Name") ? node.get("Name").asText() : node.get("SongName").asText();
		track = node.get("TrackNum").asInt();
		duration = node.get("EstimateDuration").asInt();
		coverArtFilename = node.get("CoverArtFilename").asText();
		flags = node.get("Flags").asInt();
		lowBitrateAvailable = node.get("IsLowBitrateAvailable").asBoolean();
		verified = node.get("IsVerified").asBoolean();
		popularity = node.get("Popularity").asLong();
		timeAdded = node.has("TSAdded") ? node.get("TSAdded").asText() : null;
		timeFavorited = node.has("TSFavorited") ? node.get("TSFavorited").asText() : null;
		year = node.has("Year") ? node.get("Year").asInt() : 0;
		albumId = node.get("AlbumID").asLong();
		albumName = node.get("AlbumName").asText();
		artistId = node.get("ArtistID").asLong();
		artistName = node.get("ArtistName").asText();
	}

	Song(long id, long artistId, long albumId) {
		this.id = id;
		name = null;
		track = 0;
		duration = 0;
		coverArtFilename = null;
		flags = 0;
		lowBitrateAvailable = false;
		verified = false;
		popularity = 0;
		timeAdded = null;
		timeFavorited = null;
		year = 0;
		this.albumId = albumId;
		albumName = null;
		this.artistId = artistId;
		artistName = null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Song other = (Song) obj;
		if (id != other.id) {
			return false;
		}
		return true;
	}
}
