package ro.ase.ie.g1106_s04.database;

import androidx.room.TypeConverter;
import java.util.Date;
import ro.ase.ie.g1106_s04.model.GenreEnum;

public class DateTimeConverter {
    @TypeConverter
    public Long dateToLong(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public Date longToDate(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public String fromGenre(GenreEnum genre) {
        return genre == null ? null : genre.name();
    }

    @TypeConverter
    public GenreEnum toGenre(String value) {
        return value == null ? null : GenreEnum.valueOf(value);
    }
}
