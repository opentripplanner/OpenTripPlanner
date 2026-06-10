package org.rutebanken.netex.model;

import java.util.Collection;
import org.opentripplanner.netex.mapping.MappingSupport;

public class ServiceJourneyBuilder {

  private final ServiceJourney journey = new ServiceJourney();

  public ServiceJourneyBuilder(String id) {
    journey.setId(id);
  }

  public ServiceJourneyBuilder withPatternId(String id) {
    var ref = MappingSupport.createWrappedRef(id, JourneyPatternRefStructure.class);
    journey.withJourneyPatternRef(ref);
    return this;
  }

  public ServiceJourneyBuilder withPassingTimes(
    PointsInJourneyPattern_RelStructure pointsInSequence
  ) {
    return withPassingTimes(
      pointsInSequence
        .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
        .stream()
        .map(EntityStructure::getId)
        .toList()
    );
  }

  public ServiceJourneyBuilder withPassingTimes(Collection<String> ids) {
    var passingTimes = new TimetabledPassingTimes_RelStructure();
    passingTimes.withTimetabledPassingTime(
      ids.stream().map(ServiceJourneyBuilder::timetabledPassingTime).toList()
    );
    journey.withPassingTimes(passingTimes);
    return this;
  }

  public ServiceJourney build() {
    return journey;
  }

  private static TimetabledPassingTime timetabledPassingTime(String pointInPatternRef) {
    var passingTime = new TimetabledPassingTime();
    passingTime.withPointInJourneyPatternRef(
      MappingSupport.createWrappedRef(pointInPatternRef, PointInJourneyPatternRefStructure.class)
    );
    return passingTime;
  }
}
