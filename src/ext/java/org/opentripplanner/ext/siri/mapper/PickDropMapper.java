package org.opentripplanner.ext.siri.mapper;

import static org.opentripplanner.model.PickDrop.CANCELLED;
import static org.opentripplanner.model.PickDrop.NONE;
import static org.opentripplanner.model.PickDrop.SCHEDULED;

import java.util.Optional;
import org.opentripplanner.model.PickDrop;
import uk.org.siri.siri20.ArrivalBoardingActivityEnumeration;
import uk.org.siri.siri20.DepartureBoardingActivityEnumeration;

public class PickDropMapper {

  /**
   * This method maps an ArrivalBoardingActivity to a pick drop type.
   *
   * The Siri ArrivalBoardingActivity includes less information than the pick drop type, therefore is it only
   * changed if routability has changed.
   *
   * @param currentValue The current pick drop value on a stopTime
   * @param arrivalBoardingActivityEnumeration The incoming boardingActivity to be mapped
   * @return Mapped PickDrop type, empty if routability is not changed.
   */
  public static Optional<PickDrop> mapDropOffType(
    PickDrop currentValue,
    ArrivalBoardingActivityEnumeration arrivalBoardingActivityEnumeration
  ) {
    if (arrivalBoardingActivityEnumeration == null) {
      return Optional.empty();
    }

    return switch (arrivalBoardingActivityEnumeration) {
      case ALIGHTING -> currentValue.isNotRoutable() ? Optional.of(SCHEDULED) : Optional.empty();
      case NO_ALIGHTING -> Optional.of(NONE);
      case PASS_THRU -> Optional.of(CANCELLED);
    };
  }

  /**
   * This method maps an departureBoardingActivity to a pick drop type.
   *
   * The Siri DepartureBoardingActivity includes less information than the planned data, therefore is it only
   * changed if routability has changed.
   *
   * @param currentValue The current pick drop value on a stopTime
   * @param departureBoardingActivityEnumeration The incoming departureBoardingActivityEnumeration to be mapped
   * @return Mapped PickDrop type, empty if routability is not changed.
   */
  public static Optional<PickDrop> mapPickUpType(
    PickDrop currentValue,
    DepartureBoardingActivityEnumeration departureBoardingActivityEnumeration
  ) {
    if (departureBoardingActivityEnumeration == null) {
      return Optional.empty();
    }

    return switch (departureBoardingActivityEnumeration) {
      case BOARDING -> currentValue.isNotRoutable() ? Optional.of(SCHEDULED) : Optional.empty();
      case NO_BOARDING -> Optional.of(NONE);
      case PASS_THRU -> Optional.of(CANCELLED);
    };
  }
}
