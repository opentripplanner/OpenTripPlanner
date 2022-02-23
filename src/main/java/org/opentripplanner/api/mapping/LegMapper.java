package org.opentripplanner.api.mapping;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import org.opentripplanner.api.model.ApiAlert;
import org.opentripplanner.api.model.ApiLeg;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.util.PolylineEncoder;

public class LegMapper {
    private final WalkStepMapper walkStepMapper;
    private final StreetNoteMaperMapper streetNoteMaperMapper;
    private final AlertMapper alertMapper;
    private final PlaceMapper placeMapper;
    private final boolean addIntermediateStops;

    public LegMapper(Locale locale, boolean addIntermediateStops) {
        this.walkStepMapper = new WalkStepMapper(locale);
        this.streetNoteMaperMapper = new StreetNoteMaperMapper(locale);
        this.alertMapper = new AlertMapper(locale);
        this.placeMapper = new PlaceMapper(locale);
        this.addIntermediateStops = addIntermediateStops;
    }

    public List<ApiLeg> mapLegs(List<Leg> domain) {
        if(domain == null) { return null; }

        List<ApiLeg> apiLegs = new ArrayList<>();

        final int size = domain.size();
        final int lastIdx = size-1;

        for (int i=0; i < size; ++i) {
            Calendar arrivalTimeFromPlace = (i == 0) ? null : domain.get(i - 1).getEndTime();
            Calendar departureTimeToPlace = (i == lastIdx) ? null : domain.get(i + 1)
                    .getStartTime();

            apiLegs.add(mapLeg(domain.get(i), arrivalTimeFromPlace, departureTimeToPlace));
        }
        return apiLegs;
    }

    public ApiLeg mapLeg(Leg domain, Calendar arrivalTimeFromPlace, Calendar departureTimeToPlace) {
        if(domain == null) { return null; }
        ApiLeg api = new ApiLeg();
        api.startTime = domain.getStartTime();
        api.endTime = domain.getEndTime();

        // Set the arrival and departure times, even if this is redundant information
        api.from = placeMapper.mapPlace(
                domain.getFrom(),
                arrivalTimeFromPlace,
                api.startTime,
                domain.getBoardStopPosInPattern(),
                domain.getBoardingGtfsStopSequence()
        );
        api.to = placeMapper.mapPlace(
                domain.getTo(),
                api.endTime,
                departureTimeToPlace,
                domain.getAlightStopPosInPattern(),
                domain.getAlightGtfsStopSequence()
        );

        api.departureDelay = domain.getDepartureDelay();
        api.arrivalDelay = domain.getArrivalDelay();
        api.realTime = domain.getRealTime();
        api.isNonExactFrequency = domain.getNonExactFrequency();
        api.headway = domain.getHeadway();
        api.distance = domain.getDistanceMeters();
        api.generalizedCost = domain.getGeneralizedCost();
        api.pathway = domain.getPathwayId() != null;
        api.mode = TraverseModeMapper.mapToApi(domain.getMode());
        api.agencyTimeZoneOffset = domain.getAgencyTimeZoneOffset();
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
            api.routeType = domain.getRouteType();
            api.routeId = FeedScopedIdMapper.mapToApi(route.getId());
            api.routeShortName = route.getShortName();
            api.routeLongName = route.getLongName();
            api.routeTextColor = route.getTextColor();

            var trip = domain.getTrip();
            api.tripId = FeedScopedIdMapper.mapToApi(trip.getId());
            api.tripShortName = trip.getTripShortName();
            api.tripBlockId = trip.getBlockId();
        }
        else if (domain.getPathwayId() != null) {
            api.route = FeedScopedIdMapper.mapToApi(domain.getPathwayId());
        }
        else {
            // TODO OTP2 - This should be set to the street name according to the JavaDoc
            api.route = "";
        }

        api.interlineWithPreviousLeg = domain.isInterlinedWithPreviousLeg();
        api.headsign = domain.getHeadsign();
        api.serviceDate = ServiceDateMapper.mapToApi(domain.getServiceDate());
        api.routeBrandingUrl = domain.getRouteBrandingUrl();
        if(addIntermediateStops) {
            api.intermediateStops = placeMapper.mapStopArrivals(domain.getIntermediateStops());
        }
        api.legGeometry = PolylineEncoder.createEncodings(domain.getLegGeometry());
        api.steps = walkStepMapper.mapWalkSteps(domain.getWalkSteps());
        api.alerts = concatenateAlerts(
            streetNoteMaperMapper.mapToApi(domain.getStreetNotes()),
            alertMapper.mapToApi(domain.getTransitAlerts())
        );
        api.boardRule = getBoardAlightMessage(domain.getBoardRule());
        api.alightRule = getBoardAlightMessage(domain.getAlightRule());

        api.pickupBookingInfo = BookingInfoMapper.mapBookingInfo(domain.getPickupBookingInfo(), true);
        api.dropOffBookingInfo = BookingInfoMapper.mapBookingInfo(domain.getDropOffBookingInfo(), false);

        api.rentedBike = domain.getRentedVehicle();
        api.walkingBike = domain.getWalkingBike();

        return api;
    }

    private static String getBoardAlightMessage (PickDrop boardAlightType) {
        if (boardAlightType == null) { return null; }
        switch (boardAlightType) {
            case NONE:
                return "impossible";
            case CALL_AGENCY:
                return "mustPhone";
            case COORDINATE_WITH_DRIVER:
                return "coordinateWithDriver";
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
