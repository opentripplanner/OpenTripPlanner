package org.opentripplanner.transit.raptor.speed_test.transit;

import org.opentripplanner.graph_builder.module.NearbyStopFinder;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.transit.raptor.api.path.AccessPathLeg;
import org.opentripplanner.transit.raptor.api.path.EgressPathLeg;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransferPathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.speed_test.SpeedTestRequest;
import org.opentripplanner.transit.raptor.speed_test.model.Itinerary;
import org.opentripplanner.transit.raptor.speed_test.model.Leg;
import org.opentripplanner.transit.raptor.speed_test.model.Place;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    public static List<Itinerary> mapItineraries(
            SpeedTestRequest request,
            Collection<Path<TripSchedule>> paths,
            EgressAccessRouter streetRouter,
            TransitLayer transitLayer
    ) {
        ItineraryMapper mapper = new ItineraryMapper(request, transitLayer);
        List<Itinerary> itineraries = new ArrayList<>();

        for (Path<TripSchedule> p : paths) {
            int accessToStopIndex = p.accessLeg().toStop();
            int egressToStopIndex = p.egressLeg().fromStop();

            Itinerary itinerary = mapper.createItinerary(
                    p,
                    streetRouter.getAccessPath(accessToStopIndex),
                    streetRouter.getEgressPath(egressToStopIndex)
            );
            itineraries.add(itinerary);
        }
        return itineraries;
    }

    private Itinerary createItinerary(
            Path<TripSchedule> path,
            NearbyStopFinder.StopAtDistance accessPath,
            NearbyStopFinder.StopAtDistance egressPath
    ) {
        if (path == null) {  return null; }

        Itinerary itinerary = new Itinerary();

        itinerary.walkDistance = 0.0;
        itinerary.transitTime = 0;
        itinerary.waitingTime = 0;
        itinerary.weight = path.cost();

        int numberOfTransits = 0;

        // Access leg
        Leg leg = new Leg();
        AccessPathLeg<TripSchedule> accessLeg = path.accessLeg();

        leg.startTime = accessLeg.fromTime();
        leg.endTime = accessLeg.toTime();
        leg.from = request.tc().fromPlace;
        leg.to = request.tc().toPlace;
        leg.mode = WALK;

        leg.distance = accessPath.distance;

        itinerary.addLeg(leg);

        PathLeg<TripSchedule> pathLeg = accessLeg.nextLeg();

        int previousArrivalTime = -1;

        while (pathLeg.isTransitLeg() || pathLeg.isTransferLeg()) {
            leg = new Leg();

            // Transfer leg if present
            if (pathLeg.isTransferLeg()) {
                TransferPathLeg<?> it = pathLeg.asTransferLeg();
                previousArrivalTime = it.toTime();

                leg.startTime = it.fromTime();
                leg.endTime = previousArrivalTime;
                leg.mode = WALK;

                leg.from = mapToPlace(it.fromStop());
                leg.to = mapToPlace(it.toStop());
                leg.distance = -1.0; //distanceMMToMeters (transferPath.getDistance());
            }
            else {
                // Transit leg
                TransitPathLeg<TripSchedule> it = pathLeg.asTransitLeg();

                itinerary.transitTime += it.toTime() - it.fromTime();
                itinerary.waitingTime += it.fromTime() - previousArrivalTime;
                previousArrivalTime = it.toTime();

                ++numberOfTransits;
                leg.distance = 0.0;

                TripSchedule tripSchedule = it.trip();
                TripPattern tripPattern = tripSchedule.getOriginalTripPattern();
                Route routeInfo = tripPattern.route;


                leg.from = mapToPlace(it.fromStop());
                leg.to = mapToPlace(it.toStop());

                leg.route = routeInfo.getShortName();
                leg.agencyName = routeInfo.getAgency().getName();
                leg.tripShortName = tripSchedule.getOriginalTripPattern().name;
                leg.agencyId = routeInfo.getAgency().getId();
                leg.routeShortName = routeInfo.getShortName()   ;
                leg.routeLongName = routeInfo.getLongName();
                leg.mode = TraverseMode.fromTransitMode(tripSchedule.getOriginalTripPattern().getMode());

                leg.startTime = it.fromTime();
                leg.endTime = it.toTime();
            }
            itinerary.addLeg(leg);
            pathLeg = pathLeg.nextLeg();
        }

        // Egress leg
        leg = new Leg();
        EgressPathLeg<TripSchedule> egressLeg = pathLeg.asEgressLeg();

        leg.startTime = egressLeg.fromTime();
        leg.endTime = egressLeg.toTime();
        leg.from = mapToPlace(egressLeg.fromStop());
        leg.to = request.tc().toPlace;
        leg.mode = WALK;
        leg.distance = egressPath.distance;

        itinerary.addLeg(leg);

        itinerary.startTime = itinerary.legs.get(0).startTime;
        itinerary.endTime = leg.endTime;
        itinerary.duration = itinerary.endTime - itinerary.startTime;

        // The number of transfers is the number of transits minus one, we can NOT count the number of Transfers
        // in the path or itinerary, because transfers at the same stop does not produce a transfer object, just two
        // transits following each other.
        itinerary.transfers = numberOfTransits-1;

        return itinerary;
    }

    private Place mapToPlace(int stopIndex) {
        return new Place(transitLayer.getStopByIndex(stopIndex), stopIndex);
    }
}
