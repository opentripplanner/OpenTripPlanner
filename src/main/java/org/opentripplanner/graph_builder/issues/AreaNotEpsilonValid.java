package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.graph_builder.module.osm.WalkableAreaBuilder;

public class AreaNotEpsilonValid implements DataImportIssue {

        public static final String FMT = "Area %s is not epsilon-valid (epsilon = " + WalkableAreaBuilder.VISIBILITY_EPSILON + ")";

        final long areaId;

        public AreaNotEpsilonValid(long areaId) {
                this.areaId = areaId;
        }

        @Override
        public String getMessage() {
                return String.format(FMT, areaId);
        }
}
