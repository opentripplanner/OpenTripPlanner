package org.opentripplanner.netex.mapping;

import static org.opentripplanner.model.PickDrop.COORDINATE_WITH_DRIVER;
import static org.opentripplanner.model.PickDrop.NONE;
import static org.opentripplanner.model.PickDrop.SCHEDULED;

import jakarta.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMap;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMapById;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.netex.support.JourneyPatternHelper;
import org.opentripplanner.transit.model.framework.ImmutableEntityById;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.DestinationDisplay_VersionStructure;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.JourneyPattern_VersionStructure;
import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.PointInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.rutebanken.netex.model.VersionOfObjectRefStructure;
import org.rutebanken.netex.model.Via_VersionedChildStructure;
import org.rutebanken.netex.model.Vias_RelStructure;

/**
 * This maps a list of TimetabledPassingTimes to a list of StopTimes. It also makes sure the
 * StopTime has a reference to the correct stop. DestinationDisplay is mapped to HeadSign. There is
 * logic to take care of the the fact that DestinationsDisplay is also valid for each subsequent
 * TimeTabledPassingTime, while HeadSign has to be explicitly defined for each StopTime.
 * This class does not take Daylight Saving Time transitions into account, this is an error and
 * should be fixed. See https://github.com/opentripplanner/OpenTripPlanner/issues/5109
 */
class StopTimesMapper {

  private static final int DAY_IN_SECONDS = 3600 * 24;
  private final DataImportIssueStore issueStore;
  private final FeedScopedIdFactory idFactory;

  private final ReadOnlyHierarchicalMap<String, DestinationDisplay> destinationDisplayById;

  private final ImmutableEntityById<RegularStop> stopsById;

  private final ImmutableEntityById<AreaStop> flexibleStopLocationsById;

  private final ImmutableEntityById<GroupStop> groupStopById;

  private final ReadOnlyHierarchicalMap<String, String> quayIdByStopPointRef;

  private final ReadOnlyHierarchicalMap<String, String> flexibleStopPlaceIdByStopPointRef;

  private final ReadOnlyHierarchicalMap<String, Route> routeByid;

  private final ReadOnlyHierarchicalMapById<FlexibleLine> flexibleLinesById;

  private I18NString currentHeadSign;

  private List<String> currentHeadSignVias;

  StopTimesMapper(
    DataImportIssueStore issueStore,
    FeedScopedIdFactory idFactory,
    ImmutableEntityById<RegularStop> stopsById,
    ImmutableEntityById<AreaStop> areaStopById,
    ImmutableEntityById<GroupStop> groupStopById,
    ReadOnlyHierarchicalMap<String, DestinationDisplay> destinationDisplayById,
    ReadOnlyHierarchicalMap<String, String> quayIdByStopPointRef,
    ReadOnlyHierarchicalMap<String, String> flexibleStopPlaceIdByStopPointRef,
    ReadOnlyHierarchicalMapById<FlexibleLine> flexibleLinesById,
    ReadOnlyHierarchicalMap<String, Route> routeById
  ) {
    this.issueStore = issueStore;
    this.idFactory = idFactory;
    this.destinationDisplayById = destinationDisplayById;
    this.stopsById = stopsById;
    this.flexibleStopLocationsById = areaStopById;
    this.groupStopById = groupStopById;
    this.quayIdByStopPointRef = quayIdByStopPointRef;
    this.flexibleStopPlaceIdByStopPointRef = flexibleStopPlaceIdByStopPointRef;
    this.flexibleLinesById = flexibleLinesById;
    this.routeByid = routeById;
  }

  static int calculateOtpTime(LocalTime time, BigInteger dayOffset) {
    int otpTime = time.toSecondOfDay();
    if (dayOffset != null) {
      otpTime += DAY_IN_SECONDS * dayOffset.intValue();
    }
    return otpTime;
  }

  /**
   * @return a map of stop-times indexed by the TimetabledPassingTime id.
   */
  @Nullable
  StopTimesMapperResult mapToStopTimes(
    JourneyPattern_VersionStructure journeyPattern,
    Trip trip,
    List<TimetabledPassingTime> passingTimes,
    ServiceJourney serviceJourney
  ) {
    StopTimesMapperResult result = new StopTimesMapperResult();
    List<String> scheduledStopPointIds = new ArrayList<>();

    for (int i = 0; i < passingTimes.size(); i++) {
      TimetabledPassingTime currentPassingTime = passingTimes.get(i);

      String pointInJourneyPattern = currentPassingTime
        .getPointInJourneyPatternRef()
        .getValue()
        .getRef();

      StopPointInJourneyPattern stopPoint = findStopPoint(pointInJourneyPattern, journeyPattern);

      StopLocation stop = lookUpStopLocation(stopPoint);

      if (stop == null) {
        if (
          stopPoint != null &&
          isFalse(stopPoint.isForAlighting()) &&
          isFalse(stopPoint.isForBoarding())
        ) {
          continue;
        }
        issueStore.add(
          "JourneyPatternStopNotFound",
          "Stop with id %s not found for StopPoint %s in JourneyPattern %s. " +
          "Trip %s will not be mapped.",
          stopPoint != null && stopPoint.getScheduledStopPointRef() != null
            ? stopPoint.getScheduledStopPointRef().getValue().getRef()
            : "null",
          stopPoint != null ? stopPoint.getId() : "null",
          journeyPattern.getId(),
          trip.getId()
        );
        return null;
      }

      scheduledStopPointIds.add(stopPoint.getScheduledStopPointRef().getValue().getRef());

      StopTime stopTime = mapToStopTime(trip, stopPoint, stop, currentPassingTime, i);
      if (stopTime == null) {
        return null;
      }

      BookingInfo bookingInfo = new BookingInfoMapper(issueStore).map(
        stopPoint,
        serviceJourney,
        lookUpFlexibleLine(serviceJourney, journeyPattern)
      );
      stopTime.setDropOffBookingInfo(bookingInfo);
      stopTime.setPickupBookingInfo(bookingInfo);

      result.addStopTime(currentPassingTime.getId(), stopTime);
    }
    result.setScheduledStopPointIds(scheduledStopPointIds);

    return result;
  }

  /**
   * @return a map of stop-times indexed by the TimetabledPassingTime id.
   */
  @Nullable
  String findTripHeadsign(
    JourneyPattern_VersionStructure journeyPattern,
    TimetabledPassingTime firstPassingTime
  ) {
    String pointInJourneyPattern = firstPassingTime
      .getPointInJourneyPatternRef()
      .getValue()
      .getRef();

    var stopPoint = findStopPoint(pointInJourneyPattern, journeyPattern);

    if (stopPoint == null) {
      return null;
    }

    if (stopPoint.getDestinationDisplayRef() == null) {
      return null;
    }

    var destinationDisplay = destinationDisplayById.lookup(
      stopPoint.getDestinationDisplayRef().getRef()
    );

    return destinationDisplay == null
      ? null
      : MultilingualStringMapper.nullableValueOf(destinationDisplay.getFrontText());
  }

  @Nullable
  private static StopPointInJourneyPattern findStopPoint(
    String pointInJourneyPatterRef,
    JourneyPattern_VersionStructure journeyPattern
  ) {
    var points = journeyPattern
      .getPointsInSequence()
      .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern();

    for (PointInLinkSequence_VersionedChildStructure point : points) {
      if (point instanceof StopPointInJourneyPattern stopPoint) {
        if (stopPoint.getId().equals(pointInJourneyPatterRef)) {
          return stopPoint;
        }
      }
    }
    return null;
  }

  private static int calculateOtpTime(
    LocalTime time,
    BigInteger dayOffset,
    LocalTime fallbackTime,
    BigInteger fallbackDayOffset
  ) {
    return time != null
      ? calculateOtpTime(time, dayOffset)
      : calculateOtpTime(fallbackTime, fallbackDayOffset);
  }

  private static boolean isFalse(Boolean value) {
    return value != null && !value;
  }

  private StopTime mapToStopTime(
    Trip trip,
    StopPointInJourneyPattern stopPoint,
    StopLocation stop,
    TimetabledPassingTime passingTime,
    int stopSequence
  ) {
    StopTime stopTime = new StopTime();
    stopTime.setTrip(trip);
    stopTime.setStopSequence(stopSequence);
    stopTime.setStop(stop);
    if (passingTime.getArrivalTime() != null || passingTime.getDepartureTime() != null) {
      stopTime.setArrivalTime(
        calculateOtpTime(
          passingTime.getArrivalTime(),
          passingTime.getArrivalDayOffset(),
          passingTime.getDepartureTime(),
          passingTime.getDepartureDayOffset()
        )
      );
      stopTime.setDepartureTime(
        calculateOtpTime(
          passingTime.getDepartureTime(),
          passingTime.getDepartureDayOffset(),
          passingTime.getArrivalTime(),
          passingTime.getArrivalDayOffset()
        )
      );

      // From NeTEx we define timepoint as a waitpoint with waiting time defined (also 0)
      if (Boolean.TRUE.equals(stopPoint.isIsWaitPoint()) && passingTime.getWaitingTime() != null) {
        stopTime.setTimepoint(1);
      }
    } else if (
      passingTime.getEarliestDepartureTime() != null && passingTime.getLatestArrivalTime() != null
    ) {
      stopTime.setFlexWindowStart(
        calculateOtpTime(
          passingTime.getEarliestDepartureTime(),
          passingTime.getEarliestDepartureDayOffset()
        )
      );
      stopTime.setFlexWindowEnd(
        calculateOtpTime(
          passingTime.getLatestArrivalTime(),
          passingTime.getLatestArrivalDayOffset()
        )
      );
    } else {
      return null;
    }

    if (stopPoint != null) {
      if (isFalse(stopPoint.isForAlighting())) {
        stopTime.setDropOffType(NONE);
      } else if (Boolean.TRUE.equals(stopPoint.isRequestStop())) {
        stopTime.setDropOffType(COORDINATE_WITH_DRIVER);
      } else {
        stopTime.setDropOffType(SCHEDULED);
      }

      if (isFalse(stopPoint.isForBoarding())) {
        stopTime.setPickupType(NONE);
      } else if (Boolean.TRUE.equals(stopPoint.isRequestStop())) {
        stopTime.setPickupType(COORDINATE_WITH_DRIVER);
      } else {
        stopTime.setPickupType(SCHEDULED);
      }

      if (stopPoint.getDestinationDisplayRef() != null) {
        DestinationDisplay destinationDisplay = destinationDisplayById.lookup(
          stopPoint.getDestinationDisplayRef().getRef()
        );

        if (destinationDisplay != null) {
          currentHeadSign = new NonLocalizedString(destinationDisplay.getFrontText().getValue());
          Vias_RelStructure viaValues = destinationDisplay.getVias();
          if (viaValues != null && viaValues.getVia() != null) {
            currentHeadSignVias = viaValues
              .getVia()
              .stream()
              .map(Via_VersionedChildStructure::getDestinationDisplayRef)
              .filter(Objects::nonNull)
              .map(VersionOfObjectRefStructure::getRef)
              .filter(Objects::nonNull)
              .map(destinationDisplayById::lookup)
              .filter(Objects::nonNull)
              .map(DestinationDisplay_VersionStructure::getFrontText)
              .filter(Objects::nonNull)
              .map(MultilingualString::getValue)
              .filter(Objects::nonNull)
              .collect(Collectors.toList());

            if (currentHeadSignVias.isEmpty()) {
              currentHeadSignVias = null;
            }
          } else {
            currentHeadSignVias = null;
          }
        }
      }
    }

    if (
      passingTime.getArrivalTime() == null &&
      passingTime.getDepartureTime() == null &&
      passingTime.getEarliestDepartureTime() == null &&
      passingTime.getLatestArrivalTime() == null
    ) {
      issueStore.add("TripWithoutTime", "Time missing for trip %s", trip.getId());
    }

    if (currentHeadSign != null) {
      stopTime.setStopHeadsign(currentHeadSign);
    }
    if (currentHeadSignVias != null) {
      stopTime.setHeadsignVias(currentHeadSignVias);
    }
    return stopTime;
  }

  @Nullable
  private StopLocation lookUpStopLocation(StopPointInJourneyPattern stopPointInJourneyPattern) {
    if (stopPointInJourneyPattern == null) {
      return null;
    }

    String stopPointRef = stopPointInJourneyPattern.getScheduledStopPointRef().getValue().getRef();

    String stopId = quayIdByStopPointRef.lookup(stopPointRef);
    String flexibleStopPlaceId = flexibleStopPlaceIdByStopPointRef.lookup(stopPointRef);

    if (stopId == null && flexibleStopPlaceId == null) {
      issueStore.add(
        "PassengerStopAssignmentNotFound",
        "No passengerStopAssignment found for %s",
        stopPointRef
      );
      return null;
    }

    StopLocation stopLocation;
    if (stopId != null) {
      stopLocation = stopsById.get(idFactory.createId(stopId));
    } else {
      AreaStop areaStop = flexibleStopLocationsById.get(idFactory.createId(flexibleStopPlaceId));
      GroupStop groupStop = groupStopById.get(idFactory.createId(flexibleStopPlaceId));

      if (areaStop != null) {
        stopLocation = areaStop;
      } else stopLocation = groupStop;
    }

    if (stopLocation == null) {
      issueStore.add(
        "StopPointInJourneyPatternMissingStopLocation",
        "No Quay or FlexibleStopPlace found for %s",
        stopPointRef
      );
    }

    return stopLocation;
  }

  private FlexibleLine lookUpFlexibleLine(
    ServiceJourney serviceJourney,
    JourneyPattern_VersionStructure journeyPattern
  ) {
    if (serviceJourney == null) {
      return null;
    }
    String lineRef = null;
    // Check for direct connection to Line
    JAXBElement<? extends LineRefStructure> lineRefStruct = serviceJourney.getLineRef();

    if (lineRefStruct != null) {
      // Connect to Line referenced directly from ServiceJourney
      lineRef = lineRefStruct.getValue().getRef();
    } else if (serviceJourney.getJourneyPatternRef() != null) {
      // Connect to Line referenced through JourneyPattern->Route
      lineRef = JourneyPatternHelper.getLineFromRoute(routeByid, journeyPattern);
    }
    return flexibleLinesById.lookup(lineRef);
  }
}
