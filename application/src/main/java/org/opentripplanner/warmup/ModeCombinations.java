package org.opentripplanner.warmup;

import java.util.List;
import org.opentripplanner.street.model.StreetMode;

/**
 * Access/egress mode pairs to cycle through during warmup. Entries {@code i} of the access and
 * egress lists form one pair. The cycle is driven by a caller-supplied counter; modulo arithmetic
 * maps the counter to a pair so warmup queries iterate through every combination before repeating.
 */
class ModeCombinations {

  private final List<StreetMode> accessModes;
  private final List<StreetMode> egressModes;

  ModeCombinations(List<StreetMode> accessModes, List<StreetMode> egressModes) {
    if (accessModes.size() != egressModes.size()) {
      throw new IllegalArgumentException(
        "accessModes and egressModes must have the same size, got %d and %d.".formatted(
          accessModes.size(),
          egressModes.size()
        )
      );
    }
    this.accessModes = List.copyOf(accessModes);
    this.egressModes = List.copyOf(egressModes);
  }

  int size() {
    return accessModes.size();
  }

  StreetMode access(int counter) {
    return accessModes.get(indexFor(counter));
  }

  StreetMode egress(int counter) {
    return egressModes.get(indexFor(counter));
  }

  private int indexFor(int counter) {
    return Math.floorMod(counter - 1, size());
  }
}
