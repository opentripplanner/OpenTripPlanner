package org.opentripplanner.gtfs;

import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;

public interface GtfsContext {
    GtfsFeedId getFeedId();
    OtpTransitServiceBuilder getTransitBuilder();
}
