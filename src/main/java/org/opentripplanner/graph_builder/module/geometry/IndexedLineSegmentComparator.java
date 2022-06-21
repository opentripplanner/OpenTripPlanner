package org.opentripplanner.graph_builder.module.geometry;

import java.util.Comparator;
import org.locationtech.jts.geom.Coordinate;

class IndexedLineSegmentComparator implements Comparator<IndexedLineSegment> {

  private final Coordinate coord;

  public IndexedLineSegmentComparator(Coordinate coord) {
    this.coord = coord;
  }

  @Override
  public int compare(IndexedLineSegment a, IndexedLineSegment b) {
    return (int) Math.signum(a.distance(coord) - b.distance(coord));
  }
}
