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

package org.opentripplanner.routing.impl;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFactory;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.TimeZone;

import org.junit.Test;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/*
 * FIXME: This test has become seriously ugly after recent changes to OTP. Using mocks, which seemed
 * like a good idea at the time, became more of a liability when it turned out lots of mocks were no
 * longer valid. The idea of a decoupled unit test has certainly not worked out the way it should've
 * worked out in theory. It would be very wise to rewrite this test to be simpler and not use mocks.
 * FIXME too: Even worse, it didn't even fail when OnBoardDepartServiceImpl turned out to be broken.
 */
public class OnBoardDepartServiceImplTest {
    OnBoardDepartServiceImpl onBoardDepartServiceImpl = new OnBoardDepartServiceImpl();

    @Test
    public final void testOnBoardDepartureTime() {
        Coordinate[] coordinates = new Coordinate[5];
        coordinates[0] = new Coordinate(0.0, 0.0);
        coordinates[1] = new Coordinate(0.0, 1.0);
        coordinates[2] = new Coordinate(2.0, 1.0);
        coordinates[3] = new Coordinate(5.0, 1.0);
        coordinates[4] = new Coordinate(5.0, 5.0);

        PatternDepartVertex depart = mock(PatternDepartVertex.class);
        PatternArriveVertex dwell = mock(PatternArriveVertex.class);
        PatternArriveVertex arrive = mock(PatternArriveVertex.class);
        Graph graph = mock(Graph.class);
        RoutingRequest routingRequest = mock(RoutingRequest.class);
        ServiceDay serviceDay = mock(ServiceDay.class);

        // You're probably not supposed to do this to mocks (access their fields directly)
        // But I know of no other way to do this since the mock object has only action-free stub methods.
        routingRequest.modes = new TraverseModeSet("WALK,TRANSIT");

        when(graph.getTimeZone()).thenReturn(TimeZone.getTimeZone("GMT"));

        GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
        CoordinateSequenceFactory coordinateSequenceFactory =
                geometryFactory.getCoordinateSequenceFactory();
        CoordinateSequence coordinateSequence = coordinateSequenceFactory.create(coordinates);
        LineString geometry = new LineString(coordinateSequence, geometryFactory);
        ArrayList<Edge> hops = new ArrayList<Edge>(2);
        RoutingContext routingContext = new RoutingContext(routingRequest, graph, null, arrive);
        AgencyAndId agencyAndId = new AgencyAndId("Agency", "ID");
        Agency agency = new Agency();
        Route route = new Route();
        ArrayList<StopTime> stopTimes = new ArrayList<StopTime>(3);
        StopTime stopDepartTime = new StopTime();
        StopTime stopDwellTime = new StopTime();
        StopTime stopArriveTime = new StopTime();
        Stop stopDepart = new Stop();
        Stop stopDwell = new Stop();
        Stop stopArrive = new Stop();
        Trip trip = new Trip();

        routingContext.serviceDays =
                new ArrayList<ServiceDay>(Collections.singletonList(serviceDay));
        agency.setId(agencyAndId.getAgencyId());
        route.setId(agencyAndId);
        route.setAgency(agency);
        stopDepart.setId(agencyAndId);
        stopDwell.setId(agencyAndId);
        stopArrive.setId(agencyAndId);
        stopDepartTime.setStop(stopDepart);
        stopDepartTime.setDepartureTime(0);
        stopDwellTime.setArrivalTime(20);
        stopDwellTime.setStop(stopDwell);
        stopDwellTime.setDepartureTime(40);
        stopArriveTime.setArrivalTime(60);
        stopArriveTime.setStop(stopArrive);
        stopTimes.add(stopDepartTime);
        stopTimes.add(stopDwellTime);
        stopTimes.add(stopArriveTime);
        trip.setId(agencyAndId);
        trip.setTripHeadsign("The right");
        trip.setRoute(route);

        TripTimes tripTimes = new TripTimes(trip, stopTimes, new Deduplicator());
        StopPattern stopPattern = new StopPattern(stopTimes);
        TripPattern tripPattern = new TripPattern(route, stopPattern);
        TripPattern.generateUniqueIds(Arrays.asList(tripPattern));

        when(depart.getTripPattern()).thenReturn(tripPattern);
        when(dwell.getTripPattern()).thenReturn(tripPattern);

        PatternHop patternHop0 = new PatternHop(depart, dwell, stopDepart, stopDwell, 0);
        PatternHop patternHop1 = new PatternHop(dwell, arrive, stopDwell, stopArrive, 1);

        hops.add(patternHop0);
        hops.add(patternHop1);

        when(graph.getEdges()).thenReturn(hops);
        when(depart.getCoordinate()).thenReturn(new Coordinate(0, 0));
        when(dwell.getCoordinate()).thenReturn(new Coordinate(0, 0));
        when(arrive.getCoordinate()).thenReturn(new Coordinate(0, 0));
        routingRequest.from = new GenericLocation();
        routingRequest.startingTransitTripId = agencyAndId;
        when(serviceDay.secondsSinceMidnight(anyInt())).thenReturn(9);

        patternHop0.setGeometry(geometry);
        tripPattern.add(tripTimes);
        graph.index = new GraphIndex(graph);

        coordinates = new Coordinate[3];
        coordinates[0] = new Coordinate(3.5, 1.0);
        coordinates[1] = new Coordinate(5.0, 1.0);
        coordinates[2] = new Coordinate(5.0, 5.0);

        coordinateSequence = coordinateSequenceFactory.create(coordinates);
        geometry = new LineString(coordinateSequence, geometryFactory);

        Vertex vertex = onBoardDepartServiceImpl.setupDepartOnBoard(routingContext);
        Edge edge = vertex.getOutgoing().toArray(new Edge[1])[0];

        assertEquals(vertex, edge.getFromVertex());
        assertEquals(dwell, edge.getToVertex());
        assertEquals("The right", edge.getDirection());
        assertEquals(geometry, edge.getGeometry());

        assertEquals(coordinates[0].x, vertex.getX(), 0.0);
        assertEquals(coordinates[0].y, vertex.getY(), 0.0);
    }

    @Test
    public final void testOnBoardDepartureAtArrivalTime() {
        Coordinate[] coordinates = new Coordinate[2];
        coordinates[0] = new Coordinate(0.0, 0.0);
        coordinates[1] = new Coordinate(0.0, 1.0);

        TransitStop station0 = mock(TransitStop.class);
        TransitStop station1 = mock(TransitStop.class);
        PatternDepartVertex depart = mock(PatternDepartVertex.class);
        PatternArriveVertex arrive = mock(PatternArriveVertex.class);
        Graph graph = mock(Graph.class);
        RoutingRequest routingRequest = mock(RoutingRequest.class);
        ServiceDay serviceDay = mock(ServiceDay.class);

        // You're probably not supposed to do this to mocks (access their fields directly)
        // But I know of no other way to do this since the mock object has only action-free stub methods.
        routingRequest.modes = new TraverseModeSet("WALK,TRANSIT");

        when(graph.getTimeZone()).thenReturn(TimeZone.getTimeZone("GMT"));
        when(station0.getX()).thenReturn(coordinates[0].x);
        when(station0.getY()).thenReturn(coordinates[0].y);
        when(station1.getX()).thenReturn(coordinates[1].x);
        when(station1.getY()).thenReturn(coordinates[1].y);

        RoutingContext routingContext = new RoutingContext(routingRequest, graph, null, arrive);
        AgencyAndId agencyAndId = new AgencyAndId("Agency", "ID");
        Agency agency = new Agency();
        Route route = new Route();
        ArrayList<StopTime> stopTimes = new ArrayList<StopTime>(2);
        StopTime stopDepartTime = new StopTime();
        StopTime stopArriveTime = new StopTime();
        Stop stopDepart = new Stop();
        Stop stopArrive = new Stop();
        Trip trip = new Trip();

        routingContext.serviceDays =
                new ArrayList<ServiceDay>(Collections.singletonList(serviceDay));
        agency.setId(agencyAndId.getAgencyId());
        route.setId(agencyAndId);
        route.setAgency(agency);
        stopDepart.setId(new AgencyAndId("Station", "0"));
        stopArrive.setId(new AgencyAndId("Station", "1"));
        stopDepartTime.setStop(stopDepart);
        stopDepartTime.setDepartureTime(0);
        stopArriveTime.setArrivalTime(10);
        stopArriveTime.setStop(stopArrive);
        stopTimes.add(stopDepartTime);
        stopTimes.add(stopArriveTime);
        trip.setId(agencyAndId);
        trip.setRoute(route);

        TripTimes tripTimes = new TripTimes(trip, stopTimes, new Deduplicator());
        StopPattern stopPattern = new StopPattern(stopTimes);
        TripPattern tripPattern = new TripPattern(route, stopPattern);
        TripPattern.generateUniqueIds(Arrays.asList(tripPattern));

        when(depart.getTripPattern()).thenReturn(tripPattern);

        PatternHop patternHop = new PatternHop(depart, arrive, stopDepart, stopArrive, 0);

        when(graph.getEdges()).thenReturn(Collections.<Edge>singletonList(patternHop));
        when(depart.getCoordinate()).thenReturn(new Coordinate(0, 0));
        when(arrive.getCoordinate()).thenReturn(new Coordinate(0, 0));
        routingRequest.from = new GenericLocation();
        routingRequest.startingTransitTripId = agencyAndId;
        when(serviceDay.secondsSinceMidnight(anyInt())).thenReturn(10);
        when(graph.getVertex("Station_0")).thenReturn(station0);
        when(graph.getVertex("Station_1")).thenReturn(station1);

        tripPattern.add(tripTimes);
        graph.index = new GraphIndex(graph);

        Vertex vertex = onBoardDepartServiceImpl.setupDepartOnBoard(routingContext);

        assertEquals(coordinates[1].x, vertex.getX(), 0.0);
        assertEquals(coordinates[1].y, vertex.getY(), 0.0);
    }

    @Test
    public final void testOnBoardAtStation() {
        TransitStop station0 = mock(TransitStop.class);
        TransitStop station1 = mock(TransitStop.class);
        TransitStop station2 = mock(TransitStop.class);
        PatternDepartVertex depart = mock(PatternDepartVertex.class);
        PatternArriveVertex dwell = mock(PatternArriveVertex.class);
        PatternArriveVertex arrive = mock(PatternArriveVertex.class);
        Graph graph = mock(Graph.class);
        RoutingRequest routingRequest = mock(RoutingRequest.class);
        ServiceDay serviceDay = mock(ServiceDay.class);

        // You're probably not supposed to do this to mocks (access their fields directly)
        // But I know of no other way to do this since the mock object has only action-free stub methods.
        routingRequest.modes = new TraverseModeSet("WALK,TRANSIT");

        when(graph.getTimeZone()).thenReturn(TimeZone.getTimeZone("GMT"));

        ArrayList<Edge> hops = new ArrayList<Edge>(2);
        RoutingContext routingContext = new RoutingContext(routingRequest, graph, null, arrive);
        Agency agency = new Agency();
        AgencyAndId agencyAndId = new AgencyAndId("Agency", "ID");
        Route route = new Route();
        ArrayList<StopTime> stopTimes = new ArrayList<StopTime>(2);
        StopTime stopDepartTime = new StopTime();
        StopTime stopDwellTime = new StopTime();
        StopTime stopArriveTime = new StopTime();
        Stop stopDepart = new Stop();
        Stop stopDwell = new Stop();
        Stop stopArrive = new Stop();
        Trip trip = new Trip();

        routingContext.serviceDays =
                new ArrayList<ServiceDay>(Collections.singletonList(serviceDay));
        agency.setId(agencyAndId.getAgencyId());
        route.setId(agencyAndId);
        route.setAgency(agency);
        stopDepart.setId(new AgencyAndId("Station", "0"));
        stopDwell.setId(new AgencyAndId("Station", "1"));
        stopArrive.setId(new AgencyAndId("Station", "2"));
        stopDepartTime.setStop(stopDepart);
        stopDepartTime.setDepartureTime(0);
        stopDwellTime.setArrivalTime(20);
        stopDwellTime.setStop(stopDwell);
        stopDwellTime.setDepartureTime(40);
        stopArriveTime.setArrivalTime(60);
        stopArriveTime.setStop(stopArrive);
        stopTimes.add(stopDepartTime);
        stopTimes.add(stopDwellTime);
        stopTimes.add(stopArriveTime);
        trip.setId(agencyAndId);
        trip.setRoute(route);

        TripTimes tripTimes = new TripTimes(trip, stopTimes, new Deduplicator());
        StopPattern stopPattern = new StopPattern(stopTimes);
        TripPattern tripPattern = new TripPattern(route, stopPattern);
        TripPattern.generateUniqueIds(Arrays.asList(tripPattern));

        when(depart.getTripPattern()).thenReturn(tripPattern);
        when(dwell.getTripPattern()).thenReturn(tripPattern);

        PatternHop patternHop0 = new PatternHop(depart, dwell, stopDepart, stopDwell, 0);
        PatternHop patternHop1 = new PatternHop(dwell, arrive, stopDwell, stopArrive, 1);

        hops.add(patternHop0);
        hops.add(patternHop1);

        when(graph.getEdges()).thenReturn(hops);
        when(depart.getCoordinate()).thenReturn(new Coordinate(0, 0));
        when(dwell.getCoordinate()).thenReturn(new Coordinate(0, 0));
        when(arrive.getCoordinate()).thenReturn(new Coordinate(0, 0));
        routingRequest.from = new GenericLocation();
        routingRequest.startingTransitTripId = agencyAndId;
        when(graph.getVertex("Station_0")).thenReturn(station0);
        when(graph.getVertex("Station_1")).thenReturn(station1);
        when(graph.getVertex("Station_2")).thenReturn(station2);

        tripPattern.add(tripTimes);
        graph.index = new GraphIndex(graph);

        when(serviceDay.secondsSinceMidnight(anyInt())).thenReturn(0);
        assertEquals(station0, onBoardDepartServiceImpl.setupDepartOnBoard(routingContext));

        when(serviceDay.secondsSinceMidnight(anyInt())).thenReturn(20);
        assertEquals(station1, onBoardDepartServiceImpl.setupDepartOnBoard(routingContext));

        when(serviceDay.secondsSinceMidnight(anyInt())).thenReturn(30);
        assertEquals(station1, onBoardDepartServiceImpl.setupDepartOnBoard(routingContext));

        when(serviceDay.secondsSinceMidnight(anyInt())).thenReturn(40);
        assertEquals(station1, onBoardDepartServiceImpl.setupDepartOnBoard(routingContext));

        when(serviceDay.secondsSinceMidnight(anyInt())).thenReturn(60);
        assertEquals(station2, onBoardDepartServiceImpl.setupDepartOnBoard(routingContext));
    }
}
