package org.opentripplanner.graph_builder.module.transfer.api;

import java.time.Duration;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.street.model.StreetMode;

/**
 * One entry of a transfer profile's {@code maxDurations} list. {@code allowedModes}, when
 * present, restricts this duration to target stops reachable by at least one of those modes'
 * trips (see {@code TransitRepository#getStopLocationsUsedForCarsAllowedTrips}/
 * {@code #getStopLocationsUsedForBikesAllowedTrips}) - generalizing today's
 * {@code carsAllowedStopMaxDuration}/{@code bikesAllowedStopMaxDuration}. {@code null} means this
 * rule applies to any target stop.
 */
public record MaxDurationRule(Duration maxDuration, @Nullable Set<StreetMode> allowedModes) {}
