package co.arcs.groove.thresher;

import com.fasterxml.jackson.databind.JsonNode;

import javax.annotation.Nullable;

public class Song {

    public static class UserData {

        private final String timeAdded;
        private final String timeFavorited;
        private final boolean favorited;
        private final boolean collected;

        public UserData(String timeAdded, String timeFavorited) {
            this.timeFavorited = timeFavorited;
            this.timeAdded = (timeAdded != null) ? timeAdded : timeFavorited;
            this.favorited = timeFavorited != null;
            this.collected = timeAdded != null;
        }

        /**
         * Time the song was added to the current user's collection, or null.
         */
        @Nullable
        public String getTimeAdded() {
            return timeAdded;
        }

        /**
         * Time the song was added to the current user's favorites, or null.
         */
        @Nullable
        public String getTimeFavorited() {
            return timeFavorited;
        }

        public boolean isFavorited() {
            return favorited;
        }

        public boolean isCollected() {
            return collected;
        }
    }

    private final int id;
    private final String name;
    private final int track;
    private final int duration;
    private final String coverArtFilename;
    private final boolean lowBitrateAvailable;
    private final int popularity;
    private final int year;
    private final int albumId;
    private final String albumName;
    private final int artistId;
    private final String artistName;

    private final UserData userData;

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
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

    public int getId() {
        return id;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public int getTrack() {
        return track;
    }

    public int getDuration() {
        return duration;
    }

    @Nullable
    public String getCoverArtFilename() {
        return coverArtFilename;
    }

    public boolean isLowBitrateAvailable() {
        return lowBitrateAvailable;
    }

    public int getPopularity() {
        return popularity;
    }

    public int getYear() {
        return year;
    }

    public int getAlbumId() {
        return albumId;
    }

    @Nullable
    public String getAlbumName() {
        return albumName;
    }

    public int getArtistId() {
        return artistId;
    }

    @Nullable
    public String getArtistName() {
        return artistName;
    }

    /**
     * Get data about the user associated with this song instance.
     *
     * @return Data about the user, or null if no user is associated.
     */
    @Nullable
    public UserData getUserData() {
        return userData;
    }
}
