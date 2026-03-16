package org.opentripplanner.netex.support;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opentripplanner.netex.index.api.NetexEntityIndexReadOnlyView;
import org.opentripplanner.netex.support.stoptime.StopTimeAdaptor;
import org.rutebanken.netex.model.EntityStructure;
import org.rutebanken.netex.model.JourneyPattern_VersionStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.TimetabledPassingTime;

/**
 * Wrapper around {@link ServiceJourney} that provides a simpler interface
 * for using {@link TimetabledPassingTime}.
 */
public class ServiceJourneyInfo {

  private final JourneyPattern_VersionStructure journeyPattern;
  private final ServiceJourney serviceJourney;
  private final NetexEntityIndexReadOnlyView netexEntityIndex;

  public ServiceJourneyInfo(
    ServiceJourney serviceJourney,
    NetexEntityIndexReadOnlyView netexEntityIndex
  ) {
    this.serviceJourney = serviceJourney;
    this.netexEntityIndex = netexEntityIndex;
    this.journeyPattern = netexEntityIndex.getJourneyPatternsById().lookup(patternId());
  }

  /**
   * Sort the timetabled passing times according to their order in the journey pattern.
   */
  public List<StopTimeAdaptor> orderedTimetabledPassingTimeInfos() {
    Map<TimetabledPassingTime, Boolean> stopFlexibility = stopFlexibility();

    Map<String, Integer> stopPointIdToOrder = journeyPattern
      .getPointsInSequence()
      .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
      .stream()
      .collect(Collectors.toMap(EntityStructure::getId, point -> point.getOrder().intValueExact()));
    return serviceJourney
      .getPassingTimes()
      .getTimetabledPassingTime()
      .stream()
      .sorted(
        Comparator.comparing(timetabledPassingTime ->
          stopPointIdToOrder.get(stopPointId(timetabledPassingTime))
        )
      )
      .map(timetabledPassingTime ->
        StopTimeAdaptor.of(timetabledPassingTime, stopFlexibility.get(timetabledPassingTime))
      )
      .toList();
  }

  /**
   * Map a timetabledPassingTime to true if its stop is a stop area, false otherwise.
   */
  private Map<TimetabledPassingTime, Boolean> stopFlexibility() {
    Map<String, String> scheduledStopPointIdByStopPointId = scheduledStopPointIdByStopPointId();

    return serviceJourney
      .getPassingTimes()
      .getTimetabledPassingTime()
      .stream()
      .collect(
        Collectors.toMap(
          timetabledPassingTime -> timetabledPassingTime,
          timetabledPassingTime ->
            netexEntityIndex
              .getFlexibleStopPlaceByStopPointRef()
              .containsKey(
                scheduledStopPointIdByStopPointId.get(
                  timetabledPassingTime.getPointInJourneyPatternRef().getValue().getRef()
                )
              )
        )
      );
  }

  /**
   * Return the StopPointInJourneyPattern ID of a given TimeTabledPassingTime.
   */
  private static String stopPointId(TimetabledPassingTime timetabledPassingTime) {
    return timetabledPassingTime.getPointInJourneyPatternRef().getValue().getRef();
  }

  /**
   * Return the mapping between stop point id and scheduled stop point id for the journey
   * pattern.
   */
  private Map<String, String> scheduledStopPointIdByStopPointId() {
    return journeyPattern
      .getPointsInSequence()
      .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
      .stream()
      .collect(
        Collectors.toMap(EntityStructure::getId, p ->
          ((StopPointInJourneyPattern) p).getScheduledStopPointRef().getValue().getRef()
        )
      );
  }

  /**
   * Return the JourneyPattern ID of the ServiceJourney.
   */
  private String patternId() {
    return serviceJourney.getJourneyPatternRef().getValue().getRef();
  }
}
