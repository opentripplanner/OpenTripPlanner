package org.opentripplanner.ext.empiricaldelay.internal.model;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.empiricaldelay.model.EmpiricalDelay;

public record DelayAtStopDto(int sequence, FeedScopedId stopId, EmpiricalDelay empiricalDelay) {}
