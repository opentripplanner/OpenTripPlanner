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

package org.opentripplanner.routing.trippattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;

public class UpdatedTripTimesTest {
    private static ScheduledTripTimes originalTripTimesA;
    private static AgencyAndId tripId = new AgencyAndId("agency", "testtrip");
    
    private static AgencyAndId stop_a = new AgencyAndId("agency", "A"); // 0
    private static AgencyAndId stop_b = new AgencyAndId("agency", "B"); // 1
    private static AgencyAndId stop_c = new AgencyAndId("agency", "C"); // 2
    private static AgencyAndId stop_d = new AgencyAndId("agency", "D"); // 3
    private static AgencyAndId stop_e = new AgencyAndId("agency", "E"); // 4
    private static AgencyAndId stop_f = new AgencyAndId("agency", "F"); // 5
    private static AgencyAndId stop_g = new AgencyAndId("agency", "G"); // 6
    private static AgencyAndId stop_h = new AgencyAndId("agency", "H"); // 7
    private static AgencyAndId[] stops = {stop_a, stop_b, stop_c, stop_d, stop_e, stop_f, stop_g, stop_h};
    
    @BeforeClass
    public static void setUp() throws Exception {
        Trip trip = new Trip();
        trip.setId(tripId);
        
        List<StopTime> stopTimes = new LinkedList<StopTime>();
        
        for(int i =  0; i < stops.length; ++i) {
            StopTime stopTime = new StopTime();
            
            Stop stop = new Stop();
            stop.setId(stops[i]);
            stopTime.setStop(stop);
            stopTime.setArrivalTime(i * 60);
            stopTime.setDepartureTime(i * 60);
            stopTimes.add(stopTime);
        }
        originalTripTimesA = new ScheduledTripTimes(trip, stopTimes);
        
        stopTimes = new LinkedList<StopTime>();
        for(int i =  0; i < stops.length; ++i) {
            StopTime stopTime = new StopTime();
            
            Stop stop = new Stop();
            stop.setId(stops[i]);
            stopTime.setStop(stop);
            stopTime.setStopSequence(i);
            stopTime.setArrivalTime(i * 60); 
            stopTime.setDepartureTime(i * 60 + (i > 3 && i < 6 ? i * 10 : 0));
            stopTimes.add(stopTime);
        }
        new ScheduledTripTimes(trip, stopTimes);
    }

    @Test
    public void testStopCancellingUpdate() {
        TripUpdateList tripUpdateList;
        
        List<Update> updates = new LinkedList<Update>();
        updates.add(new Update(tripId, stop_a, 0, 0, 0, Update.Status.PLANNED, 0, new ServiceDate()));
        updates.add(new Update(tripId, stop_b, 1, 0, 0, Update.Status.PLANNED, 0, new ServiceDate()));
        updates.add(new Update(tripId, stop_c, 2, 0, 0, Update.Status.CANCEL , 0, new ServiceDate()));
        updates.add(new Update(tripId, stop_d, 3, 0, 0, Update.Status.CANCEL , 0, new ServiceDate()));
        
        tripUpdateList = TripUpdateList.forUpdatedTrip(tripId, 0, new ServiceDate(), updates);
        
        UpdatedTripTimes updateTriptimesA = new UpdatedTripTimes(originalTripTimesA, tripUpdateList, 0);

        assertTrue(updateTriptimesA.timesIncreasing());
        
        assertEquals(1 * 60            , updateTriptimesA.getDepartureTime(1));
        assertEquals(TripTimes.CANCELED, updateTriptimesA.getDepartureTime(2));
        assertEquals(TripTimes.CANCELED, updateTriptimesA.getDepartureTime(3));
        assertEquals(4 * 60            , updateTriptimesA.getDepartureTime(4));

        assertEquals(1 * 60            , updateTriptimesA.getArrivalTime(0));
        assertEquals(TripTimes.CANCELED, updateTriptimesA.getArrivalTime(1));
        assertEquals(TripTimes.CANCELED, updateTriptimesA.getArrivalTime(2));
        assertEquals(4 * 60            , updateTriptimesA.getArrivalTime(3));

        assertEquals( 60, updateTriptimesA.getRunningTime(0));
        assertEquals(  0, updateTriptimesA.getRunningTime(1));
        assertEquals(  0, updateTriptimesA.getRunningTime(2));
        assertEquals(180, updateTriptimesA.getRunningTime(3));
        assertEquals( 60, updateTriptimesA.getRunningTime(4));
    }

    @Test
    public void testStopUpdate() {
        TripUpdateList tripUpdateList;
        
        List<Update> updates = new LinkedList<Update>();
        updates.add(new Update(tripId, stop_d, 3, 190, 190, Update.Status.PREDICTION , 0, new ServiceDate()));
        
        tripUpdateList = TripUpdateList.forUpdatedTrip(tripId, 0, new ServiceDate(), updates);
        
        UpdatedTripTimes updateTriptimesA = new UpdatedTripTimes(originalTripTimesA, tripUpdateList, 3);

        assertEquals(TripTimes.PASSED, updateTriptimesA.getDepartureTime(2));
        assertEquals(3 * 60 + 10, updateTriptimesA.getDepartureTime(3));
        assertEquals(4 * 60     , updateTriptimesA.getDepartureTime(4));

        assertEquals(TripTimes.PASSED, updateTriptimesA.getArrivalTime(1));
        assertEquals(3 * 60 + 10, updateTriptimesA.getArrivalTime(2));
        assertEquals(4 * 60     , updateTriptimesA.getArrivalTime(3));
    }

    @Test
    public void testPassedUpdate() {
        TripUpdateList tripUpdateList;
        
        List<Update> updates = new LinkedList<Update>();
        updates.add(new Update(tripId, stop_a, 0, 0, 0, Update.Status.PASSED, 0, new ServiceDate()));
        
        tripUpdateList = TripUpdateList.forUpdatedTrip(tripId, 0, new ServiceDate(), updates);
        
        UpdatedTripTimes updateTriptimesA = new UpdatedTripTimes(originalTripTimesA, tripUpdateList, 0);

        assertEquals(TripTimes.PASSED, updateTriptimesA.getDepartureTime(0));
        assertEquals(60, updateTriptimesA.getArrivalTime(0));
    }
}
