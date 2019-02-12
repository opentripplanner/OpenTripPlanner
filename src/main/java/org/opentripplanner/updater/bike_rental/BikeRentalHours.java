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

    // The start and end times of the period when people may use the bike share system always have a time zone attached
    // in GBFS data, via the required GBFS file system_information.json. However, OTP does not yet read that file
    // from the GBFS source data. Therefore we make the usual OTP assumption that all data is from a single time zone.

    LocalTime startTime;
    LocalTime endTime; // FIXME: the spec requires supporting times > 24 hours, up to 48 hours.

    /**
     * Load data about when a bike rental system can be used from GBFS format.
     */
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
     * BikeRentalHours instance. TODO handle time zones (when OTP as a whole handles time zones properly)
     *
     * @param localDateTime the local date and time at which the person wants to rent a vehicle.
     *
     * @param isSystemMember whether the person renting the bike is a member of the service (which may
     *                       allow then to use the system over different extended hours than non-members).
     */
    public boolean matches (LocalDateTime localDateTime, boolean isSystemMember) {
        DayOfWeek dayOfWeek = localDateTime.getDayOfWeek();
        LocalTime localTime = localDateTime.toLocalTime();
        BikeShareUserType userType = isSystemMember ? BikeShareUserType.MEMBER : BikeShareUserType.NONMEMBER;
        return userTypes.contains(userType) && days.contains(dayOfWeek)
                && localTime.isAfter(startTime) && localTime.isBefore(endTime);
    }

}
