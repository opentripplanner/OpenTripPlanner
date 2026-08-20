package org.opentripplanner.updater.trip.siri;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import uk.org.siri.siri21.JourneyRelationStructure;
import uk.org.siri.siri21.JourneyRelationTypeEnumeration;
import uk.org.siri.siri21.StopPointRefStructure;

final class JourneyRelationWrapper {

  private final JourneyRelationStructure relation;
  private final List<CallWrapper> calls;

  public JourneyRelationWrapper(JourneyRelationStructure journeyRelation, List<CallWrapper> calls) {
    this.relation = journeyRelation;
    this.calls = calls;
  }

  public boolean isReplacedBy() {
    return JourneyRelationTypeEnumeration.REPLACED_BY_JOURNEY.equals(
      relation.getJourneyRelationType()
    );
  }

  List<VehicleJourneyIdAndServiceDate> relatedJourneys() {
    if (relation.getRelatedJourneies() == null) {
      return List.of();
    }
    return relation
      .getRelatedJourneies()
      .stream()
      .map(relatedJourney ->
        VehicleJourneyIdAndServiceDate.of(relatedJourney.getFramedVehicleJourneyRef())
      )
      .filter(Objects::nonNull)
      .toList();
  }

  List<JourneyPartData> journeyParts() {
    var parts = relation.getJourneyParts();
    if (parts == null || parts.getJourneyPartInfos() == null) {
      return List.of();
    }
    var result = new ArrayList<JourneyPartData>();
    for (var part : parts.getJourneyPartInfos()) {
      var fromPos = resolvePosInPattern(part.getFromStopPointRef(), part.getStartTime());
      var toPos = resolvePosInPattern(part.getToStopPointRef(), part.getEndTime());
      if (fromPos.isEmpty() || toPos.isEmpty()) {
        continue;
      }
      if (fromPos.get() >= toPos.get()) {
        // Ignore invalid parts that start after they end.
        continue;
      }
      result.add(new JourneyPartData(fromPos.get(), toPos.get()));
    }
    return result;
  }

  private Optional<Integer> resolvePosInPattern(
    @Nullable StopPointRefStructure stopPointRef,
    @Nullable ZonedDateTime time
  ) {
    if (stopPointRef == null) {
      return Optional.empty();
    }
    for (int i = 0; i < calls.size(); i++) {
      var call = calls.get(i);
      if (stopPointRef.getValue().equals(call.getStopPointRef())) {
        if (time == null) {
          return Optional.of(i);
        }
        if (time.equals(call.getAimedArrivalTime()) || time.equals(call.getAimedDepartureTime())) {
          // The spec is vague in how JourneyPartInfoStructure.startTime and endTime should be interpreted.
          // If it is provided we require the call to match the aimed arrival time or aimed departure time.
          return Optional.of(i);
        }
      }
    }
    return Optional.empty();
  }
}
