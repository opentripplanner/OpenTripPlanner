package org.opentripplanner.model.plan.walkstep;

import javax.annotation.Nullable;

/**
 * Represents information about vertical transportation equipment stored in
 * {@WalkStep}.
 */
public record VerticalTransportationUse(@Nullable Double toLevel, @Nullable Double fromLevel) {}
