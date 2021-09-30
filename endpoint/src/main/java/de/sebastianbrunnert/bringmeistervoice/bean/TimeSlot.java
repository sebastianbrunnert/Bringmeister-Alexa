package de.sebastianbrunnert.bringmeistervoice.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

/**
 * Objekt, das die Daten der Auswahl eines Lieferzeitpunktes verwaltet.
 *
 * @author Sebastian Brunnert
 */
@NoArgsConstructor
public class TimeSlot {

    @Getter @JsonIgnore @Setter
    private String timeStart;
    @Getter @JsonIgnore @Setter
    private String timeEnd;
    @Getter @JsonIgnore @Setter
    private String day;
    @Getter @JsonIgnore @Setter
    private double price;
    @Getter @Setter
    private String id;

    /**
     * Methode, um das nächste Datum augehend von heute zu erhalten, welches ein day ist
     *
     * @param day String, Tag auf Deutsch (Montag, Dienstag ...)
     * @return Datum im Format YYYY-mm-dd
     */
    public static String getDate(String day) {
        switch (day) {
            case "Heute":
                return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            case "Montag":
                return getDate(DayOfWeek.MONDAY);
            case "Dienstag":
                return getDate(DayOfWeek.TUESDAY);
            case "Mittwoch":
                return getDate(DayOfWeek.WEDNESDAY);
            case "Donnerstag":
                return getDate(DayOfWeek.THURSDAY);
            case "Freitag":
                return getDate(DayOfWeek.FRIDAY);
            case "Samstag":
                return getDate(DayOfWeek.SATURDAY);
            default:
                return getDate(DayOfWeek.SUNDAY);
        }
    }

    /**
     * Methode, um das nächste Datum ausgehend von heute zu erhalten, welches ein day ist.
     *
     * @param day DayOfWeek
     * @return Datum in Format YYYY-mm-dd
     */
    private static String getDate(DayOfWeek day) {
        LocalDate localeData = LocalDate.now().with(TemporalAdjusters.next(day));
        return localeData.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

}
