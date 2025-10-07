package org.opentripplanner.model.plan.walkstep.verticaltransportationuse;

import javax.annotation.Nullable;

/**
 * Represents information about an elevator stored in
 * {@WalkStep}.
 */
public record ElevatorUse(@Nullable String toLevelName) implements VerticalTransportationUse {}
