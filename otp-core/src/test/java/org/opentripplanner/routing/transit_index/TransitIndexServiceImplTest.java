package org.opentripplanner.routing.transit_index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

public class TransitIndexServiceImplTest {
    @Test
    public final void testRoutesForStop() {
        AgencyAndId arrive = new AgencyAndId("Agency", "Arrive");
        AgencyAndId depart = new AgencyAndId("Agency", "Depart");
        Route route = new Route();
        Trip trip = new Trip();

        trip.setRoute(route);

        PreAlightEdge preAlightEdge = mock(PreAlightEdge.class);
        PreBoardEdge preBoardEdge = mock(PreBoardEdge.class);
        Vertex alightVertex = mock(Vertex.class);
        Vertex boardVertex = mock(Vertex.class);
        TransitBoardAlight alightEdge = mock(TransitBoardAlight.class);
        TransitBoardAlight boardEdge = mock(TransitBoardAlight.class);
        TableTripPattern tableTripPattern = mock(TableTripPattern.class);

        when(preAlightEdge.getFromVertex()).thenReturn(alightVertex);
        when(preBoardEdge.getToVertex()).thenReturn(boardVertex);
        when(alightVertex.getIncoming()).thenReturn(Collections.singleton((Edge) alightEdge));
        when(boardVertex.getOutgoing()).thenReturn(Collections.singleton((Edge) boardEdge));
        when(alightEdge.isBoarding()).thenReturn(false);
        when(alightEdge.getPattern()).thenReturn(tableTripPattern);
        when(boardEdge.isBoarding()).thenReturn(true);
        when(boardEdge.getPattern()).thenReturn(tableTripPattern);
        when(tableTripPattern.getExemplar()).thenReturn(trip);

        HashMap<AgencyAndId, PreAlightEdge> preAlightEdges =
                new HashMap<AgencyAndId, PreAlightEdge>(1, 1);
        HashMap<AgencyAndId, PreBoardEdge> preBoardEdges =
                new HashMap<AgencyAndId, PreBoardEdge>(1, 1);

        preAlightEdges.put(arrive, preAlightEdge);
        preBoardEdges.put(depart, preBoardEdge);

        TransitIndexServiceImpl transitIndexServiceImpl = new TransitIndexServiceImpl(null,
                null, null,null, preBoardEdges, preAlightEdges, null, null, null, null, null, null);
        List<AgencyAndId> arrivals = transitIndexServiceImpl.getRoutesForStop(arrive);
        List<AgencyAndId> departures = transitIndexServiceImpl.getRoutesForStop(depart);

        assertNull(departures.get(0));
        assertEquals(1, departures.size());
        assertEquals(1, arrivals.size());
        assertNull(arrivals.get(0));
    }

    @Test
    public final void testStopsForStation() {
        AgencyAndId station0 = new AgencyAndId("Agency", "Station 0");
        AgencyAndId station1 = new AgencyAndId("Agency", "Station 1");
        Stop stop0 = new Stop();
        Stop stop1 = new Stop();
        Stop stop2 = new Stop();
        ArrayList<Stop> stops0 = new ArrayList<Stop>(1);
        ArrayList<Stop> stops1 = new ArrayList<Stop>(2);
        HashMap<AgencyAndId, List<Stop>> hashMap = new HashMap<AgencyAndId, List<Stop>>(2, 1);
        TransitIndexServiceImpl transitIndexServiceImpl = new TransitIndexServiceImpl(hashMap, null,
                null, null, null, null, null, null, null, null, null, null);

        stop0.setId(new AgencyAndId("Agency", "Stop 0"));
        stop1.setId(new AgencyAndId("Agency", "Stop 1"));
        stop2.setId(new AgencyAndId("Agency", "Stop 2"));
        stops0.add(stop0);
        stops1.add(stop1);
        stops1.add(stop2);
        hashMap.put(station0, stops0);
        hashMap.put(station1, stops1);

        List<Stop> empty = transitIndexServiceImpl.getStopsForStation(null);
        assertEquals(0, empty.size());

        List<Stop> oneStop = transitIndexServiceImpl.getStopsForStation(station0);
        assertEquals(1, oneStop.size());
        assertEquals(stop0, oneStop.get(0));

        List<Stop> twoStops = transitIndexServiceImpl.getStopsForStation(station1);
        assertEquals(2, twoStops.size());
        assertEquals(stop1, twoStops.get(0));
        assertEquals(stop2, twoStops.get(1));
    }
}
