package org.opentripplanner.util;

import org.junit.Test;
import org.opentripplanner.model.TransitMode;

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

        HashSet<TransitMode> transitModes = new HashSet<>();
        transitModes.add(TransitMode.BUS);

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

        transitModes.add(TransitMode.RAIL);

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