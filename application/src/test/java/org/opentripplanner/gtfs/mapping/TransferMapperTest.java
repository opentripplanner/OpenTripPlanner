package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Transfer;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transfer.constrained.model.DeferredConstrainedTransferPoint;
import org.opentripplanner.transfer.constrained.model.PendingConstrainedTransfer;
import org.opentripplanner.transfer.constrained.model.TransferPriority;
import org.opentripplanner.transit.model.site.StopTransferPriority;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;

public class TransferMapperTest {

  private static final GtfsTestData TEST_DATA = new GtfsTestData();

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

  private static final Integer ID = 45;

  private static final Route FROM_ROUTE = TEST_DATA.route;

  private static final Stop FROM_STOP = TEST_DATA.stop_3;

  private static final Trip FROM_TRIP = TEST_DATA.trip;

  private static final Route TO_ROUTE = TEST_DATA.route_2;

  private static final Stop TO_STOP = TEST_DATA.stop_3;

  private static final Trip TO_TRIP = TEST_DATA.trip_2;

  private static final int MIN_TRANSFER_TIME = 200;

  private static final TransferPriority TRANSFER_TYPE = TransferPriority.NOT_ALLOWED;

  private Transfer transfer;

  @BeforeEach
  void prepare() {
    ROUTE_MAPPER = new RouteMapper(
      ID_FACTORY,
      new AgencyMapper(ID_FACTORY),
      new RouteNetworkAssignmentMapper(ID_FACTORY),
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
    TransferMapper transferMapper = new TransferMapper(
      ROUTE_MAPPER,
      STATION_MAPPER,
      STOP_MAPPER,
      TRIP_MAPPER,
      false,
      ISSUE_STORE
    );
    assertTrue(transferMapper.map(Collections.emptyList()).pendingConstrainedTransfers().isEmpty());
  }

  @Test
  public void testMapCollection() throws Exception {
    TransferMapper transferMapper = new TransferMapper(
      ROUTE_MAPPER,
      STATION_MAPPER,
      STOP_MAPPER,
      TRIP_MAPPER,
      false,
      ISSUE_STORE
    );
    assertEquals(
      1,
      transferMapper.map(Collections.singleton(transfer)).pendingConstrainedTransfers().size()
    );
  }

  @Test
  public void testMapStopTransfer() {
    transfer.setFromTrip(null);
    transfer.setToTrip(null);
    transfer.setFromRoute(null);
    transfer.setToRoute(null);
    TransferMapper transferMapper = new TransferMapper(
      ROUTE_MAPPER,
      STATION_MAPPER,
      STOP_MAPPER,
      TRIP_MAPPER,
      false,
      ISSUE_STORE
    );
    PendingConstrainedTransfer result = transferMapper.map(transfer);

    assertNotNull(result);
    assertNotNull(resolved(result.from()).asStopTransferPoint().getStop());
    assertNotNull(resolved(result.to()).asStopTransferPoint().getStop());
  }

  /**
   * A trip-based endpoint can't be resolved to a stop position by {@link TransferMapper} alone -
   * that only happens once {@link org.opentripplanner.model.impl.TransitDataImportBuilder#build()}
   * has settled the trip's stop times. See {@link DeferredConstrainedTransferPoint}.
   */
  @Test
  public void testMap() throws Exception {
    TransferMapper transferMapper = new TransferMapper(
      ROUTE_MAPPER,
      STATION_MAPPER,
      STOP_MAPPER,
      TRIP_MAPPER,
      false,
      ISSUE_STORE
    );
    PendingConstrainedTransfer result = transferMapper.map(transfer);
    DeferredConstrainedTransferPoint.PendingTripPosition from = pending(result.from());
    DeferredConstrainedTransferPoint.PendingTripPosition to = pending(result.to());

    assertNotNull(from.trip());
    assertFalse(from.boardTrip());
    assertNotNull(to.trip());
    assertTrue(to.boardTrip());
    assertEquals(MIN_TRANSFER_TIME, result.constraint().getMinTransferTime());
    assertEquals(TRANSFER_TYPE, result.constraint().getPriority());
  }

  private static DeferredConstrainedTransferPoint.Resolved resolvedPoint(
    DeferredConstrainedTransferPoint point
  ) {
    if (point instanceof DeferredConstrainedTransferPoint.Resolved r) {
      return r;
    }
    throw new AssertionError("Expected a resolved transfer point, got: " + point);
  }

  private static org.opentripplanner.transfer.constrained.model.TransferPoint resolved(
    DeferredConstrainedTransferPoint point
  ) {
    return resolvedPoint(point).point();
  }

  private static DeferredConstrainedTransferPoint.PendingTripPosition pending(
    DeferredConstrainedTransferPoint point
  ) {
    if (point instanceof DeferredConstrainedTransferPoint.PendingTripPosition p) {
      return p;
    }
    throw new AssertionError("Expected a pending trip-position transfer point, got: " + point);
  }
}
