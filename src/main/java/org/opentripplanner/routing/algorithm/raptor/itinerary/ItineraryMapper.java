package org.opentripplanner.routing.algorithm.raptor.itinerary;

import com.conveyal.r5.otp2.api.path.AccessPathLeg;
import com.conveyal.r5.otp2.api.path.EgressPathLeg;
import com.conveyal.r5.otp2.api.path.Path;
import com.conveyal.r5.otp2.api.path.PathLeg;
import com.conveyal.r5.otp2.api.path.TransferPathLeg;
import com.conveyal.r5.otp2.api.path.TransitPathLeg;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.VertexType;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.vertextype.TransitVertex;
import org.opentripplanner.util.PolylineEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class ItineraryMapper {
    private final TransitLayer transitLayer;

    private final RoutingRequest request;

    private final ZonedDateTime startOfTime;

    private final Map<Stop, Transfer> accessTransfers;

    private final Map<Stop, Transfer> egressTransfers;

    private static Logger LOG = LoggerFactory.getLogger(ItineraryMapper.class);

    public ItineraryMapper(TransitLayer transitLayer, ZonedDateTime startOfTime, RoutingRequest request, Map<Stop, Transfer> accessTransfers, Map<Stop, Transfer> egressTransfers) {
        this.transitLayer = transitLayer;
        this.startOfTime = startOfTime;
        this.request = request;
        this.accessTransfers = accessTransfers;
        this.egressTransfers = egressTransfers;
    }

    public TripPlan createTripPlan(List<Itinerary> itineraries) {
        Place from = new Place();
        Place to = new Place();
        if (!itineraries.isEmpty()) {
            from = itineraries.get(0).legs.get(0).from;
            to = itineraries.get(0).legs.get(itineraries.get(0).legs.size() - 1).to;
        }
        TripPlan tripPlan = new TripPlan(from, to, request.getDateTime());
        itineraries = itineraries.stream().sorted(Comparator.comparing(i -> i.endTime))
                .limit(request.numItineraries).collect(Collectors.toList());
        tripPlan.itinerary = itineraries;
        LOG.info("Returning {} itineraries", itineraries.size());
        return tripPlan;
    }

    public Itinerary createItinerary(Path<TripSchedule> path) {
        Itinerary itinerary = new Itinerary();

        // Map access leg
        Leg accessLeg = mapAccessLeg(request, path.accessLeg(), accessTransfers);


        if (accessLeg.distance > 0) {
            itinerary.walkDistance += accessLeg.distance;
            itinerary.addLeg(accessLeg);
        }

        // Increment counters
        itinerary.walkTime += path.accessLeg().toTime() - path.accessLeg().fromTime();

        // TODO: Add back this code when PathLeg interface contains object references

        PathLeg<TripSchedule> pathLeg = path.accessLeg().nextLeg();

        while (!pathLeg.isEgressLeg()) {
            // Map transit leg
            if (pathLeg.isTransitLeg()) {
                Leg transitLeg = mapTransitLeg(request, pathLeg.asTransitLeg());
                itinerary.addLeg(transitLeg);
                // Increment counters
                itinerary.transitTime += pathLeg.duration();
            }

            // Map transfer leg
            if (pathLeg.isTransferLeg()) {
                Leg transferLeg = mapTransferLeg(pathLeg.asTransferLeg());

                itinerary.walkDistance += transferLeg.distance;
                itinerary.addLeg(transferLeg);

                // Increment counters
                itinerary.walkTime += pathLeg.duration();
            }

            pathLeg = pathLeg.nextLeg();
        }

        // Map egress leg
        EgressPathLeg<TripSchedule> egressPathLeg = pathLeg.asEgressLeg();

        Leg egressLeg = mapEgressLeg(request, egressPathLeg, egressTransfers);

        if (egressLeg.distance > 0) {
            itinerary.walkDistance += egressLeg.distance;
            itinerary.addLeg(egressLeg);
        }

        // Increment counters
        itinerary.walkTime += egressPathLeg.toTime() - egressPathLeg.fromTime();

        // Map general itinerary fields
        itinerary.transfers = path.numberOfTransfers();
        itinerary.startTime = createCalendar(path.accessLeg().fromTime());
        itinerary.endTime = createCalendar(egressPathLeg.toTime());
        itinerary.duration = (long) egressPathLeg.toTime() - path.accessLeg().fromTime();
        itinerary.waitingTime = itinerary.duration - itinerary.walkTime - itinerary.transitTime;

        return itinerary;
    }

    private Leg mapAccessLeg(
            RoutingRequest request,
            AccessPathLeg<TripSchedule> accessPathLeg,
            Map<Stop, Transfer> accessPaths
    ) {
        Stop accessToStop = transitLayer.getStopByIndex(accessPathLeg.toStop());
        Transfer accessPath = accessPaths.get(accessToStop);
        Leg leg = new Leg();
        leg.stop = new ArrayList<>();
        leg.startTime = createCalendar(accessPathLeg.fromTime());
        leg.endTime = createCalendar(accessPathLeg.toTime());
        leg.mode = "WALK";
        if (request.rctx.fromVertex instanceof TransitVertex) {
            leg.from = new Place(request.rctx.fromVertex.getLon(), request.rctx.fromVertex.getLat(), request.rctx.fromVertex.getName());
            leg.from.stopId = ((TransitVertex) request.rctx.fromVertex).getStopId();
            leg.from.vertexType = VertexType.TRANSIT;
        }
        else {
            leg.from = new Place(request.from.lng, request.from.lat, "Coordinate");
        }
        leg.to = new Place(accessToStop.getLon(), accessToStop.getLat(), accessToStop.getName());
        leg.to.stopId = accessToStop.getId();
        leg.to.vertexType = VertexType.TRANSIT;
        leg.legGeometry = PolylineEncoder.createEncodings(accessPath.getCoordinates());
        leg.distance = (double)accessPath.getDistanceMeters();
        leg.walkSteps = new ArrayList<>(); //TODO: Add walk steps test
        return leg;
    }

    private Leg mapTransitLeg(
            RoutingRequest request,
            TransitPathLeg<TripSchedule> pathLeg
    ) {
        Leg leg = new Leg();

        Stop boardStop = transitLayer.getStopByIndex(pathLeg.fromStop());
        Stop alightStop = transitLayer.getStopByIndex(pathLeg.toStop());
        Trip trip = pathLeg.trip().getOriginalTrip();
        TripPattern tripPattern = pathLeg.trip().getOriginalTripPattern();
        Route route = tripPattern.route;

        leg.serviceDate = new ServiceDate(request.getDateTime()).getAsString(); // TODO: This has to be changed for multi-day searches
        leg.stop = new ArrayList<>();
        leg.startTime = createCalendar(pathLeg.fromTime());
        leg.endTime = createCalendar(pathLeg.toTime());
        leg.mode = tripPattern.mode.toString();
        leg.tripId = trip.getId();
        leg.from = new Place(boardStop.getLon(), boardStop.getLat(), boardStop.getName());
        leg.from.stopId = boardStop.getId();
        leg.from.vertexType = VertexType.TRANSIT;
        leg.to = new Place(alightStop.getLon(), alightStop.getLat(), alightStop.getName());
        leg.to.stopId = alightStop.getId();
        leg.to.vertexType = VertexType.TRANSIT;
        List<Coordinate> transitLegCoordinates = extractTransitLegCoordinates(pathLeg);
        leg.legGeometry = PolylineEncoder.createEncodings(transitLegCoordinates);
        leg.distance = getDistanceFromCoordinates(transitLegCoordinates);

        if (request.showIntermediateStops) {
            leg.stop = extractIntermediateStops(pathLeg);
        }

        leg.route = route.getLongName();
        leg.routeId = route.getId();
        leg.agencyName = route.getAgency().getName();
        leg.routeColor = route.getColor();
        leg.tripShortName = route.getShortName();
        leg.agencyId = route.getAgency().getId();
        leg.routeShortName = route.getShortName();
        leg.routeLongName = route.getLongName();
        leg.walkSteps = new ArrayList<>();
        return leg;
    }

    private Leg mapTransferLeg(TransferPathLeg<TripSchedule> pathLeg) {
        Stop transferFromStop = transitLayer.getStopByIndex(pathLeg.fromStop());
        Stop transferToStop = transitLayer.getStopByIndex(pathLeg.toStop());
        Transfer transfer = transitLayer.getTransferByStopIndex().get(pathLeg.fromStop()).stream().filter(t -> t.stop() == pathLeg.toStop()).findFirst().get();

        Leg leg = new Leg();
        leg.stop = new ArrayList<>(); // TODO: Map intermediate stops
        leg.startTime = createCalendar(pathLeg.fromTime());
        leg.endTime = createCalendar(pathLeg.toTime());
        leg.mode = "WALK";
        leg.from = new Place(transferFromStop.getLon(), transferFromStop.getLat(), transferFromStop.getName());
        leg.from.stopId = transferFromStop.getId();
        leg.from.vertexType = VertexType.TRANSIT;
        leg.to = new Place(transferToStop.getLon(), transferToStop.getLat(), transferToStop.getName());
        leg.to.stopId = transferToStop.getId();
        leg.to.vertexType = VertexType.TRANSIT;
        leg.legGeometry = PolylineEncoder.createEncodings(transfer.getCoordinates());
        leg.distance = (double)transfer.getDistanceMeters();
        leg.walkSteps = new ArrayList<>(); //TODO: Add walk steps
        return leg;
    }

    private Leg mapEgressLeg(
            RoutingRequest request,
            EgressPathLeg<TripSchedule> egressPathLeg,
            Map<Stop, Transfer> egressPaths
    ) {

        Stop egressStop = transitLayer.getStopByIndex(egressPathLeg.fromStop());
        Transfer egressPath = egressPaths.get(egressStop);

        Leg leg = new Leg();
        leg.stop = new ArrayList<>();
        leg.startTime = createCalendar(egressPathLeg.fromTime());

        leg.endTime = createCalendar(egressPathLeg.toTime());
        leg.mode = "WALK";
        leg.from = new Place(egressStop.getLon(), egressStop.getLat(), egressStop.getName());
        leg.from.stopId = egressStop.getId();
        leg.from.vertexType = VertexType.TRANSIT;
        if (request.rctx.toVertex instanceof TransitVertex) {
            leg.to = new Place(request.rctx.toVertex.getLon(), request.rctx.toVertex.getLat(), request.rctx.toVertex.getName());
            leg.to.stopId = ((TransitVertex) request.rctx.toVertex).getStopId();
            leg.to.vertexType = VertexType.TRANSIT;
        }
        else {
            leg.to = new Place(request.to.lng, request.to.lat, "Coordinate");
        }
        leg.legGeometry = PolylineEncoder.createEncodings(egressPath.getCoordinates());
        leg.distance = (double)egressPath.getDistanceMeters();
        leg.walkSteps = new ArrayList<>(); //TODO: Add walk steps
        return leg;
    }

    private Calendar createCalendar(int timeInSeconds) {
        ZonedDateTime zdt = startOfTime.plusSeconds(timeInSeconds);
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone(zdt.getZone()));
        c.setTimeInMillis(zdt.toInstant().toEpochMilli());
        return c;
    }

    private List<Place> extractIntermediateStops(TransitPathLeg<TripSchedule> pathLeg) {
        List<Place> places = new ArrayList<>();
        TripPattern tripPattern = pathLeg.trip().getOriginalTripPattern();
        TripSchedule tripSchedule = pathLeg.trip();
        boolean boarded = false;
        for (int j = 0; j < tripPattern.stopPattern.stops.length; j++) {
            if (boarded && tripSchedule.arrival(j) == pathLeg.toTime()) {
                break;
            }
            if (boarded) {
                Stop stop = tripPattern.stopPattern.stops[j];
                Place place = new Place();
                place.name = stop.getName();
                place.lon = stop.getLon();
                place.lat = stop.getLat();
                place.stopId = stop.getId();
                place.stopCode = stop.getCode();
                place.platformCode = stop.getPlatformCode();
                place.zoneId = stop.getZoneId();
                place.stopIndex = j;
                // TODO: fill out stopSequence
                place.arrival = createCalendar(tripSchedule.arrival(j));
                place.departure = createCalendar(tripSchedule.departure(j));
                places.add(place);
            }
            if (!boarded && tripSchedule.departure(j) == pathLeg.fromTime()) {
                boarded = true;
            }
        }
        return places;
    }

    private List<Coordinate> extractTransitLegCoordinates(TransitPathLeg<TripSchedule> pathLeg) {
        List<Coordinate> transitLegCoordinates = new ArrayList<>();
        TripPattern tripPattern = pathLeg.trip().getOriginalTripPattern();
        TripSchedule tripSchedule = pathLeg.trip();
        boolean boarded = false;
        for (int j = 0; j < tripPattern.stopPattern.stops.length; j++) {
            if (boarded) {
                transitLegCoordinates.addAll(Arrays.asList(tripPattern.hopEdges[j - 1].getGeometry().getCoordinates()));
            }
            if (!boarded && tripSchedule.departure(j) == pathLeg.fromTime()) {
                boarded = true;
            }
            if (boarded && tripSchedule.arrival(j) == pathLeg.toTime()) {
                break;
            }
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
}
