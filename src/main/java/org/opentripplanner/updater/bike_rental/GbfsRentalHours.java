package org.opentripplanner.updater.bike_rental;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.EnumSet;
import java.util.Locale;

/**
 * Model object used for deserializing and checking GBFS hours of operation as defined in
 * https://github.com/NABSA/gbfs/blob/master/gbfs.md#system_hoursjson
 *
 * Created by abyrd on 2018-07-17
 */
public class GbfsRentalHours {

    private static final Logger LOG = LoggerFactory.getLogger(GbfsRentalHours.class);

    private static final DateTimeFormatter shortDayFormatter = new DateTimeFormatterBuilder().parseCaseInsensitive().
            appendPattern("EEE").toFormatter(Locale.ENGLISH);

    EnumSet<GBFSUserType> userTypes = EnumSet.noneOf(GBFSUserType.class);
    EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
    LocalTime startTime;
    LocalTime endTime; // Note: we do not yet support times > 24 hours.

    public static GbfsRentalHours fromJsonNode (JsonNode jsonNode) {
        GbfsRentalHours rentalHours = new GbfsRentalHours();
        for (JsonNode userTypeNode : jsonNode.path("user_types")) {
            rentalHours.userTypes.add(GBFSUserType.valueOf(userTypeNode.asText().toUpperCase()));
        }
        for (JsonNode dayNode : jsonNode.path("days")) {
            TemporalAccessor temporalAccessor = shortDayFormatter.parse(dayNode.asText());
            rentalHours.days.add(DayOfWeek.from(temporalAccessor));
        }
        rentalHours.startTime = LocalTime.parse(jsonNode.path("start_time").asText(), DateTimeFormatter.ISO_LOCAL_TIME);
        rentalHours.endTime = LocalTime.parse(jsonNode.path("end_time").asText(), DateTimeFormatter.ISO_LOCAL_TIME);
        return rentalHours;
    }

    /**
     * Check whether the given local DateTime falls within the time and days specified by this
     * GbfsRentalHours instance.
     */
    public boolean matches (LocalDateTime localDateTime, boolean isSystemMember) {
        DayOfWeek dayOfWeek = localDateTime.getDayOfWeek();
        LocalTime localTime = localDateTime.toLocalTime();
        GBFSUserType userType = isSystemMember ? GBFSUserType.MEMBER : GBFSUserType.NONMEMBER;
        return userTypes.contains(userType) && days.contains(dayOfWeek)
                && localTime.isAfter(startTime) && localTime.isBefore(endTime);
    }

}
