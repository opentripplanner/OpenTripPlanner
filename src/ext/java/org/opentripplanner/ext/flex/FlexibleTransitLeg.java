package org.opentripplanner.ext.flex;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.core.TraverseMode;

/**
 * One leg of a trip -- that is, a temporally continuous piece of the journey that takes place on a
 * particular vehicle, which is running on flexible trip, i.e. not using fixed schedule and
 * stops.
 */
public class FlexibleTransitLeg implements Leg {

    private final Trip trip;

    private final Calendar startTime;

    private final Calendar endTime;

    private final Double distanceMeters;

    private final ServiceDate serviceDate;

    private final Place from;

    private final Place to;

    private final LineString legGeometry;

    private final Set<TransitAlert> transitAlerts = new HashSet<>();

    private final PickDrop boardRule;

    private final PickDrop alightRule;

    private final BookingInfo dropOffBookingInfo;

    private final BookingInfo pickupBookingInfo;

    private final Integer boardStopPosInPattern;

    private final Integer alightStopPosInPattern;

    private final int generalizedCost;

    public FlexibleTransitLeg(
            FlexTripEdge flexTripEdge,
            Calendar startTime,
            Calendar endTime,
            int generalizedCost
    ) {
        this.trip = flexTripEdge.getTrip();

        this.startTime = startTime;
        this.endTime = endTime;

        this.from = Place.forFlexStop(flexTripEdge.s1, flexTripEdge.getFromVertex());
        this.to = Place.forFlexStop(flexTripEdge.s2, flexTripEdge.getToVertex());

        this.distanceMeters = flexTripEdge.getDistanceMeters();

        this.serviceDate = flexTripEdge.flexTemplate.serviceDate;

        this.legGeometry = flexTripEdge.getGeometry();

        this.boardStopPosInPattern = flexTripEdge.flexTemplate.fromStopIndex;
        this.alightStopPosInPattern = flexTripEdge.flexTemplate.toStopIndex;

        FlexTrip flexTrip = flexTripEdge.getFlexTrip();
        this.dropOffBookingInfo = flexTrip.getDropOffBookingInfo(boardStopPosInPattern);
        this.pickupBookingInfo = flexTrip.getPickupBookingInfo(alightStopPosInPattern);

        this.boardRule = flexTrip.getBoardRule(boardStopPosInPattern);
        this.alightRule = flexTrip.getAlightRule(alightStopPosInPattern);

        this.generalizedCost = generalizedCost;
    }


    @Override
    public boolean isTransitLeg() {
        return true;
    }

    public void addAlert(TransitAlert alert) {
        transitAlerts.add(alert);
    }

    @Override
    public Agency getAgency() {
        return getRoute().getAgency();
    }

    @Override
    public Operator getOperator() {
        return trip.getOperator();
    }

    @Override
    public Route getRoute() {
        return trip.getRoute();
    }

    @Override
    public Trip getTrip() {
        return trip;
    }

    @Override
    public TraverseMode getMode() {
        return TraverseMode.fromTransitMode(trip.getRoute().getMode());
    }

    @Override
    public Calendar getStartTime() {
        return startTime;
    }

    @Override
    public Calendar getEndTime() {
        return endTime;
    }

    @Override
    public boolean isFlexibleTrip() {
        return true;
    }

    @Override
    public Double getDistanceMeters() {
        return distanceMeters;
    }

    @Override
    public Integer getRouteType() {
        return trip.getRoute().getGtfsType();
    }

    @Override
    public String getHeadsign() {
        return trip.getTripHeadsign();
    }

    @Override
    public ServiceDate getServiceDate() {
        return serviceDate;
    }

    @Override
    public Place getFrom() {
        return from;
    }

    @Override
    public Place getTo() {
        return to;
    }

    @Override
    public List<StopArrival> getIntermediateStops() {
        return List.of();
    }

    @Override
    public LineString getLegGeometry() {
        return legGeometry;
    }

    @Override
    public Set<TransitAlert> getTransitAlerts() {
        return transitAlerts;
    }

    @Override
    public PickDrop getBoardRule() {
        return boardRule;
    }

    @Override
    public PickDrop getAlightRule() {
        return alightRule;
    }

    @Override
    public BookingInfo getDropOffBookingInfo() {
        return dropOffBookingInfo;
    }

    @Override
    public BookingInfo getPickupBookingInfo() {
        return pickupBookingInfo;
    }

    @Override
    public Integer getBoardStopPosInPattern() {
        return boardStopPosInPattern;
    }

    @Override
    public Integer getAlightStopPosInPattern() {
        return alightStopPosInPattern;
    }

    @Override
    public int getGeneralizedCost() {
        return generalizedCost;
    }

    /**
     * Should be used for debug logging only
     */
    @Override
    public String toString() {
        return ToStringBuilder.of(FlexibleTransitLeg.class)
                .addObj("from", from)
                .addObj("to", to)
                .addTimeCal("startTime", startTime)
                .addTimeCal("endTime", endTime)
                .addNum("distance", distanceMeters, "m")
                .addNum("cost", generalizedCost)
                .addEntityId("agencyId", getAgency())
                .addEntityId("routeId", getRoute())
                .addEntityId("tripId", trip)
                .addObj("serviceDate", serviceDate)
                .addObj("legGeometry", legGeometry)
                .addCol("transitAlerts", transitAlerts)
                .addNum("boardingStopIndex", boardStopPosInPattern)
                .addNum("alightStopIndex", alightStopPosInPattern)
                .addEnum("boardRule", boardRule)
                .addEnum("alightRule", alightRule)
                .addObj("pickupBookingInfo", pickupBookingInfo)
                .addObj("dropOffBookingInfo", dropOffBookingInfo)
                .toString();
    }
}