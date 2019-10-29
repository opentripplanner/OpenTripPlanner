package org.opentripplanner.transit.raptor.speed_test.transit;

import com.conveyal.r5.profile.ProfileRequest;
import com.conveyal.r5.profile.StreetPath;
import com.conveyal.r5.streets.StreetRouter;
import com.conveyal.r5.transit.RouteInfo;
import com.conveyal.r5.transit.TransitLayer;
import com.conveyal.r5.transit.TransportNetwork;
import com.conveyal.r5.transit.TripPattern;
import com.vividsolutions.jts.geom.Coordinate;
import org.opentripplanner.transit.raptor.api.path.AccessPathLeg;
import org.opentripplanner.transit.raptor.api.path.EgressPathLeg;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransferPathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.speed_test.api.model.AgencyAndId;
import org.opentripplanner.transit.raptor.speed_test.api.model.Leg;
import org.opentripplanner.transit.raptor.speed_test.api.model.Place;
import org.opentripplanner.transit.raptor.speed_test.api.model.PolylineEncoder;
import org.opentripplanner.transit.raptor.transitadapter.TripScheduleAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class ItineraryMapper2 {
    private Logger LOG = LoggerFactory.getLogger(org.opentripplanner.transit.raptor.speed_test.transit.ItineraryMapper2.class);
    private TransportNetwork transportNetwork;
    private ProfileRequest request;

    private ItineraryMapper2(ProfileRequest request, TransportNetwork transportNetwork) {
        this.request = request;
        this.transportNetwork = transportNetwork;
    }

    public static ItinerarySet mapItineraries(
            ProfileRequest request,
            Collection<Path<TripScheduleAdapter>> paths,
            EgressAccessRouter streetRouter,
            TransportNetwork transportNetwork
    ) {
        org.opentripplanner.transit.raptor.speed_test.transit.ItineraryMapper2 mapper = new org.opentripplanner.transit.raptor.speed_test.transit.ItineraryMapper2(request, transportNetwork);
        ItinerarySet itineraries = new ItinerarySet();

        for (Path<TripScheduleAdapter> p : paths) {
            StreetPath accessPath = streetRouter.accessPath(p.accessLeg().toStop());
            StreetPath egressPath = streetRouter.egressPath(p.egressLeg().fromStop());
            SpeedTestItinerary itinerary = mapper.createItinerary(p, accessPath, egressPath);
            itineraries.add(itinerary);
        }
        return itineraries;
    }


    private SpeedTestItinerary createItinerary(Path<TripScheduleAdapter> path, StreetPath accessPath, StreetPath egressPath) {
        SpeedTestItinerary itinerary = new SpeedTestItinerary();
        if (path == null) {
            return null;
        }

        itinerary.walkDistance = 0.0;
        itinerary.transitTime = 0;
        itinerary.waitingTime = 0;
        itinerary.weight = path.cost();

        int numberOfTransits = 0;
        int accessTime = accessPath.getDuration();
        int egressTime = egressPath.getDuration();

        List<Coordinate> acessCoords = accessPath.getEdges().stream()
                .map(t -> new Coordinate(accessPath.getEdge(t).getGeometry().getCoordinate().x, accessPath.getEdge(t).getGeometry().getCoordinate().y)).collect(Collectors.toList());
        List<Coordinate> egressCoords = egressPath.getEdges().stream()
                .map(t -> new Coordinate(egressPath.getEdge(t).getGeometry().getCoordinate().x, egressPath.getEdge(t).getGeometry().getCoordinate().y)).collect(Collectors.toList());
        Collections.reverse(egressCoords);

        // Access leg
        Leg leg = new Leg();
        AccessPathLeg<TripScheduleAdapter> accessLeg = path.accessLeg();

        Stop firstStop = stop(accessLeg.toStop());

        leg.startTime = createCalendar(request.date, accessLeg.fromTime());
        leg.endTime = createCalendar(request.date, accessLeg.toTime());
        leg.from = new Place(request.fromLon, request.fromLat, "Origin");
        leg.from.stopIndex = -1;
        leg.to = new Place(firstStop.locLat(), firstStop.locLon(), firstStop.name());
        leg.to.stopId = new AgencyAndId("RB", firstStop.id());
        leg.to.stopIndex = accessLeg.toStop();
        leg.mode = "WALK";
        leg.legGeometry = PolylineEncoder.createEncodings(acessCoords);
        leg.distance = distanceMMToMeters(accessPath.getDistance());

        itinerary.addLeg(leg);

        PathLeg<TripScheduleAdapter> pathLeg = accessLeg.nextLeg();

        int previousArrivalTime = -1;

        while (pathLeg.isTransitLeg() || pathLeg.isTransferLeg()) {
            leg = new Leg();

            // Transfer leg if present
            if (pathLeg.isTransferLeg()) {
                TransferPathLeg it = pathLeg.asTransferLeg();
                Stop fromStop = stop(it.fromStop());
                Stop toStop = stop(it.toStop());
                previousArrivalTime = it.toTime();


                StreetPath transferPath = getWalkLegCoordinates(it.fromStop(), it.toStop());
                List<Coordinate> transferCoords = transferPath.getEdges().stream()
                        .map(t -> new Coordinate(transferPath.getEdge(t).getGeometry().getCoordinate().x, transferPath
                                .getEdge(t).getGeometry().getCoordinate().y)).collect(Collectors.toList());

                leg.startTime = createCalendar(request.date, it.fromTime());
                leg.endTime = createCalendar(request.date, previousArrivalTime);
                leg.mode = "WALK";
                leg.from = new Place(fromStop.locLat(), fromStop.locLon(), fromStop.name());
                leg.to = new Place(toStop.locLat(), toStop.locLon(), toStop.name());
                leg.legGeometry = PolylineEncoder.createEncodings(transferCoords);

                leg.distance = distanceMMToMeters (transferPath.getDistance());

            }
            else {
                // Transit leg
                TransitPathLeg<TripScheduleAdapter> it = pathLeg.asTransitLeg();
                Stop fromStop = stop(it.fromStop());
                Stop toStop = stop(it.toStop());

                itinerary.transitTime += it.toTime() - it.fromTime();
                itinerary.waitingTime += it.fromTime() - previousArrivalTime;
                previousArrivalTime = it.toTime();

                ++numberOfTransits;
                leg.distance = 0.0;

                TripScheduleAdapter tripSchedule = it.trip();
                TripPattern tripPattern = tripSchedule.tripPattern();
                RouteInfo routeInfo = transitLayer().routes.get(tripPattern.routeIndex);


                leg.from = new Place(fromStop.locLat(), fromStop.locLon(), fromStop.name());
                leg.from.stopId = new AgencyAndId("RB", fromStop.id());
                leg.from.stopIndex = it.fromStop();

                leg.to = new Place(toStop.locLat(), toStop.locLon(), toStop.name());
                leg.to.stopId = new AgencyAndId("RB", toStop.id());
                leg.to.stopIndex = it.toStop();

                leg.route = routeInfo.route_short_name;
                leg.agencyName = routeInfo.agency_name;
                leg.routeColor = routeInfo.color;
                leg.tripShortName = tripSchedule.tripSchedule().tripId;
                leg.agencyId = routeInfo.agency_id;
                leg.routeShortName = routeInfo.route_short_name;
                leg.routeLongName = routeInfo.route_long_name;
                leg.mode = TransitLayer.getTransitModes(routeInfo.route_type).toString();

                List<Coordinate> transitLegCoordinates = new ArrayList<>();
                boolean boarded = false;
                for (int j = 0; j < tripPattern.stops.length; j++) {
                    if (!boarded && tripSchedule.tripSchedule().departures[j] == it.fromTime()) {
                        boarded = true;
                    }
                    if (boarded) {
                        transitLegCoordinates.add(
                                new Coordinate(
                                        stop(tripPattern.stops[j]).locLon(),
                                        stop(tripPattern.stops[j]).locLat()
                                ));
                    }
                    if (boarded && tripSchedule.tripSchedule().arrivals[j] == it.toTime()) {
                        break;
                    }
                }

                leg.legGeometry = PolylineEncoder.createEncodings(transitLegCoordinates);

                leg.startTime = createCalendar(request.date, it.fromTime());
                leg.endTime = createCalendar(request.date, it.toTime());
            }
            itinerary.addLeg(leg);
            pathLeg = pathLeg.nextLeg();
        }

        // Egress leg
        leg = new Leg();
        EgressPathLeg<TripScheduleAdapter> egressLeg = pathLeg.asEgressLeg();

        Stop lastStop = stop(egressLeg.fromStop());
        leg.startTime = createCalendar(request.date, egressLeg.fromTime());
        leg.endTime = createCalendar(request.date, egressLeg.fromTime() + egressTime);
        leg.from = new Place(lastStop.locLat(), lastStop.locLon(), lastStop.name());
        leg.from.stopIndex = egressLeg.fromStop();
        leg.from.stopId = new AgencyAndId("RB", lastStop.id());
        leg.to = new Place(request.toLon, request.toLat, "Destination");
        leg.mode = "WALK";
        leg.legGeometry = PolylineEncoder.createEncodings(egressCoords);

        leg.distance = distanceMMToMeters (egressPath.getDistance());

        itinerary.addLeg(leg);

        itinerary.startTime = itinerary.legs.get(0).startTime;
        itinerary.endTime = leg.endTime;
        itinerary.duration = (itinerary.endTime.getTimeInMillis() - itinerary.startTime.getTimeInMillis())/1000;

        // The number of transfers is the number of transits minus one, we can NOT count the number of Transfers
        // in the path or itinerary, because transfers at the same stop does not produce a transfer object, just two
        // transits following each other.
        itinerary.transfers = numberOfTransits-1;

        itinerary.initParetoVector();

        return itinerary;
    }

    private TransitLayer transitLayer() {
        return transportNetwork.transitLayer;
    }

    private Stop stop(int stopIndex) {
        return new Stop(stopIndex);
    }

    private StreetPath getWalkLegCoordinates(int originStop, int destinationStop) {
        StreetRouter sr = new StreetRouter(transportNetwork.streetLayer);
        sr.profileRequest = new ProfileRequest();
        int originStreetVertex = transitLayer().streetVertexForStop.get(originStop);
        sr.setOrigin(originStreetVertex);
        sr.quantityToMinimize = StreetRouter.State.RoutingVariable.DURATION_SECONDS;
        sr.distanceLimitMeters = 1000;
        sr.route();
        StreetRouter.State transferState = sr.getStateAtVertex(transitLayer().streetVertexForStop
                .get(destinationStop));
        return new StreetPath(transferState, transportNetwork, false);
    }

    private Calendar createCalendar(LocalDate date, int timeinSeconds) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Oslo"));
        calendar.set(date.getYear(), date.getMonth().getValue(), date.getDayOfMonth()
                , 0, 0, 0);
        calendar.add(Calendar.SECOND, timeinSeconds);
        return calendar;
    }

    private double distanceMMToMeters(int distanceMm) {
        return (double) (distanceMm / 1000);
    }


    /**
     * This class is just a wrapper around the 'transportNetwork' to make it easy to
     * retrieve information about a stop from the 'transportNetwork'.
     */
    private class Stop {
        private final int stopIndex;
        private final int vertexIndex;

        Stop(int stopIndex) {
            this.stopIndex = stopIndex;
            this.vertexIndex = findVertexIndex();
        }

        String name() {
            return transitLayer().stopNames.get(stopIndex);
        }

        String id() {
            return transitLayer().stopIdForIndex.get(stopIndex);
        }

        double locLat() {
            return vertexIndex < 0 ? -1.0 : transportNetwork.streetLayer.vertexStore.getCursor(vertexIndex).getLat();
        }

        double locLon() {
            return vertexIndex < 0 ? -1.0 : transportNetwork.streetLayer.vertexStore.getCursor(vertexIndex).getLon();
        }

        private int findVertexIndex() {
            int vertexIndex = transitLayer().streetVertexForStop.get(stopIndex);
            if(vertexIndex < 0) {
                LOG.warn(
                        "The vertex for stop {}({}) is not found! This stop is not connected to the street network.",
                        name(), id()
                );
            }
            return vertexIndex;
        }
    }
}
