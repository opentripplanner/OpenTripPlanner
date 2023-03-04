package org.opentripplanner.ext.fares.model;

import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record RiderCategory(FeedScopedId id, String name, @Nullable String url) {}
