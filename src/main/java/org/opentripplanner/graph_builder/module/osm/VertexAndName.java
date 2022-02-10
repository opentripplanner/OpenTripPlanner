package org.opentripplanner.graph_builder.module.osm;

import java.util.Objects;
import org.opentripplanner.routing.vertextype.OsmVertex;
import org.opentripplanner.util.I18NString;

class VertexAndName {

    private final I18NString name;
    private final OsmVertex vertex;

    VertexAndName(I18NString name, OsmVertex vertex) {
        this.name = name;
        this.vertex = vertex;
    }

    public I18NString getName() {
        return this.name;
    }

    public OsmVertex getVertex() {
        return this.vertex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        final VertexAndName that = (VertexAndName) o;
        return Objects.equals(name, that.name) && vertex.equals(that.vertex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, vertex);
    }
}
