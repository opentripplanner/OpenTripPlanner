package org.opentripplanner.ext.examples.configure;

import org.opentripplanner.ext.examples.statistics.GraphStatisticsResource;

public class ExamplesConfiguration {

    public GraphStatisticsResource createStatisticsResource() {
        return new GraphStatisticsResource();
    }
}
