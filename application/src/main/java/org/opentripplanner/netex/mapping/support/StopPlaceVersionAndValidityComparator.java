package org.opentripplanner.netex.mapping.support;

import java.util.Comparator;
import org.opentripplanner.netex.support.NetexVersionHelper;
import org.rutebanken.netex.model.StopPlace;

/**
 * This compares StopPlaces first by validity, then by version.
 */

public class StopPlaceVersionAndValidityComparator implements Comparator<StopPlace> {

  private final ValidityComparator validityComparator = new ValidityComparator();

  @Override
  public int compare(StopPlace s1, StopPlace s2) {
    int compareValue = validityComparator.compare(s1.getValidBetween(), s2.getValidBetween());

    // If both are equally valid, sort by version
    if (compareValue == 0) {
      return NetexVersionHelper.comparingVersion().compare(s2, s1);
    } else {
      return compareValue;
    }
  }
}
