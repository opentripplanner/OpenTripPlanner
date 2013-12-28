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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.routing.edgetype.PatternDwell;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.ScheduledStopPattern;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;

import java.util.LinkedList;
import java.util.List;

public class TripTimesMocker {


    public static AgencyAndId tripId = new AgencyAndId("agency", "testtrip");

    public static AgencyAndId stop_a = new AgencyAndId("agency", "A"); // 0
    public static AgencyAndId stop_b = new AgencyAndId("agency", "B"); // 1
    public static AgencyAndId stop_c = new AgencyAndId("agency", "C"); // 2
    public static AgencyAndId stop_d = new AgencyAndId("agency", "D"); // 3
    public static AgencyAndId stop_e = new AgencyAndId("agency", "E"); // 4
    public static AgencyAndId stop_f = new AgencyAndId("agency", "F"); // 5
    public static AgencyAndId stop_g = new AgencyAndId("agency", "G"); // 6
    public static AgencyAndId stop_h = new AgencyAndId("agency", "H"); // 7
    public static AgencyAndId[] stops = {stop_a, stop_b, stop_c, stop_d, stop_e, stop_f, stop_g, stop_h};

    private static Trip mockTrip(){
        Trip trip = new Trip();
        trip.setId(tripId);
        return trip;
    }

    private static List<StopTime> mockStopTimeSimpleList(){
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
        return stopTimes;
    }

    public static T2<TableTripPattern,ScheduledTripTimes>  mockPatternAndTT(){
        List<StopTime> stopTimes = mockStopTimeSimpleList();
        Trip trip = mockTrip();
        ScheduledStopPattern stopPattern = ScheduledStopPattern.fromTrip(trip, stopTimes);
        TableTripPattern ttPattern = new TableTripPattern(trip, stopPattern, 0);
        for(int i = 0; i < stopTimes.size() - 1; i++){
            StopTime startStopTime = stopTimes.get(i);
            StopTime endStopTime = stopTimes.get(i +1);
            PatternDepartVertex departV = new PatternDepartVertex(null, ttPattern, startStopTime);
            PatternArriveVertex arriveV = new PatternArriveVertex(null, ttPattern, endStopTime);
            //the assign of the hop to the pattern is in the hop contractor
            new PatternHop(departV, arriveV, startStopTime.getStop(), endStopTime.getStop(), i);
        }
        return new T2<TableTripPattern, ScheduledTripTimes>
                (ttPattern, new ScheduledTripTimes(trip, stopTimes));
    }

    public static ScheduledTripTimes mockSimpleTrip() throws Exception {

        return new ScheduledTripTimes(mockTrip(), mockStopTimeSimpleList());

//       stopTimes = new LinkedList<StopTime>();
//       for(int i =  0; i < stops.length; ++i) {
//           StopTime stopTime = new StopTime();
//
//           Stop stop = new Stop();
//           stop.setId(stops[i]);
//           stopTime.setStop(stop);
//           stopTime.setStopSequence(i);
//           stopTime.setArrivalTime(i * 60);
//           stopTime.setDepartureTime(i * 60 + (i > 3 && i < 6 ? i * 10 : 0));
//           stopTimes.add(stopTime);
//       }
//       new ScheduledTripTimes(trip, stopTimes);
    }



}
