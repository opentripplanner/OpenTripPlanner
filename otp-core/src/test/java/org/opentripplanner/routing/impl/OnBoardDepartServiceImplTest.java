package org.opentripplanner.routing.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFactory;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

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

        PatternArriveVertex arrive = mock(PatternArriveVertex.class);
        PatternHop patternHop = mock(PatternHop.class);
        TripTimes tripTimes = mock(TripTimes.class);
        TableTripPattern tableTripPattern = mock(TableTripPattern.class);
        TransitIndexService transitIndexService = mock(TransitIndexService.class);
        Graph graph = mock(Graph.class);
        RoutingRequest routingRequest = mock(RoutingRequest.class);
        ServiceDay serviceDay = mock(ServiceDay.class);

        GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
        CoordinateSequenceFactory coordinateSequenceFactory =
                geometryFactory.getCoordinateSequenceFactory();
        CoordinateSequence coordinateSequence = coordinateSequenceFactory.create(coordinates);
        LineString geometry = new LineString(coordinateSequence, geometryFactory);
        RoutingContext routingContext = new RoutingContext(routingRequest, graph, null, arrive);

        routingContext.serviceDays =
                new ArrayList<ServiceDay>(Collections.singletonList(serviceDay));

        when(graph.getService(TransitIndexService.class)).thenReturn(transitIndexService);
        when(transitIndexService.getTripPatternForTrip(any(AgencyAndId.class)))
                .thenReturn(tableTripPattern);
        when(tableTripPattern.getPatternHops()).thenReturn(Collections.singletonList(patternHop));
        when(routingRequest.getFrom()).thenReturn(new GenericLocation());
        when(tableTripPattern.getTripTimes(anyInt())).thenReturn(tripTimes);
        when(tripTimes.getDepartureTime(anyInt())).thenReturn(0);
        when(tripTimes.getArrivalTime(anyInt())).thenReturn(20);
        when(serviceDay.secondsSinceMidnight(anyInt())).thenReturn(9);
        when(patternHop.getToVertex()).thenReturn(arrive);
        when(patternHop.getGeometry()).thenReturn(geometry);
        when(tripTimes.getHeadsign(anyInt())).thenReturn("The right");

        coordinates = new Coordinate[3];
        coordinates[0] = new Coordinate(3.5, 1.0);
        coordinates[1] = new Coordinate(5.0, 1.0);
        coordinates[2] = new Coordinate(5.0, 5.0);

        coordinateSequence = coordinateSequenceFactory.create(coordinates);
        geometry = new LineString(coordinateSequence, geometryFactory);

        Vertex vertex = onBoardDepartServiceImpl.setupDepartOnBoard(routingContext);
        Edge edge = vertex.getOutgoing().toArray(new Edge[1])[0];

        assertEquals(vertex, edge.getFromVertex());
        assertEquals(arrive, edge.getToVertex());
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

        Stop stop0 = new Stop();
        Stop stop1 = new Stop();

        stop0.setId(new AgencyAndId("Station", "0"));
        stop1.setId(new AgencyAndId("Station", "1"));

        TransitStop station0 = mock(TransitStop.class);
        TransitStop station1 = mock(TransitStop.class);
        PatternArriveVertex arrive = mock(PatternArriveVertex.class);
        PatternHop patternHop = mock(PatternHop.class);
        TripTimes tripTimes = mock(TripTimes.class);
        TableTripPattern tableTripPattern = mock(TableTripPattern.class);
        TransitIndexService transitIndexService = mock(TransitIndexService.class);
        Graph graph = mock(Graph.class);
        RoutingRequest routingRequest = mock(RoutingRequest.class);
        ServiceDay serviceDay = mock(ServiceDay.class);

        GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
        CoordinateSequenceFactory coordinateSequenceFactory =
                geometryFactory.getCoordinateSequenceFactory();
        CoordinateSequence coordinateSequence = coordinateSequenceFactory.create(coordinates);
        LineString geometry = new LineString(coordinateSequence, geometryFactory);
        RoutingContext routingContext = new RoutingContext(routingRequest, graph, null, arrive);

        routingContext.serviceDays =
                new ArrayList<ServiceDay>(Collections.singletonList(serviceDay));

        when(station0.getX()).thenReturn(coordinates[0].x);
        when(station0.getY()).thenReturn(coordinates[0].y);
        when(station1.getX()).thenReturn(coordinates[1].x);
        when(station1.getY()).thenReturn(coordinates[1].y);
        when(graph.getService(TransitIndexService.class)).thenReturn(transitIndexService);
        when(transitIndexService.getTripPatternForTrip(any(AgencyAndId.class)))
                .thenReturn(tableTripPattern);
        when(tableTripPattern.getPatternHops()).thenReturn(Collections.singletonList(patternHop));
        when(routingRequest.getFrom()).thenReturn(new GenericLocation());
        when(tableTripPattern.getTripTimes(anyInt())).thenReturn(tripTimes);
        when(tripTimes.getDepartureTime(anyInt())).thenReturn(0);
        when(tripTimes.getArrivalTime(anyInt())).thenReturn(10);
        when(serviceDay.secondsSinceMidnight(anyInt())).thenReturn(10);
        when(patternHop.getBeginStop()).thenReturn(stop0);
        when(patternHop.getToVertex()).thenReturn(arrive);
        when(patternHop.getGeometry()).thenReturn(geometry);
        when(patternHop.getEndStop()).thenReturn(stop1);
        when(graph.getVertex("Station_0")).thenReturn(station0);
        when(graph.getVertex("Station_1")).thenReturn(station1);

        Vertex vertex = onBoardDepartServiceImpl.setupDepartOnBoard(routingContext);

        assertEquals(coordinates[1].x, vertex.getX(), 0.0);
        assertEquals(coordinates[1].y, vertex.getY(), 0.0);
    }

    @Test
    public final void testOnBoardAtStation() {
        Stop stop0 = new Stop();
        Stop stop1 = new Stop();
        Stop stop2 = new Stop();

        stop0.setId(new AgencyAndId("Station", "0"));
        stop1.setId(new AgencyAndId("Station", "1"));
        stop2.setId(new AgencyAndId("Station", "2"));

        TransitStop station0 = mock(TransitStop.class);
        TransitStop station1 = mock(TransitStop.class);
        TransitStop station2 = mock(TransitStop.class);
        PatternArriveVertex arrive = mock(PatternArriveVertex.class);
        PatternHop patternHop0 = mock(PatternHop.class);
        PatternHop patternHop1 = mock(PatternHop.class);
        TripTimes tripTimes = mock(TripTimes.class);
        TableTripPattern tableTripPattern = mock(TableTripPattern.class);
        TransitIndexService transitIndexService = mock(TransitIndexService.class);
        Graph graph = mock(Graph.class);
        RoutingRequest routingRequest = mock(RoutingRequest.class);
        ServiceDay serviceDay = mock(ServiceDay.class);

        ArrayList<PatternHop> hops = new ArrayList<PatternHop>(2);
        RoutingContext routingContext = new RoutingContext(routingRequest, graph, null, arrive);

        hops.add(patternHop0);
        hops.add(patternHop1);
        routingContext.serviceDays =
                new ArrayList<ServiceDay>(Collections.singletonList(serviceDay));

        when(graph.getService(TransitIndexService.class)).thenReturn(transitIndexService);
        when(transitIndexService.getTripPatternForTrip(any(AgencyAndId.class)))
                .thenReturn(tableTripPattern);
        when(tableTripPattern.getPatternHops()).thenReturn(hops);
        when(routingRequest.getFrom()).thenReturn(new GenericLocation());
        when(tableTripPattern.getTripTimes(anyInt())).thenReturn(tripTimes);
        when(tripTimes.getDepartureTime(0)).thenReturn(0);
        when(tripTimes.getArrivalTime(0)).thenReturn(20);
        when(tripTimes.getDepartureTime(1)).thenReturn(40);
        when(tripTimes.getArrivalTime(1)).thenReturn(60);
        when(patternHop0.getBeginStop()).thenReturn(stop0);
        when(patternHop0.getStopIndex()).thenReturn(0);
        when(patternHop0.getEndStop()).thenReturn(stop1);
        when(patternHop1.getBeginStop()).thenReturn(stop1);
        when(patternHop1.getStopIndex()).thenReturn(1);
        when(patternHop1.getEndStop()).thenReturn(stop2);
        when(graph.getVertex("Station_0")).thenReturn(station0);
        when(graph.getVertex("Station_1")).thenReturn(station1);
        when(graph.getVertex("Station_2")).thenReturn(station2);

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
