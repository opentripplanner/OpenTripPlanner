package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Transfer;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.TripStopTimes;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.TransferPriority;
import org.opentripplanner.transit.model.site.StopTransferPriority;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;

public class TransferMapperTest {

  private static final GtfsTestData testData = new GtfsTestData();

  private static final String FEED_ID = "FEED";

  private static final TranslationHelper TRANSLATION_HELPER = new TranslationHelper();

  private static final DataImportIssueStore ISSUE_STORE = DataImportIssueStore.NOOP;
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
  private static final BookingRuleMapper BOOKING_RULE_MAPPER = new BookingRuleMapper();

  private static final LocationMapper LOCATION_MAPPER = new LocationMapper(
    ID_FACTORY,
    SITE_REPOSITORY_BUILDER,
    ISSUE_STORE
  );

  private static final LocationGroupMapper LOCATION_GROUP_MAPPER = new LocationGroupMapper(
    ID_FACTORY,
    STOP_MAPPER,
    LOCATION_MAPPER,
    SITE_REPOSITORY_BUILDER
  );
  private static StopTimeMapper STOP_TIME_MAPPER;

  private static final Integer ID = 45;

  private static final Route FROM_ROUTE = testData.route;

  private static final Stop FROM_STOP = testData.stop_3;

  private static final Trip FROM_TRIP = testData.trip;

  private static final Route TO_ROUTE = testData.route_2;

  private static final Stop TO_STOP = testData.stop_3;

  private static final Trip TO_TRIP = testData.trip_2;

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
      STOP_MAPPER,
      LOCATION_MAPPER,
      LOCATION_GROUP_MAPPER,
      new TripMapper(
        ID_FACTORY,
        new RouteMapper(ID_FACTORY, new AgencyMapper(ID_FACTORY), ISSUE_STORE, TRANSLATION_HELPER),
        new DirectionMapper(ISSUE_STORE),
        TRANSLATION_HELPER
      ),
      BOOKING_RULE_MAPPER,
      new TranslationHelper()
    );

    tripStopTimes = new TripStopTimes();

    transfer = new Transfer();
    transfer.setId(ID);
    transfer.setFromRoute(FROM_ROUTE);
    transfer.setFromStop(FROM_STOP);
    transfer.setFromTrip(FROM_TRIP);
    transfer.setToRoute(TO_ROUTE);
    transfer.setToStop(TO_STOP);
    transfer.setToTrip(TO_TRIP);
    transfer.setMinTransferTime(MIN_TRANSFER_TIME);
    transfer.setTransferType(3);
  }

  @Test
  public void testEmptyMapCollection() {
    tripStopTimes.addAll(STOP_TIME_MAPPER.map(createStopTimes()));

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
    tripStopTimes.addAll(STOP_TIME_MAPPER.map(createStopTimes()));

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
    tripStopTimes.addAll(STOP_TIME_MAPPER.map(createStopTimes()));
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
    tripStopTimes.addAll(STOP_TIME_MAPPER.map(createStopTimes()));

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
    var stop_1 = new Stop();
    stop_1.setId(new AgencyAndId("F", "S1"));
    var stop_2 = new Stop();
    stop_2.setId(new AgencyAndId("F", "S2"));
    var stop_3 = new Stop();
    stop_3.setId(new AgencyAndId("F", "S3"));

    var stopTimes = List.of(
      createStopTime(stop_1, 0, 0, FROM_TRIP),
      createStopTime(stop_2, 1, 1, FROM_TRIP),
      createStopTime(stop_3, 2, 2, FROM_TRIP),
      createStopTime(stop_1, 3, 3, FROM_TRIP),
      createStopTime(stop_1, 4, 0, TO_TRIP),
      createStopTime(stop_2, 5, 1, TO_TRIP),
      createStopTime(stop_3, 6, 2, TO_TRIP),
      createStopTime(stop_1, 7, 3, TO_TRIP)
    );

    tripStopTimes.addAll(STOP_TIME_MAPPER.map(stopTimes));

    transfer.setId(ID);
    transfer.setFromRoute(FROM_ROUTE);
    transfer.setFromStop(stop_1);
    transfer.setFromTrip(FROM_TRIP);
    transfer.setToRoute(TO_ROUTE);
    transfer.setToStop(stop_1);
    transfer.setToTrip(TO_TRIP);
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

  private static StopTime createStopTime(Stop stop, int id, int stopSeq, Trip trip) {
    StopTime stopTime = new StopTime();
    stopTime.setStop(stop);
    stopTime.setId(id);
    stopTime.setStopSequence(stopSeq);
    stopTime.setTrip(trip);

    return stopTime;
  }

  private static List<StopTime> createStopTimes() {
    return List.of(
      createStopTime(testData.stop, 0, 0, FROM_TRIP),
      createStopTime(testData.stop_2, 1, 1, FROM_TRIP),
      createStopTime(testData.stop_3, 2, 2, FROM_TRIP),
      createStopTime(testData.stop_3, 3, 0, TO_TRIP),
      createStopTime(testData.stop_4, 4, 1, TO_TRIP),
      createStopTime(testData.stop_5, 5, 2, TO_TRIP)
    );
  }
}
