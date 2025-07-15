package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Transfer;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TripStopTimes;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.TransferPriority;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopTransferPriority;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;

public class TransferMapperTest {

  private static final GtfsTestData testData = new GtfsTestData();

  private static final String FEED_ID = "FEED";

  private static final TranslationHelper TRANSLATION_HELPER = new TranslationHelper();

  private static final DataImportIssueStore ISSUE_STORE = DataImportIssueStore.NOOP;
  public final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();
  private static RouteMapper ROUTE_MAPPER;

  private static TripMapper TRIP_MAPPER;

  private static StationMapper STATION_MAPPER;

  private static final SiteRepositoryBuilder SITE_REPOSITORY_BUILDER = SiteRepository.of();

  private static final IdFactory ID_FACTORY = new IdFactory(FEED_ID);

  private static final StopMapper STOP_MAPPER = new StopMapper(
    ID_FACTORY,
    TRANSLATION_HELPER,
    stationId -> null,
    SITE_REPOSITORY_BUILDER
  );

  private static StopTimeMapper STOP_TIME_MAPPER;

  private static final Integer ID = 45;

  private static final Route FROM_ROUTE = testData.route;

  private static final Stop FROM_STOP = testData.stop_3;

  private static final Trip FROM_TRIP = TimetableRepositoryForTest.trip("t1").build();

  private static final Route TO_ROUTE = testData.route_2;

  private static final Stop TO_STOP = testData.stop_3;

  private static final Trip TO_TRIP = TimetableRepositoryForTest.trip("t2").build();

  private static final int MIN_TRANSFER_TIME = 200;

  private static final TransferPriority TRANSFER_TYPE = TransferPriority.NOT_ALLOWED;

  private Transfer transfer;

  private TripStopTimes tripStopTimes = new TripStopTimes();

  @BeforeEach
  void prepare() {
    ROUTE_MAPPER = new RouteMapper(
      ID_FACTORY,
      new AgencyMapper(ID_FACTORY),
      ISSUE_STORE,
      new TranslationHelper()
    );

    TRIP_MAPPER = new TripMapper(
      ID_FACTORY,
      ROUTE_MAPPER,
      new DirectionMapper(ISSUE_STORE),
      TRANSLATION_HELPER
    );

    STATION_MAPPER = new StationMapper(
      ID_FACTORY,
      TRANSLATION_HELPER,
      StopTransferPriority.ALLOWED
    );

    STOP_TIME_MAPPER = new StopTimeMapper(
      ID_FACTORY,
      null,
      new BookingRuleMapper(ID_FACTORY),
      new TranslationHelper()
    );

    tripStopTimes = new TripStopTimes();

    transfer = new Transfer();
    transfer.setId(ID);
    transfer.setFromRoute(FROM_ROUTE);
    transfer.setFromStop(FROM_STOP);
    transfer.setFromTrip(testData.trip_2);
    transfer.setToRoute(TO_ROUTE);
    transfer.setToStop(TO_STOP);
    transfer.setToTrip(testData.trip);
    transfer.setMinTransferTime(MIN_TRANSFER_TIME);
    transfer.setTransferType(3);
  }

  @Test
  public void testEmptyMapCollection() {
    tripStopTimes.addAll(createStopTimes());

    TransferMapper transferMapper = new TransferMapper(
      ROUTE_MAPPER,
      STATION_MAPPER,
      STOP_MAPPER,
      TRIP_MAPPER,
      tripStopTimes,
      false,
      ISSUE_STORE
    );
    assertTrue(transferMapper.map(Collections.emptyList()).constrainedTransfers().isEmpty());
  }

  @Test
  public void testMapCollection() throws Exception {
    tripStopTimes.addAll(createStopTimes());

    TransferMapper transferMapper = new TransferMapper(
      ROUTE_MAPPER,
      STATION_MAPPER,
      STOP_MAPPER,
      TRIP_MAPPER,
      tripStopTimes,
      false,
      ISSUE_STORE
    );
    assertEquals(
      1,
      transferMapper.map(Collections.singleton(transfer)).constrainedTransfers().size()
    );
  }

  @Test
  public void testMapStopTransfer() {
    tripStopTimes.addAll(createStopTimes());
    transfer.setFromTrip(null);
    transfer.setToTrip(null);
    transfer.setFromRoute(null);
    transfer.setToRoute(null);
    TransferMapper transferMapper = new TransferMapper(
      ROUTE_MAPPER,
      STATION_MAPPER,
      STOP_MAPPER,
      TRIP_MAPPER,
      tripStopTimes,
      false,
      ISSUE_STORE
    );
    ConstrainedTransfer result = transferMapper.map(transfer);

    assertNotNull(result);
    assertNotNull(result.getFrom());
    assertNotNull(result.getFrom().asStopTransferPoint().getStop());
    assertNotNull(result.getTo());
    assertNotNull(result.getTo().asStopTransferPoint().getStop());
  }

  @Test
  public void testMap() throws Exception {
    tripStopTimes.addAll(createStopTimes());

    TransferMapper transferMapper = new TransferMapper(
      ROUTE_MAPPER,
      STATION_MAPPER,
      STOP_MAPPER,
      TRIP_MAPPER,
      tripStopTimes,
      false,
      ISSUE_STORE
    );
    ConstrainedTransfer result = transferMapper.map(transfer);
    assertNotNull(result.getFrom());
    assertNotNull(result.getFrom().asTripTransferPoint().getTrip());
    assertNotNull(result.getTo());
    assertNotNull(result.getTo().asTripTransferPoint().getTrip());
    assertEquals(MIN_TRANSFER_TIME, result.getTransferConstraint().getMinTransferTime());
    assertEquals(TRANSFER_TYPE, result.getTransferConstraint().getPriority());
  }

  @Test
  public void testFromToSameStation() {
    var stop_1 = testModel.stop("S1").build();
    var stop_2 = testModel.stop("S2").build();
    var stop_3 = testModel.stop("S3").build();

    var stopTimes = Stream.of(
      createStopTime(stop_1, 0, FROM_TRIP),
      createStopTime(stop_2, 1, FROM_TRIP),
      createStopTime(stop_3, 2, FROM_TRIP),
      createStopTime(stop_1, 3, FROM_TRIP),
      createStopTime(stop_1, 0, TO_TRIP),
      createStopTime(stop_2, 1, TO_TRIP),
      createStopTime(stop_3, 2, TO_TRIP),
      createStopTime(stop_1, 3, TO_TRIP)
    );

    tripStopTimes.addAll(stopTimes);

    transfer.setId(ID);
    transfer.setFromRoute(FROM_ROUTE);
    transfer.setFromStop(stop("S1"));
    transfer.setFromTrip(testData.trip_2);
    transfer.setToRoute(TO_ROUTE);
    transfer.setToStop(stop("S2"));
    transfer.setToTrip(testData.trip);
    transfer.setMinTransferTime(MIN_TRANSFER_TIME);
    transfer.setTransferType(3);

    ConstrainedTransfer result = new TransferMapper(
      ROUTE_MAPPER,
      STATION_MAPPER,
      STOP_MAPPER,
      TRIP_MAPPER,
      tripStopTimes,
      false,
      ISSUE_STORE
    ).map(transfer);

    assertNotNull(result);
    assertNotNull(result.getFrom());
    assertNotNull(result.getFrom().asTripTransferPoint().getTrip());
    assertNotNull(result.getTo());
    assertNotNull(result.getTo().asTripTransferPoint().getTrip());
    assertEquals(MIN_TRANSFER_TIME, result.getTransferConstraint().getMinTransferTime());
    assertEquals(TRANSFER_TYPE, result.getTransferConstraint().getPriority());
  }

  private static StopTime createStopTime(RegularStop stop, int stopSeq, Trip trip) {
    StopTime stopTime = new StopTime();
    stopTime.setStop(stop);
    stopTime.setStopSequence(stopSeq);
    stopTime.setTrip(trip);
    return stopTime;
  }

  private static Stop stop(String id) {
    var s = new Stop();
    s.setId(new AgencyAndId("a", id));
    return s;
  }

  private Stream<StopTime> createStopTimes() {
    return Stream.of(
      createStopTime(testModel.stop("1").build(), 0, FROM_TRIP),
      createStopTime(testModel.stop("2").build(), 1, FROM_TRIP),
      createStopTime(testModel.stop("3").build(), 2, FROM_TRIP),
      createStopTime(testModel.stop("3").build(), 0, TO_TRIP),
      createStopTime(testModel.stop("4").build(), 1, TO_TRIP),
      createStopTime(testModel.stop("5").build(), 2, TO_TRIP)
    );
  }
}
