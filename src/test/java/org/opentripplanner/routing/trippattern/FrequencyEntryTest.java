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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Frequency;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;

public class FrequencyEntryTest {
    private static final AgencyAndId tripId = new AgencyAndId("agency", "testtrip");

    private static final AgencyAndId stop_a = new AgencyAndId("agency", "A"); // 0
    private static final AgencyAndId stop_b = new AgencyAndId("agency", "B"); // 1
    private static final AgencyAndId stop_c = new AgencyAndId("agency", "C"); // 2
    private static final AgencyAndId stop_d = new AgencyAndId("agency", "D"); // 3
    private static final AgencyAndId stop_e = new AgencyAndId("agency", "E"); // 4
    private static final AgencyAndId stop_f = new AgencyAndId("agency", "F"); // 5
    private static final AgencyAndId stop_g = new AgencyAndId("agency", "G"); // 6
    private static final AgencyAndId stop_h = new AgencyAndId("agency", "H"); // 7

    private static final AgencyAndId[] stops =
        {stop_a, stop_b, stop_c, stop_d, stop_e, stop_f, stop_g, stop_h};

    private static final TripTimes tripTimes;

    static {
        Trip trip = new Trip();
        trip.setId(tripId);

        List<StopTime> stopTimes = new LinkedList<StopTime>();

        int time = 0;
        for(int i = 0; i < stops.length; ++i) {
            StopTime stopTime = new StopTime();

            Stop stop = new Stop();
            stop.setId(stops[i]);
            stopTime.setStop(stop);
            stopTime.setArrivalTime(time);
            if(i != 0 && i != stops.length - 1) {
                time += 10;
            }
            stopTime.setDepartureTime(time);
            time += 50;
            stopTime.setStopSequence(i);
            stopTimes.add(stopTime);
        }

        tripTimes = new TripTimes(trip, stopTimes, new Deduplicator());
    }

    @Test
    public void testInexactTimes() {
        Frequency f = new Frequency();
        f.setStartTime(100000);
        f.setEndTime(150000);
        f.setHeadwaySecs(100);
        f.setExactTimes(0);

        FrequencyEntry freqEntry = new FrequencyEntry(f, tripTimes);

        // 1st stop 00:00:00, 00:00:00
        // 6th stop 00:04:50, 00:05:00
        // 8th stop 00:06:50, 06:50:00

        // testing first trip departure
        assertEquals(100000, freqEntry.nextDepartureTime(0, 90000));  // first stop, before begin
        assertEquals(100100, freqEntry.nextDepartureTime(0, 100000)); // first stop, on begin
        assertEquals(100300, freqEntry.nextDepartureTime(5, 100000)); // 6th stop, before begin
        assertEquals(100150, freqEntry.nextDepartureTime(0, 100050)); // first stop, after begin
        assertEquals(100350, freqEntry.nextDepartureTime(5, 100250)); // 6th stop, after begin

        // testing last trip departure
        assertEquals(150000, freqEntry.nextDepartureTime(0, 149900)); // first stop, before end
        assertEquals(150000, freqEntry.nextDepartureTime(0, 150000)); // first stop, on end
        assertEquals(-1,     freqEntry.nextDepartureTime(0, 150200)); // first stop, after end
        assertEquals(150300, freqEntry.nextDepartureTime(5, 150200)); // 6th stop, before end
        assertEquals(150300, freqEntry.nextDepartureTime(5, 150300)); // 6th stop, on end
        assertEquals(-1,     freqEntry.nextDepartureTime(5, 150400)); // 6th stop, after end

        // testing first trip arrival
        assertEquals(-1,     freqEntry.prevArrivalTime(5,  90000)); // 6th stop, before begin
        assertEquals(100290, freqEntry.prevArrivalTime(5, 100290)); // 6th stop, on begin
        assertEquals(100290, freqEntry.prevArrivalTime(5, 100390)); // 6th stop, after begin
        assertEquals(100350, freqEntry.prevArrivalTime(5, 100450)); // 6th stop, after begin
        assertEquals(-1,     freqEntry.prevArrivalTime(7, 100400)); // 8th stop, before begin
        assertEquals(100410, freqEntry.prevArrivalTime(7, 100500)); // 8th stop, after begin

        // testing last trip arrival
        assertEquals(150290, freqEntry.prevArrivalTime(5, 150390)); // 6th stop
        assertEquals(150310, freqEntry.prevArrivalTime(7, 150410)); // 8th stop, on end
        assertEquals(150410, freqEntry.prevArrivalTime(7, 151000)); // 8th stop, after end
    }

    @Test
    public void testExactTimes() {
        Frequency f = new Frequency();
        f.setStartTime(100000);
        f.setEndTime(150000);
        f.setHeadwaySecs(100);
        f.setExactTimes(1);

        FrequencyEntry freqEntry = new FrequencyEntry(f, tripTimes);

        // testing first trip departure
        assertEquals(100000, freqEntry.nextDepartureTime(0, 90000));  // first stop, before begin
        assertEquals(100000, freqEntry.nextDepartureTime(0, 100000)); // first stop, on begin
        assertEquals(100300, freqEntry.nextDepartureTime(5, 100000)); // 6th stop, before begin
        assertEquals(100100, freqEntry.nextDepartureTime(0, 100050)); // first stop, after begin
        assertEquals(100300, freqEntry.nextDepartureTime(5, 100250)); // 6th stop, after begin

        // testing last trip departure
        assertEquals(149900, freqEntry.nextDepartureTime(0, 149900)); // first stop, on end
        assertEquals(-1,     freqEntry.nextDepartureTime(0, 150000)); // first stop, after end
        assertEquals(-1,     freqEntry.nextDepartureTime(0, 150200)); // first stop, after end
        assertEquals(150200, freqEntry.nextDepartureTime(5, 150200)); // 6th stop, on end
        assertEquals(-1,     freqEntry.nextDepartureTime(5, 150300)); // 6th stop, after end
        assertEquals(-1,     freqEntry.nextDepartureTime(5, 150400)); // 6th stop, after end

        // testing first trip arrival
        assertEquals(-1,     freqEntry.prevArrivalTime(5,  90000)); // 6th stop, before begin
        assertEquals(100290, freqEntry.prevArrivalTime(5, 100290)); // 6th stop, on begin
        assertEquals(100390, freqEntry.prevArrivalTime(5, 100390)); // 6th stop, after begin
        assertEquals(100390, freqEntry.prevArrivalTime(5, 100450)); // 6th stop, after begin
        assertEquals(-1,     freqEntry.prevArrivalTime(7, 100400)); // 8th stop, before begin
        assertEquals(100410, freqEntry.prevArrivalTime(7, 100500)); // 8th stop, after begin

        // testing last trip arrival
        assertEquals(150190, freqEntry.prevArrivalTime(5, 150390)); // 6th stop
        assertEquals(150310, freqEntry.prevArrivalTime(7, 150410)); // 8th stop, on end
        assertEquals(150310, freqEntry.prevArrivalTime(7, 151000)); // 8th stop, after end
    }

    @Test
    public void testExactTimesWithExtra() {
        // test if end - start is not a multiple of headway
        Frequency f = new Frequency();
        f.setStartTime(100000);
        f.setEndTime(110050);
        f.setHeadwaySecs(100);
        f.setExactTimes(1);

        FrequencyEntry freqEntry = new FrequencyEntry(f, tripTimes);

        // check last departure time is correct
        assertEquals(110000, freqEntry.nextDepartureTime(0, 109950));
        assertEquals(110000, freqEntry.nextDepartureTime(0, 110000));
        assertEquals(-1,     freqEntry.nextDepartureTime(0, 110050));

        // check last arrival time is correct
        assertEquals(110290, freqEntry.prevArrivalTime(5, 110300));
        assertEquals(110410, freqEntry.prevArrivalTime(7, 110410));
        assertEquals(110410, freqEntry.prevArrivalTime(7, 110500));
    }

}
