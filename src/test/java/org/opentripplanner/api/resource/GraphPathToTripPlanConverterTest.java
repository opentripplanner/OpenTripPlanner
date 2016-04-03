/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.api.resource;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import org.junit.Test;
import org.onebusaway.gtfs.impl.calendar.CalendarServiceImpl;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.api.model.AbsoluteDirection;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.RelativeDirection;
import org.opentripplanner.api.model.WalkStep;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.gtfs.BikeAccess;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.alertpatch.TimePeriod;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.WrappedCurrency;
import org.opentripplanner.routing.edgetype.AreaEdge;
import org.opentripplanner.routing.edgetype.AreaEdgeList;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.LegSwitchingEdge;
import org.opentripplanner.routing.edgetype.OnBoardDepartPatternHop;
import org.opentripplanner.routing.edgetype.PartialStreetEdge;
import org.opentripplanner.routing.edgetype.PatternDwell;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.PatternInterlineDwell;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.RentABikeOffEdge;
import org.opentripplanner.routing.edgetype.RentABikeOnEdge;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.StreetBikeRentalLink;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.StreetWithElevationEdge;
import org.opentripplanner.routing.edgetype.TimetableSnapshot;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.error.TrivialPathException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.services.notes.StreetNotesService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.ExitVertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.OnboardDepartVertex;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.model.EncodedPolylineBean;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GraphPathToTripPlanConverterTest {
    private static final double F_DISTANCE[] = {3, 9996806.8, 3539050.5, 7, 2478638.8, 4, 2, 1, 0};
    private static final double O_DISTANCE = 7286193.2;
    private static final double OCTANT = Math.PI / 4;
    private static final double NORTH = OCTANT * 0;
    private static final double NORTHEAST = OCTANT * 1;
    private static final double EAST = OCTANT * 2;
    private static final double NORTHWEST = OCTANT * -1;
    private static final double SOUTH = OCTANT * 4;
    private static final double EPSILON = 1e-1;

    private static final SimpleTimeZone timeZone = new SimpleTimeZone(2, "CEST");

    private static final String alertsExample =
            "Mine is the last voice that you will ever hear. Do not be alarmed.";

    private static final Locale locale = new Locale("en");

    /**
     * Test the generateItinerary() method. This test is intended to be comprehensive but fast.
     * Any future changes to the generateItinerary() method should be accompanied by changes in this
     * test, to ensure continued maximum coverage.
     */
    @Test
    public void testGenerateItinerary() {
        GraphPath[] graphPaths = buildPaths();

        compare(GraphPathToTripPlanConverter.generateItinerary(graphPaths[0], true, locale), Type.FORWARD);
        compare(GraphPathToTripPlanConverter.generateItinerary(graphPaths[1], true, locale), Type.BACKWARD);
        compare(GraphPathToTripPlanConverter.generateItinerary(graphPaths[2], true, locale), Type.ONBOARD);
    }

    /**
     * Test that a LEG_SWITCH mode at the end of a graph path does not generate an extra leg.
     * Also test that such a LEG_SWITCH mode does not show up as part of the itinerary.
     */
    @Test
    public void testEndWithLegSwitch() {
        // Reuse testGenerateItinerary()'s graph path, but shorten it
        GraphPath graphPath = new GraphPath(buildPaths()[0].states.get(3), false);

        Itinerary itinerary = GraphPathToTripPlanConverter.generateItinerary(graphPath, false, locale);

        assertEquals(1, itinerary.legs.size());
        assertEquals("WALK", itinerary.legs.get(0).mode);
    }

    /**
     * Test that empty graph paths throw a TrivialPathException
     */
    @Test(expected = TrivialPathException.class)
    public void testEmptyGraphPath() {
        RoutingRequest options = new RoutingRequest();
        Graph graph = new Graph();
        ExitVertex vertex = new ExitVertex(graph, "Vertex", 0, 0, 0);

        options.rctx = new RoutingContext(options, graph, vertex, vertex);

        GraphPath graphPath = new GraphPath(new State(options), false);

        GraphPathToTripPlanConverter.generateItinerary(graphPath, false, locale);
    }

    /**
     * Test that graph paths with only null and LEG_SWITCH modes throw a TrivialPathException
     */
    @Test(expected = TrivialPathException.class)
    public void testLegSwitchOnlyGraphPath() {
        RoutingRequest options = new RoutingRequest();
        Graph graph = new Graph();

        ExitVertex start = new ExitVertex(graph, "Start", 0, -90, 0);
        ExitVertex middle = new ExitVertex(graph, "Middle", 0, 0, 0);
        ExitVertex end = new ExitVertex(graph, "End", 0, 90, 0);

        FreeEdge depart = new FreeEdge(start, middle);
        LegSwitchingEdge arrive = new LegSwitchingEdge(middle, end);

        options.rctx = new RoutingContext(options, graph, start, end);

        State intermediate = depart.traverse(new State(options));

        GraphPath graphPath = new GraphPath(arrive.traverse(intermediate), false);

        GraphPathToTripPlanConverter.generateItinerary(graphPath, false, locale);
    }

    /**
     * Build three GraphPath objects that can be used for testing for forward, backward and onboard.
     * This method doesn't rely on any routing code.
     * Leg 0: Walking towards the train station
     * Leg 1: First train leg, interlined with leg 2
     * Leg 2: Second train leg, interlined with leg 1
     * Leg 3: Simple transfer from the train station to the ferry
     * Leg 4: Ferry leg
     * Leg 5: Walking towards the bike rental station
     * Leg 6: Cycling on a rented bike
     * Leg 7: Cycling on a rented bike, continued (to demonstrate a {@link LegSwitchingEdge})
     * Leg 8: Leaving the bike rental station on foot
     * @return An array containing the generated GraphPath objects: forward, then backward, onboard.
     */
    private GraphPath[] buildPaths() {
        // This set of requested traverse modes implies that bike rental is a possibility.
        RoutingRequest options = new RoutingRequest("BICYCLE_RENT,TRANSIT");

        String feedId = "FEED";

        Graph graph = new Graph();

        // Vertices for leg 0
        ExitVertex v0 = new ExitVertex(
                graph, "Vertex 0", 0, 0, 0);
        IntersectionVertex v2 = new IntersectionVertex(
                graph, "Vertex 2", 0, 0);
        IntersectionVertex v4 = new IntersectionVertex(
                graph, "Vertex 4", 1, 1);

        // Stops for legs 1, 2 and 4, plus initialization and storage in a list
        Stop trainStopDepart = new Stop();
        Stop trainStopDwell = new Stop();
        Stop trainStopInterline = new Stop();
        Stop trainStopArrive = new Stop();
        Stop ferryStopDepart = new Stop();
        Stop ferryStopArrive = new Stop();

        trainStopDepart.setId(new AgencyAndId(feedId, "Depart"));
        trainStopDepart.setName("Train stop depart");
        trainStopDepart.setLon(1);
        trainStopDepart.setLat(1);
        trainStopDepart.setCode("Train depart code");
        trainStopDepart.setPlatformCode("Train depart platform");
        trainStopDepart.setZoneId("Train depart zone");
        trainStopDwell.setId(new AgencyAndId(feedId, "Dwell"));
        trainStopDwell.setName("Train stop dwell");
        trainStopDwell.setLon(45);
        trainStopDwell.setLat(23);
        trainStopDwell.setCode("Train dwell code");
        trainStopDwell.setPlatformCode("Train dwell platform");
        trainStopDwell.setZoneId("Train dwell zone");
        trainStopInterline.setId(new AgencyAndId(feedId, "Interline"));
        trainStopInterline.setName("Train stop interline");
        trainStopInterline.setLon(89);
        trainStopInterline.setLat(45);
        trainStopInterline.setCode("Train interline code");
        trainStopInterline.setPlatformCode("Train interline platform");
        trainStopInterline.setZoneId("Train interline zone");
        trainStopArrive.setId(new AgencyAndId(feedId, "Arrive"));
        trainStopArrive.setName("Train stop arrive");
        trainStopArrive.setLon(133);
        trainStopArrive.setLat(67);
        trainStopArrive.setCode("Train arrive code");
        trainStopArrive.setPlatformCode("Train arrive platform");
        trainStopArrive.setZoneId("Train arrive zone");
        ferryStopDepart.setId(new AgencyAndId(feedId, "Depart"));
        ferryStopDepart.setName("Ferry stop depart");
        ferryStopDepart.setLon(135);
        ferryStopDepart.setLat(67);
        ferryStopDepart.setCode("Ferry depart code");
        ferryStopDepart.setPlatformCode("Ferry depart platform");
        ferryStopDepart.setZoneId("Ferry depart zone");
        ferryStopArrive.setId(new AgencyAndId(feedId, "Arrive"));
        ferryStopArrive.setName("Ferry stop arrive");
        ferryStopArrive.setLon(179);
        ferryStopArrive.setLat(89);
        ferryStopArrive.setCode("Ferry arrive code");
        ferryStopArrive.setPlatformCode("Ferry arrive platform");
        ferryStopArrive.setZoneId("Ferry arrive zone");

        ArrayList<Stop> firstStops = new ArrayList<Stop>();
        ArrayList<Stop> secondStops = new ArrayList<Stop>();
        ArrayList<Stop> thirdStops = new ArrayList<Stop>();

        firstStops.add(trainStopDepart);
        firstStops.add(trainStopDwell);
        firstStops.add(trainStopInterline);
        secondStops.add(trainStopInterline);
        secondStops.add(trainStopArrive);
        thirdStops.add(ferryStopDepart);
        thirdStops.add(ferryStopArrive);

        // Agencies for legs 1, 2 and 4, plus initialization
        Agency trainAgency = new Agency();
        Agency ferryAgency = new Agency();

        trainAgency.setId("Train");
        trainAgency.setName("John Train");
        trainAgency.setUrl("http://www.train.org/");
        ferryAgency.setId("Ferry");
        ferryAgency.setName("Brian Ferry");
        ferryAgency.setUrl("http://www.ferry.org/");

        // Routes for legs 1, 2 and 4, plus initialization
        Route firstRoute = new Route();
        Route secondRoute = new Route();
        Route thirdRoute = new Route();

        firstRoute.setId(new AgencyAndId(feedId, "A"));
        firstRoute.setAgency(trainAgency);
        firstRoute.setShortName("A");
        firstRoute.setLongName("'A' Train");
        firstRoute.setType(2);
        firstRoute.setColor("White");
        firstRoute.setTextColor("Black");
        secondRoute.setId(new AgencyAndId(feedId, "B"));
        secondRoute.setAgency(trainAgency);
        secondRoute.setShortName("B");
        secondRoute.setLongName("Another Train");
        secondRoute.setType(2);
        secondRoute.setColor("Cyan");
        secondRoute.setTextColor("Yellow");
        thirdRoute.setId(new AgencyAndId(feedId, "C"));
        thirdRoute.setAgency(ferryAgency);
        thirdRoute.setShortName("C");
        thirdRoute.setLongName("Ferry Cross the Mersey");
        thirdRoute.setType(4);
        thirdRoute.setColor("Black");
        thirdRoute.setTextColor("White");

        // Trips for legs 1, 2 and 4, plus initialization
        Trip firstTrip = new Trip();
        Trip secondTrip = new Trip();
        Trip thirdTrip = new Trip();

        firstTrip.setId(new AgencyAndId(feedId, "A"));
        firstTrip.setTripShortName("A");
        firstTrip.setBlockId("Alock");
        firstTrip.setRoute(firstRoute);
        BikeAccess.setForTrip(firstTrip, BikeAccess.ALLOWED);
        firstTrip.setTripHeadsign("Street Fighting Man");
        secondTrip.setId(new AgencyAndId(feedId, "B"));
        secondTrip.setTripShortName("B");
        secondTrip.setBlockId("Block");
        secondTrip.setRoute(secondRoute);
        BikeAccess.setForTrip(secondTrip, BikeAccess.ALLOWED);
        secondTrip.setTripHeadsign("No Expectations");
        thirdTrip.setId(new AgencyAndId(feedId, "C"));
        thirdTrip.setTripShortName("C");
        thirdTrip.setBlockId("Clock");
        thirdTrip.setRoute(thirdRoute);
        BikeAccess.setForTrip(thirdTrip, BikeAccess.ALLOWED);
        thirdTrip.setTripHeadsign("Handsome Molly");

        // Scheduled stop times for legs 1, 2 and 4, plus initialization and storage in a list
        StopTime trainStopDepartTime = new StopTime();
        StopTime trainStopDwellTime = new StopTime();
        StopTime trainStopInterlineFirstTime = new StopTime();
        StopTime trainStopInterlineSecondTime = new StopTime();
        StopTime trainStopArriveTime = new StopTime();
        StopTime ferryStopDepartTime = new StopTime();
        StopTime ferryStopArriveTime = new StopTime();

        trainStopDepartTime.setTrip(firstTrip);
        trainStopDepartTime.setStop(trainStopDepart);
        trainStopDepartTime.setStopSequence(Integer.MIN_VALUE);
        trainStopDepartTime.setDepartureTime(4);
        trainStopDepartTime.setPickupType(3);
        trainStopDwellTime.setTrip(firstTrip);
        trainStopDwellTime.setStop(trainStopDwell);
        trainStopDwellTime.setStopSequence(0);
        trainStopDwellTime.setArrivalTime(8);
        trainStopDwellTime.setDepartureTime(12);
        trainStopInterlineFirstTime.setTrip(firstTrip);
        trainStopInterlineFirstTime.setStop(trainStopInterline);
        trainStopInterlineFirstTime.setStopSequence(Integer.MAX_VALUE);
        trainStopInterlineFirstTime.setArrivalTime(16);
        trainStopInterlineSecondTime.setTrip(secondTrip);
        trainStopInterlineSecondTime.setStop(trainStopInterline);
        trainStopInterlineSecondTime.setStopSequence(0);
        trainStopInterlineSecondTime.setDepartureTime(20);
        trainStopArriveTime.setTrip(secondTrip);
        trainStopArriveTime.setStop(trainStopArrive);
        trainStopArriveTime.setStopSequence(1);
        trainStopArriveTime.setArrivalTime(24);
        trainStopArriveTime.setDropOffType(2);
        ferryStopDepartTime.setTrip(thirdTrip);
        ferryStopDepartTime.setStop(ferryStopDepart);
        ferryStopDepartTime.setStopSequence(-1);
        ferryStopDepartTime.setDepartureTime(32);
        ferryStopDepartTime.setPickupType(2);
        ferryStopArriveTime.setTrip(thirdTrip);
        ferryStopArriveTime.setStop(ferryStopArrive);
        ferryStopArriveTime.setStopSequence(0);
        ferryStopArriveTime.setArrivalTime(36);
        ferryStopArriveTime.setDropOffType(3);

        ArrayList<StopTime> firstStopTimes = new ArrayList<StopTime>();
        ArrayList<StopTime> secondStopTimes = new ArrayList<StopTime>();
        ArrayList<StopTime> thirdStopTimes = new ArrayList<StopTime>();

        firstStopTimes.add(trainStopDepartTime);
        firstStopTimes.add(trainStopDwellTime);
        firstStopTimes.add(trainStopInterlineFirstTime);
        secondStopTimes.add(trainStopInterlineSecondTime);
        secondStopTimes.add(trainStopArriveTime);
        thirdStopTimes.add(ferryStopDepartTime);
        thirdStopTimes.add(ferryStopArriveTime);

        // Various patterns that are required to construct a full graph path, plus initialization
        StopPattern firstStopPattern  = new StopPattern(firstStopTimes);
        StopPattern secondStopPattern = new StopPattern(secondStopTimes);
        StopPattern thirdStopPattern  = new StopPattern(thirdStopTimes);

        TripPattern firstTripPattern  = new TripPattern(firstRoute, firstStopPattern);
        TripPattern secondTripPattern = new TripPattern(secondRoute, secondStopPattern);
        TripPattern thirdTripPattern  = new TripPattern(thirdRoute, thirdStopPattern);

        TripTimes firstTripTimes  = new TripTimes(firstTrip, firstStopTimes, new Deduplicator());
        TripTimes secondTripTimes = new TripTimes(secondTrip, secondStopTimes, new Deduplicator());
        TripTimes thirdTripTimes  = new TripTimes(thirdTrip, thirdStopTimes, new Deduplicator());

        firstTripPattern.add(firstTripTimes);
        secondTripPattern.add(secondTripTimes);
        thirdTripPattern.add(thirdTripTimes);

        // Vertices for legs 1, 2 and 3
        TransitStop v6 = new TransitStop(graph, trainStopDepart);
        TransitStopDepart v8 = new TransitStopDepart(graph, trainStopDepart, v6);
        // To understand the stop indexes in the vertex constructors, look at firstStopTimes.add() etc. above
        PatternDepartVertex v10 = new PatternDepartVertex(graph, firstTripPattern, 0);
        PatternArriveVertex v12 = new PatternArriveVertex(graph, firstTripPattern, 1);
        PatternDepartVertex v14 = new PatternDepartVertex(graph, firstTripPattern, 1);
        PatternArriveVertex v16 = new PatternArriveVertex(graph, firstTripPattern, 2);
        PatternDepartVertex v18 = new PatternDepartVertex(graph, secondTripPattern, 0);
        PatternArriveVertex v20 = new PatternArriveVertex(graph, secondTripPattern, 1);
        TransitStop v24 = new TransitStop(graph, trainStopArrive);
        TransitStopArrive v22 = new TransitStopArrive(graph, trainStopArrive, v24);

        // Vertices for legs 3 and 4
        TransitStop v26 = new TransitStop(graph, ferryStopDepart);
        TransitStopDepart v28 = new TransitStopDepart(graph, ferryStopDepart, v26);
        PatternDepartVertex v30 = new PatternDepartVertex(graph, thirdTripPattern, 0);
        PatternArriveVertex v32 = new PatternArriveVertex(graph, thirdTripPattern, 1);
        TransitStop v36 = new TransitStop(graph, ferryStopArrive);
        TransitStopArrive v34 = new TransitStopArrive(graph, ferryStopArrive, v36);

        // Vertices for leg 5
        IntersectionVertex v38 = new IntersectionVertex(graph, "Vertex 38", 179, 89);
        IntersectionVertex v40 = new IntersectionVertex(graph, "Vertex 40", 180, 89);
        IntersectionVertex v42 = new IntersectionVertex(graph, "Vertex 42", 180, 90);

        // Bike rental stations for legs 5, 6 and 7, plus initialization
        BikeRentalStation enterPickupStation = new BikeRentalStation();
        BikeRentalStation exitPickupStation = new BikeRentalStation();
        BikeRentalStation enterDropoffStation = new BikeRentalStation();
        BikeRentalStation exitDropoffStation = new BikeRentalStation();

        enterPickupStation.id = "Enter pickup";
        enterPickupStation.name = new NonLocalizedString("Enter pickup station");
        enterPickupStation.x = 180;
        enterPickupStation.y = 90;
        exitPickupStation.id = "Exit pickup";
        exitPickupStation.name = new NonLocalizedString("Exit pickup station");
        exitPickupStation.x = 180;
        exitPickupStation.y = 90;
        enterDropoffStation.id = "Enter dropoff";
        enterDropoffStation.name = new NonLocalizedString("Enter dropoff station");
        enterDropoffStation.x = 0;
        enterDropoffStation.y = 90;
        exitDropoffStation.id = "Exit dropoff";
        exitDropoffStation.name = new NonLocalizedString("Exit dropoff station");
        exitDropoffStation.x = 0;
        exitDropoffStation.y = 90;

        // Vertices for legs 5 and 6
        BikeRentalStationVertex v44 = new BikeRentalStationVertex(
                graph, enterPickupStation);
        BikeRentalStationVertex v46 = new BikeRentalStationVertex(
                graph, exitPickupStation);
        IntersectionVertex v48 = new IntersectionVertex(
                graph, "Vertex 48", 180, 90);
        IntersectionVertex v50 = new IntersectionVertex(
                graph, "Vertex 50", 90, 90);

        // Vertices for leg 7
        IntersectionVertex v52 = new IntersectionVertex(
                graph, "Vertex 52", 90, 90);
        IntersectionVertex v54 = new IntersectionVertex(
                graph, "Vertex 54", 0, 90);

        // Vertices for legs 7 and 8
        BikeRentalStationVertex v56 = new BikeRentalStationVertex(
                graph, enterDropoffStation);
        BikeRentalStationVertex v58 = new BikeRentalStationVertex(
                graph, exitDropoffStation);
        StreetLocation v60 = new StreetLocation("Vertex 60", new Coordinate(0, 90), "Vertex 60");

        // Vertex initialization that can't be done using the constructor
        v0.setExitName("Ausfahrt");
        v2.freeFlowing = (true);
        v4.freeFlowing = (true);
        v38.freeFlowing = (true);
        v40.freeFlowing = (true);
        v42.freeFlowing = (true);
        v48.freeFlowing = (true);
        v50.freeFlowing = (true);
        v52.freeFlowing = (true);
        v54.freeFlowing = (true);

        // Elevation profiles for the street edges that will be created later
        PackedCoordinateSequence elevation3 = new PackedCoordinateSequence.Double(
                new double[]{0.0, 0.0, 3.0, 9.9}, 2);
        PackedCoordinateSequence elevation39 = new PackedCoordinateSequence.Double(
                new double[]{0.0, 9.9, 2.1, 0.1}, 2);
        PackedCoordinateSequence elevation41 = new PackedCoordinateSequence.Double(
                new double[]{0.0, 0.1, 1.9, 2.8}, 2);
        PackedCoordinateSequence elevation49 = new PackedCoordinateSequence.Double(
                new double[]{0.0, 2.8, 2.0, 2.6}, 2);
        PackedCoordinateSequence elevation53 = new PackedCoordinateSequence.Double(
                new double[]{0.0, 2.6, 1.0, 6.0}, 2);

        // Coordinate sequences and line strings for those same edges
        PackedCoordinateSequence coordinates3 = new PackedCoordinateSequence.Double(
                new double[]{0, 0, 1, 1}, 2);
        PackedCoordinateSequence coordinates25 = new PackedCoordinateSequence.Double(
                new double[]{133, 67, 135, 67}, 2);
        PackedCoordinateSequence coordinates39 = new PackedCoordinateSequence.Double(
                new double[]{179, 89, 180, 89}, 2);
        PackedCoordinateSequence coordinates41 = new PackedCoordinateSequence.Double(
                new double[]{180, 89, 180, 90}, 2);
        PackedCoordinateSequence coordinates49 = new PackedCoordinateSequence.Double(
                new double[]{180, 90, 90, 90}, 2);
        PackedCoordinateSequence coordinates53 = new PackedCoordinateSequence.Double(
                new double[]{90, 90, 0, 90}, 2);

        GeometryFactory geometryFactory = new GeometryFactory();

        LineString l3 = new LineString(coordinates3, geometryFactory);
        LineString l25 = new LineString(coordinates25, geometryFactory);
        LineString l39 = new LineString(coordinates39, geometryFactory);
        LineString l41 = new LineString(coordinates41, geometryFactory);
        LineString l49 = new LineString(coordinates49, geometryFactory);
        LineString l53 = new LineString(coordinates53, geometryFactory);

        // Edges for leg 0
        FreeEdge e1 = new FreeEdge(
                v0, v2);
        StreetWithElevationEdge e3 = new StreetWithElevationEdge(
                v2, v4, l3, "Edge 3", 3.0, StreetTraversalPermission.ALL, false);

        // Edges for legs 1 and 2
        StreetTransitLink e5 = new StreetTransitLink(
                v4, v6, false);
        PreBoardEdge e7 = new PreBoardEdge(
                v6, v8);
        TransitBoardAlight e9 = new TransitBoardAlight(
                v8, v10, 0, TraverseMode.RAIL);
        PatternHop e11 = new PatternHop(
                v10, v12, trainStopDepart, trainStopDwell, 0);
        PatternDwell e13 = new PatternDwell(
                v12, v14, 1, firstTripPattern);
        PatternHop e15 = new PatternHop(
                v14, v16, trainStopDwell, trainStopInterline, 1);
        PatternInterlineDwell e17 = new PatternInterlineDwell(
                v16, v18);
        PatternHop e19 = new PatternHop(
                v18, v20, trainStopInterline, trainStopArrive, 0);
        TransitBoardAlight e21 = new TransitBoardAlight(
                v20, v22, 1, TraverseMode.RAIL);
        PreAlightEdge e23 = new PreAlightEdge(
                v22, v24);

        // Edges for legs 3 and 4
        SimpleTransfer e25 = new SimpleTransfer(
                v24, v26, 7, l25);
        PreBoardEdge e27 = new PreBoardEdge(
                v26, v28);
        TransitBoardAlight e29 = new TransitBoardAlight(
                v28, v30, 0, TraverseMode.FERRY);
        PatternHop e31 = new PatternHop(
                v30, v32, ferryStopDepart, ferryStopArrive, 0);
        TransitBoardAlight e33 = new TransitBoardAlight(
                v32, v34, 1, TraverseMode.FERRY);
        PreAlightEdge e35 = new PreAlightEdge(
                v34, v36);
        StreetTransitLink e37 = new StreetTransitLink(
                v36, v38, true);

        // Edges for legs 5 and 6, where edges 39 and 41 have the same name to trigger stayOn = true
        AreaEdge e39 = new AreaEdge(
                v38, v40, l39, "Edge 39 / 41", 2.1, StreetTraversalPermission.ALL, false,
                new AreaEdgeList());
        StreetWithElevationEdge e41 = new StreetWithElevationEdge(
                v40, v42, l41, "Edge 39 / 41", 1.9, StreetTraversalPermission.ALL, false);
        StreetBikeRentalLink e43 = new StreetBikeRentalLink(
                v42, v44);
        RentABikeOnEdge e45 = new RentABikeOnEdge(
                v44, v46, Collections.singleton(""));
        StreetBikeRentalLink e47 = new StreetBikeRentalLink(
                v46, v48);
        StreetWithElevationEdge e49 = new StreetWithElevationEdge(
                v48, v50, l49, "Edge 49", 2.0, StreetTraversalPermission.ALL, false);

        // Edges for legs 6, 7 and 8
        LegSwitchingEdge e51 = new LegSwitchingEdge(
                v50, v52);
        StreetEdge e53p = new StreetEdge(v52, v54, l53, "Edge 53", 1.0,
                StreetTraversalPermission.ALL, false);
        PartialStreetEdge e53 = new PartialStreetEdge(e53p, v52, v54, l53, "Edge 53", 1.0);
        StreetBikeRentalLink e55 = new StreetBikeRentalLink(
                v54, v56);
        RentABikeOffEdge e57 = new RentABikeOffEdge(
                v56, v58, Collections.singleton(""));
        StreetBikeRentalLink e59 = new StreetBikeRentalLink(
                v58, v60);

        // Alert for testing GTFS-RT
        AlertPatch alertPatch = new AlertPatch();

        alertPatch.setTimePeriods(Collections.singletonList(new TimePeriod(0, Long.MAX_VALUE)));
        alertPatch.setAlert(Alert.createSimpleAlerts(alertsExample));

        // Edge initialization that can't be done using the constructor
        e3.setElevationProfile(elevation3, false);
        e17.add(firstTrip, secondTrip);
        e39.setElevationProfile(elevation39, false);
        e41.setElevationProfile(elevation41, false);
        e41.setHasBogusName(true);
        e49.setElevationProfile(elevation49, false);
        e53.setElevationProfile(elevation53, false);
        graph.streetNotesService.addStaticNote(e53p, Alert.createSimpleAlerts(alertsExample),
                StreetNotesService.ALWAYS_MATCHER);

        // Add an extra edge to the graph in order to generate stayOn = true for one walk step.
        new StreetEdge(v40,
                new IntersectionVertex(graph, "Extra vertex", 180, 88),
                new LineString(new PackedCoordinateSequence.Double(
                        new double[]{180, 89, 180, 88}, 2), geometryFactory),
                "Extra edge", 1.9, StreetTraversalPermission.NONE, true);

        // Various bookkeeping operations
        graph.serviceCodes.put(firstTrip.getId(), 0);
        graph.serviceCodes.put(secondTrip.getId(), 1);
        graph.serviceCodes.put(thirdTrip.getId(), 2);

        firstTripTimes.serviceCode = graph.serviceCodes.get(firstTrip.getId());
        secondTripTimes.serviceCode = graph.serviceCodes.get(secondTrip.getId());
        thirdTripTimes.serviceCode = graph.serviceCodes.get(thirdTrip.getId());

        CalendarServiceData calendarServiceData = new CalendarServiceDataStub(graph.serviceCodes.keySet());
        CalendarServiceImpl calendarServiceImpl = new CalendarServiceImpl(calendarServiceData);

        calendarServiceData.putTimeZoneForAgencyId(feedId, timeZone);
        calendarServiceData.putTimeZoneForAgencyId(feedId, timeZone);

        FareServiceStub fareServiceStub = new FareServiceStub();

        ServiceDate serviceDate = new ServiceDate(1970, 1, 1);

        // Updates for leg 4, the ferry leg
        TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

        tripDescriptorBuilder.setTripId("C");

        StopTimeEvent.Builder ferryStopDepartTimeEventBuilder = StopTimeEvent.newBuilder();
        StopTimeEvent.Builder ferryStopArriveTimeEventBuilder = StopTimeEvent.newBuilder();

        ferryStopDepartTimeEventBuilder.setTime(40L);
        ferryStopArriveTimeEventBuilder.setTime(43L);

        StopTimeUpdate.Builder ferryStopDepartUpdateBuilder = StopTimeUpdate.newBuilder();
        StopTimeUpdate.Builder ferryStopArriveUpdateBuilder = StopTimeUpdate.newBuilder();

        ferryStopDepartUpdateBuilder.setStopSequence(-1);
        ferryStopDepartUpdateBuilder.setDeparture(ferryStopDepartTimeEventBuilder);
        ferryStopDepartUpdateBuilder.setArrival(ferryStopDepartTimeEventBuilder);
        ferryStopDepartUpdateBuilder.setScheduleRelationship(ScheduleRelationship.SCHEDULED);
        ferryStopArriveUpdateBuilder.setStopSequence(0);
        ferryStopArriveUpdateBuilder.setDeparture(ferryStopArriveTimeEventBuilder);
        ferryStopArriveUpdateBuilder.setArrival(ferryStopArriveTimeEventBuilder);
        ferryStopArriveUpdateBuilder.setScheduleRelationship(ScheduleRelationship.SCHEDULED);

        TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

        tripUpdateBuilder.setTrip(tripDescriptorBuilder);
        tripUpdateBuilder.addStopTimeUpdate(0, ferryStopDepartUpdateBuilder);
        tripUpdateBuilder.addStopTimeUpdate(1, ferryStopArriveUpdateBuilder);

        TripUpdate tripUpdate = tripUpdateBuilder.build();

        // Create dummy TimetableSnapshot
        TimetableSnapshot snapshot = new TimetableSnapshot();
        
        // Mock TimetableSnapshotSource to return dummy TimetableSnapshot
        TimetableSnapshotSource timetableSnapshotSource = mock(TimetableSnapshotSource.class);

        when(timetableSnapshotSource.getTimetableSnapshot()).thenReturn(snapshot);

        TripTimes updatedTripTimes = thirdTripPattern.scheduledTimetable.createUpdatedTripTimes(tripUpdate,
                timeZone, serviceDate);
        timetableSnapshotSource.getTimetableSnapshot().update(feedId, thirdTripPattern, updatedTripTimes, serviceDate);

        // Further graph initialization

        graph.putService(CalendarServiceData.class, calendarServiceData);
        graph.putService(FareService.class, fareServiceStub);
        graph.addAgency(feedId, trainAgency);
        graph.addAgency(feedId, ferryAgency);
        graph.timetableSnapshotSource = (timetableSnapshotSource);
        graph.addAlertPatch(e29, alertPatch);

        // Routing context creation and initialization
        ServiceDay serviceDay = new ServiceDay(graph, 0, calendarServiceImpl, feedId);

        // Temporary graph objects for onboard depart tests
        OnboardDepartVertex onboardDepartVertex = new OnboardDepartVertex("Onboard", 23.0, 12.0);
        OnBoardDepartPatternHop onBoardDepartPatternHop = new OnBoardDepartPatternHop(
                onboardDepartVertex, v12, firstTripPattern.scheduledTimetable.getTripTimes(0), serviceDay, 0, 0.5);

        // Traverse the path forward first
        RoutingRequest forwardOptions = options.clone();
        RoutingContext forwardContext = new RoutingContext(forwardOptions, graph, v0, v60);

        forwardContext.serviceDays = new ArrayList<ServiceDay>(1);
        forwardContext.serviceDays.add(serviceDay);

        forwardOptions.rctx = forwardContext;
        forwardOptions.dateTime = 0L;
        forwardOptions.bikeRentalPickupTime = 4;
        forwardOptions.bikeRentalDropoffTime = 2;

        // Forward traversal of all edges
        State s0Forward = new State(forwardOptions);
        State s2Forward = e1.traverse(s0Forward);
        State s4Forward = e3.traverse(s2Forward);
        State s6Forward = e5.traverse(s4Forward);
        State s8Forward = e7.traverse(s6Forward);
        State s10Forward = e9.traverse(s8Forward);
        State s12Forward = e11.traverse(s10Forward);
        State s14Forward = e13.traverse(s12Forward);
        State s16Forward = e15.traverse(s14Forward);
        State s18Forward = e17.traverse(s16Forward);
        State s20Forward = e19.traverse(s18Forward);
        State s22Forward = e21.traverse(s20Forward);
        State s24Forward = e23.traverse(s22Forward);
        State s26Forward = e25.traverse(s24Forward);
        State s28Forward = e27.traverse(s26Forward);
        State s30Forward = e29.traverse(s28Forward);
        State s32Forward = e31.traverse(s30Forward);
        State s34Forward = e33.traverse(s32Forward);
        State s36Forward = e35.traverse(s34Forward);
        State s38Forward = e37.traverse(s36Forward);
        State s40Forward = e39.traverse(s38Forward);
        State s42Forward = e41.traverse(s40Forward);
        State s44Forward = e43.traverse(s42Forward);
        State s46Forward = e45.traverse(s44Forward);
        State s48Forward = e47.traverse(s46Forward);
        State s50Forward = e49.traverse(s48Forward);
        State s52Forward = e51.traverse(s50Forward);
        State s54Forward = e53.traverse(s52Forward);
        State s56Forward = e55.traverse(s54Forward);
        State s58Forward = e57.traverse(s56Forward);
        State s60Forward = e59.traverse(s58Forward);

        // Also traverse the path backward
        RoutingRequest backwardOptions = options.clone();
        RoutingContext backwardContext = new RoutingContext(backwardOptions, graph, v60, v0);

        backwardContext.serviceDays = new ArrayList<ServiceDay>(1);
        backwardContext.serviceDays.add(serviceDay);

        backwardOptions.rctx = backwardContext;
        backwardOptions.dateTime = 60L;
        backwardOptions.bikeRentalPickupTime = 4;
        backwardOptions.bikeRentalDropoffTime = 2;
        backwardOptions.setArriveBy(true);

        // Backward traversal of all edges
        State s60Backward = new State(backwardOptions);
        State s58Backward = e59.traverse(s60Backward);
        State s56Backward = e57.traverse(s58Backward);
        State s54Backward = e55.traverse(s56Backward);
        State s52Backward = e53.traverse(s54Backward);
        State s50Backward = e51.traverse(s52Backward);
        State s48Backward = e49.traverse(s50Backward);
        State s46Backward = e47.traverse(s48Backward);
        State s44Backward = e45.traverse(s46Backward);
        State s42Backward = e43.traverse(s44Backward);
        State s40Backward = e41.traverse(s42Backward);
        State s38Backward = e39.traverse(s40Backward);
        State s36Backward = e37.traverse(s38Backward);
        State s34Backward = e35.traverse(s36Backward);
        State s32Backward = e33.traverse(s34Backward);
        State s30Backward = e31.traverse(s32Backward);
        State s28Backward = e29.traverse(s30Backward);
        State s26Backward = e27.traverse(s28Backward);
        State s24Backward = e25.traverse(s26Backward);
        State s22Backward = e23.traverse(s24Backward);
        State s20Backward = e21.traverse(s22Backward);
        State s18Backward = e19.traverse(s20Backward);
        State s16Backward = e17.traverse(s18Backward);
        State s14Backward = e15.traverse(s16Backward);
        State s12Backward = e13.traverse(s14Backward);
        State s10Backward = e11.traverse(s12Backward);
        State s8Backward = e9.traverse(s10Backward);
        State s6Backward = e7.traverse(s8Backward);
        State s4Backward = e5.traverse(s6Backward);
        State s2Backward = e3.traverse(s4Backward);
        State s0Backward = e1.traverse(s2Backward);

        // Perform a forward traversal starting onboard
        RoutingRequest onboardOptions = options.clone();
        RoutingContext onboardContext = new RoutingContext(onboardOptions, graph,
                onboardDepartVertex, v60);

        onboardContext.serviceDays = new ArrayList<ServiceDay>(1);
        onboardContext.serviceDays.add(serviceDay);

        onboardOptions.rctx = onboardContext;
        onboardOptions.dateTime = 6L;
        onboardOptions.bikeRentalPickupTime = 4;
        onboardOptions.bikeRentalDropoffTime = 2;

        // Onboard traversal of all edges
        State s10Onboard = new State(onboardOptions);
        State s12Onboard = onBoardDepartPatternHop.traverse(s10Onboard);
        State s14Onboard = e13.traverse(s12Onboard);
        State s16Onboard = e15.traverse(s14Onboard);
        State s18Onboard = e17.traverse(s16Onboard);
        State s20Onboard = e19.traverse(s18Onboard);
        State s22Onboard = e21.traverse(s20Onboard);
        State s24Onboard = e23.traverse(s22Onboard);
        State s26Onboard = e25.traverse(s24Onboard);
        State s28Onboard = e27.traverse(s26Onboard);
        State s30Onboard = e29.traverse(s28Onboard);
        State s32Onboard = e31.traverse(s30Onboard);
        State s34Onboard = e33.traverse(s32Onboard);
        State s36Onboard = e35.traverse(s34Onboard);
        State s38Onboard = e37.traverse(s36Onboard);
        State s40Onboard = e39.traverse(s38Onboard);
        State s42Onboard = e41.traverse(s40Onboard);
        State s44Onboard = e43.traverse(s42Onboard);
        State s46Onboard = e45.traverse(s44Onboard);
        State s48Onboard = e47.traverse(s46Onboard);
        State s50Onboard = e49.traverse(s48Onboard);
        State s52Onboard = e51.traverse(s50Onboard);
        State s54Onboard = e53.traverse(s52Onboard);
        State s56Onboard = e55.traverse(s54Onboard);
        State s58Onboard = e57.traverse(s56Onboard);
        State s60Onboard = e59.traverse(s58Onboard);

        return new GraphPath[] {new GraphPath(s60Forward, false),
                new GraphPath(s0Backward, false), new GraphPath(s60Onboard, false)};
    }

    /**
     * This method compares the itinerary's fields to their expected values. The actual work is
     * delegated to other methods. This method just calls them with the right arguments.
     */
    private void compare(Itinerary itinerary, Type type) {
        compareItinerary(itinerary, type);

        compareFare(itinerary.fare);

        Leg[] legs = itinerary.legs.toArray(new Leg[9]);
        if (type == Type.ONBOARD) {
            legs[8] = legs[7];
            legs[7] = legs[6];
            legs[6] = legs[5];
            legs[5] = legs[4];
            legs[4] = legs[3];
            legs[3] = legs[2];
            legs[2] = legs[1];
            legs[1] = legs[0];
            legs[0] = null;
        }
        compareLegs(legs, type);

        WalkStep[][] steps = new WalkStep[9][0];
        for (int i = 0; i < steps.length; i++) {
            if (legs[i] == null) continue;
            steps[i] = legs[i].walkSteps.toArray(steps[i]);
        }
        compareSteps(steps, type);

        EncodedPolylineBean[] geometries = new EncodedPolylineBean[9];
        for (int i = 0; i < geometries.length; i++) {
            if (legs[i] == null) continue;
            geometries[i] = legs[i].legGeometry;
        }
        compareGeometries(geometries, type);

        // Java's multidimensional arrays are actually arrays of arrays, meaning they can be jagged.
        Place[][] places = new Place[9][2];
        for (int i = 0; i < places.length; i++) {
            if (legs[i] == null) continue;
            if (legs[i].stop == null) {
                places[i][0] = legs[i].from;
                places[i][1] = legs[i].to;
            } else {
                List<Place> allStops = new ArrayList<Place>(legs[i].stop.size() + 2);
                allStops.add(legs[i].from);
                allStops.addAll(legs[i].stop);
                allStops.add(legs[i].to);
                places[i] = allStops.toArray(places[i]);
            }
        }
        comparePlaces(places, type);

        AgencyAndId[][] stopIds = new AgencyAndId[9][2];
        for (int i = 0; i < stopIds.length; i++) {
            if (places[i].length > 2) {
                stopIds[i] = new AgencyAndId[places[i].length];
            }
            for (int j = 0; j < stopIds[i].length; j++) {
                if (places[i][j] == null) continue;
                stopIds[i][j] = places[i][j].stopId;
            }
        }
        compareStopIds(stopIds, type);

        /*
         * This four-dimensional array is indexed as follows:
         *
         * [X][ ][ ][ ] The leg number
         * [ ][X][ ][ ] The walk step number
         * [ ][ ][X][ ] 0 for the start of the walk step, 1 for the end
         * [ ][ ][ ][X] 0 for the distance traveled, 1 for the actual elevation
         *
         * Although technically, this particular array is not jagged, some of its elements are null.
         */
        Double[][][][] elevations = new Double[9][2][2][2];
        for (int i = 0; i < elevations.length; i++) {
            for (int j = 0; j < elevations[i].length; j++) {
                if (steps[i].length <= j) break;
                for (int k = 0; k < elevations[i][j].length; k++) {
                    if (steps[i][j].elevation.size() <= k) break;
                    elevations[i][j][k][0] = steps[i][j].elevation.get(k).first;
                    elevations[i][j][k][1] = steps[i][j].elevation.get(k).second;
                }
            }
        }
        compareElevations(elevations, type);
    }

    /** Compare all simple itinerary fields to their expected values. */
    private void compareItinerary(Itinerary itinerary, Type type) {
        if (type == Type.FORWARD || type == Type.BACKWARD) {
            assertEquals(60.0, itinerary.duration.doubleValue(), 0.0);
            assertEquals(0L, itinerary.startTime.getTimeInMillis());
        } else if (type == Type.ONBOARD) {
            assertEquals(54.0, itinerary.duration.doubleValue(), 0.0);
            assertEquals(6000L, itinerary.startTime.getTimeInMillis());
        }
        assertEquals(60000L, itinerary.endTime.getTimeInMillis());

        if (type == Type.FORWARD || type == Type.BACKWARD) {
            assertEquals(27L, itinerary.walkTime);
            assertEquals(23L, itinerary.transitTime);
            assertEquals(10L, itinerary.waitingTime);
        } else if (type == Type.ONBOARD) {
            assertEquals(24L, itinerary.walkTime);
            assertEquals(21L, itinerary.transitTime);
            assertEquals(9L, itinerary.waitingTime);
        }

        if (type == Type.FORWARD || type == Type.BACKWARD) {
            assertEquals(17.0, itinerary.walkDistance, 0.0);
        } else if (type == Type.ONBOARD) {
            assertEquals(14.0, itinerary.walkDistance, 0.0);
        }

        assertFalse(itinerary.walkLimitExceeded);

        assertEquals(10.0, itinerary.elevationLost, 0.0);
        if (type == Type.FORWARD || type == Type.BACKWARD) {
            assertEquals(16.0, itinerary.elevationGained, 0.0);
        } else if (type == Type.ONBOARD) {
            assertEquals(6.1, itinerary.elevationGained, 0.0);
        }

        assertEquals(1, itinerary.transfers.intValue());

        assertFalse(itinerary.tooSloped);
    }

    /** Compare the computed fare to its expected value. */
    private void compareFare(Fare fare) {
        assertEquals(0, fare.getFare(FareType.regular).getCents());
        assertEquals(1, fare.getFare(FareType.student).getCents());
        assertEquals(2, fare.getFare(FareType.senior).getCents());
        assertEquals(4, fare.getFare(FareType.tram).getCents());
        assertEquals(8, fare.getFare(FareType.special).getCents());
    }

    /** Compare all simple leg fields to their expected values, leg by leg. */
    private void compareLegs(Leg[] legs, Type type) {
        assertEquals(9, legs.length);

        if (type == Type.FORWARD || type == Type.BACKWARD) {
            assertNull(legs[0].agencyId);
            assertNull(legs[0].agencyName);
            assertNull(legs[0].agencyUrl);
            assertEquals(2, legs[0].agencyTimeZoneOffset);
            assertNull(legs[0].alerts);
            assertEquals("", legs[0].route);
            assertNull(legs[0].routeId);
            assertNull(legs[0].routeShortName);
            assertNull(legs[0].routeLongName);
            assertNull(legs[0].routeType);
            assertNull(legs[0].routeColor);
            assertNull(legs[0].routeTextColor);
            assertNull(legs[0].tripId);
            assertNull(legs[0].tripShortName);
            assertNull(legs[0].tripBlockId);
            assertNull(legs[0].serviceDate);
            assertNull(legs[0].headsign);
            assertFalse(legs[0].rentedBike);
            assertFalse(legs[0].isTransitLeg());
            assertFalse(legs[0].interlineWithPreviousLeg);
            assertNull(legs[0].boardRule);
            assertNull(legs[0].alightRule);
            assertFalse(legs[0].pathway);
            assertEquals("WALK", legs[0].mode);
            assertEquals(0L, legs[0].startTime.getTimeInMillis());
            assertEquals(3000L, legs[0].endTime.getTimeInMillis());
            assertEquals(3.0, legs[0].getDuration(), 0.0);
            assertEquals(0, legs[0].departureDelay);
            assertEquals(0, legs[0].arrivalDelay);
            assertFalse(legs[0].realTime);
            assertNull(legs[0].isNonExactFrequency);
            assertNull(legs[0].headway);
            assertEquals(F_DISTANCE[0], legs[0].distance, 0.0);
        } else if (type == Type.ONBOARD) {
            assertNull(legs[0]);
        }

        assertEquals("Train", legs[1].agencyId);
        assertEquals("John Train", legs[1].agencyName);
        assertEquals("http://www.train.org/", legs[1].agencyUrl);
        assertEquals(2, legs[1].agencyTimeZoneOffset);
        assertNull(legs[1].alerts);
        assertEquals("A", legs[1].route);
        assertEquals("A", legs[1].routeId.getId());
        assertEquals("A", legs[1].routeShortName);
        assertEquals("'A' Train", legs[1].routeLongName);
        assertEquals(2, legs[1].routeType.intValue());
        assertEquals("White", legs[1].routeColor);
        assertEquals("Black", legs[1].routeTextColor);
        assertEquals("A", legs[1].tripId.getId());
        assertEquals("A", legs[1].tripShortName);
        assertEquals("Alock", legs[1].tripBlockId);
        assertEquals("Street Fighting Man", legs[1].headsign);
        assertEquals("19700101", legs[1].serviceDate);
        assertFalse(legs[1].rentedBike);
        assertTrue(legs[1].isTransitLeg());
        assertFalse(legs[1].interlineWithPreviousLeg);
        if (type == Type.FORWARD || type == Type.BACKWARD) {
            assertEquals("coordinateWithDriver", legs[1].boardRule);
        } else if (type == Type.ONBOARD) {
            assertNull(legs[1].boardRule);
        }
        assertNull(legs[1].alightRule);
        assertFalse(legs[1].pathway);
        assertEquals("RAIL", legs[1].mode);
        if (type == Type.FORWARD || type == Type.BACKWARD) {
            assertEquals(4000L, legs[1].startTime.getTimeInMillis());
            assertEquals(12.0, legs[1].getDuration(), 0.0);
        } else if (type == Type.ONBOARD) {
            assertEquals(6000L, legs[1].startTime.getTimeInMillis());
            assertEquals(10.0, legs[1].getDuration(), 0.0);
        }
        assertEquals(16000L, legs[1].endTime.getTimeInMillis());
        assertEquals(0, legs[1].departureDelay);
        assertEquals(0, legs[1].arrivalDelay);
        assertFalse(legs[1].realTime);
        assertNull(legs[1].isNonExactFrequency);
        assertNull(legs[1].headway);
        if (type == Type.FORWARD || type == Type.BACKWARD) {
            assertEquals(F_DISTANCE[1], legs[1].distance, EPSILON);
        } else if (type == Type.ONBOARD) {
            assertEquals(O_DISTANCE, legs[1].distance, EPSILON);
        }

        assertEquals("Train", legs[2].agencyId);
        assertEquals("John Train", legs[2].agencyName);
        assertEquals("http://www.train.org/", legs[2].agencyUrl);
        assertEquals(2, legs[2].agencyTimeZoneOffset);
        assertNull(legs[2].alerts);
        assertEquals("B", legs[2].route);
        assertEquals("B", legs[2].routeId.getId());
        assertEquals("B", legs[2].routeShortName);
        assertEquals("Another Train", legs[2].routeLongName);
        assertEquals(2, legs[2].routeType.intValue());
        assertEquals("Cyan", legs[2].routeColor);
        assertEquals("Yellow", legs[2].routeTextColor);
        assertEquals("B", legs[2].tripId.getId());
        assertEquals("B", legs[2].tripShortName);
        assertEquals("Block", legs[2].tripBlockId);
        assertEquals("No Expectations", legs[2].headsign);
        assertEquals("19700101", legs[2].serviceDate);
        assertFalse(legs[2].rentedBike);
        assertTrue(legs[2].isTransitLeg());
        assertTrue(legs[2].interlineWithPreviousLeg);
        assertNull(legs[2].boardRule);
        assertEquals("mustPhone", legs[2].alightRule);
        assertFalse(legs[2].pathway);
        assertEquals("RAIL", legs[2].mode);
        assertEquals(20000L, legs[2].startTime.getTimeInMillis());
        assertEquals(24000L, legs[2].endTime.getTimeInMillis());
        assertEquals(4.0, legs[2].getDuration(), 0.0);
        assertEquals(0, legs[2].departureDelay);
        assertEquals(0, legs[2].arrivalDelay);
        assertFalse(legs[2].realTime);
        assertNull(legs[2].isNonExactFrequency);
        assertNull(legs[2].headway);
        assertEquals(F_DISTANCE[2], legs[2].distance, EPSILON);

        assertNull(legs[3].agencyId);
        assertNull(legs[3].agencyName);
        assertNull(legs[3].agencyUrl);
        assertEquals(2, legs[3].agencyTimeZoneOffset);
        assertNull(legs[3].alerts);
        assertEquals("", legs[3].route);
        assertNull(legs[3].routeId);
        assertNull(legs[3].routeShortName);
        assertNull(legs[3].routeLongName);
        assertNull(legs[3].routeType);
        assertNull(legs[3].routeColor);
        assertNull(legs[3].routeTextColor);
        assertNull(legs[3].tripId);
        assertNull(legs[3].tripShortName);
        assertNull(legs[3].tripBlockId);
        assertNull(legs[3].headsign);
        assertNull(legs[3].serviceDate);
        assertFalse(legs[3].rentedBike);
        assertFalse(legs[3].isTransitLeg());
        assertFalse(legs[3].interlineWithPreviousLeg);
        assertNull(legs[3].boardRule);
        assertNull(legs[3].alightRule);
        assertFalse(legs[3].pathway);
        assertEquals("WALK", legs[3].mode);
        if (type == Type.FORWARD || type == Type.ONBOARD) {
            assertEquals(24000L, legs[3].startTime.getTimeInMillis());
            assertEquals(32000L, legs[3].endTime.getTimeInMillis());
        } else if (type == Type.BACKWARD) {
            assertEquals(32000L, legs[3].startTime.getTimeInMillis());
            assertEquals(40000L, legs[3].endTime.getTimeInMillis());
        }
        assertEquals(8.0, legs[3].getDuration(), 0.0);
        assertEquals(0, legs[3].departureDelay);
        assertEquals(0, legs[3].arrivalDelay);
        assertFalse(legs[3].realTime);
        assertNull(legs[3].isNonExactFrequency);
        assertNull(legs[3].headway);
        assertEquals(F_DISTANCE[3], legs[3].distance, 0.0);

        assertEquals("Ferry", legs[4].agencyId);
        assertEquals("Brian Ferry", legs[4].agencyName);
        assertEquals("http://www.ferry.org/", legs[4].agencyUrl);
        assertEquals(2, legs[4].agencyTimeZoneOffset);
        assertEquals(1, legs[4].alerts.size());
        assertEquals(Alert.createSimpleAlerts(alertsExample), legs[4].alerts.get(0).alert);
        assertEquals("C", legs[4].route);
        assertEquals("C", legs[4].routeId.getId());
        assertEquals("C", legs[4].routeShortName);
        assertEquals("Ferry Cross the Mersey", legs[4].routeLongName);
        assertEquals(4, legs[4].routeType.intValue());
        assertEquals("Black", legs[4].routeColor);
        assertEquals("White", legs[4].routeTextColor);
        assertEquals("C", legs[4].tripId.getId());
        assertEquals("C", legs[4].tripShortName);
        assertEquals("Clock", legs[4].tripBlockId);
        assertEquals("Handsome Molly", legs[4].headsign);
        assertEquals("19700101", legs[4].serviceDate);
        assertFalse(legs[4].rentedBike);
        assertTrue(legs[4].isTransitLeg());
        assertFalse(legs[4].interlineWithPreviousLeg);
        assertEquals("mustPhone", legs[4].boardRule);
        assertEquals("coordinateWithDriver", legs[4].alightRule);
        assertFalse(legs[4].pathway);
        assertEquals("FERRY", legs[4].mode);
        assertEquals(40000L, legs[4].startTime.getTimeInMillis());
        assertEquals(43000L, legs[4].endTime.getTimeInMillis());
        assertEquals(3.0, legs[4].getDuration(), 0.0);
        assertEquals(8, legs[4].departureDelay);
        assertEquals(7, legs[4].arrivalDelay);
        assertTrue(legs[4].realTime);
        assertNull(legs[4].isNonExactFrequency);
        assertNull(legs[4].headway);
        assertEquals(F_DISTANCE[4], legs[4].distance, EPSILON);

        assertNull(legs[5].agencyId);
        assertNull(legs[5].agencyName);
        assertNull(legs[5].agencyUrl);
        assertEquals(2, legs[5].agencyTimeZoneOffset);
        assertNull(legs[5].alerts);
        assertEquals("", legs[5].route);
        assertNull(legs[5].routeId);
        assertNull(legs[5].routeShortName);
        assertNull(legs[5].routeLongName);
        assertNull(legs[5].routeType);
        assertNull(legs[5].routeColor);
        assertNull(legs[5].routeTextColor);
        assertNull(legs[5].tripId);
        assertNull(legs[5].tripShortName);
        assertNull(legs[5].tripBlockId);
        assertNull(legs[5].headsign);
        assertNull(legs[5].serviceDate);
        assertFalse(legs[5].rentedBike);
        assertFalse(legs[5].isTransitLeg());
        assertFalse(legs[5].interlineWithPreviousLeg);
        assertNull(legs[5].boardRule);
        assertNull(legs[5].alightRule);
        assertFalse(legs[5].pathway);
        assertEquals("WALK", legs[5].mode);
        assertEquals(44000L, legs[5].startTime.getTimeInMillis());
        assertEquals(53000L, legs[5].endTime.getTimeInMillis());
        assertEquals(9.0, legs[5].getDuration(), 0.0);
        assertEquals(0, legs[5].departureDelay);
        assertEquals(0, legs[5].arrivalDelay);
        assertFalse(legs[5].realTime);
        assertNull(legs[5].isNonExactFrequency);
        assertNull(legs[5].headway);
        assertEquals(F_DISTANCE[5], legs[5].distance, 0.0);

        assertNull(legs[6].agencyId);
        assertNull(legs[6].agencyName);
        assertNull(legs[6].agencyUrl);
        assertEquals(2, legs[6].agencyTimeZoneOffset);
        assertNull(legs[6].alerts);
        assertEquals("", legs[6].route);
        assertNull(legs[6].routeId);
        assertNull(legs[6].routeShortName);
        assertNull(legs[6].routeLongName);
        assertNull(legs[6].routeType);
        assertNull(legs[6].routeColor);
        assertNull(legs[6].routeTextColor);
        assertNull(legs[6].tripId);
        assertNull(legs[6].tripShortName);
        assertNull(legs[6].tripBlockId);
        assertNull(legs[6].headsign);
        assertNull(legs[6].serviceDate);
        assertTrue(legs[6].rentedBike);
        assertFalse(legs[6].isTransitLeg());
        assertFalse(legs[6].interlineWithPreviousLeg);
        assertNull(legs[6].boardRule);
        assertNull(legs[6].alightRule);
        assertFalse(legs[6].pathway);
        assertEquals("BICYCLE", legs[6].mode);
        assertEquals(53000L, legs[6].startTime.getTimeInMillis());
        assertEquals(55000L, legs[6].endTime.getTimeInMillis());
        assertEquals(2.0, legs[6].getDuration(), 0.0);
        assertEquals(0, legs[6].departureDelay);
        assertEquals(0, legs[6].arrivalDelay);
        assertFalse(legs[6].realTime);
        assertNull(legs[6].isNonExactFrequency);
        assertNull(legs[6].headway);
        assertEquals(F_DISTANCE[6], legs[6].distance, 0.0);

        assertNull(legs[7].agencyId);
        assertNull(legs[7].agencyName);
        assertNull(legs[7].agencyUrl);
        assertEquals(2, legs[7].agencyTimeZoneOffset);
        assertEquals(1, legs[7].alerts.size());
        assertEquals(Alert.createSimpleAlerts(alertsExample), legs[7].alerts.get(0).alert);
        assertEquals("", legs[7].route);
        assertNull(legs[7].routeId);
        assertNull(legs[7].routeShortName);
        assertNull(legs[7].routeLongName);
        assertNull(legs[7].routeType);
        assertNull(legs[7].routeColor);
        assertNull(legs[7].routeTextColor);
        assertNull(legs[7].tripId);
        assertNull(legs[7].tripShortName);
        assertNull(legs[7].tripBlockId);
        assertNull(legs[7].headsign);
        assertNull(legs[7].serviceDate);
        assertTrue(legs[7].rentedBike);
        assertFalse(legs[7].isTransitLeg());
        assertFalse(legs[7].interlineWithPreviousLeg);
        assertNull(legs[7].boardRule);
        assertNull(legs[7].alightRule);
        assertFalse(legs[7].pathway);
        assertEquals("BICYCLE", legs[7].mode);
        assertEquals(55000L, legs[7].startTime.getTimeInMillis());
        assertEquals(57000L, legs[7].endTime.getTimeInMillis());
        assertEquals(2.0, legs[7].getDuration(), 0.0);
        assertEquals(0, legs[7].departureDelay);
        assertEquals(0, legs[7].arrivalDelay);
        assertFalse(legs[7].realTime);
        assertNull(legs[7].isNonExactFrequency);
        assertNull(legs[7].headway);
        assertEquals(F_DISTANCE[7], legs[7].distance, 0.0);

        assertNull(legs[8].agencyId);
        assertNull(legs[8].agencyName);
        assertNull(legs[8].agencyUrl);
        assertEquals(2, legs[8].agencyTimeZoneOffset);
        assertNull(legs[8].alerts);
        assertEquals("", legs[8].route);
        assertNull(legs[8].routeId);
        assertNull(legs[8].routeShortName);
        assertNull(legs[8].routeLongName);
        assertNull(legs[8].routeType);
        assertNull(legs[8].routeColor);
        assertNull(legs[8].routeTextColor);
        assertNull(legs[8].tripId);
        assertNull(legs[8].tripShortName);
        assertNull(legs[8].tripBlockId);
        assertNull(legs[8].headsign);
        assertNull(legs[8].serviceDate);
        assertFalse(legs[8].rentedBike);
        assertFalse(legs[8].isTransitLeg());
        assertFalse(legs[8].interlineWithPreviousLeg);
        assertNull(legs[8].boardRule);
        assertNull(legs[8].alightRule);
        assertFalse(legs[8].pathway);
        assertEquals("WALK", legs[8].mode);
        assertEquals(57000L, legs[8].startTime.getTimeInMillis());
        assertEquals(60000L, legs[8].endTime.getTimeInMillis());
        assertEquals(3.0, legs[8].getDuration(), 0.0);
        assertEquals(0, legs[8].departureDelay);
        assertEquals(0, legs[8].arrivalDelay);
        assertFalse(legs[8].realTime);
        assertNull(legs[8].isNonExactFrequency);
        assertNull(legs[8].headway);
        assertEquals(F_DISTANCE[8], legs[8].distance, 0.0);
    }

    /** Compare all simple walk step fields to their expected values, step by step. */
    private void compareSteps(WalkStep[][] steps, Type type) {
        if (type == Type.FORWARD || type == Type.BACKWARD) {
            assertEquals(1, steps[0].length);
        } else if (type == Type.ONBOARD) {
            assertEquals(0, steps[0].length);
        }
        assertEquals(0, steps[1].length);
        assertEquals(0, steps[2].length);
        assertEquals(1, steps[3].length);
        assertEquals(0, steps[4].length);
        assertEquals(2, steps[5].length);
        assertEquals(1, steps[6].length);
        assertEquals(1, steps[7].length);
        assertEquals(0, steps[8].length);

        if (type == Type.FORWARD || type == Type.BACKWARD) {
            assertEquals(AbsoluteDirection.NORTHEAST, steps[0][0].absoluteDirection);
            assertEquals(RelativeDirection.DEPART, steps[0][0].relativeDirection);
            assertEquals(NORTHEAST, steps[0][0].angle, EPSILON);
            assertEquals("Edge 3", steps[0][0].streetName);
            assertEquals(3.0, steps[0][0].distance, 0.0);
            assertFalse(steps[0][0].bogusName);
            assertFalse(steps[0][0].stayOn);
            assertEquals(0, steps[0][0].lon, 0.0);
            assertEquals(0, steps[0][0].lat, 0.0);
            assertNull(steps[0][0].alerts);
            assertFalse(steps[0][0].area);
            assertEquals("Ausfahrt", steps[0][0].exit);
        }

        assertEquals(AbsoluteDirection.EAST, steps[3][0].absoluteDirection);
        assertEquals(RelativeDirection.DEPART, steps[3][0].relativeDirection);
        assertEquals(EAST, steps[3][0].angle, EPSILON);
        assertEquals("Train stop arrive => Ferry stop depart", steps[3][0].streetName);
        assertEquals(7.0, steps[3][0].distance, 0.0);
        assertFalse(steps[3][0].bogusName);
        assertFalse(steps[3][0].stayOn);
        assertEquals(133, steps[3][0].lon, 0.0);
        assertEquals(67, steps[3][0].lat, 0.0);
        assertNull(steps[3][0].alerts);
        assertFalse(steps[3][0].area);
        assertNull(steps[3][0].exit);

        assertEquals(AbsoluteDirection.EAST, steps[5][0].absoluteDirection);
        assertEquals(RelativeDirection.DEPART, steps[5][0].relativeDirection);
        assertEquals(EAST, steps[5][0].angle, EPSILON);
        assertEquals("Edge 39 / 41", steps[5][0].streetName);
        assertEquals(2.1, steps[5][0].distance, 0.0);
        assertFalse(steps[5][0].bogusName);
        assertFalse(steps[5][0].stayOn);
        assertEquals(179, steps[5][0].lon, 0.0);
        assertEquals(89, steps[5][0].lat, 0.0);
        assertNull(steps[5][0].alerts);
        assertTrue(steps[5][0].area);
        assertNull(steps[5][0].exit);

        assertEquals(AbsoluteDirection.NORTH, steps[5][1].absoluteDirection);
        assertEquals(RelativeDirection.LEFT, steps[5][1].relativeDirection);
        assertEquals(NORTH, steps[5][1].angle, EPSILON);
        assertEquals("Edge 39 / 41", steps[5][1].streetName);
        assertEquals(1.9, steps[5][1].distance, 0.0);
        assertTrue(steps[5][1].bogusName);
        assertTrue(steps[5][1].stayOn);
        assertEquals(180, steps[5][1].lon, 0.0);
        assertEquals(89, steps[5][1].lat, 0.0);
        assertNull(steps[5][1].alerts);
        assertFalse(steps[5][1].area);
        assertNull(steps[5][1].exit);

        assertEquals(AbsoluteDirection.SOUTH, steps[6][0].absoluteDirection);
        assertEquals(RelativeDirection.HARD_LEFT, steps[6][0].relativeDirection);
        assertEquals(SOUTH, steps[6][0].angle, EPSILON);
        assertEquals("Edge 49", steps[6][0].streetName);
        assertEquals(2.0, steps[6][0].distance, 0.0);
        assertFalse(steps[6][0].bogusName);
        assertFalse(steps[6][0].stayOn);
        assertEquals(180, steps[6][0].lon, 0.0);
        assertEquals(90, steps[6][0].lat, 0.0);
        assertNull(steps[6][0].alerts);
        assertFalse(steps[6][0].area);
        assertNull(steps[6][0].exit);

        /*
         * The behavior of the relative direction computation code should now be correct here.
         * Anyway, it seems unlikely that anyone would care about correct relative directions in
         * the arctic regions. Of course, longitude becomes meaningless at the poles themselves, but
         * walking towards the pole, past it, and then back again will now yield correct results.
         */
        assertEquals(alertsExample, steps[7][0].alerts.get(0).getAlertHeaderText());
        assertEquals(AbsoluteDirection.SOUTH, steps[7][0].absoluteDirection);
        assertEquals(RelativeDirection.CONTINUE, steps[7][0].relativeDirection);
        assertEquals(SOUTH, steps[7][0].angle, EPSILON);
        assertEquals("Edge 53", steps[7][0].streetName);
        assertEquals(1.0, steps[7][0].distance, 0.0);
        assertEquals(1, steps[7][0].alerts.size());
        assertFalse(steps[7][0].bogusName);
        assertFalse(steps[7][0].stayOn);
        assertEquals(90, steps[7][0].lon, 0.0);
        assertEquals(90, steps[7][0].lat, 0.0);
        assertFalse(steps[7][0].area);
        assertNull(steps[7][0].exit);
    }

    /** Compare the encoded geometries to their expected values, leg by leg. */
    private void compareGeometries(EncodedPolylineBean[] geometries, Type type) {
        if (type == Type.FORWARD || type == Type.BACKWARD) {
            assertEquals(2, geometries[0].getLength());
            assertEquals("??_ibE_ibE", geometries[0].getPoints());
        } else if (type == Type.ONBOARD) {
            assertNull(geometries[0]);
        }

        assertEquals(3, geometries[1].getLength());
        if (type == Type.FORWARD || type == Type.BACKWARD) {
            assertEquals("_ibE_ibE_{geC_wpkG_{geC_wpkG", geometries[1].getPoints());
        } else if (type == Type.ONBOARD) {
            assertEquals("_wfhA_ekkC_mcbA_{geC_{geC_wpkG", geometries[1].getPoints());
        }

        assertEquals(2, geometries[2].getLength());
        assertEquals("_atqG_ye~O_{geC_wpkG", geometries[2].getPoints());

        assertEquals(2, geometries[3].getLength());
        assertEquals("_}|wK_qwjX?_seK", geometries[3].getPoints());

        assertEquals(2, geometries[4].getLength());
        assertEquals("_}|wK_e~vX_{geC_wpkG", geometries[4].getPoints());

        assertEquals(3, geometries[5].getLength());
        assertEquals("_ye~O_}oca@?_ibE_ibE?", geometries[5].getPoints());

        assertEquals(2, geometries[6].getLength());
        assertEquals("_cidP_gsia@?~bidP", geometries[6].getPoints());

        assertEquals(2, geometries[7].getLength());
        assertEquals("_cidP_cidP?~bidP", geometries[7].getPoints());

        assertEquals(0, geometries[8].getLength());
        assertEquals("", geometries[8].getPoints());
    }

    /** Compare all simple place fields to their expected values, place by place. */
    private void comparePlaces(Place[][] places, Type type) {
        assertEquals(2, places[0].length);
        assertEquals(3, places[1].length);
        assertEquals(2, places[2].length);
        assertEquals(2, places[3].length);
        assertEquals(2, places[4].length);
        assertEquals(2, places[5].length);
        assertEquals(2, places[6].length);
        assertEquals(2, places[7].length);
        assertEquals(2, places[8].length);

        if (type == Type.FORWARD || type == Type.BACKWARD) {
            assertEquals(0, places[0][0].lon, 0.0);
            assertEquals(0, places[0][0].lat, 0.0);
            assertNull(places[0][0].stopIndex);
            assertNull(places[0][0].stopSequence);
            assertNull(places[0][0].stopCode);
            assertNull(places[0][0].platformCode);
            assertNull(places[0][0].zoneId);
            assertNull(places[0][0].orig);
            assertNull(places[0][0].arrival);
            assertEquals(0L, places[0][0].departure.getTimeInMillis());

            assertEquals("Train stop depart", places[0][1].name);
            assertEquals(1, places[0][1].lon, 0.0);
            assertEquals(1, places[0][1].lat, 0.0);
            assertEquals(0, places[0][1].stopIndex.intValue());
            assertEquals(Integer.MIN_VALUE, places[0][1].stopSequence.intValue());
            assertEquals("Train depart code", places[0][1].stopCode);
            assertEquals("Train depart platform", places[0][1].platformCode);
            assertEquals("Train depart zone", places[0][1].zoneId);
            assertNull(places[0][1].orig);
            assertEquals(3000L, places[0][1].arrival.getTimeInMillis());
            assertEquals(4000L, places[0][1].departure.getTimeInMillis());

            assertEquals("Train stop depart", places[1][0].name);
            assertEquals(1, places[1][0].lon, 0.0);
            assertEquals(1, places[1][0].lat, 0.0);
            assertEquals(0, places[1][0].stopIndex.intValue());
            assertEquals(Integer.MIN_VALUE, places[1][0].stopSequence.intValue());
            assertEquals("Train depart code", places[1][0].stopCode);
            assertEquals("Train depart platform", places[1][0].platformCode);
            assertEquals("Train depart zone", places[1][0].zoneId);
            assertNull(places[1][0].orig);
            assertEquals(3000L, places[1][0].arrival.getTimeInMillis());
            assertEquals(4000L, places[1][0].departure.getTimeInMillis());
        } else if (type == Type.ONBOARD) {
            assertNull(places[0][0]);

            assertNull(places[0][1]);

            assertEquals("Onboard", places[1][0].name);
            assertEquals(23, places[1][0].lon, 0.0);
            assertEquals(12, places[1][0].lat, 0.0);
            assertNull(places[1][0].stopIndex);
            assertNull(places[1][0].stopSequence);
            assertNull(places[1][0].stopCode);
            assertNull(places[1][0].platformCode);
            assertNull(places[1][0].zoneId);
            assertNull(places[1][0].orig);
            assertNull(places[1][0].arrival);
            assertEquals(6000L, places[1][0].departure.getTimeInMillis());
        }

        assertEquals("Train stop dwell", places[1][1].name);
        assertEquals(45, places[1][1].lon, 0.0);
        assertEquals(23, places[1][1].lat, 0.0);
        assertEquals(1, places[1][1].stopIndex.intValue());
        assertEquals(0, places[1][1].stopSequence.intValue());
        assertEquals("Train dwell code", places[1][1].stopCode);
        assertEquals("Train dwell platform", places[1][1].platformCode);
        assertEquals("Train dwell zone", places[1][1].zoneId);
        assertNull(places[1][1].orig);
        assertEquals(8000L, places[1][1].arrival.getTimeInMillis());
        assertEquals(12000L, places[1][1].departure.getTimeInMillis());

        assertEquals("Train stop interline", places[1][2].name);
        assertEquals(89, places[1][2].lon, 0.0);
        assertEquals(45, places[1][2].lat, 0.0);
        assertEquals(2, places[1][2].stopIndex.intValue());
        assertEquals(Integer.MAX_VALUE, places[1][2].stopSequence.intValue());
        assertEquals("Train interline code", places[1][2].stopCode);
        assertEquals("Train interline platform", places[1][2].platformCode);
        assertEquals("Train interline zone", places[1][2].zoneId);
        assertNull(places[1][2].orig);
        assertEquals(16000L, places[1][2].arrival.getTimeInMillis());
        assertEquals(20000L, places[1][2].departure.getTimeInMillis());

        assertEquals("Train stop interline", places[2][0].name);
        assertEquals(89, places[2][0].lon, 0.0);
        assertEquals(45, places[2][0].lat, 0.0);
        assertEquals(0, places[2][0].stopIndex.intValue());
        assertEquals(0, places[2][0].stopSequence.intValue());
        assertEquals("Train interline code", places[2][0].stopCode);
        assertEquals("Train interline platform", places[2][0].platformCode);
        assertEquals("Train interline zone", places[2][0].zoneId);
        assertNull(places[2][0].orig);
        assertEquals(16000L, places[2][0].arrival.getTimeInMillis());
        assertEquals(20000L, places[2][0].departure.getTimeInMillis());

        assertEquals("Train stop arrive", places[2][1].name);
        assertEquals(133, places[2][1].lon, 0.0);
        assertEquals(67, places[2][1].lat, 0.0);
        assertEquals(1, places[2][1].stopIndex.intValue());
        assertEquals(1, places[2][1].stopSequence.intValue());
        assertEquals("Train arrive code", places[2][1].stopCode);
        assertEquals("Train arrive platform", places[2][1].platformCode);
        assertEquals("Train arrive zone", places[2][1].zoneId);
        assertNull(places[2][1].orig);
        assertEquals(24000L, places[2][1].arrival.getTimeInMillis());
        if (type == Type.FORWARD || type == Type.ONBOARD) {
            assertEquals(24000L, places[2][1].departure.getTimeInMillis());
        } else if (type == Type.BACKWARD) {
            assertEquals(32000L, places[2][1].departure.getTimeInMillis());
        }

        assertEquals("Train stop arrive", places[3][0].name);
        assertEquals(133, places[3][0].lon, 0.0);
        assertEquals(67, places[3][0].lat, 0.0);
        assertEquals(1, places[3][0].stopIndex.intValue());
        assertEquals(1, places[3][0].stopSequence.intValue());
        assertEquals("Train arrive code", places[3][0].stopCode);
        assertEquals("Train arrive platform", places[3][0].platformCode);
        assertEquals("Train arrive zone", places[3][0].zoneId);
        assertNull(places[3][0].orig);
        assertEquals(24000L, places[3][0].arrival.getTimeInMillis());
        if (type == Type.FORWARD || type == Type.ONBOARD) {
            assertEquals(24000L, places[3][0].departure.getTimeInMillis());
        } else if (type == Type.BACKWARD) {
            assertEquals(32000L, places[3][0].departure.getTimeInMillis());
        }

        assertEquals("Ferry stop depart", places[3][1].name);
        assertEquals(135, places[3][1].lon, 0.0);
        assertEquals(67, places[3][1].lat, 0.0);
        assertEquals(0, places[3][1].stopIndex.intValue());
        assertEquals(-1, places[3][1].stopSequence.intValue());
        assertEquals("Ferry depart code", places[3][1].stopCode);
        assertEquals("Ferry depart platform", places[3][1].platformCode);
        assertEquals("Ferry depart zone", places[3][1].zoneId);
        assertNull(places[3][1].orig);
        if (type == Type.FORWARD || type == Type.ONBOARD) {
            assertEquals(32000L, places[3][1].arrival.getTimeInMillis());
        } else if (type == Type.BACKWARD) {
            assertEquals(40000L, places[3][1].arrival.getTimeInMillis());
        }
        assertEquals(40000L, places[3][1].departure.getTimeInMillis());

        assertEquals("Ferry stop depart", places[4][0].name);
        assertEquals(135, places[4][0].lon, 0.0);
        assertEquals(67, places[4][0].lat, 0.0);
        assertEquals(0, places[4][0].stopIndex.intValue());
        assertEquals(-1, places[4][0].stopSequence.intValue());
        assertEquals("Ferry depart code", places[4][0].stopCode);
        assertEquals("Ferry depart platform", places[4][0].platformCode);
        assertEquals("Ferry depart zone", places[4][0].zoneId);
        assertNull(places[4][0].orig);
        if (type == Type.FORWARD || type == Type.ONBOARD) {
            assertEquals(32000L, places[4][0].arrival.getTimeInMillis());
        } else if (type == Type.BACKWARD) {
            assertEquals(40000L, places[4][0].arrival.getTimeInMillis());
        }
        assertEquals(40000L, places[4][0].departure.getTimeInMillis());

        assertEquals("Ferry stop arrive", places[4][1].name);
        assertEquals(179, places[4][1].lon, 0.0);
        assertEquals(89, places[4][1].lat, 0.0);
        assertEquals(1, places[4][1].stopIndex.intValue());
        assertEquals(0, places[4][1].stopSequence.intValue());
        assertEquals("Ferry arrive code", places[4][1].stopCode);
        assertEquals("Ferry arrive platform", places[4][1].platformCode);
        assertEquals("Ferry arrive zone", places[4][1].zoneId);
        assertNull(places[4][1].orig);
        assertEquals(43000L, places[4][1].arrival.getTimeInMillis());
        assertEquals(44000L, places[4][1].departure.getTimeInMillis());

        assertEquals("Ferry stop arrive", places[5][0].name);
        assertEquals(179, places[5][0].lon, 0.0);
        assertEquals(89, places[5][0].lat, 0.0);
        assertEquals(1, places[5][0].stopIndex.intValue());
        assertEquals(0, places[5][0].stopSequence.intValue());
        assertEquals("Ferry arrive code", places[5][0].stopCode);
        assertEquals("Ferry arrive platform", places[5][0].platformCode);
        assertEquals("Ferry arrive zone", places[5][0].zoneId);
        assertNull(places[5][0].orig);
        assertEquals(43000L, places[5][0].arrival.getTimeInMillis());
        assertEquals(44000L, places[5][0].departure.getTimeInMillis());

        assertEquals("Exit pickup station", places[5][1].name);
        assertEquals(180, places[5][1].lon, 0.0);
        assertEquals(90, places[5][1].lat, 0.0);
        assertNull(places[5][1].stopIndex);
        assertNull(places[5][1].stopSequence);
        assertNull(places[5][1].stopCode);
        assertNull(places[5][1].platformCode);
        assertNull(places[5][1].zoneId);
        assertNull(places[5][1].orig);
        assertEquals(53000L, places[5][1].arrival.getTimeInMillis());
        assertEquals(53000L, places[5][1].departure.getTimeInMillis());

        assertEquals("Exit pickup station", places[6][0].name);
        assertEquals(180, places[6][0].lon, 0.0);
        assertEquals(90, places[6][0].lat, 0.0);
        assertNull(places[6][0].stopIndex);
        assertNull(places[6][0].stopSequence);
        assertNull(places[6][0].stopCode);
        assertNull(places[6][0].platformCode);
        assertNull(places[6][0].zoneId);
        assertNull(places[6][0].orig);
        assertEquals(53000L, places[6][0].arrival.getTimeInMillis());
        assertEquals(53000L, places[6][0].departure.getTimeInMillis());

        assertEquals(90, places[6][1].lon, 0.0);
        assertEquals(90, places[6][1].lat, 0.0);
        assertNull(places[6][1].stopIndex);
        assertNull(places[6][1].stopSequence);
        assertNull(places[6][1].stopCode);
        assertNull(places[6][1].platformCode);
        assertNull(places[6][1].zoneId);
        assertNull(places[6][1].orig);
        assertEquals(55000L, places[6][1].arrival.getTimeInMillis());
        assertEquals(55000L, places[6][1].departure.getTimeInMillis());

        assertEquals(90, places[7][0].lon, 0.0);
        assertEquals(90, places[7][0].lat, 0.0);
        assertNull(places[7][0].stopIndex);
        assertNull(places[7][0].stopSequence);
        assertNull(places[7][0].stopCode);
        assertNull(places[7][0].platformCode);
        assertNull(places[7][0].zoneId);
        assertNull(places[7][0].orig);
        assertEquals(55000L, places[7][0].arrival.getTimeInMillis());
        assertEquals(55000L, places[7][0].departure.getTimeInMillis());

        assertEquals("Enter dropoff station", places[7][1].name);
        assertEquals(0, places[7][1].lon, 0.0);
        assertEquals(90, places[7][1].lat, 0.0);
        assertNull(places[7][1].stopIndex);
        assertNull(places[7][1].stopSequence);
        assertNull(places[7][1].stopCode);
        assertNull(places[7][1].platformCode);
        assertNull(places[7][1].zoneId);
        assertNull(places[7][1].orig);
        assertEquals(57000L, places[7][1].arrival.getTimeInMillis());
        assertEquals(57000L, places[7][1].departure.getTimeInMillis());

        assertEquals("Enter dropoff station", places[8][0].name);
        assertEquals(0, places[8][0].lon, 0.0);
        assertEquals(90, places[8][0].lat, 0.0);
        assertNull(places[8][0].stopIndex);
        assertNull(places[8][0].stopSequence);
        assertNull(places[8][0].stopCode);
        assertNull(places[8][0].platformCode);
        assertNull(places[8][0].zoneId);
        assertNull(places[8][0].orig);
        assertEquals(57000L, places[8][0].arrival.getTimeInMillis());
        assertEquals(57000L, places[8][0].departure.getTimeInMillis());

        assertEquals(0, places[8][1].lon, 0.0);
        assertEquals(90, places[8][1].lat, 0.0);
        assertNull(places[8][1].stopIndex);
        assertNull(places[8][1].stopSequence);
        assertNull(places[8][1].stopCode);
        assertNull(places[8][1].platformCode);
        assertNull(places[8][1].zoneId);
        assertNull(places[8][1].orig);
        assertEquals(60000L, places[8][1].arrival.getTimeInMillis());
        assertNull(places[8][1].departure);
    }

    /** Compare the stop ids to their expected values, place by place. */
    private void compareStopIds(AgencyAndId[][] stopIds, Type type) {
        assertEquals(2, stopIds[0].length);
        assertEquals(3, stopIds[1].length);
        assertEquals(2, stopIds[2].length);
        assertEquals(2, stopIds[3].length);
        assertEquals(2, stopIds[4].length);
        assertEquals(2, stopIds[5].length);
        assertEquals(2, stopIds[6].length);
        assertEquals(2, stopIds[7].length);
        assertEquals(2, stopIds[8].length);

        assertNull(stopIds[0][0]);

        if (type == Type.FORWARD || type == Type.BACKWARD) {
            assertEquals("FEED", stopIds[0][1].getAgencyId());
            assertEquals("Depart", stopIds[0][1].getId());
        } else if (type == Type.ONBOARD) {
            assertNull(stopIds[0][1]);
        }

        if (type == Type.FORWARD || type == Type.BACKWARD) {
            assertEquals("FEED", stopIds[1][0].getAgencyId());
            assertEquals("Depart", stopIds[1][0].getId());
        } else if (type == Type.ONBOARD) {
            assertNull(stopIds[1][0]);
        }

        assertEquals("FEED", stopIds[1][1].getAgencyId());
        assertEquals("Dwell", stopIds[1][1].getId());

        assertEquals("FEED", stopIds[1][2].getAgencyId());
        assertEquals("Interline", stopIds[1][2].getId());

        assertEquals("FEED", stopIds[2][0].getAgencyId());
        assertEquals("Interline", stopIds[2][0].getId());

        assertEquals("FEED", stopIds[2][1].getAgencyId());
        assertEquals("Arrive", stopIds[2][1].getId());

        assertEquals("FEED", stopIds[3][0].getAgencyId());
        assertEquals("Arrive", stopIds[3][0].getId());

        assertEquals("FEED", stopIds[3][1].getAgencyId());
        assertEquals("Depart", stopIds[3][1].getId());

        assertEquals("FEED", stopIds[4][0].getAgencyId());
        assertEquals("Depart", stopIds[4][0].getId());

        assertEquals("FEED", stopIds[4][1].getAgencyId());
        assertEquals("Arrive", stopIds[4][1].getId());

        assertEquals("FEED", stopIds[5][0].getAgencyId());
        assertEquals("Arrive", stopIds[5][0].getId());

        assertNull(stopIds[5][1]);

        assertNull(stopIds[6][0]);

        assertNull(stopIds[6][1]);

        assertNull(stopIds[7][0]);

        assertNull(stopIds[7][1]);

        assertNull(stopIds[8][0]);

        assertNull(stopIds[8][1]);
    }

    /** Compare the elevations to their expected values, step by step. */
    private void compareElevations(Double[][][][] elevations, Type type) {
        if (type == Type.FORWARD || type == Type.BACKWARD) {
            assertEquals(0.0, elevations[0][0][0][0], 0.0);
            assertEquals(0.0, elevations[0][0][0][1], 0.0);
            assertEquals(3.0, elevations[0][0][1][0], 0.0);
            assertEquals(9.9, elevations[0][0][1][1], 0.0);
        } else if (type == Type.ONBOARD) {
            assertNull(elevations[0][0][0][0]);
            assertNull(elevations[0][0][0][1]);
            assertNull(elevations[0][0][1][0]);
            assertNull(elevations[0][0][1][1]);
        }

        assertNull(elevations[0][1][0][0]);
        assertNull(elevations[0][1][0][1]);
        assertNull(elevations[0][1][1][0]);
        assertNull(elevations[0][1][1][1]);

        assertNull(elevations[1][0][0][0]);
        assertNull(elevations[1][0][0][1]);
        assertNull(elevations[1][0][1][0]);
        assertNull(elevations[1][0][1][1]);

        assertNull(elevations[1][1][0][0]);
        assertNull(elevations[1][1][0][1]);
        assertNull(elevations[1][1][1][0]);
        assertNull(elevations[1][1][1][1]);

        assertNull(elevations[2][0][0][0]);
        assertNull(elevations[2][0][0][1]);
        assertNull(elevations[2][0][1][0]);
        assertNull(elevations[2][0][1][1]);

        assertNull(elevations[2][1][0][0]);
        assertNull(elevations[2][1][0][1]);
        assertNull(elevations[2][1][1][0]);
        assertNull(elevations[2][1][1][1]);

        assertNull(elevations[3][0][0][0]);
        assertNull(elevations[3][0][0][1]);
        assertNull(elevations[3][0][1][0]);
        assertNull(elevations[3][0][1][1]);

        assertNull(elevations[3][1][0][0]);
        assertNull(elevations[3][1][0][1]);
        assertNull(elevations[3][1][1][0]);
        assertNull(elevations[3][1][1][1]);

        assertNull(elevations[4][0][0][0]);
        assertNull(elevations[4][0][0][1]);
        assertNull(elevations[4][0][1][0]);
        assertNull(elevations[4][0][1][1]);

        assertNull(elevations[4][1][0][0]);
        assertNull(elevations[4][1][0][1]);
        assertNull(elevations[4][1][1][0]);
        assertNull(elevations[4][1][1][1]);

        assertEquals(0.0, elevations[5][0][0][0], 0.0);
        assertEquals(9.9, elevations[5][0][0][1], 0.0);
        assertEquals(2.1, elevations[5][0][1][0], 0.0);
        assertEquals(0.1, elevations[5][0][1][1], 0.0);

        assertEquals(0.0, elevations[5][1][0][0], 0.0);
        assertEquals(0.1, elevations[5][1][0][1], 0.0);
        assertEquals(1.9, elevations[5][1][1][0], 0.0);
        assertEquals(2.8, elevations[5][1][1][1], 0.0);

        assertEquals(0.0, elevations[6][0][0][0], 0.0);
        assertEquals(2.8, elevations[6][0][0][1], 0.0);
        assertEquals(2.0, elevations[6][0][1][0], 0.0);
        assertEquals(2.6, elevations[6][0][1][1], 0.0);

        assertNull(elevations[6][1][0][0]);
        assertNull(elevations[6][1][0][1]);
        assertNull(elevations[6][1][1][0]);
        assertNull(elevations[6][1][1][1]);

        assertEquals(0.0, elevations[7][0][0][0], 0.0);
        assertEquals(2.6, elevations[7][0][0][1], 0.0);
        assertEquals(1.0, elevations[7][0][1][0], 0.0);
        assertEquals(6.0, elevations[7][0][1][1], 0.0);

        assertNull(elevations[7][1][0][0]);
        assertNull(elevations[7][1][0][1]);
        assertNull(elevations[7][1][1][0]);
        assertNull(elevations[7][1][1][1]);

        assertNull(elevations[8][0][0][0]);
        assertNull(elevations[8][0][0][1]);
        assertNull(elevations[8][0][1][0]);
        assertNull(elevations[8][0][1][1]);

        assertNull(elevations[8][1][0][0]);
        assertNull(elevations[8][1][0][1]);
        assertNull(elevations[8][1][1][0]);
        assertNull(elevations[8][1][1][1]);
    }

    /**
     * This class extends the {@link CalendarServiceData} class to allow for easier testing.
     * It includes methods to return both the set of service ids and the time zone used for testing.
     */
    private static final class CalendarServiceDataStub extends CalendarServiceData {
        private static final long serialVersionUID = 1L;

        final Set<AgencyAndId> serviceIds;

        CalendarServiceDataStub(Set<AgencyAndId> serviceIds) {
            this.serviceIds = serviceIds;
        }

        @Override
        public Set<AgencyAndId> getServiceIds() {
            return serviceIds;
        }

        @Override
        public Set<AgencyAndId> getServiceIdsForDate(ServiceDate date) {
            return serviceIds;
        }

        @Override
        public TimeZone getTimeZoneForAgencyId(String agencyId) {
            return timeZone;
        }
    }

    /**
     * This class implements the {@link FareService} interface to allow for testing.
     * It will return the same fare at every invocation.
     */
    private static final class FareServiceStub implements FareService {
        @Override
        public Fare getCost(GraphPath path) {
            Fare fare = new Fare();
            fare.addFare(FareType.regular, new WrappedCurrency(), 0);
            fare.addFare(FareType.student, new WrappedCurrency(), 1);
            fare.addFare(FareType.senior, new WrappedCurrency(), 2);
            fare.addFare(FareType.tram, new WrappedCurrency(), 4);
            fare.addFare(FareType.special, new WrappedCurrency(), 8);
            return fare;
        }
    }

    /**
     * This enum is used to distinguish between the different graph paths and resulting itineraries.
     * Its three values correspond to the forward, backward and onboard graph paths and itineraries.
     * When future values are added, the test should not be assumed to fail upon encountering those.
     */
    private static enum Type {
        FORWARD,
        BACKWARD,
        ONBOARD
    }
}
