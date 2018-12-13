package org.opentripplanner.routing.edgetype;

import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TemporarySplitterVertex;
import org.opentripplanner.util.I18NString;

final public class TemporaryPartialStreetEdge extends PartialStreetEdge implements TemporaryEdge {
    public TemporaryPartialStreetEdge(StreetEdge parentEdge, TemporaryStreetLocation v1,
            TemporaryStreetLocation v2, LineString geometry, I18NString name, double length) {
        super(parentEdge, v1, v2, geometry, name, length);

        if (v1.isEndVertex()) {
            throw new IllegalStateException("A temporary edge is directed away from an end vertex");
        } else if (!v2.isEndVertex()) {
            throw new IllegalStateException("A temporary edge is directed towards a start vertex");
        }
    }

    public TemporaryPartialStreetEdge(StreetEdge parentEdge, TemporaryStreetLocation v1,
            StreetVertex v2, LineString geometry, I18NString name, double length) {
        super(parentEdge, v1, v2, geometry, name, length);

        if (v1.isEndVertex()) {
            throw new IllegalStateException("A temporary edge is directed away from an end vertex");
        }
    }

    public TemporaryPartialStreetEdge(StreetEdge parentEdge, TemporarySplitterVertex v1,
        StreetVertex v2, LineString geometry, I18NString name, double length) {
        super(parentEdge, v1, v2, geometry, name, length);

        if (v1.isEndVertex()) {
            throw new IllegalStateException("A temporary edge is directed away from an end vertex");
        }
    }

    public TemporaryPartialStreetEdge(StreetEdge parentEdge, StreetVertex v1,
            TemporaryStreetLocation v2, LineString geometry, I18NString name, double length) {
        super(parentEdge, v1, v2, geometry, name, length);

        if (!v2.isEndVertex()) {
            throw new IllegalStateException("A temporary edge is directed towards a start vertex");
        }
    }

    public TemporaryPartialStreetEdge(StreetEdge parentEdge, StreetVertex v1,
        TemporarySplitterVertex v2, LineString geometry, I18NString name, double length) {
        super(parentEdge, v1, v2, geometry, name, length);

        if (!v2.isEndVertex()) {
            throw new IllegalStateException("A temporary edge is directed towards a start vertex");
        }
    }

    @Override
    public String toString() {
        return "Temporary" + super.toString();
    }
}
