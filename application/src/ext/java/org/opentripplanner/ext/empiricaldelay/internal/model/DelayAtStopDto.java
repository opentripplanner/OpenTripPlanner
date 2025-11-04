package org.opentripplanner.ext.empiricaldelay.internal.model;

import org.opentripplanner.ext.empiricaldelay.model.EmpiricalDelay;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record DelayAtStopDto(int sequence, FeedScopedId stopId, EmpiricalDelay empiricalDelay) {}
