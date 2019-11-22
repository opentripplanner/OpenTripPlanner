package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.graph_builder.module.osm.WalkableAreaBuilder;

public class AreaTooComplicated implements DataImportIssue {

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
