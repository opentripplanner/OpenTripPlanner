package org.opentripplanner.ext.stopconsolidation.model;

import java.util.Collection;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record ConsolidatedStopGroup(FeedScopedId primary, Collection<FeedScopedId> secondaries) {}
