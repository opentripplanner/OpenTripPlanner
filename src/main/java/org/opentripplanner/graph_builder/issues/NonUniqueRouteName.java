package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;

public class NonUniqueRouteName implements DataImportIssue {

        public static final String FMT = "Route had non-unique name. Generated one to ensure uniqueness of TripPattern names: %s";

        final String generatedRouteName;

        public NonUniqueRouteName(String generatedRouteName) {
                this.generatedRouteName = generatedRouteName;
        }

        @Override
        public String getMessage() {
                return String.format(FMT, generatedRouteName);
        }
}
