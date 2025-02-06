package org.opentripplanner.graph_builder.module.osm;

import java.util.ArrayList;
import javax.annotation.Nullable;
import org.opentripplanner.street.model.edge.StreetEdge;

public record StreetEdgePair(@Nullable StreetEdge main, @Nullable StreetEdge back) {
  /**
   * Return the non-null elements of this pair as an Iterable.
   */
  public Iterable<StreetEdge> asIterable() {
    var ret = new ArrayList<StreetEdge>(2);
    if (main != null) {
      ret.add(main);
    }
    if (back != null) {
      ret.add(back);
    }
    return ret;
  }

  /**
   * Select one of the edges contained in this pair that is not null. No particular algorithm is
   * guaranteed, and it may change in the future.
   */
  public StreetEdge pickAny() {
    if (main != null) {
      return main;
    } else if (back != null) {
      return back;
    }
    throw new IllegalStateException(
      "%s must not contain two null elements".formatted(getClass().getSimpleName())
    );
  }
}
