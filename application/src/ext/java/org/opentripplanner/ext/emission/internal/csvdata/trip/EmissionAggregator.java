package org.opentripplanner.ext.emission.internal.csvdata.trip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.opentripplanner.ext.emission.model.TripPatternEmission;
import org.opentripplanner.framework.error.OtpError;
import org.opentripplanner.framework.error.WordList;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * This class is responsible for merging duplicates by calculating the average. This can be
 * problematic, since the rows in the CSV file might not represent the same "volume" of
 * samples. For example if the feed contains samples for each day a trip runs, but the
 * the trip is crouded during weekdays and almost empty on weekends, then just adding the
 * days together and taking the average does not produce the average emission per passenger.
 */
class EmissionAggregator {

  private final FeedScopedId tripId;
  private final List<StopLocation> stops;
  private final Emission[] emissions;
  private final int[] counts;
  private final List<OtpError> issues = new ArrayList<>();
  private boolean semanticValidationDone = false;

  EmissionAggregator(FeedScopedId tripId, List<StopLocation> stops) {
    this.tripId = tripId;
    this.stops = stops;

    if (this.stops == null || this.stops.isEmpty()) {
      this.emissions = null;
      this.counts = null;
      warnOnMissingStopPatternForTrip();
    } else {
      int size = stops.size() - 1;

      this.emissions = new Emission[size];
      Arrays.fill(emissions, Emission.ZERO);

      this.counts = new int[size];
      Arrays.fill(counts, 0);
    }
  }

  EmissionAggregator mergeEmissionForleg(TripLegsRow row) {
    if (stops == null) {
      return this;
    }

    if (semanticValidationDone) {
      throw new IllegalStateException("Rows can not be added after validate() is called.");
    }

    var legEmission = Emission.of(row.co2());
    int boardStopPosInPattern = row.fromStopSequence() - 1;

    if (!verifyStop(boardStopPosInPattern, row)) {
      return this;
    }

    var existing = emissions[boardStopPosInPattern];
    if (existing.isZero()) {
      emissions[boardStopPosInPattern] = legEmission;
      counts[boardStopPosInPattern] = 1;
    } else {
      emissions[boardStopPosInPattern] = existing.plus(legEmission);
      counts[boardStopPosInPattern] = ++counts[boardStopPosInPattern];
    }
    return this;
  }

  private boolean verifyStop(int stopPosInPattern, TripLegsRow row) {
    if (stopPosInPattern < 0 || stopPosInPattern >= stops.size()) {
      addEmissionStopStartSeqNrIssue(row, stops.size() - 1);
      return false;
    }
    var stop = stops.get(stopPosInPattern);
    // Ignore feed id when comparing stopId
    if (!stop.getId().getId().equals(row.fromStopId())) {
      addEmissionStopIdMissmatchIssue(row);
      return false;
    }
    return true;
  }

  TripPatternEmission build() {
    if (!issues.isEmpty()) {
      throw new IllegalStateException("Can not build when there are issues!");
    }
    if (!semanticValidationDone) {
      throw new IllegalStateException("Forgot to call validate()?");
    }

    var agerageEmissions = new Emission[emissions.length];
    for (int i = 0; i < emissions.length; ++i) {
      agerageEmissions[i] = emissions[i].dividedBy(counts[i]);
    }

    return new TripPatternEmission(Arrays.asList(agerageEmissions));
  }

  boolean validate() {
    if (stops == null) {
      return false;
    }

    performSemanticValidation();
    boolean hasErrors = issues.isEmpty();

    // Process warnings, but do not abort
    warnOnDuplicates();

    return hasErrors;
  }

  List<OtpError> listIssues() {
    return issues;
  }

  private void performSemanticValidation() {
    this.semanticValidationDone = true;
    // There is no point in doing semantic validation if issues already exists.
    if (!issues.isEmpty()) {
      return;
    }
    verifyAllLegsHasEmissions();
  }

  private void verifyAllLegsHasEmissions() {
    var buf = WordList.of();
    for (int i = 0; i < emissions.length; ++i) {
      if (emissions[i].isZero()) {
        buf.add(Integer.toString(i + 1));
      }
    }
    if (buf.isEmpty()) {
      return;
    }
    addEmissionMissingLegIssue(buf);
  }

  private void warnOnMissingStopPatternForTrip() {
    issues.add(
      OtpError.of(
        "EmissionTripLegMissingTripStopPattern",
        "Warn! No trip with a stop pattern found for trip(%s). The trip is skipped.",
        tripId
      )
    );
  }

  private void warnOnDuplicates() {
    if (Arrays.stream(counts).anyMatch(i -> i > 1)) {
      issues.add(
        OtpError.of(
          "EmissionTripLegDuplicates",
          "Warn! The emission import contains duplicate rows for the same leg for trip(%s). " +
          "A average value is used.",
          tripId
        )
      );
    }
  }

  private void addEmissionStopStartSeqNrIssue(TripLegsRow row, int upperBoundInclusive) {
    issues.add(
      OtpError.of(
        "EmissionStopSeqNr",
        "The emission 'from_stop_sequence'(%d) is out of bounds[1, %d]: %s",
        row.fromStopSequence(),
        upperBoundInclusive,
        row.toString()
      )
    );
  }

  private void addEmissionMissingLegIssue(WordList buf) {
    issues.add(
      OtpError.of(
        "EmissionMissingLeg",
        "All legs in a trip(%s) must have an emission value. Leg number %s does not have an emission value.",
        tripId,
        buf.toString()
      )
    );
  }

  private void addEmissionStopIdMissmatchIssue(TripLegsRow row) {
    issues.add(
      OtpError.of(
        "EmissionStopIdMissmatch",
        "Emission 'from_stop_id'(%s) not found in stop pattern for trip(%s): %s",
        row.fromStopId(),
        tripId,
        row.toString()
      )
    );
  }
}
