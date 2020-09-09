package org.opentripplanner.graph_builder.module.time;

import java.util.Objects;

public class EdgeLine {

    private final long startNodeId;
    private final long endNodeId;

    public EdgeLine(long startNodeId, long endNodeId) {
        this.startNodeId = startNodeId;
        this.endNodeId = endNodeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EdgeLine edgeLine = (EdgeLine) o;
        return startNodeId == edgeLine.startNodeId &&
                endNodeId == edgeLine.endNodeId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startNodeId, endNodeId);
    }
}
