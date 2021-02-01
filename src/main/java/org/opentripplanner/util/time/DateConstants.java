package org.opentripplanner.util.time;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * String Constants - related to date
 * 
 * @author Frank Purcell
 * @version $Revision: 1.0 $
 * @since 1.0
 */
public interface DateConstants {

    // NOTE: don't change the order of these strings...the simplest should be on the
    // bottom...you risk parsing the wrong thing (and ending up with year 0012)
    List<String> DF_LIST = List.of(
        "yyyy.MM.dd.HH.mm.ss",
        "yyyy.MM.dd.HH.mm",
        "yyyy.MM.dd.HH.mm.ss.SS",
        "M.d.yy h.mm a",
        "M.d.yyyy h.mm a",
        "M.d.yyyy h.mma",
        "M.d.yyyy h.mm",
        "M.d.yyyy k.mm",
        "M.d.yyyy",
        "yyyy.M.d",
        "h.mm a"
        // NOTE: don't change the order of these strings...the simplest should be on the
        // bottom...you risk parsing the wrong thing (and ending up with year 0012)
    );

    List<String> SMALL_DF_LIST = List.of("M.d.yy", "yy.M.d", "h.mm a");

    // from apache date utils
    String ISO_DATETIME_TIME_ZONE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZ";

    // milli second times
    Long ONE_DAY_MILLI = 86400000L;
    Long ONE_MINUTE_MILLI = 60000L;
    Long THIRTY_MINUTES_MILLI = ONE_MINUTE_MILLI * 30;
    Long FORTY_5_MINUTES_MILLI = ONE_MINUTE_MILLI * 45;
    Integer ONE_DAY_SECONDS = 24 * 60 * 60;

    Date NOW = new Date();

    String SIMPLE_TIME_FORMAT = "h:mm a";
    String TIME_FORMAT = "hh:mm:ss a";
    String DATE_FORMAT = "MM-dd-yyyy";
    String DATE_TIME_FORMAT = "M.d.yy_k.m";
    String DATE_TIME_FORMAT_NICE = "MM.dd.yyyy 'at' h:mm:a z";
    String PRETTY_DATE_FORMAT = "MMMM d, yyyy";
    String PRETTY_DT_FORMAT = PRETTY_DATE_FORMAT + " 'at' h:mm a z";
    String DT_FORMAT = "M.d.yyyy h:mm a";

    SimpleDateFormat dateSDF = new SimpleDateFormat(DATE_FORMAT);
    SimpleDateFormat timeSDF = new SimpleDateFormat(TIME_FORMAT);
    SimpleDateFormat simpTimeSDF = new SimpleDateFormat(SIMPLE_TIME_FORMAT);
    SimpleDateFormat dateTimeSDF = new SimpleDateFormat(DATE_TIME_FORMAT);
    SimpleDateFormat PRETTY_DATE = new SimpleDateFormat(PRETTY_DATE_FORMAT);
    SimpleDateFormat PRETTY_DT = new SimpleDateFormat(PRETTY_DT_FORMAT);
    SimpleDateFormat YEAR = new SimpleDateFormat("yyyy");
    SimpleDateFormat dowSDF = new SimpleDateFormat("E");

    String DATE = "date";
    String EFFECTIVE_DATE = "effectiveDate";
    String TODAY = "today";
    String TIME = "time";
    String HOUR = "Hour";
    String MINUTE = "Minute";
    String AM_PM = "AmPm";
    String MONTH = "Month";
    String DAY = "Day";

    String WEEK = "week";
    String SAT = "sat";
    String SUN = "sun";
    String AM = "am";
    String PM = "pm";
}
