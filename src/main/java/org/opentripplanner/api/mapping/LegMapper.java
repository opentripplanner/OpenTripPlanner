package org.opentripplanner.api.mapping;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.opentripplanner.api.model.ApiAlert;
import org.opentripplanner.api.model.ApiBookingInfo;
import org.opentripplanner.api.model.ApiBookingMethod;
import org.opentripplanner.api.model.ApiBookingTime;
import org.opentripplanner.api.model.ApiContactInfo;
import org.opentripplanner.api.model.ApiLeg;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.BookingMethod;
import org.opentripplanner.model.BookingTime;
import org.opentripplanner.model.ContactInfo;
import org.opentripplanner.model.plan.Leg;

public class LegMapper {
    private final WalkStepMapper walkStepMapper;
    private final StreetNoteMaperMapper streetNoteMaperMapper;
    private final AlertMapper alertMapper;

    public LegMapper(Locale locale) {
        this.walkStepMapper = new WalkStepMapper(locale);
        this.streetNoteMaperMapper = new StreetNoteMaperMapper(locale);
        this.alertMapper = new AlertMapper(locale);
    }

    public List<ApiLeg> mapLegs(List<Leg> domain) {
        if(domain == null) { return null; }

        List<ApiLeg> apiLegs = new ArrayList<>();

        final int size = domain.size();
        final int lastIdx = size-1;

        for (int i=0; i < size; ++i) {
            Calendar arrivalTimeFromPlace = (i == 0) ? null : domain.get(i-1).endTime;
            Calendar departureTimeToPlace = (i == lastIdx) ? null : domain.get(i+1).startTime;

            apiLegs.add(mapLeg(domain.get(i), arrivalTimeFromPlace, departureTimeToPlace));
        }
        return apiLegs;
    }

    public ApiLeg mapLeg(Leg domain, Calendar arrivalTimeFromPlace, Calendar departureTimeToPlace) {
        if(domain == null) { return null; }
        ApiLeg api = new ApiLeg();
        api.startTime = domain.startTime;
        api.endTime = domain.endTime;

        // Set the arrival and departure times, even if this is redundant information
        api.from = PlaceMapper.mapPlace(domain.from, arrivalTimeFromPlace, api.startTime);
        api.to = PlaceMapper.mapPlace(domain.to, api.endTime, departureTimeToPlace);

        api.departureDelay = domain.departureDelay;
        api.arrivalDelay = domain.arrivalDelay;
        api.realTime = domain.realTime;
        api.isNonExactFrequency = domain.isNonExactFrequency;
        api.headway = domain.headway;
        api.distance = domain.distanceMeters;
        api.generalizedCost = domain.generalizedCost;
        api.pathway = domain.pathway;
        api.mode = TraverseModeMapper.mapToApi(domain.mode);
        api.agencyTimeZoneOffset = domain.agencyTimeZoneOffset;
        api.transitLeg = domain.isTransitLeg();

        if(domain.isTransitLeg()) {
            var agency = domain.getAgency();
            api.agencyId = FeedScopedIdMapper.mapToApi(agency.getId());
            api.agencyName = agency.getName();
            api.agencyUrl = agency.getUrl();
            api.agencyBrandingUrl = agency.getBrandingUrl();

            var route = domain.getRoute();
            api.route = route.getLongName();
            api.routeColor = route.getColor();
            api.routeType = domain.routeType;
            api.routeId = FeedScopedIdMapper.mapToApi(route.getId());
            api.routeShortName = route.getShortName();
            api.routeLongName = route.getLongName();
            api.routeTextColor = route.getTextColor();

            var trip = domain.getTrip();
            api.tripId = FeedScopedIdMapper.mapToApi(trip.getId());
            api.tripShortName = trip.getTripShortName();
            api.tripBlockId = trip.getBlockId();
        }
        else if (domain.pathway) {
            api.route = FeedScopedIdMapper.mapToApi(domain.pathwayId);
        }
        else {
            // TODO OTP2 - This should be set to the street name according to the JavaDoc
            api.route = "";
        }

        api.interlineWithPreviousLeg = domain.isInterlinedWithPreviousLeg();
        api.headsign = domain.headsign;
        api.serviceDate = ServiceDateMapper.mapToApi(domain.serviceDate);
        api.routeBrandingUrl = domain.routeBrandingUrl;
        api.intermediateStops = PlaceMapper.mapStopArrivals(domain.intermediateStops);
        api.legGeometry = domain.legGeometry;
        api.steps = walkStepMapper.mapWalkSteps(domain.walkSteps);
        api.alerts = concatenateAlerts(
            streetNoteMaperMapper.mapToApi(domain.streetNotes),
            alertMapper.mapToApi(domain.transitAlerts)
        );
        api.boardRule = domain.boardRule;
        api.alightRule = domain.alightRule;

        api.pickupBookingInfo = toApi(domain.pickupBookingInfo);
        api.dropOffBookingInfo = toApi(domain.dropOffBookingInfo);

        api.rentedBike = domain.rentedVehicle;
        api.walkingBike = domain.walkingBike;

        return api;
    }

    private ApiBookingInfo toApi(BookingInfo info) {
        if (info != null) {

            return new ApiBookingInfo(
                    toApi(info.getContactInfo()),
                    toApi(info.bookingMethods()),
                    toApi(info.getEarliestBookingTime()),
                    toApi(info.getLatestBookingTime()),
                    info.getMinimumBookingNotice(),
                    info.getMaximumBookingNotice(),
                    info.getMessage(),
                    info.getPickupMessage(),
                    info.getDropOffMessage()
            );
        }
        else {
            return null;
        }
    }

    private ApiContactInfo toApi(ContactInfo info) {
        if (info != null) {
            return new ApiContactInfo(
                    info.getContactPerson(),
                    info.getPhoneNumber(),
                    info.geteMail(),
                    info.getFaxNumber(),
                    info.getInfoUrl(),
                    info.getBookingUrl(),
                    info.getAdditionalDetails()
            );
        }
        else {
            return null;
        }
    }

    private EnumSet<ApiBookingMethod> toApi(EnumSet<BookingMethod> m) {
           if(m !=null) {

        return m.stream()
                .map(this::toApi)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ApiBookingMethod.class)));
        } else {
            return null;
        }
    }

    private ApiBookingTime toApi(BookingTime time) {
        if(time != null) {
            return new ApiBookingTime(time.getTime().toSecondOfDay(), time.getDaysPrior());
        } else {
            return null;
        }
    }

    private ApiBookingMethod toApi(BookingMethod m) {
        switch (m) {
            case CALL_DRIVER:
                return ApiBookingMethod.CALL_DRIVER;
            case CALL_OFFICE:
                return ApiBookingMethod.CALL_OFFICE;
            case ONLINE:
                return ApiBookingMethod.ONLINE;
            case PHONE_AT_STOP:
                return ApiBookingMethod.PHONE_AT_STOP;
            case TEXT_MESSAGE:
                return ApiBookingMethod.TEXT_MESSAGE;
            default:
                return null;
        }

    }

    private static List<ApiAlert> concatenateAlerts(List<ApiAlert> a, List<ApiAlert> b) {
        if (a == null) {
            return b;
        } else if (b == null) {
            return a;
        } else {
            List<ApiAlert> ret = new ArrayList<>(a);
            ret.addAll(b);
            return ret;
        }
    }

}
