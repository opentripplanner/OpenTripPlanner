package org.opentripplanner.netex.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.opentripplanner.netex.index.api.HMapValidationRule;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMapById;
import org.opentripplanner.netex.mapping.NetexTestDataSample;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.JourneyPattern_VersionStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.TimetabledPassingTime;

class ServiceJourneyNonIncreasingPassingTimeTest {

  @Test
  void testValidateServiceJourneyWithRegularStop() {
    NetexTestDataSample sample = new NetexTestDataSample();
    ServiceJourney serviceJourney = sample.getServiceJourney();
    JourneyPattern journeyPattern = sample.getJourneyPattern();

    NetexEntityIndex netexEntityIndex = new NetexEntityIndex();
    netexEntityIndex.journeyPatternsById.add(journeyPattern);
    netexEntityIndex.serviceJourneyById.add(serviceJourney);

    ServiceJourneyNonIncreasingPassingTime serviceJourneyNonIncreasingPassingTime =
      new ServiceJourneyNonIncreasingPassingTime();
    serviceJourneyNonIncreasingPassingTime.setup(netexEntityIndex.readOnlyView());

    assertEquals(
      HMapValidationRule.Status.OK,
      serviceJourneyNonIncreasingPassingTime.validate(serviceJourney)
    );
  }

  @Test
  void testValidateServiceJourneyWithRegularStopMissingTime() {
    NetexTestDataSample sample = new NetexTestDataSample();
    ServiceJourney serviceJourney = sample.getServiceJourney();
    JourneyPattern journeyPattern = sample.getJourneyPattern();

    // remove arrival time and departure time
    TimetabledPassingTime timetabledPassingTime = getFirstPassingTime(serviceJourney);
    timetabledPassingTime.withArrivalTime(null).withDepartureTime(null);

    NetexEntityIndex netexEntityIndex = new NetexEntityIndex();
    netexEntityIndex.journeyPatternsById.add(journeyPattern);
    netexEntityIndex.serviceJourneyById.add(serviceJourney);

    ServiceJourneyNonIncreasingPassingTime serviceJourneyNonIncreasingPassingTime =
      new ServiceJourneyNonIncreasingPassingTime();
    serviceJourneyNonIncreasingPassingTime.setup(netexEntityIndex.readOnlyView());

    assertEquals(
      HMapValidationRule.Status.DISCARD,
      serviceJourneyNonIncreasingPassingTime.validate(serviceJourney)
    );
  }

  @Test
  void testValidateServiceJourneyWithRegularStopInconsistentTime() {
    NetexTestDataSample sample = new NetexTestDataSample();
    ServiceJourney serviceJourney = sample.getServiceJourney();
    JourneyPattern journeyPattern = sample.getJourneyPattern();

    // set arrival time after departure time
    TimetabledPassingTime timetabledPassingTime = getFirstPassingTime(serviceJourney);
    timetabledPassingTime.withArrivalTime(timetabledPassingTime.getDepartureTime().plusMinutes(1));

    ServiceJourneyNonIncreasingPassingTime serviceJourneyNonIncreasingPassingTime =
      new ServiceJourneyNonIncreasingPassingTime();
    NetexEntityIndex netexEntityIndex = new NetexEntityIndex();
    netexEntityIndex.journeyPatternsById.add(journeyPattern);
    netexEntityIndex.serviceJourneyById.add(serviceJourney);
    serviceJourneyNonIncreasingPassingTime.setup(netexEntityIndex.readOnlyView());
    HMapValidationRule.Status status = serviceJourneyNonIncreasingPassingTime.validate(
      serviceJourney
    );

    assertEquals(HMapValidationRule.Status.DISCARD, status);
  }

  @Test
  void testValidateServiceJourneyWithAreaStop() {
    NetexTestDataSample sample = new NetexTestDataSample();
    ServiceJourney serviceJourney = sample.getServiceJourney();
    JourneyPattern journeyPattern = sample.getJourneyPattern();

    HierarchicalMapById<ServiceJourney> serviceJourneyById = sample.getServiceJourneyById();

    // remove arrival time and departure time and add flex window
    TimetabledPassingTime timetabledPassingTime = getFirstPassingTime(serviceJourney);
    timetabledPassingTime
      .withArrivalTime(null)
      .withDepartureTime(null)
      .withEarliestDepartureTime(LocalTime.MIDNIGHT)
      .withLatestArrivalTime(LocalTime.MIDNIGHT.plusMinutes(1));

    ServiceJourneyNonIncreasingPassingTime serviceJourneyNonIncreasingPassingTime =
      new ServiceJourneyNonIncreasingPassingTime();
    NetexEntityIndex netexEntityIndex = new NetexEntityIndex();
    netexEntityIndex.journeyPatternsById.add(journeyPattern);
    netexEntityIndex.serviceJourneyById.addAll(serviceJourneyById.localValues());
    netexEntityIndex.flexibleStopPlaceByStopPointRef.add(
      getFirstScheduledStopPointRef(journeyPattern),
      null
    );
    serviceJourneyNonIncreasingPassingTime.setup(netexEntityIndex.readOnlyView());

    assertEquals(
      HMapValidationRule.Status.OK,
      serviceJourneyNonIncreasingPassingTime.validate(serviceJourney)
    );
  }

  @Test
  void testValidateServiceJourneyWithAreaStopMissingTimeWindow() {
    NetexTestDataSample sample = new NetexTestDataSample();
    ServiceJourney serviceJourney = sample.getServiceJourney();
    JourneyPattern journeyPattern = sample.getJourneyPattern();

    HierarchicalMapById<ServiceJourney> serviceJourneyById = sample.getServiceJourneyById();

    // remove arrival time and departure time and add flex window
    TimetabledPassingTime timetabledPassingTime = getFirstPassingTime(serviceJourney);
    timetabledPassingTime.withArrivalTime(null).withDepartureTime(null);

    ServiceJourneyNonIncreasingPassingTime serviceJourneyNonIncreasingPassingTime =
      new ServiceJourneyNonIncreasingPassingTime();
    NetexEntityIndex netexEntityIndex = new NetexEntityIndex();
    netexEntityIndex.journeyPatternsById.add(journeyPattern);
    netexEntityIndex.serviceJourneyById.addAll(serviceJourneyById.localValues());
    StopPointInJourneyPattern stopPointInJourneyPattern = (StopPointInJourneyPattern) journeyPattern
      .getPointsInSequence()
      .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
      .get(0);
    netexEntityIndex.flexibleStopPlaceByStopPointRef.add(
      stopPointInJourneyPattern.getScheduledStopPointRef().getValue().getRef(),
      null
    );
    serviceJourneyNonIncreasingPassingTime.setup(netexEntityIndex.readOnlyView());

    assertEquals(
      HMapValidationRule.Status.DISCARD,
      serviceJourneyNonIncreasingPassingTime.validate(serviceJourney)
    );
  }

  @Test
  void testValidateServiceJourneyWithAreaStopInconsistentTimeWindow() {
    NetexTestDataSample sample = new NetexTestDataSample();
    ServiceJourney serviceJourney = sample.getServiceJourney();
    JourneyPattern journeyPattern = sample.getJourneyPattern();

    HierarchicalMapById<ServiceJourney> serviceJourneyById = sample.getServiceJourneyById();

    // remove arrival time and departure time and add flex window
    TimetabledPassingTime timetabledPassingTime = serviceJourney
      .getPassingTimes()
      .getTimetabledPassingTime()
      .get(0);
    timetabledPassingTime
      .withArrivalTime(null)
      .withDepartureTime(null)
      .withEarliestDepartureTime(LocalTime.MIDNIGHT.plusMinutes(1))
      .withLatestArrivalTime(LocalTime.MIDNIGHT);

    ServiceJourneyNonIncreasingPassingTime serviceJourneyNonIncreasingPassingTime =
      new ServiceJourneyNonIncreasingPassingTime();
    NetexEntityIndex netexEntityIndex = new NetexEntityIndex();
    netexEntityIndex.journeyPatternsById.add(journeyPattern);
    netexEntityIndex.serviceJourneyById.addAll(serviceJourneyById.localValues());
    StopPointInJourneyPattern stopPointInJourneyPattern = (StopPointInJourneyPattern) journeyPattern
      .getPointsInSequence()
      .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
      .get(0);
    netexEntityIndex.flexibleStopPlaceByStopPointRef.add(
      stopPointInJourneyPattern.getScheduledStopPointRef().getValue().getRef(),
      null
    );
    serviceJourneyNonIncreasingPassingTime.setup(netexEntityIndex.readOnlyView());

    assertEquals(
      HMapValidationRule.Status.DISCARD,
      serviceJourneyNonIncreasingPassingTime.validate(serviceJourney)
    );
  }

  @Test
  void testValidateServiceJourneyWithRegularStopFollowedByRegularStopNonIncreasingTime() {
    NetexTestDataSample sample = new NetexTestDataSample();
    ServiceJourney serviceJourney = sample.getServiceJourney();
    JourneyPattern journeyPattern = sample.getJourneyPattern();

    // remove arrival time and departure time and add flex window on second stop
    TimetabledPassingTime firstPassingTime = getFirstPassingTime(serviceJourney);
    TimetabledPassingTime secondPassingTime = getSecondPassingTime(serviceJourney);
    secondPassingTime.withArrivalTime(firstPassingTime.getDepartureTime().minusMinutes(1));

    NetexEntityIndex netexEntityIndex = new NetexEntityIndex();
    netexEntityIndex.journeyPatternsById.add(journeyPattern);

    ServiceJourneyNonIncreasingPassingTime serviceJourneyNonIncreasingPassingTime =
      new ServiceJourneyNonIncreasingPassingTime();
    serviceJourneyNonIncreasingPassingTime.setup(netexEntityIndex.readOnlyView());

    assertEquals(
      HMapValidationRule.Status.DISCARD,
      serviceJourneyNonIncreasingPassingTime.validate(serviceJourney)
    );
  }

  /**
   * This test makes sure all passing times are complete and consistent, before it checks for
   * increasing times.
   */
  @Test
  void testValidateWithRegularStopFollowedByRegularStopWithMissingTime() {
    var sample = new NetexTestDataSample();
    var serviceJourney = sample.getServiceJourney();
    var journeyPattern = sample.getJourneyPattern();

    // Set arrivalTime AFTER departure time (not valid)
    getSecondPassingTime(serviceJourney).withDepartureTime(null).withArrivalTime(null);

    var entityIndex = new NetexEntityIndex();
    entityIndex.journeyPatternsById.add(journeyPattern);

    var subject = new ServiceJourneyNonIncreasingPassingTime();
    subject.setup(entityIndex.readOnlyView());

    assertEquals(HMapValidationRule.Status.DISCARD, subject.validate(serviceJourney));
  }

  @Test
  void testValidateServiceJourneyWithRegularStopFollowedByStopArea() {
    NetexTestDataSample sample = new NetexTestDataSample();
    ServiceJourney serviceJourney = sample.getServiceJourney();
    JourneyPattern journeyPattern = sample.getJourneyPattern();

    // remove arrival time and departure time and add flex window on second stop
    TimetabledPassingTime timetabledPassingTime = getSecondPassingTime(serviceJourney);
    timetabledPassingTime
      .withEarliestDepartureTime(timetabledPassingTime.getDepartureTime())
      .withLatestArrivalTime(timetabledPassingTime.getDepartureTime().plusMinutes(1))
      .withArrivalTime(null)
      .withDepartureTime(null);

    NetexEntityIndex netexEntityIndex = new NetexEntityIndex();
    netexEntityIndex.journeyPatternsById.add(journeyPattern);
    netexEntityIndex.serviceJourneyById.add(serviceJourney);
    netexEntityIndex.flexibleStopPlaceByStopPointRef.add(
      getSecondScheduledStopPointRef(journeyPattern),
      null
    );

    ServiceJourneyNonIncreasingPassingTime serviceJourneyNonIncreasingPassingTime =
      new ServiceJourneyNonIncreasingPassingTime();
    serviceJourneyNonIncreasingPassingTime.setup(netexEntityIndex.readOnlyView());

    assertEquals(
      HMapValidationRule.Status.OK,
      serviceJourneyNonIncreasingPassingTime.validate(serviceJourney)
    );
  }

  @Test
  void testValidateServiceJourneyWithRegularStopFollowedByStopAreaNonIncreasingTime() {
    NetexTestDataSample sample = new NetexTestDataSample();
    ServiceJourney serviceJourney = sample.getServiceJourney();
    JourneyPattern journeyPattern = sample.getJourneyPattern();

    // remove arrival time and departure time and add flex window with decreasing time on second stop
    TimetabledPassingTime firstPassingTime = getFirstPassingTime(serviceJourney);
    TimetabledPassingTime secondPassingTime = getSecondPassingTime(serviceJourney);
    secondPassingTime
      .withEarliestDepartureTime(firstPassingTime.getDepartureTime().minusMinutes(1))
      .withArrivalTime(null)
      .withDepartureTime(null);

    NetexEntityIndex netexEntityIndex = new NetexEntityIndex();
    netexEntityIndex.journeyPatternsById.add(journeyPattern);
    netexEntityIndex.serviceJourneyById.add(serviceJourney);
    netexEntityIndex.flexibleStopPlaceByStopPointRef.add(
      getFirstScheduledStopPointRef(journeyPattern),
      null
    );

    ServiceJourneyNonIncreasingPassingTime serviceJourneyNonIncreasingPassingTime =
      new ServiceJourneyNonIncreasingPassingTime();
    serviceJourneyNonIncreasingPassingTime.setup(netexEntityIndex.readOnlyView());

    assertEquals(
      HMapValidationRule.Status.DISCARD,
      serviceJourneyNonIncreasingPassingTime.validate(serviceJourney)
    );
  }

  @Test
  void testValidateServiceJourneyWithStopAreaFollowedByRegularStop() {
    NetexTestDataSample sample = new NetexTestDataSample();
    ServiceJourney serviceJourney = sample.getServiceJourney();
    JourneyPattern journeyPattern = sample.getJourneyPattern();

    // remove arrival time and departure time and add flex window on first stop
    TimetabledPassingTime firstPassingTime = getFirstPassingTime(serviceJourney);
    firstPassingTime
      .withEarliestDepartureTime(firstPassingTime.getDepartureTime())
      .withLatestArrivalTime(firstPassingTime.getDepartureTime())
      .withArrivalTime(null)
      .withDepartureTime(null);

    NetexEntityIndex netexEntityIndex = new NetexEntityIndex();
    netexEntityIndex.journeyPatternsById.add(journeyPattern);
    netexEntityIndex.serviceJourneyById.add(serviceJourney);
    netexEntityIndex.flexibleStopPlaceByStopPointRef.add(
      getFirstScheduledStopPointRef(journeyPattern),
      null
    );

    ServiceJourneyNonIncreasingPassingTime serviceJourneyNonIncreasingPassingTime =
      new ServiceJourneyNonIncreasingPassingTime();
    serviceJourneyNonIncreasingPassingTime.setup(netexEntityIndex.readOnlyView());

    assertEquals(
      HMapValidationRule.Status.OK,
      serviceJourneyNonIncreasingPassingTime.validate(serviceJourney)
    );
  }

  @Test
  void testValidateServiceJourneyWithStopAreaFollowedByStopArea() {
    NetexTestDataSample sample = new NetexTestDataSample();
    ServiceJourney serviceJourney = sample.getServiceJourney();
    JourneyPattern journeyPattern = sample.getJourneyPattern();

    // remove arrival time and departure time and add flex window on first stop
    TimetabledPassingTime firstPassingTime = getFirstPassingTime(serviceJourney);
    firstPassingTime
      .withEarliestDepartureTime(firstPassingTime.getDepartureTime())
      .withLatestArrivalTime(firstPassingTime.getDepartureTime())
      .withArrivalTime(null)
      .withDepartureTime(null);

    TimetabledPassingTime secondPassingTime = getSecondPassingTime(serviceJourney);
    secondPassingTime
      .withEarliestDepartureTime(secondPassingTime.getDepartureTime())
      .withLatestArrivalTime(secondPassingTime.getDepartureTime().plusMinutes(1))
      .withArrivalTime(null)
      .withDepartureTime(null);

    NetexEntityIndex netexEntityIndex = new NetexEntityIndex();
    netexEntityIndex.journeyPatternsById.add(journeyPattern);
    netexEntityIndex.serviceJourneyById.add(serviceJourney);
    netexEntityIndex.flexibleStopPlaceByStopPointRef.add(
      getFirstScheduledStopPointRef(journeyPattern),
      null
    );
    netexEntityIndex.flexibleStopPlaceByStopPointRef.add(
      getSecondScheduledStopPointRef(journeyPattern),
      null
    );
    ServiceJourneyNonIncreasingPassingTime serviceJourneyNonIncreasingPassingTime =
      new ServiceJourneyNonIncreasingPassingTime();
    serviceJourneyNonIncreasingPassingTime.setup(netexEntityIndex.readOnlyView());

    assertEquals(
      HMapValidationRule.Status.OK,
      serviceJourneyNonIncreasingPassingTime.validate(serviceJourney)
    );
  }

  @Test
  void testValidateServiceJourneyWithStopAreaFollowedByStopAreaNonIncreasingTime() {
    NetexTestDataSample sample = new NetexTestDataSample();
    ServiceJourney serviceJourney = sample.getServiceJourney();
    JourneyPattern journeyPattern = sample.getJourneyPattern();

    // remove arrival time and departure time and add flex window on first stop and second stop
    // and add decreasing time on second stop
    TimetabledPassingTime firstPassingTime = getFirstPassingTime(serviceJourney);
    firstPassingTime
      .withEarliestDepartureTime(firstPassingTime.getDepartureTime())
      .withLatestArrivalTime(firstPassingTime.getDepartureTime())
      .withArrivalTime(null)
      .withDepartureTime(null);

    TimetabledPassingTime secondPassingTime = getSecondPassingTime(serviceJourney);
    secondPassingTime
      .withEarliestDepartureTime(firstPassingTime.getEarliestDepartureTime().minusMinutes(1))
      .withLatestArrivalTime(secondPassingTime.getEarliestDepartureTime().plusMinutes(1))
      .withArrivalTime(null)
      .withDepartureTime(null);

    NetexEntityIndex netexEntityIndex = new NetexEntityIndex();
    netexEntityIndex.journeyPatternsById.add(journeyPattern);
    netexEntityIndex.serviceJourneyById.add(serviceJourney);
    netexEntityIndex.flexibleStopPlaceByStopPointRef.add(
      getFirstScheduledStopPointRef(journeyPattern),
      null
    );
    netexEntityIndex.flexibleStopPlaceByStopPointRef.add(
      getSecondScheduledStopPointRef(journeyPattern),
      null
    );
    ServiceJourneyNonIncreasingPassingTime serviceJourneyNonIncreasingPassingTime =
      new ServiceJourneyNonIncreasingPassingTime();
    serviceJourneyNonIncreasingPassingTime.setup(netexEntityIndex.readOnlyView());

    assertEquals(
      HMapValidationRule.Status.DISCARD,
      serviceJourneyNonIncreasingPassingTime.validate(serviceJourney)
    );
  }

  @Test
  void testValidateServiceJourneyWithStopAreaFollowedByRegularStopNonIncreasingTime() {
    NetexTestDataSample sample = new NetexTestDataSample();
    ServiceJourney serviceJourney = sample.getServiceJourney();
    JourneyPattern journeyPattern = sample.getJourneyPattern();

    // remove arrival time and departure time and add flex window on first stop
    // and add decreasing time on second stop
    TimetabledPassingTime firstPassingTime = getFirstPassingTime(serviceJourney);
    firstPassingTime
      .withEarliestDepartureTime(firstPassingTime.getDepartureTime())
      .withLatestArrivalTime(firstPassingTime.getDepartureTime())
      .withArrivalTime(null)
      .withDepartureTime(null);

    TimetabledPassingTime secondPassingTime = getSecondPassingTime(serviceJourney);
    secondPassingTime
      .withArrivalTime(firstPassingTime.getLatestArrivalTime().minusMinutes(1))
      .withDepartureTime(null);

    NetexEntityIndex netexEntityIndex = new NetexEntityIndex();
    netexEntityIndex.journeyPatternsById.add(journeyPattern);
    netexEntityIndex.serviceJourneyById.add(serviceJourney);
    netexEntityIndex.flexibleStopPlaceByStopPointRef.add(
      getFirstScheduledStopPointRef(journeyPattern),
      null
    );

    ServiceJourneyNonIncreasingPassingTime serviceJourneyNonIncreasingPassingTime =
      new ServiceJourneyNonIncreasingPassingTime();
    serviceJourneyNonIncreasingPassingTime.setup(netexEntityIndex.readOnlyView());

    assertEquals(
      HMapValidationRule.Status.DISCARD,
      serviceJourneyNonIncreasingPassingTime.validate(serviceJourney)
    );
  }

  private TimetabledPassingTime getPassingTime(ServiceJourney serviceJourney, int order) {
    return serviceJourney.getPassingTimes().getTimetabledPassingTime().get(order);
  }

  private TimetabledPassingTime getFirstPassingTime(ServiceJourney serviceJourney) {
    return getPassingTime(serviceJourney, 0);
  }

  private TimetabledPassingTime getSecondPassingTime(ServiceJourney serviceJourney) {
    return getPassingTime(serviceJourney, 1);
  }

  private String getScheduledStopPointRef(
    JourneyPattern_VersionStructure journeyPattern,
    int order
  ) {
    return (
      (StopPointInJourneyPattern) journeyPattern
        .getPointsInSequence()
        .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
        .get(order)
    ).getScheduledStopPointRef()
      .getValue()
      .getRef();
  }

  private String getFirstScheduledStopPointRef(JourneyPattern_VersionStructure journeyPattern) {
    return getScheduledStopPointRef(journeyPattern, 0);
  }

  private String getSecondScheduledStopPointRef(JourneyPattern_VersionStructure journeyPattern) {
    return getScheduledStopPointRef(journeyPattern, 1);
  }
}
