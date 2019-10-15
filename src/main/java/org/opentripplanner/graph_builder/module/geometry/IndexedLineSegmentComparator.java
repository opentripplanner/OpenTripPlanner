package org.opentripplanner.graph_builder.module.geometry;

import org.apache.commons.math3.util.FastMath;
import org.locationtech.jts.geom.Coordinate;

import java.util.Comparator;

class IndexedLineSegmentComparator implements Comparator<IndexedLineSegment> {

    private Coordinate coord;

    public IndexedLineSegmentComparator(Coordinate coord) {
        this.coord = coord;
    }

    @Override
    public int compare(IndexedLineSegment a, IndexedLineSegment b) {
        return (int) FastMath.signum(a.distance(coord) - b.distance(coord));
    }
}
