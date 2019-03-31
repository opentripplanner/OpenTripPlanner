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
import org.opentripplanner.routing.algorithm.raptor.transit_data_provider.TripSchedule;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.TransitLayer;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.vertextype.TransitVertex;
import org.opentripplanner.util.PolylineEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class ItineraryMapper {
    private final TransitLayer transitLayer;

    private final RoutingRequest request;

    public ItineraryMapper(TransitLayer transitLayer, RoutingRequest request) {
        this.transitLayer = transitLayer;
        this.request = request;
    }

    public TripPlan createTripPlan(RoutingRequest request, List<Itinerary> itineraries) {
        Place from = new Place();
        Place to = new Place();
        if (!itineraries.isEmpty()) {
            from = itineraries.get(0).legs.get(0).from;
            to = itineraries.get(0).legs.get(itineraries.get(0).legs.size() - 1).to;
        }
        TripPlan tripPlan = new TripPlan(from, to, request.getDateTime());
        itineraries = itineraries.stream().sorted((i1, i2) -> i1.endTime.compareTo(i2.endTime))
                .limit(request.numItineraries).collect(Collectors.toList());
        tripPlan.itinerary = itineraries;
        return tripPlan;
    }

    public Itinerary createItinerary(RoutingRequest request, Path<TripSchedule> path, Map<Stop, Transfer> accessPaths, Map<Stop, Transfer> egressPaths) {
        Itinerary itinerary = new Itinerary();

        // Map access leg
        Leg accessLeg = mapAccessLeg(request, path.accessLeg(), accessPaths);


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
                itinerary.transfers++;
                itinerary.walkTime += pathLeg.duration();
            }
            pathLeg = pathLeg.nextLeg();
        }

        // Map egress leg
        EgressPathLeg<TripSchedule> egressPathLeg = pathLeg.asEgressLeg();

        Leg egressLeg = mapEgressLeg(request, egressPathLeg, egressPaths);

        if (egressLeg.distance > 0) {
            itinerary.walkDistance = egressLeg.distance;
            itinerary.addLeg(egressLeg);
        }

        // Increment counters
        itinerary.walkTime += egressPathLeg.toTime() - egressPathLeg.fromTime();

        // Map general itinerary fields
        itinerary.startTime = createCalendar(path.accessLeg().fromTime());
        itinerary.endTime = createCalendar(egressPathLeg.toTime());
        itinerary.duration = (long) egressPathLeg.toTime() - path.accessLeg().fromTime();
        itinerary.waitingTime = itinerary.duration - itinerary.walkTime - itinerary.transitTime;
        itinerary.walkDistance = itinerary.legs.stream().mapToDouble(l -> l.distance).sum();

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

    private Calendar createCalendar(int timeinSeconds) {
        Date date = request.getDateTime();
        LocalDate localDate = Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Oslo")); // TODO: Get time zone from request
        calendar.set(localDate.getYear(), localDate.getMonth().getValue() - 1, localDate.getDayOfMonth()
                , 0, 0, 0);
        calendar.add(Calendar.SECOND, timeinSeconds);
        return calendar;
    }

    private List<Coordinate> extractTransitLegCoordinates(TransitPathLeg<TripSchedule> pathLeg) {
        List<Coordinate> transitLegCoordinates = new ArrayList<>();
        TripPattern tripPattern = pathLeg.trip().getOriginalTripPattern();
        TripSchedule tripSchedule = pathLeg.trip();
        boolean boarded = false;
        for (int j = 0; j < tripPattern.stopPattern.stops.length; j++) {
            if (!boarded && tripSchedule.departure(j) == pathLeg.fromTime()) {
                boarded = true;
            }
            if (boarded) {
                transitLegCoordinates.add(new Coordinate(tripPattern.stopPattern.stops[j].getLon(),
                        tripPattern.stopPattern.stops[j].getLat()));
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
