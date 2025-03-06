package org.opentripplanner.apis.gtfs.mapping.routerequest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.JourneyRequest;

/**
 * Mapping and validation methods for StreetModes.
 */
public class StreetModeMapper {

  /**
   * This is meant to be used when mapping from StreetModes provided in API calls to
   * {@link JourneyRequest} StreetMode. Current support:
   * 1. If only one mode is defined, it needs to be WALK, BICYCLE, CAR or some parking mode.
   * 2. If two modes are defined, they can't be BICYCLE or CAR, and WALK needs to be one of them.
   * 3. More than two modes can't be defined for the same leg.
   * <p>
   * TODO future support:
   * 1. Any mode can be defined alone. If it's not used in a leg, the leg gets filtered away.
   * 2. If two modes are defined, they can't be BICYCLE or CAR. Usually WALK is required as the second
   *    mode but in some cases it's possible to define other modes as well such as BICYCLE_RENTAL together
   *    with SCOOTER_RENTAL. In that case, legs which don't use BICYCLE_RENTAL or SCOOTER_RENTAL would be filtered
   *    out.
   * 3. When more than two modes are used, some combinations are supported such as WALK, BICYCLE_RENTAL and SCOOTER_RENTAL.
   */
  public static StreetMode getStreetModeForRouting(List<StreetMode> modes) {
    if (modes.size() > 2) {
      throw new IllegalArgumentException(
        "Only one or two modes can be specified for a leg, got: %.".formatted(modes)
      );
    }
    if (modes.size() == 1) {
      var mode = modes.getFirst();
      // TODO in the future, we will support defining other modes alone as well and filter out legs
      // which don't contain the only specified mode as opposed to also returning legs which contain
      // only walking.
      if (!isAlwaysPresentInLeg(mode)) {
        throw new IllegalArgumentException(
          "For the time being, %s needs to be combined with WALK mode for the same leg.".formatted(
              mode
            )
        );
      }
      return mode;
    }
    if (modes.contains(StreetMode.BIKE)) {
      throw new IllegalArgumentException(
        "Bicycle can't be combined with other modes for the same leg: %s.".formatted(modes)
      );
    }
    if (modes.contains(StreetMode.CAR)) {
      throw new IllegalArgumentException(
        "Car can't be combined with other modes for the same leg: %s.".formatted(modes)
      );
    }
    if (!modes.contains(StreetMode.WALK)) {
      throw new IllegalArgumentException(
        "For the time being, WALK needs to be added as a mode for a leg when using %s and these two can't be used in the same leg.".formatted(
            modes
          )
      );
    }
    // Walk is currently always used as an implied mode when mode is not car.
    return modes.stream().filter(mode -> mode != StreetMode.WALK).findFirst().get();
  }

  /**
   * This is meant to be used when mapping from {@link JourneyRequest} StreetMode into StreetMode
   * combinations currently used by the API. The logic is as follows:
   * 1. If the mode is WALK, BICYCLE, CAR or some parking mode, then it is returned alone.
   * 2. Otherwise, return WALK + the mode.
   */
  public static List<StreetMode> getStreetModesForApi(StreetMode mode) {
    if (isAlwaysPresentInLeg(mode)) {
      return List.of(mode);
    }
    return List.of(StreetMode.WALK, mode);
  }

  /**
   * TODO this doesn't support multiple street modes yet
   */
  public static void validateStreetModes(JourneyRequest journey) {
    Set<StreetMode> modes = new HashSet();
    modes.add(journey.access().mode());
    modes.add(journey.egress().mode());
    modes.add(journey.transfer().mode());
    if (modes.contains(StreetMode.BIKE) && modes.size() != 1) {
      throw new IllegalArgumentException(
        "If BICYCLE is used for access, egress or transfer, then it should be used for all."
      );
    }
    if (modes.contains(StreetMode.CAR) && modes.size() != 1) {
      throw new IllegalArgumentException(
        "If CAR is used for access, egress or transfer, then it should be used for all."
      );
    }
  }

  private static boolean isAlwaysPresentInLeg(StreetMode mode) {
    return (
      mode == StreetMode.BIKE ||
      mode == StreetMode.CAR ||
      mode == StreetMode.WALK ||
      mode.includesParking()
    );
  }
}
