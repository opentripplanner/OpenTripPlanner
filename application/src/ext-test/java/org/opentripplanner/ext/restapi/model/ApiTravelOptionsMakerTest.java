package org.opentripplanner.ext.restapi.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.TransitMode;

/**
 * Created by mabu on 28.7.2015.
 */
public class ApiTravelOptionsMakerTest {

  @Test
  public void testMakeOptions() throws Exception {
    boolean hasParkRide = false;
    boolean hasBikeRide = false;
    boolean hasBikeShare = false;

    HashSet<TransitMode> transitModes = new HashSet<>();
    transitModes.add(TransitMode.BUS);

    List<ApiTravelOption> options = ApiTravelOptionsMaker.makeOptions(
      transitModes,
      hasBikeShare,
      hasBikeRide,
      hasParkRide
    );

    Set<ApiTravelOption> expected = new HashSet<>();
    expected.add(new ApiTravelOption("TRANSIT,WALK", "TRANSIT"));
    expected.add(new ApiTravelOption("BUS,WALK", "BUS"));
    expected.add(new ApiTravelOption("WALK", "WALK"));
    expected.add(new ApiTravelOption("BICYCLE", "BICYCLE"));
    expected.add(new ApiTravelOption("CAR", "CAR"));
    expected.add(new ApiTravelOption("TRANSIT,BICYCLE", "TRANSIT_BICYCLE"));
    expected.add(new ApiTravelOption("CAR,WALK,TRANSIT", "KISSRIDE"));
    assertEquals(expected, new HashSet<>(options));

    transitModes.add(TransitMode.RAIL);

    hasBikeRide = true;

    options = ApiTravelOptionsMaker.makeOptions(
      transitModes,
      hasBikeShare,
      hasBikeRide,
      hasParkRide
    );
    expected = new HashSet<>();
    expected.add(new ApiTravelOption("TRANSIT,WALK", "TRANSIT"));
    expected.add(new ApiTravelOption("BUS,WALK", "BUS"));
    expected.add(new ApiTravelOption("RAIL,WALK", "RAIL"));
    expected.add(new ApiTravelOption("WALK", "WALK"));
    expected.add(new ApiTravelOption("BICYCLE", "BICYCLE"));
    expected.add(new ApiTravelOption("CAR", "CAR"));
    expected.add(new ApiTravelOption("TRANSIT,BICYCLE", "TRANSIT_BICYCLE"));
    expected.add(new ApiTravelOption("BICYCLE_PARK,WALK,TRANSIT", "BIKERIDE"));
    expected.add(new ApiTravelOption("CAR,WALK,TRANSIT", "KISSRIDE"));

    assertEquals(expected, new HashSet<>(options));

    hasBikeRide = false;

    hasBikeShare = true;

    options = ApiTravelOptionsMaker.makeOptions(
      transitModes,
      hasBikeShare,
      hasBikeRide,
      hasParkRide
    );
    expected = new HashSet<>();
    expected.add(new ApiTravelOption("TRANSIT,WALK", "TRANSIT"));
    expected.add(new ApiTravelOption("BUS,WALK", "BUS"));
    expected.add(new ApiTravelOption("RAIL,WALK", "RAIL"));
    expected.add(new ApiTravelOption("WALK", "WALK"));
    expected.add(new ApiTravelOption("BICYCLE", "BICYCLE"));
    expected.add(new ApiTravelOption("CAR", "CAR"));
    expected.add(new ApiTravelOption("WALK,BICYCLE_RENT", "BICYCLERENT"));
    expected.add(new ApiTravelOption("TRANSIT,BICYCLE", "TRANSIT_BICYCLE"));
    expected.add(new ApiTravelOption("TRANSIT,WALK,BICYCLE_RENT", "TRANSIT_BICYCLERENT"));
    expected.add(new ApiTravelOption("CAR,WALK,TRANSIT", "KISSRIDE"));

    assertEquals(expected, new HashSet<>(options));

    hasBikeShare = false;
    hasParkRide = true;

    options = ApiTravelOptionsMaker.makeOptions(
      transitModes,
      hasBikeShare,
      hasBikeRide,
      hasParkRide
    );
    expected = new HashSet<>();
    expected.add(new ApiTravelOption("TRANSIT,WALK", "TRANSIT"));
    expected.add(new ApiTravelOption("BUS,WALK", "BUS"));
    expected.add(new ApiTravelOption("RAIL,WALK", "RAIL"));
    expected.add(new ApiTravelOption("WALK", "WALK"));
    expected.add(new ApiTravelOption("BICYCLE", "BICYCLE"));
    expected.add(new ApiTravelOption("CAR", "CAR"));
    expected.add(new ApiTravelOption("TRANSIT,BICYCLE", "TRANSIT_BICYCLE"));
    expected.add(new ApiTravelOption("CAR_PARK,WALK,TRANSIT", "PARKRIDE"));
    expected.add(new ApiTravelOption("CAR,WALK,TRANSIT", "KISSRIDE"));

    assertEquals(expected, new HashSet<>(options));

    hasBikeShare = true;
    hasParkRide = true;
    hasBikeRide = true;

    options = ApiTravelOptionsMaker.makeOptions(
      transitModes,
      hasBikeShare,
      hasBikeRide,
      hasParkRide
    );
    expected = new HashSet<>();
    expected.add(new ApiTravelOption("TRANSIT,WALK", "TRANSIT"));
    expected.add(new ApiTravelOption("BUS,WALK", "BUS"));
    expected.add(new ApiTravelOption("RAIL,WALK", "RAIL"));
    expected.add(new ApiTravelOption("WALK", "WALK"));
    expected.add(new ApiTravelOption("BICYCLE", "BICYCLE"));
    expected.add(new ApiTravelOption("CAR", "CAR"));
    expected.add(new ApiTravelOption("WALK,BICYCLE_RENT", "BICYCLERENT"));
    expected.add(new ApiTravelOption("TRANSIT,BICYCLE", "TRANSIT_BICYCLE"));
    expected.add(new ApiTravelOption("TRANSIT,WALK,BICYCLE_RENT", "TRANSIT_BICYCLERENT"));
    expected.add(new ApiTravelOption("CAR_PARK,WALK,TRANSIT", "PARKRIDE"));
    expected.add(new ApiTravelOption("BICYCLE_PARK,WALK,TRANSIT", "BIKERIDE"));
    expected.add(new ApiTravelOption("CAR,WALK,TRANSIT", "KISSRIDE"));

    assertEquals(expected, new HashSet<>(options));

    transitModes = new HashSet<>();

    options = ApiTravelOptionsMaker.makeOptions(
      transitModes,
      hasBikeShare,
      hasBikeRide,
      hasParkRide
    );
    expected = new HashSet<>();
    expected.add(new ApiTravelOption("WALK", "WALK"));
    expected.add(new ApiTravelOption("BICYCLE", "BICYCLE"));
    expected.add(new ApiTravelOption("CAR", "CAR"));
    expected.add(new ApiTravelOption("WALK,BICYCLE_RENT", "BICYCLERENT"));

    assertEquals(expected, new HashSet<>(options));

    hasBikeRide = false;
    hasParkRide = false;
    hasBikeShare = false;

    options = ApiTravelOptionsMaker.makeOptions(
      transitModes,
      hasBikeShare,
      hasBikeRide,
      hasParkRide
    );
    expected = new HashSet<>();
    expected.add(new ApiTravelOption("WALK", "WALK"));
    expected.add(new ApiTravelOption("BICYCLE", "BICYCLE"));
    expected.add(new ApiTravelOption("CAR", "CAR"));

    assertEquals(expected, new HashSet<>(options));
  }
}
