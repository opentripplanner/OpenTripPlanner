package org.opentripplanner.model.impl;

import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;

/**
 * Generates {@code TripPattern}s from a {@link TransitDataImportBuilder}'s trips and stop times,
 * as the last step of {@link TransitDataImportBuilder#build()} - after trips/stopTimes/flexTrips
 * have been pruned and stop times have been validated/interpolated.
 * <p>
 * GTFS is the only format that needs this: NeTEx builds its {@code TripPattern}s directly while
 * mapping, so it passes {@link #NONE}.
 */
@FunctionalInterface
public interface TripPatternGenerator {
  TripPatternGenerator NONE = (builder, issueStore) -> {};

  void generate(TransitDataImportBuilder builder, DataImportIssueStore issueStore);
}
