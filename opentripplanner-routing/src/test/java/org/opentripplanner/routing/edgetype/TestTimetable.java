package org.opentripplanner.routing.edgetype;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.trippattern.Update;
import org.opentripplanner.routing.trippattern.UpdateBlock;

public class TestTimetable {

	@Test
	public void testGetNextTrip() {

		ArrayList<Stop> stops = new ArrayList<Stop>();
		for (int i = 0; i < 3; i++) {
			Stop stop = new Stop();
			stop.setId(new AgencyAndId("1", "S{}" + i));
			stops.add(stop);
		}
	    ArrayList<Integer> dropoffs = new ArrayList<Integer>(Arrays.asList(1, 3, 5));
	    ArrayList<Integer> pickups = new ArrayList<Integer>(Arrays.asList(2, 4, 6));

	    RoutingRequest options = new RoutingRequest();
	    AgencyAndId calendarId = new AgencyAndId("1", "42");
	    int serviceId = 23;
	    
	    ScheduledStopPattern pattern = new ScheduledStopPattern(stops, pickups, dropoffs, calendarId);
	    
	    Route route = new Route();
	    route.setId(new AgencyAndId("1", "R1"));

	    Trip exemplar = new Trip();
	    exemplar.setId(new AgencyAndId("1", "T1"));
	    exemplar.setRoute(route);
	    
	    TableTripPattern ttPattern = new TableTripPattern(exemplar, pattern, serviceId);
	    Timetable timetable = new Timetable(ttPattern);

	    // Create three trips to have a set of trips to tests, small enough for the TripTimes to NOT have an index    
        ArrayList<Trip> trips = new ArrayList<Trip>(3);
        for (int i = 0; i < 3; i++) {
        	Trip trip = new Trip();
    	    trip.setId(new AgencyAndId("1", "T{}" + i));
    	    trip.setRoute(route);

    	    ArrayList<StopTime> sTimes = new ArrayList<StopTime>(3);
    	    for (int j = 0; j < 3; j++) {
    	    	StopTime st = new StopTime();
        	    st.setStop(stops.get(j));
        	    st.setArrivalTime(i*10+j*2+1);
        	    st.setDepartureTime(i*10+j*2+2);
        	    st.setTrip(trip);
        	    sTimes.add(st);
			}
    	    
            trips.add(trip);    	    
            timetable.addTrip(trip, sTimes);
        }
            
	    timetable.finish();
	    
	    // Getting the next departure time of the next trip after time 10 should return 14, because of st22.setDepartureTime(14) and no trips have been canceled yet
	    assertEquals(14, timetable.getNextTrip(1, 10, false, options, true).getDepartureTime(1));
	    
	    // Let's cancel trip2 in stop2
	    Update update = new Update(trips.get(1).getId(), stops.get(1).getId().getId(), 1, 13, 14, Update.Status.CANCEL, 32423423);
	    ArrayList<Update> updates = new ArrayList<Update>(Arrays.asList(update));
	    UpdateBlock block = UpdateBlock.splitByTrip(updates).get(0);
	    timetable.update(block);
	    
	    // Getting the next departure time of the next trip after time 10 should return 24, because of st22 (which was departing at 14) has been canceled
	    assertEquals(24, timetable.getNextTrip(1, 10, false, options, true).getDepartureTime(1));
	
	    // If we cancel all three trips, then we should not get any results
	    for (int i = 0; i < trips.size(); i++) {
		    update = new Update(trips.get(i).getId(), stops.get(1).getId().getId(), 1, i*10+3, i*10+4, Update.Status.CANCEL, 32423423);
		    updates = new ArrayList<Update>(Arrays.asList(update));    
		    block = UpdateBlock.splitByTrip(updates).get(0);
		    timetable.update(block);
		}
	    
	    // Check if the result is null while searching forward
	    assertNull(timetable.getNextTrip(1, 10, false, options, true));
	    
	    // Check if the result is null while searching backwards
	    assertNull(timetable.getNextTrip(1, 10, false, options, false));
	    
	    
	    // Create seventeen trips to have a set of trips to tests, large enough for the TripTimes to have an index
	    timetable = new Timetable(ttPattern);
        trips = new ArrayList<Trip>(17);
        for (int i = 0; i < 17; i++) {
        	Trip trip = new Trip();
    	    trip.setId(new AgencyAndId("1", "T{}" + i));
    	    trip.setRoute(route);

    	    ArrayList<StopTime> sTimes = new ArrayList<StopTime>(3);
    	    for (int j = 0; j < 3; j++) {
    	    	StopTime st = new StopTime();
        	    st.setStop(stops.get(j));
        	    st.setArrivalTime(i*10+j*2+1);
        	    st.setDepartureTime(i*10+j*2+2);
        	    st.setTrip(trip);
        	    sTimes.add(st);
			}
    	    
            trips.add(trip);	    
            timetable.addTrip(trip, sTimes);
        }
        
	    timetable.finish();
	    
	    // Getting the next departure time of the next trip after time 10 should return 14, because of st22.setDepartureTime(14) and no trips have been canceled yet
	    assertEquals(14, timetable.getNextTrip(1, 10, false, options, true).getDepartureTime(1));
	    
	    // Let's cancel trip2 in stop2
	    update = new Update(trips.get(1).getId(), stops.get(1).getId().getId(), 1, 13, 14, Update.Status.CANCEL, 32423423);
	    updates = new ArrayList<Update>(Arrays.asList(update));
	    block = UpdateBlock.splitByTrip(updates).get(0);
	    timetable.update(block);
	    
	    // Getting the next departure time of the next trip after time 10 should return 24, because of st22 (which was departing at 14) has been canceled
	    assertEquals(24, timetable.getNextTrip(1, 10, false, options, true).getDepartureTime(1));
	
	    // If we cancel all seventeen trips, then we should not get any results
	    for (int i = 0; i < trips.size(); i++) {
		    update = new Update(trips.get(i).getId(), stops.get(1).getId().getId(), 1, i*10+3, i*10+4, Update.Status.CANCEL, 32423423);
		    updates = new ArrayList<Update>(Arrays.asList(update));    
		    block = UpdateBlock.splitByTrip(updates).get(0);
		    timetable.update(block);
		}
	    
	    // Check if the result is null while searching forward
	    assertNull(timetable.getNextTrip(1, 10, false, options, true));
	    
	    // Check if the result is null while searching backwards
	    assertNull(timetable.getNextTrip(1, 10, false, options, false));
	}
}