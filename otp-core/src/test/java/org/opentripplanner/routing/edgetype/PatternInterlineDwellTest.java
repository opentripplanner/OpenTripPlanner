package org.opentripplanner.routing.edgetype;

import static org.junit.Assert.assertNotSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.pathparser.PathParser;
import org.opentripplanner.routing.request.BannedStopSet;
import org.opentripplanner.routing.vertextype.OnboardVertex;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;

public class PatternInterlineDwellTest {
    @Test
    public void testResolver() {
        OnboardVertex startJourney = mock(OnboardVertex.class);
        OnboardVertex endJourney = mock(OnboardVertex.class);
        RoutingRequest routingRequest = mock(RoutingRequest.class);
        ServiceDay serviceDay = mock(ServiceDay.class);
        ServiceDate serviceDate = mock(ServiceDate.class);
        Graph graph = mock(Graph.class);
        TimetableSnapshotSource timetableSnapshotSource = mock(TimetableSnapshotSource.class);

        Route route = new Route();

        Stop stopDepart = new Stop();
        Stop stopInterline = new Stop();
        Stop stopArrive = new Stop();

        ArrayList<Stop> firstStops = new ArrayList<Stop>();
        ArrayList<Stop> secondStops = new ArrayList<Stop>();

        firstStops.add(stopDepart);
        firstStops.add(stopInterline);
        secondStops.add(stopInterline);
        secondStops.add(stopArrive);

        final Trip firstTrip = new Trip();
        final Trip secondTrip = new Trip();

        firstTrip.setRoute(route);
        secondTrip.setRoute(route);

        StopTime stopDepartTime = new StopTime();
        StopTime stopInterlineFirstTime = new StopTime();
        StopTime stopInterlineSecondTime = new StopTime();
        StopTime stopArriveTime = new StopTime();

        stopDepartTime.setStop(stopDepart);
        stopInterlineFirstTime.setStop(stopInterline);
        stopInterlineSecondTime.setStop(stopInterline);
        stopArriveTime.setStop(stopArrive);

        ArrayList<StopTime> firstStopTimes = new ArrayList<StopTime>();
        ArrayList<StopTime> secondStopTimes = new ArrayList<StopTime>();

        firstStopTimes.add(stopDepartTime);
        firstStopTimes.add(stopInterlineFirstTime);
        secondStopTimes.add(stopInterlineSecondTime);
        secondStopTimes.add(stopArriveTime);

        ScheduledStopPattern firstStopPattern = ScheduledStopPattern.fromTrip(
                firstTrip, firstStopTimes);
        ScheduledStopPattern secondStopPattern = ScheduledStopPattern.fromTrip(
                secondTrip, secondStopTimes);

        TableTripPattern firstTripPattern = new TableTripPattern(firstTrip, firstStopPattern, 0);
        TableTripPattern secondTripPattern = new TableTripPattern(secondTrip, secondStopPattern, 1);

        firstTripPattern.addTrip(firstTrip, firstStopTimes);
        secondTripPattern.addTrip(secondTrip, secondStopTimes);

        HashMap<AgencyAndId, BannedStopSet> bannedTrips = new HashMap<AgencyAndId, BannedStopSet>();

        TimetableResolver resolver = new TimetableResolver() {
            @Override
            public Timetable resolve(TableTripPattern pattern, ServiceDate serviceDate) {
                Trip trip = new Trip();
                StopTime stopTime = new StopTime();
                ArrayList<StopTime> stopTimes = new ArrayList<StopTime>();
                stopTimes.add(stopTime);
                stopTimes.add(stopTime);
                Timetable timetable = new Timetable(pattern);
                timetable.addTrip(trip, stopTimes);
                return timetable;
            }
        };

        when(routingRequest.getModes()).thenReturn(TraverseModeSet.allModes());
        when(endJourney.getTripPattern()).thenReturn(secondTripPattern);
        when(serviceDay.getServiceDate()).thenReturn(serviceDate);
        when(graph.getTimetableSnapshotSource()).thenReturn(timetableSnapshotSource);
        when(timetableSnapshotSource.getTimetableSnapshot()).thenReturn(resolver);

        RoutingContext routingContext =
                new RoutingContext(routingRequest, graph, startJourney, endJourney);

        routingContext.serviceDays = new ArrayList<ServiceDay>(1);
        routingContext.serviceDays.add(serviceDay);
        routingContext.pathParsers = new PathParser[0];

        routingRequest.rctx = routingContext;
        routingRequest.bannedTrips = bannedTrips;

        PatternInterlineDwell patternInterlineDwell =
                new PatternInterlineDwell(startJourney, endJourney, secondTrip);

        patternInterlineDwell.addTrip(firstTrip, secondTrip, 0, 0, 0);

        StateEditor stateEditor = new StateEditor(routingRequest, startJourney);

        stateEditor.setServiceDay(serviceDay);
        stateEditor.setTripTimes(firstTripPattern.getTripTimes(0));

        State s0 = stateEditor.makeState();
        State s1 = patternInterlineDwell.traverse(s0);
        State s2 = patternInterlineDwell.traverse(s0);

        assertNotSame(s0.getTripTimes(), s1.getTripTimes());
        assertNotSame(s0.getTripTimes(), s2.getTripTimes());
        assertNotSame(s1.getTripTimes(), s2.getTripTimes());
    }
}
