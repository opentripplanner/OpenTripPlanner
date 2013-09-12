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
import org.opentripplanner.routing.vertextype.PatternDepartVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFactory;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class OnBoardDepartServiceImplTest {
    @Test
    public final void testOnBoardDepartureTime() {
        Coordinate[] coordinates = new Coordinate[5];
        coordinates[0] = new Coordinate(0.0, 0.0);
        coordinates[1] = new Coordinate(0.0, 1.0);
        coordinates[2] = new Coordinate(2.0, 1.0);
        coordinates[3] = new Coordinate(5.0, 1.0);
        coordinates[4] = new Coordinate(5.0, 5.0);

        PatternDepartVertex depart = mock(PatternDepartVertex.class);
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
        RoutingContext routingContext = new RoutingContext(routingRequest, graph, depart, arrive);

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

        Vertex vertex = new OnBoardDepartServiceImpl().setupDepartOnBoard(routingContext);
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

        PatternDepartVertex depart = mock(PatternDepartVertex.class);
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
        RoutingContext routingContext = new RoutingContext(routingRequest, graph, depart, arrive);

        routingContext.serviceDays =
                new ArrayList<ServiceDay>(Collections.singletonList(serviceDay));

        when(graph.getService(TransitIndexService.class)).thenReturn(transitIndexService);
        when(transitIndexService.getTripPatternForTrip(any(AgencyAndId.class)))
                .thenReturn(tableTripPattern);
        when(tableTripPattern.getPatternHops()).thenReturn(Collections.singletonList(patternHop));
        when(routingRequest.getFrom()).thenReturn(new GenericLocation());
        when(tableTripPattern.getTripTimes(anyInt())).thenReturn(tripTimes);
        when(tripTimes.getDepartureTime(anyInt())).thenReturn(0);
        when(tripTimes.getArrivalTime(anyInt())).thenReturn(10);
        when(serviceDay.secondsSinceMidnight(anyInt())).thenReturn(10);
        when(patternHop.getToVertex()).thenReturn(arrive);
        when(patternHop.getGeometry()).thenReturn(geometry);

        Vertex vertex = new OnBoardDepartServiceImpl().setupDepartOnBoard(routingContext);

        assertEquals(coordinates[1].x, vertex.getX(), 0.0);
        assertEquals(coordinates[1].y, vertex.getY(), 0.0);
    }
}
