package org.opentripplanner.ext.stopconsolidation.model;

import java.util.Collection;
import org.opentripplanner.core.model.id.FeedScopedId;

public record ConsolidatedStopGroup(FeedScopedId primary, Collection<FeedScopedId> secondaries) {}
