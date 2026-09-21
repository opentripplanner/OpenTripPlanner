package org.opentripplanner.ext.stopconsolidation.model;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;

public record StopReplacement(StopLocation primary, FeedScopedId secondary) {}
