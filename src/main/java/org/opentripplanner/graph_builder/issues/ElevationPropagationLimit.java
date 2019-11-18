package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.routing.graph.Vertex;

public class ElevationPropagationLimit implements DataImportIssue {

        private static final String FMT = "While propagating elevations, hit 2km distance limit at %s ";

        final Vertex vertex;

        public ElevationPropagationLimit(Vertex vertex) {
                this.vertex = vertex;
        }

        @Override
        public String getMessage() {
                return String.format(FMT, vertex);
        }

        @Override
        public Vertex getReferencedVertex() {
                return this.vertex;
        }

}
