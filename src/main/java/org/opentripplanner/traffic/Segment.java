package org.opentripplanner.traffic;

import io.opentraffic.engine.data.pbf.ExchangeFormat;
import org.opentripplanner.routing.edgetype.StreetEdge;

import java.io.Serializable;

/**
 * Represents a segment of an OSM way.
 */
public class Segment implements Serializable {
    /**
     * ID of the way to which this is attached
     */
    public final long wayId;

    /**
     * ID of the starting OSM node.
     */
    public final long startNodeId;

    /**
     * ID of the ending OSM node.
     */
    public final long endNodeId;

    public Segment (ExchangeFormat.SegmentDefinition segmentDefinition) {
        this.wayId = segmentDefinition.getWayId();
        this.startNodeId = segmentDefinition.getStartNodeId();
        this.endNodeId = segmentDefinition.getEndNodeId();
    }

    public Segment(StreetEdge edge) {
        this.wayId = edge.wayId;
        this.endNodeId = edge.getEndOsmNodeId();
        this.startNodeId = edge.getStartOsmNodeId();
    }

    public Segment (long wayId, long startNodeId, long endNodeId) {
        this.wayId = wayId;
        this.startNodeId = startNodeId;
        this.endNodeId = endNodeId;
    }

    // this is used as a hashmap key so needs to have semantic equality and hash
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Segment segment = (Segment) o;

        if (endNodeId != segment.endNodeId)
            return false;
        if (startNodeId != segment.startNodeId)
            return false;
        if (wayId != segment.wayId)
            return false;

        return true;
    }

    @Override public int hashCode() {
        int result = (int) (wayId ^ (wayId >>> 32));
        result = 31 * result + (int) (startNodeId ^ (startNodeId >>> 32));
        result = 31 * result + (int) (endNodeId ^ (endNodeId >>> 32));
        return result;
    }
}
