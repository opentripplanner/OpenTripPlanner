package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.util.List;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Raptor uses an integer index to reference stops. This is not the stop id, but just a sequence
 * number - an index. Hence, we don´t care about the order - as long as the order does not change.
 * Raptor reference stops as integers for performance reasons, it never accesses stops, it does not
 * need to. The returned itineraries from Raptor contain stop indexes, not references to stops, so
 * OTP must maintain the stop index.
 */
public interface StopIndexForRaptor {
  StopLocation stopByIndex(int index);

  int indexOf(StopLocation stop);

  int size();

  default int[] listStopIndexesForStops(List<StopLocation> stops) {
    int[] stopIndex = new int[stops.size()];

    for (int i = 0; i < stops.size(); i++) {
      stopIndex[i] = indexOf(stops.get(i));
    }
    return stopIndex;
  }
}
