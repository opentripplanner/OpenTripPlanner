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

package org.opentripplanner.util;

import org.junit.Test;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Created by mabu on 28.7.2015.
 */
public class TravelOptionsMakerTest {

    @Test
    public void testMakeOptions() throws Exception {
        boolean hasParkRide = false;
        boolean hasBikeRide = false;
        boolean hasBikeShare = false;

        HashSet<TraverseMode> transitModes = new HashSet<>();
        transitModes.add(TraverseMode.BUS);

        List<TravelOption> options = TravelOptionsMaker.makeOptions(transitModes, hasBikeShare, hasBikeRide, hasParkRide);

        Set<TravelOption> expected = new HashSet<>();
        expected.add(new TravelOption("TRANSIT,WALK", "TRANSIT"));
        expected.add(new TravelOption("BUS,WALK", "BUS"));
        expected.add(new TravelOption("WALK", "WALK"));
        expected.add(new TravelOption("BICYCLE", "BICYCLE"));
        expected.add(new TravelOption("CAR", "CAR"));
        expected.add(new TravelOption("TRANSIT,BICYCLE", "TRANSIT_BICYCLE"));
        expected.add(new TravelOption("CAR,WALK,TRANSIT", "KISSRIDE"));
        assertEquals(expected, new HashSet<>(options));

        transitModes.add(TraverseMode.RAIL);

        hasBikeRide = true;

        options = TravelOptionsMaker.makeOptions(transitModes, hasBikeShare, hasBikeRide, hasParkRide);
        expected = new HashSet<>();
        expected.add(new TravelOption("TRANSIT,WALK", "TRANSIT"));
        expected.add(new TravelOption("BUS,WALK", "BUS"));
        expected.add(new TravelOption("RAIL,WALK", "RAIL"));
        expected.add(new TravelOption("WALK", "WALK"));
        expected.add(new TravelOption("BICYCLE", "BICYCLE"));
        expected.add(new TravelOption("CAR", "CAR"));
        expected.add(new TravelOption("TRANSIT,BICYCLE", "TRANSIT_BICYCLE"));
        expected.add(new TravelOption("BICYCLE_PARK,WALK,TRANSIT", "BIKERIDE"));
        expected.add(new TravelOption("CAR,WALK,TRANSIT", "KISSRIDE"));

        assertEquals(expected, new HashSet<>(options));

        hasBikeRide = false;

        hasBikeShare = true;

        options = TravelOptionsMaker.makeOptions(transitModes, hasBikeShare, hasBikeRide, hasParkRide);
        expected = new HashSet<>();
        expected.add(new TravelOption("TRANSIT,WALK", "TRANSIT"));
        expected.add(new TravelOption("BUS,WALK", "BUS"));
        expected.add(new TravelOption("RAIL,WALK", "RAIL"));
        expected.add(new TravelOption("WALK", "WALK"));
        expected.add(new TravelOption("BICYCLE", "BICYCLE"));
        expected.add(new TravelOption("CAR", "CAR"));
        expected.add(new TravelOption("WALK,BICYCLE_RENT", "BICYCLERENT"));
        expected.add(new TravelOption("TRANSIT,BICYCLE", "TRANSIT_BICYCLE"));
        expected.add(new TravelOption("TRANSIT,WALK,BICYCLE_RENT", "TRANSIT_BICYCLERENT"));
        expected.add(new TravelOption("CAR,WALK,TRANSIT", "KISSRIDE"));

        assertEquals(expected, new HashSet<>(options));

        hasBikeShare = false;
        hasParkRide = true;

        options = TravelOptionsMaker.makeOptions(transitModes, hasBikeShare, hasBikeRide, hasParkRide);
        expected = new HashSet<>();
        expected.add(new TravelOption("TRANSIT,WALK", "TRANSIT"));
        expected.add(new TravelOption("BUS,WALK", "BUS"));
        expected.add(new TravelOption("RAIL,WALK", "RAIL"));
        expected.add(new TravelOption("WALK", "WALK"));
        expected.add(new TravelOption("BICYCLE", "BICYCLE"));
        expected.add(new TravelOption("CAR", "CAR"));
        expected.add(new TravelOption("TRANSIT,BICYCLE", "TRANSIT_BICYCLE"));
        expected.add(new TravelOption("CAR_PARK,WALK,TRANSIT", "PARKRIDE"));
        expected.add(new TravelOption("CAR,WALK,TRANSIT", "KISSRIDE"));

        assertEquals(expected, new HashSet<>(options));

        hasBikeShare = true;
        hasParkRide = true;
        hasBikeRide = true;

        options = TravelOptionsMaker.makeOptions(transitModes, hasBikeShare, hasBikeRide, hasParkRide);
        expected = new HashSet<>();
        expected.add(new TravelOption("TRANSIT,WALK", "TRANSIT"));
        expected.add(new TravelOption("BUS,WALK", "BUS"));
        expected.add(new TravelOption("RAIL,WALK", "RAIL"));
        expected.add(new TravelOption("WALK", "WALK"));
        expected.add(new TravelOption("BICYCLE", "BICYCLE"));
        expected.add(new TravelOption("CAR", "CAR"));
        expected.add(new TravelOption("WALK,BICYCLE_RENT", "BICYCLERENT"));
        expected.add(new TravelOption("TRANSIT,BICYCLE", "TRANSIT_BICYCLE"));
        expected.add(new TravelOption("TRANSIT,WALK,BICYCLE_RENT", "TRANSIT_BICYCLERENT"));
        expected.add(new TravelOption("CAR_PARK,WALK,TRANSIT", "PARKRIDE"));
        expected.add(new TravelOption("BICYCLE_PARK,WALK,TRANSIT", "BIKERIDE"));
        expected.add(new TravelOption("CAR,WALK,TRANSIT", "KISSRIDE"));

        assertEquals(expected, new HashSet<>(options));

        transitModes = new HashSet<>();

        options = TravelOptionsMaker.makeOptions(transitModes, hasBikeShare, hasBikeRide, hasParkRide);
        expected = new HashSet<>();
        expected.add(new TravelOption("WALK", "WALK"));
        expected.add(new TravelOption("BICYCLE", "BICYCLE"));
        expected.add(new TravelOption("CAR", "CAR"));
        expected.add(new TravelOption("WALK,BICYCLE_RENT", "BICYCLERENT"));

        assertEquals(expected, new HashSet<>(options));

        hasBikeRide = false;
        hasParkRide = false;
        hasBikeShare = false;

        options = TravelOptionsMaker.makeOptions(transitModes, hasBikeShare, hasBikeRide, hasParkRide);
        expected = new HashSet<>();
        expected.add(new TravelOption("WALK", "WALK"));
        expected.add(new TravelOption("BICYCLE", "BICYCLE"));
        expected.add(new TravelOption("CAR", "CAR"));

        assertEquals(expected, new HashSet<>(options));

    }
}