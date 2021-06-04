package org.opentripplanner.gtfs.mapping;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.BookingRule;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.BookingMethod;
import org.opentripplanner.model.ContactInfo;
import org.opentripplanner.model.BookingTime;

import java.time.Duration;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/** Responsible for mapping GTFS BookingRule into the OTP model. */
class BookingRuleMapper {

    private final Map<AgencyAndId, BookingInfo> cachedBookingInfos = new HashMap<>();

    /** Map from GTFS to OTP model, {@code null} safe.  */
    BookingInfo map(
            BookingRule rule
    ) {
        if (rule == null) {
            return null;
        }

        return cachedBookingInfos.computeIfAbsent(rule.getId(), k -> new BookingInfo(
                contactInfo(rule),
                bookingMethods(rule),
                earliestBookingTime(rule),
                latestBookingTime(rule),
                minimumBookingNotice(rule),
                maximumBookingNotice(rule),
                message(rule),
                dropOffMessage(rule),
                pickupMessage(rule)

        ));
    }

    private ContactInfo contactInfo(BookingRule rule) {
        return new ContactInfo(null,
                rule.getPhoneNumber(),
                null,
                null,
                rule.getInfoUrl(),
                rule.getUrl(),
                null);
    }

    private EnumSet<BookingMethod> bookingMethods(BookingRule rule) {
        return null;
    }

    private BookingTime earliestBookingTime(BookingRule rule) {
        return new BookingTime(LocalTime.ofSecondOfDay(rule.getPriorNoticeStartTime()), rule.getPriorNoticeStartDay());
    }

    private BookingTime latestBookingTime(BookingRule rule) {
        return new BookingTime(LocalTime.ofSecondOfDay(rule.getPriorNoticeLastTime()), rule.getPriorNoticeLastDay());
    }

    private Duration minimumBookingNotice(BookingRule rule) {
        return Duration.ofSeconds(rule.getPriorNoticeDurationMin());
    }

    private Duration maximumBookingNotice(BookingRule rule) {
        return Duration.ofSeconds(rule.getPriorNoticeDurationMax());
    }

    private String message(BookingRule rule) {
        return rule.getMessage();
    }

    private String pickupMessage(BookingRule rule) {
        return rule.getPickupMessage();
    }

    private String dropOffMessage(BookingRule rule) {
        return rule.getDropOffMessage();
    }
}
