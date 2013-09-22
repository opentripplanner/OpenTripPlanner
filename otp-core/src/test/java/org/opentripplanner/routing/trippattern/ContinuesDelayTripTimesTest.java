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


import org.junit.BeforeClass;
import org.junit.Test;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.trippattern.strategy.ContinuesDelayTTUpdater;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.routing.trippattern.TripTimesMocker.*;
import static org.opentripplanner.routing.trippattern.TripTimesMocker.stop_d;
import static org.opentripplanner.routing.trippattern.TripTimesMocker.tripId;

public class ContinuesDelayTripTimesTest{

    private ContinuesDelayTTUpdater cUpdater = new ContinuesDelayTTUpdater();

    private static ScheduledTripTimes scheduledTripTimesA;

    private static TableTripPattern patternA;

    @BeforeClass
    public static void setUp() throws Exception {
        T2<TableTripPattern,ScheduledTripTimes> t2 = mockPatternAndTT();
        scheduledTripTimesA = t2.getSecond();
        patternA = t2.getFirst();
    }

    @Test
    public void testDelayStopsUpdate(){
        TripUpdateList tripUpdateList;

        List<Update> updates = new LinkedList<Update>();
        updates.add(new Update(tripId, stop_c, 2, 30, null, true, Update.Status.PREDICTION, 0, new ServiceDate()));
        updates.add(new Update(tripId, stop_d, 3, 60, 60, true, Update.Status.PREDICTION, 0, new ServiceDate()));
        updates.add(new Update(tripId, stop_f, 5, 0, 0, true, Update.Status.PREDICTION, 0, new ServiceDate()));

        tripUpdateList = TripUpdateList.forUpdatedTrip(tripId, 0, new ServiceDate(), updates);

        TripTimes continuesUpdateA = cUpdater.updateTimes(scheduledTripTimesA,patternA,tripUpdateList);

        //stop a
        assertEquals(TripTimes.PASSED, continuesUpdateA.getDepartureTime(0));
        //stop b
        assertEquals(TripTimes.PASSED, continuesUpdateA.getArrivalTime(0));
        assertEquals(TripTimes.PASSED, continuesUpdateA.getDepartureTime(1));
        //stop c
        assertEquals(scheduledTripTimesA.getArrivalTime(1) + 30, continuesUpdateA.getArrivalTime(1));
        assertEquals(scheduledTripTimesA.getDepartureTime(2) + 30, continuesUpdateA.getDepartureTime(2));
        //stop d
        assertEquals(scheduledTripTimesA.getArrivalTime(2) + 60, continuesUpdateA.getArrivalTime(2));
        assertEquals(scheduledTripTimesA.getDepartureTime(3) + 60, continuesUpdateA.getDepartureTime(3));
        //stop e
        assertEquals(scheduledTripTimesA.getArrivalTime(3) + 60, continuesUpdateA.getArrivalTime(3));
        assertEquals(scheduledTripTimesA.getDepartureTime(4) + 60, continuesUpdateA.getDepartureTime(4));
        //stop f
        assertEquals(scheduledTripTimesA.getArrivalTime(4), continuesUpdateA.getArrivalTime(4));
        assertEquals(scheduledTripTimesA.getDepartureTime(5), continuesUpdateA.getDepartureTime(5));
    }

    @Test
    public void testTimesStopsUpdate(){
        TripUpdateList tripUpdateList;

        List<Update> updates = new LinkedList<Update>();
        updates.add(new Update(tripId, stop_c, 2, 150, 150, false, Update.Status.PREDICTION, 0, new ServiceDate()));
        updates.add(new Update(tripId, stop_d, 3, 240, 240, false, Update.Status.PREDICTION, 0, new ServiceDate()));
        updates.add(new Update(tripId, stop_f, 5, 300, 300, false, Update.Status.PREDICTION, 0, new ServiceDate()));

        tripUpdateList = TripUpdateList.forUpdatedTrip(tripId, 0, new ServiceDate(), updates);

        TripTimes continuesUpdateA = cUpdater.updateTimes(scheduledTripTimesA,patternA,tripUpdateList);

        //stop a
        assertEquals(TripTimes.PASSED, continuesUpdateA.getDepartureTime(0));
        //stop b
        assertEquals(TripTimes.PASSED, continuesUpdateA.getArrivalTime(0));
        assertEquals(TripTimes.PASSED, continuesUpdateA.getDepartureTime(1));
        //stop c
        assertEquals(scheduledTripTimesA.getArrivalTime(1) + 30, continuesUpdateA.getArrivalTime(1));
        assertEquals(scheduledTripTimesA.getDepartureTime(2) + 30, continuesUpdateA.getDepartureTime(2));
        //stop d
        assertEquals(scheduledTripTimesA.getArrivalTime(2) + 60, continuesUpdateA.getArrivalTime(2));
        assertEquals(scheduledTripTimesA.getDepartureTime(3) + 60, continuesUpdateA.getDepartureTime(3));
        //stop e
        assertEquals(scheduledTripTimesA.getArrivalTime(3) + 60, continuesUpdateA.getArrivalTime(3));
        assertEquals(scheduledTripTimesA.getDepartureTime(4) + 60, continuesUpdateA.getDepartureTime(4));
        //stop f
        assertEquals(scheduledTripTimesA.getArrivalTime(4), continuesUpdateA.getArrivalTime(4));
        assertEquals(scheduledTripTimesA.getDepartureTime(5), continuesUpdateA.getDepartureTime(5));
    }


}
