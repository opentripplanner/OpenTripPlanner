package org.opentripplanner.ext.emission.internal.csvdata.trip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.ext.emission.model.TripPatternEmission;
import org.opentripplanner.framework.error.OtpError;
import org.opentripplanner.framework.error.WordList;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.collection.ListUtils;
import org.opentripplanner.utils.lang.IntRange;

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
  private final IntRange fromStopSequenceRange;
  private final List<OtpError> errors = new ArrayList<>();
  private final List<OtpError> warnings = new ArrayList<>();

  EmissionAggregator(FeedScopedId tripId, @Nullable List<StopLocation> stops) {
    this.tripId = tripId;
    this.stops = stops;

    if (this.stops == null || this.stops.isEmpty()) {
      this.emissions = null;
      this.counts = null;
      this.fromStopSequenceRange = null;
      warnOnMissingStopPatternForTrip();
    } else {
      int size = stops.size() - 1;
      this.fromStopSequenceRange = IntRange.ofInclusive(1, size);
      this.emissions = new Emission[size];
      Arrays.fill(emissions, Emission.ZERO);

      this.counts = new int[size];
      Arrays.fill(counts, 0);
    }
  }

  EmissionAggregator mergeEmissionsForHop(TripHopsRow row) {
    if (stops == null) {
      return this;
    }

    if (!verifyStop(row)) {
      return this;
    }

    int boardStopPosition = row.boardStopPosInPattern();
    var emission = Emission.of(row.co2());

    emissions[boardStopPosition] = emissions[boardStopPosition].plus(emission);
    counts[boardStopPosition] = counts[boardStopPosition] + 1;

    return this;
  }

  private boolean verifyStop(TripHopsRow row) {
    if (fromStopSequenceRange.isOutside(row.fromStopSequence())) {
      addEmissionStopStartSeqNrIssue(row);
      return false;
    }
    var stop = stops.get(row.boardStopPosInPattern());
    // Ignore feed id when comparing stopId
    if (!stop.getId().getId().equals(row.fromStopId())) {
      addEmissionStopIdMismatchIssue(row);
      return false;
    }
    return true;
  }

  Optional<TripPatternEmission> build() {
    if (hasErrors()) {
      return Optional.empty();
    }
    // Currently semantic rules only add warnings, not errors. Hence, no need to abort after the
    // semantic verification.
    new SemanticValidation().verify();

    var agerageEmissions = new Emission[emissions.length];
    for (int i = 0; i < emissions.length; ++i) {
      agerageEmissions[i] = emissions[i].dividedBy(counts[i]);
    }

    return Optional.of(new TripPatternEmission(Arrays.asList(agerageEmissions)));
  }

  List<OtpError> listIssues() {
    return ListUtils.combine(errors, warnings);
  }

  private boolean hasErrors() {
    return !errors.isEmpty();
  }

  private void warnOnMissingStopPatternForTrip() {
    errors.add(
      OtpError.of(
        "EmissionMissingTripStopPattern",
        "No trip with stop pattern found for trip(%s). Trip or stop-pattern is missing. The trip is skipped.",
        tripId
      )
    );
  }

  private void addEmissionStopStartSeqNrIssue(TripHopsRow row) {
    errors.add(
      OtpError.of(
        "EmissionStopSeqNr",
        "The emission 'from_stop_sequence'(%d) is out of bounds%s: %s",
        row.fromStopSequence(),
        fromStopSequenceRange,
        row.toString()
      )
    );
  }

  private void addEmissionStopIdMismatchIssue(TripHopsRow row) {
    errors.add(
      OtpError.of(
        "EmissionStopIdMismatch",
        "Emission 'from_stop_id'(%s) not found in stop pattern for trip(%s): %s",
        row.fromStopId(),
        tripId,
        row.toString()
      )
    );
  }

  /**
   * Perform sematic validation AFTER all data is added. Currently all sematic issues are warings,
   * not errors. The subclass is used to scope the semantic validation and make sure it is
   * performed once.
   */
  class SemanticValidation {

    private void verify() {
      // There is no point in doing semantic validation if issues already exists.
      warnOnMissingHopEmissions();
      warnOnDuplicates();
    }

    private void warnOnMissingHopEmissions() {
      var missingHops = WordList.of();
      for (int i = 0; i < emissions.length; ++i) {
        if (emissions[i].isZero()) {
          missingHops.add(Integer.toString(i + 1));
        }
      }
      if (missingHops.isEmpty()) {
        return;
      }
      addEmissionMissingHopIssue(missingHops);
    }

    private void addEmissionMissingHopIssue(WordList buf) {
      warnings.add(
        OtpError.of(
          "EmissionMissingTripHop",
          "Warning! All hops in a trip(%s) should have an emission value. Hop %s does not have " +
          "an emission value.",
          tripId,
          buf.toString()
        )
      );
    }

    private void warnOnDuplicates() {
      if (Arrays.stream(counts).anyMatch(i -> i > 1)) {
        warnings.add(
          OtpError.of(
            "EmissionTripHopDuplicates",
            "Warning! The emission import contains duplicate rows for the same hop for " +
            "trip(%s). An average value is used.",
            tripId
          )
        );
      }
    }
  }
}
