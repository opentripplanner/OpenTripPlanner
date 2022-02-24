package org.opentripplanner.model.plan;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.trippattern.TripTimes;

/**
 * One leg of a trip -- that is, a temporally continuous piece of the journey that takes place on a
 * particular vehicle.
 */
public class ScheduledTransitLeg implements Leg {

    protected final TripTimes tripTimes;
    protected final TripPattern tripPattern;

    private final Calendar startTime;
    private final Calendar endTime;

    private Double distanceMeters;
    private final LineString legGeometry;

    private final Set<TransitAlert> transitAlerts = new HashSet<>();

    private final ConstrainedTransfer transferFromPrevLeg;
    private final ConstrainedTransfer transferToNextLeg;

    protected final Integer boardStopPosInPattern;
    protected final Integer alightStopPosInPattern;

    private final int generalizedCost;

    protected final ServiceDate serviceDate;
    protected final ZoneId zoneId;

    public ScheduledTransitLeg(
            TripTimes tripTimes,
            TripPattern tripPattern,
            int boardStopIndexInPattern,
            int alightStopIndexInPattern,
            Calendar startTime,
            Calendar endTime,
            LocalDate serviceDate,
            ZoneId zoneId,
            ConstrainedTransfer transferFromPreviousLeg,
            ConstrainedTransfer transferToNextLeg,
            int generalizedCost
    ) {
        this.tripTimes = tripTimes;
        this.tripPattern = tripPattern;

        this.boardStopPosInPattern = boardStopIndexInPattern;
        this.alightStopPosInPattern = alightStopIndexInPattern;

        this.startTime = startTime;
        this.endTime = endTime;

        this.serviceDate = new ServiceDate(serviceDate);
        this.zoneId = zoneId;

        this.transferFromPrevLeg = transferFromPreviousLeg;
        this.transferToNextLeg = transferToNextLeg;

        this.generalizedCost = generalizedCost;

        List<Coordinate> transitLegCoordinates = extractTransitLegCoordinates(tripPattern, boardStopIndexInPattern, alightStopIndexInPattern);
        this.legGeometry = GeometryUtils.makeLineString(transitLegCoordinates);
        this.distanceMeters = getDistanceFromCoordinates(transitLegCoordinates);
    }

    public boolean isScheduledTransitLeg() {
        return true;
    }

    public ScheduledTransitLeg asScheduledTransitLeg() {
        return this;
    }

    public TripTimes getTripTimes() {
        return tripTimes;
    }

    public TripPattern getTripPattern() {
        return tripPattern;
    }

    public Instant getServiceDateMidnight() {
        return serviceDate.toZonedDateTime(zoneId, 0).toInstant();
    }

    private List<Coordinate> extractTransitLegCoordinates(TripPattern tripPattern, int boardStopIndexInPattern, int alightStopIndexInPattern) {
        List<Coordinate> transitLegCoordinates = new ArrayList<>();

        for (int i = boardStopIndexInPattern + 1; i <= alightStopIndexInPattern; i++) {
            transitLegCoordinates.addAll(Arrays.asList(tripPattern.getHopGeometry(i - 1).getCoordinates()));
        }

        return transitLegCoordinates;
    }

    private double getDistanceFromCoordinates(List<Coordinate> coordinates) {
        double distance = 0;
        for (int i = 1; i < coordinates.size(); i++) {
            distance += SphericalDistanceLibrary.distance(coordinates.get(i), coordinates.get(i - 1));
        }
        return distance;
    }

    @Override
    public boolean isTransitLeg() {
        return true;
    }

    @Override
    public Boolean isInterlinedWithPreviousLeg() {
        if (transferFromPrevLeg == null) {return false;}
        return transferFromPrevLeg.getTransferConstraint().isStaySeated();
    }

    public void addAlert(TransitAlert alert) {
        transitAlerts.add(alert);
    }

    @Override
    public Agency getAgency() {
        return getTrip().getRoute().getAgency();
    }

    @Override
    public Operator getOperator() {
        return getTrip().getOperator();
    }

    @Override
    public Route getRoute() {
        return getTrip().getRoute();
    }

    @Override
    public Trip getTrip() {
        return tripTimes.getTrip();
    }

    @Override
    public TraverseMode getMode() {
        return TraverseMode.fromTransitMode(getTrip().getMode());
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
    public int getDepartureDelay() {
        return tripTimes.getDepartureDelay(boardStopPosInPattern);
    }

    @Override
    public int getArrivalDelay() {
        return tripTimes.getArrivalDelay(alightStopPosInPattern);
    }

    @Override
    public boolean getRealTime() {
        return !tripTimes.isScheduled();
    }

    @Override
    public Double getDistanceMeters() {
        return distanceMeters;
    }

    /** Only for testing purposes */
    protected void setDistanceMeters(Double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    @Override
    public Integer getRouteType() {
        return getTrip().getRoute().getGtfsType();
    }

    @Override
    public String getHeadsign() {
        return tripTimes.getHeadsign(boardStopPosInPattern);
    }

    @Override
    public ServiceDate getServiceDate() {
        return serviceDate;
    }

    @Override
    public Place getFrom() {
        return Place.forStop(tripPattern.getStop(boardStopPosInPattern));
    }

    @Override
    public Place getTo() {
        return Place.forStop(tripPattern.getStop(alightStopPosInPattern));
    }

    @Override
    public List<StopArrival> getIntermediateStops() {
        List<StopArrival> visits = new ArrayList<>();

        for (int i = boardStopPosInPattern + 1; i < alightStopPosInPattern; i++) {
            StopLocation stop = tripPattern.getStop(i);

            StopArrival visit = new StopArrival(
                    Place.forStop(stop),
                    GregorianCalendar.from(serviceDate.toZonedDateTime(zoneId, tripTimes.getArrivalTime(i))),
                    GregorianCalendar.from(serviceDate.toZonedDateTime(zoneId, tripTimes.getDepartureTime(i))),
                    i,
                    tripTimes.getOriginalGtfsStopSequence(i)
            );
            visits.add(visit);
        }
        return visits;
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
        if (transferFromPrevLeg != null && transferFromPrevLeg.getTransferConstraint().isStaySeated()) {
            return null;
        }
        return tripPattern.getBoardType(boardStopPosInPattern);
    }

    @Override
    public PickDrop getAlightRule() {
        if (transferToNextLeg != null && transferToNextLeg.getTransferConstraint().isStaySeated()) {
            return null;
        }
        return tripPattern.getAlightType(alightStopPosInPattern);
    }

    @Override
    public BookingInfo getDropOffBookingInfo() {
        return tripTimes.getDropOffBookingInfo(alightStopPosInPattern);
    }

    @Override
    public BookingInfo getPickupBookingInfo() {
        return tripTimes.getPickupBookingInfo(boardStopPosInPattern);
    }

    @Override
    public ConstrainedTransfer getTransferFromPrevLeg() {
        return transferFromPrevLeg;
    }

    @Override
    public ConstrainedTransfer getTransferToNextLeg() {
        return transferToNextLeg;
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
    public Integer getBoardingGtfsStopSequence() {
        return tripTimes.getOriginalGtfsStopSequence(boardStopPosInPattern);
    }

    @Override
    public Integer getAlightGtfsStopSequence() {
        return tripTimes.getOriginalGtfsStopSequence(alightStopPosInPattern);
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
        return ToStringBuilder.of(ScheduledTransitLeg.class)
                .addObj("from", getFrom())
                .addObj("to", getTo())
                .addTimeCal("startTime", startTime)
                .addTimeCal("endTime", endTime)
                .addBool("realTime", getRealTime())
                .addNum("distance", distanceMeters, "m")
                .addNum("cost", generalizedCost)
                .addNum("routeType", getRouteType())
                .addEntityId("agencyId", getAgency())
                .addEntityId("routeId", getRoute())
                .addEntityId("tripId", getTrip())
                .addStr("headsign", getHeadsign())
                .addObj("serviceDate", serviceDate)
                .addObj("legGeometry", legGeometry)
                .addCol("transitAlerts", transitAlerts)
                .addEnum("boardRule", getBoardRule())
                .addEnum("alightRule", getAlightRule())
                .addObj("transferFromPrevLeg", transferFromPrevLeg)
                .addObj("transferToNextLeg", transferToNextLeg)
                .toString();
    }

}