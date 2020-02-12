package org.opentripplanner.transit.raptor.speed_test.transit;

import org.opentripplanner.graph_builder.module.NearbyStopFinder;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.transit.raptor.api.path.AccessPathLeg;
import org.opentripplanner.transit.raptor.api.path.EgressPathLeg;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransferPathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.speed_test.SpeedTestRequest;
import org.opentripplanner.transit.raptor.speed_test.api.model.Leg;
import org.opentripplanner.transit.raptor.speed_test.api.model.PlaceAPI;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Collection;
import java.util.TimeZone;

import static org.opentripplanner.routing.core.TraverseMode.WALK;

public class ItineraryMapper {
    private SpeedTestRequest request;
    private TransitLayer transitLayer;

    /**
     * @param transitLayer - need to be passed in because we do not know if we use the static or real-time version
     *                     in the graph.
     */
    private ItineraryMapper(SpeedTestRequest request, TransitLayer transitLayer) {
        this.request = request;
        this.transitLayer = transitLayer;
    }

    public static ItinerarySet mapItineraries(
            SpeedTestRequest request,
            Collection<Path<TripSchedule>> paths,
            EgressAccessRouter streetRouter,
            TransitLayer transitLayer
    ) {
        ItineraryMapper mapper = new ItineraryMapper(request, transitLayer);
        ItinerarySet itineraries = new ItinerarySet();

        for (Path<TripSchedule> p : paths) {
            int accessToStopIndex = p.accessLeg().toStop();
            int egressToStopIndex = p.egressLeg().fromStop();

            SpeedTestItinerary itinerary = mapper.createItinerary(
                    p,
                    streetRouter.getAccessPath(accessToStopIndex),
                    streetRouter.getEgressPath(egressToStopIndex)
            );
            itineraries.add(itinerary);
        }
        return itineraries;
    }

    private SpeedTestItinerary createItinerary(
            Path<TripSchedule> path,
            NearbyStopFinder.StopAtDistance accessPath,
            NearbyStopFinder.StopAtDistance egressPath
    ) {
        SpeedTestItinerary itinerary = new SpeedTestItinerary();
        if (path == null) {
            return null;
        }

        itinerary.walkDistance = 0.0;
        itinerary.transitTime = 0;
        itinerary.waitingTime = 0;
        itinerary.weight = path.cost();

        int numberOfTransits = 0;

        // Access leg
        Leg leg = new Leg();
        AccessPathLeg<TripSchedule> accessLeg = path.accessLeg();

        leg.startTime = createCalendar(request.getDepartureDate(), accessLeg.fromTime());
        leg.endTime = createCalendar(request.getDepartureDate(), accessLeg.toTime());
        leg.from = new PlaceAPI(request.tc().fromPlace);
        leg.to = new PlaceAPI(request.tc().toPlace);
        leg.mode = WALK;

        // TODO TGR - This could be nice to visualize for debugging purposes, but...
        //leg.legGeometry = PolylineEncoder.createEncodings(acessCoords);

        leg.distance = accessPath.distance;

        itinerary.addLeg(leg);

        PathLeg<TripSchedule> pathLeg = accessLeg.nextLeg();

        int previousArrivalTime = -1;

        while (pathLeg.isTransitLeg() || pathLeg.isTransferLeg()) {
            leg = new Leg();

            // Transfer leg if present
            if (pathLeg.isTransferLeg()) {
                TransferPathLeg it = pathLeg.asTransferLeg();
                Stop fromStop = stop(it.fromStop());
                Stop toStop = stop(it.toStop());
                previousArrivalTime = it.toTime();


                /*
                GraphPath transferPath = getWalkLegCoordinates(it.fromStop(), it.toStop());
                List<Coordinate> transferCoords = transferPath.getEdges().stream()
                        .map(t -> new Coordinate(transferPath.getEdge(t).getGeometry().getCoordinate().x, transferPath
                                .getEdge(t).getGeometry().getCoordinate().y)).collect(Collectors.toList());
                */
                leg.startTime = createCalendar(request.getDepartureDate(), it.fromTime());
                leg.endTime = createCalendar(request.getDepartureDate(), previousArrivalTime);
                leg.mode = WALK;
                leg.from = new PlaceAPI(fromStop.locLat(), fromStop.locLon(), fromStop.name());
                leg.to = new PlaceAPI(toStop.locLat(), toStop.locLon(), toStop.name());
                //leg.legGeometry = PolylineEncoder.createEncodings(transferCoords);

                leg.distance = -1.0; //distanceMMToMeters (transferPath.getDistance());

            }
            else {
                // Transit leg
                TransitPathLeg<TripSchedule> it = pathLeg.asTransitLeg();
                Stop fromStop = stop(it.fromStop());
                Stop toStop = stop(it.toStop());

                itinerary.transitTime += it.toTime() - it.fromTime();
                itinerary.waitingTime += it.fromTime() - previousArrivalTime;
                previousArrivalTime = it.toTime();

                ++numberOfTransits;
                leg.distance = 0.0;

                TripSchedule tripSchedule = it.trip();
                TripPattern tripPattern = tripSchedule.getOriginalTripPattern();
                Route routeInfo = tripPattern.route;


                leg.from = new PlaceAPI(fromStop.locLat(), fromStop.locLon(), fromStop.name());
                leg.from.stopId = new FeedScopedId("RB", fromStop.id());
                leg.from.stopIndex = it.fromStop();

                leg.to = new PlaceAPI(toStop.locLat(), toStop.locLon(), toStop.name());
                leg.to.stopId = new FeedScopedId("RB", toStop.id());
                leg.to.stopIndex = it.toStop();

                leg.route = routeInfo.getShortName();
                leg.agencyName = routeInfo.getAgency().getName();
                leg.routeColor = routeInfo.getColor();
                leg.tripShortName = tripSchedule.getOriginalTripPattern().name;
                leg.agencyId = routeInfo.getAgency().getId();
                leg.routeShortName = routeInfo.getShortName()   ;
                leg.routeLongName = routeInfo.getLongName();
                leg.mode = tripSchedule.getOriginalTripPattern().mode;

                leg.startTime = createCalendar(request.getDepartureDate(), it.fromTime());
                leg.endTime = createCalendar(request.getDepartureDate(), it.toTime());
            }
            itinerary.addLeg(leg);
            pathLeg = pathLeg.nextLeg();
        }

        // Egress leg
        leg = new Leg();
        EgressPathLeg<TripSchedule> egressLeg = pathLeg.asEgressLeg();

        Stop lastStop = stop(egressLeg.fromStop());
        leg.startTime = createCalendar(request.getDepartureDate(), egressLeg.fromTime());
        leg.endTime = createCalendar(request.getDepartureDate(), egressLeg.toTime());
        leg.from = new PlaceAPI(lastStop.locLat(), lastStop.locLon(), lastStop.name());
        leg.from.stopIndex = egressLeg.fromStop();
        leg.from.stopId = new FeedScopedId("RB", lastStop.id());
        leg.to = new PlaceAPI(request.tc().toPlace);
        leg.mode = WALK;

        // TODO TGR
        // leg.legGeometry = PolylineEncoder.createEncodings(egressCoords);
        leg.distance = egressPath.distance;

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

    private Stop stop(int stopIndex) {
        return new Stop(transitLayer.getStopByIndex(stopIndex));
    }

    private Calendar createCalendar(LocalDate date, int timeinSeconds) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Oslo"));
        calendar.set(date.getYear(), date.getMonth().getValue(), date.getDayOfMonth()
                , 0, 0, 0);
        calendar.add(Calendar.SECOND, timeinSeconds);
        return calendar;
    }

    /**
     * This class is just a wrapper around the 'transportNetwork' to make it easy to
     * retrieve information about a stop from the 'transportNetwork'.
     */
    private static class Stop {
        private final org.opentripplanner.model.Stop stop;

        public Stop(org.opentripplanner.model.Stop stop) {
            this.stop = stop;
        }

        String name() {
            return stop.getName();
        }

        String id() {
            return stop.getId().getId();
        }

        double locLat() {
            return stop.getLat();
        }

        double locLon() {
            return stop.getLon();
        }
    }
}
