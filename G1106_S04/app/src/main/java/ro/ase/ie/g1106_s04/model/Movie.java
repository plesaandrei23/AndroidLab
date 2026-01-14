package ro.ase.ie.g1106_s04.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;

import java.util.Date;
import java.util.Objects;

@Entity(tableName = "MovieTable",
        primaryKeys = {"release", "movieTitle"},
        indices = {@Index("release"), @Index("movieTitle")}
)
public class Movie implements Parcelable {
    @NonNull
    @ColumnInfo(name = "movieTitle")
    private String title;

    @ColumnInfo
    private Double budget;

    @NonNull
    @ColumnInfo(name = "release")
    private Date release;

    @ColumnInfo
    private Integer duration;

    @ColumnInfo
    private GenreEnum genre;

    @Ignore
    private ParentalGuidanceEnum pGuidance;

    @ColumnInfo
    private Float rating;

    @ColumnInfo
    private Boolean watched;

    @ColumnInfo
    private String posterUrl;

    public Movie() {}

    protected Movie(Parcel in) {
        title = in.readString();
        if (in.readByte() == 0) {
            budget = null;
        } else {
            budget = in.readDouble();
        }
        if (in.readByte() == 0) {
            duration = null;
        } else {
            duration = in.readInt();
        }
        if (in.readByte() == 0) {
            rating = null;
        } else {
            rating = in.readFloat();
        }
        byte tmpWatched = in.readByte();
        watched = tmpWatched == 0 ? null : tmpWatched == 1;
        posterUrl = in.readString();

        String genreStr = in.readString();
        genre = genreStr != null ? GenreEnum.valueOf(genreStr) : null;

        String pGuidanceStr = in.readString();
        pGuidance = pGuidanceStr != null ? ParentalGuidanceEnum.valueOf(pGuidanceStr) : null;

        if(in.readByte() == 0)
            release = null;
        else
            release = new Date(in.readLong());
    }

    public static final Creator<Movie> CREATOR = new Creator<Movie>() {
        @Override
        public Movie createFromParcel(Parcel in) {
            return new Movie(in);
        }
        @Override
        public Movie[] newArray(int size) {
            return new Movie[size];
        }
    };

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(title);
        if (budget == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeDouble(budget);
        }
        if (duration == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeInt(duration);
        }
        if (rating == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeFloat(rating);
        }
        parcel.writeByte((byte) (watched == null ? 0 : watched ? 1 : 2));
        parcel.writeString(posterUrl);
        parcel.writeString(genre != null ? genre.name() : null);
        parcel.writeString(pGuidance != null ? pGuidance.name() : null);

        if(release == null)
            parcel.writeByte((byte)0);
        else {
            parcel.writeByte((byte)1);
            parcel.writeLong(release.getTime());
        }
    }

    @Override
    public int describeContents() { return 0; }

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Double getBudget() { return budget; }
    public void setBudget(Double budget) { this.budget = budget; }
    public Date getRelease() { return release; }
    public void setRelease(Date release) { this.release = release; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public GenreEnum getGenre() { return genre; }
    public void setGenre(GenreEnum genre) { this.genre = genre; }
    public ParentalGuidanceEnum getpGuidance() { return pGuidance; }
    public void setpGuidance(ParentalGuidanceEnum pGuidance) { this.pGuidance = pGuidance; }
    public Float getRating() { return rating; }
    public void setRating(Float rating) { this.rating = rating; }
    public Boolean getWatched() { return watched; }
    public void setWatched(Boolean watched) { this.watched = watched; }
    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Movie)) return false;
        Movie movie = (Movie) o;
        return Objects.equals(title, movie.title) && Objects.equals(release, movie.release);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, release);
    }

    @Override
    public String toString() {
        return "Movie{" + "title='" + title + '\'' + ", release=" + release + '}';
    }
}
