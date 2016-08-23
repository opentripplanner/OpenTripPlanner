package org.opentripplanner.updater;

import com.google.transit.realtime.GtfsRealtime;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.GtfsTest;
import uk.org.siri.siri20.CourseOfJourneyRefStructure;
import uk.org.siri.siri20.VehicleActivityStructure;

import java.util.Map;
import java.util.Set;

public class SiriFuzzyTripMatcherTest extends GtfsTest {

    public void testMatch() throws Exception {
        String feedId = graph.getFeedIds().iterator().next();

        SiriFuzzyTripMatcher matcher = new SiriFuzzyTripMatcher(graph.index);

        Set<Map.Entry<AgencyAndId, Trip>> entries = graph.index.tripForId.entrySet();


        VehicleActivityStructure.MonitoredVehicleJourney monitoredVehicleJourney = new VehicleActivityStructure.MonitoredVehicleJourney();
        CourseOfJourneyRefStructure courseOfJourney = new CourseOfJourneyRefStructure();
        courseOfJourney.setValue("10W1020");
        monitoredVehicleJourney.setCourseOfJourneyRef(courseOfJourney);


        VehicleActivityStructure activity = new VehicleActivityStructure();
        activity.setMonitoredVehicleJourney(monitoredVehicleJourney);
        Set<Trip> match = matcher.match(activity);

        assertTrue(match.size() == 1);

        Trip trip = match.iterator().next();
        assertNotNull(trip);
        assertTrue(trip.getId().getId().equals(courseOfJourney.getValue()));

    }

    public void testNoMatch() throws Exception {
        String feedId = graph.getFeedIds().iterator().next();

        SiriFuzzyTripMatcher matcher = new SiriFuzzyTripMatcher(graph.index);

        Set<Map.Entry<AgencyAndId, Trip>> entries = graph.index.tripForId.entrySet();


        VehicleActivityStructure.MonitoredVehicleJourney monitoredVehicleJourney = new VehicleActivityStructure.MonitoredVehicleJourney();
        CourseOfJourneyRefStructure courseOfJourney = new CourseOfJourneyRefStructure();
        courseOfJourney.setValue("FOOBAR");
        monitoredVehicleJourney.setCourseOfJourneyRef(courseOfJourney);


        VehicleActivityStructure activity = new VehicleActivityStructure();
        activity.setMonitoredVehicleJourney(monitoredVehicleJourney);
        Set<Trip> match = matcher.match(activity);

        assertTrue(match == null || match.isEmpty());

    }

    @Override
    public String getFeedName() {
        return "google_transit.zip";
    }
}
