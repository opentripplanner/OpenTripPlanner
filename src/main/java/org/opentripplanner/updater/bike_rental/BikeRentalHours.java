package org.opentripplanner.updater.bike_rental;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
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
public class BikeRentalHours {

    private static final Logger LOG = LoggerFactory.getLogger(BikeRentalHours.class);

    private static final DateTimeFormatter shortDayFormatter = new DateTimeFormatterBuilder().parseCaseInsensitive().
            appendPattern("EEE").toFormatter(Locale.ENGLISH);

    EnumSet<BikeShareUserType> userTypes = EnumSet.noneOf(BikeShareUserType.class);
    EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);

    // These times actually do necessarily have a timezone attached via the required GBFS file system_information.json
    // but OTP does not yet read that file. So we'll have to make the usual one-time-zone assumption.
    LocalTime startTime;
    LocalTime endTime; // FIXME: the spec requires supporting times > 24 hours, up to 48 hours.

    public static BikeRentalHours fromJsonNode (JsonNode jsonNode) {
        BikeRentalHours rentalHours = new BikeRentalHours();
        for (JsonNode userTypeNode : jsonNode.path("user_types")) {
            rentalHours.userTypes.add(BikeShareUserType.valueOf(userTypeNode.asText().toUpperCase()));
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
     * BikeRentalHours instance.
     * TODO handle time zones (when OTP as a whole handles time zones properly)
     */
    public boolean matches (LocalDateTime localDateTime, boolean isSystemMember) {
        DayOfWeek dayOfWeek = localDateTime.getDayOfWeek();
        LocalTime localTime = localDateTime.toLocalTime();
        BikeShareUserType userType = isSystemMember ? BikeShareUserType.MEMBER : BikeShareUserType.NONMEMBER;
        return userTypes.contains(userType) && days.contains(dayOfWeek)
                && localTime.isAfter(startTime) && localTime.isBefore(endTime);
    }

}
