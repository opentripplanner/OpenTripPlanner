package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.graph_builder.module.osm.WalkableAreaBuilder;

public class AreaTooComplicated extends GraphBuilderAnnotation {

        public static final String FMT = "Area %s is too complicated (%s > " + WalkableAreaBuilder.MAX_AREA_NODES + ")";

        final long areaId;
        final int nbNodes;

        public AreaTooComplicated(long areaId, int nbNodes) {
                this.areaId = areaId;
                this.nbNodes = nbNodes;
        }

        @Override
        public String getMessage() {
                return String.format(FMT, areaId, nbNodes);
        }
}
