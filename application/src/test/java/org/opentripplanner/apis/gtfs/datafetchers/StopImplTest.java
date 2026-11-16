package org.opentripplanner.apis.gtfs.datafetchers;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMultimap;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.gtfs.TestGtfsGraphQLRequestContext;
import org.opentripplanner.apis.support.graphql.DataFetchingSupport;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.transfer.regular.FlexTransferService;
import org.opentripplanner.transfer.regular.TransferServiceTestFactory;
import org.opentripplanner.transfer.regular.model.PathTransfer;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.transfer.regular.FixedRegularTransfers;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;

class StopImplTest {

  private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 6, 27);

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of(SERVICE_DATE);
  private final RegularStop STOP_A = envBuilder.stop("A");
  private final RegularStop STOP_B = envBuilder.stop("B");
  private final RegularStop STOP_C = envBuilder.stop("C");
  private final AreaStop AREA = envBuilder.areaStop("Area");
  private final AreaStop AREA_2 = envBuilder.areaStop("Area2");

  private final RegularTransferRepositorySnapshot<NearbyStop, RouteRequest> regularTransfers =
    FixedRegularTransfers.of()
      .add(STOP_A, STOP_B, path(STOP_B, 100))
      .add(STOP_A, STOP_C, path(STOP_C, 50))
      .add(STOP_B, STOP_C, path(STOP_C, 70))
      .snapshot();

  private final FlexTransferService flexTransfers = flexTransfers(
    flexTransfer(AREA, STOP_B, 120),
    flexTransfer(AREA, STOP_A, 40),
    flexTransfer(AREA, AREA_2, 10),
    flexTransfer(STOP_A, AREA, 40)
  );

  @Test
  void transfersAreTheRegularTransfersFromTheStopByDistance() throws Exception {
    var env = env(STOP_A, Map.of());

    assertThat(toStopIds(new StopImpl().transfers().get(env)))
      .containsExactly(STOP_C.getId(), STOP_B.getId())
      .inOrder();
  }

  @Test
  void transfersAreLimitedByMaxDistance() throws Exception {
    var env = env(STOP_A, Map.of("maxDistance", 80));

    assertThat(toStopIds(new StopImpl().transfers().get(env))).containsExactly(STOP_C.getId());
  }

  @Test
  void stopWithoutTransfersHasNone() throws Exception {
    var env = env(STOP_C, Map.of());

    assertThat(toStopIds(new StopImpl().transfers().get(env))).isEmpty();
  }

  @Test
  void transfersOfARegularStopAreNotTheFlexTransfers() throws Exception {
    var env = env(STOP_A, Map.of());

    assertThat(toStopIds(new StopImpl().transfers().get(env))).doesNotContain(AREA.getId());
  }

  @Test
  void transfersOfAnAreaStopAreTheFlexTransfersToRegularStopsByDistance() throws Exception {
    var env = env(AREA, Map.of());

    assertThat(toStopIds(new StopImpl().transfers().get(env)))
      .containsExactly(STOP_A.getId(), STOP_B.getId())
      .inOrder();
  }

  @Test
  void flexTransfersAreLimitedByMaxDistance() throws Exception {
    var env = env(AREA, Map.of("maxDistance", 80));

    assertThat(toStopIds(new StopImpl().transfers().get(env))).containsExactly(STOP_A.getId());
  }

  private DataFetchingEnvironment env(StopLocation stop, Map<String, Object> arguments) {
    var transitEnv = envBuilder.build();
    return DataFetchingEnvironmentImpl.newDataFetchingEnvironment(
      DataFetchingSupport.executionContext()
    )
      .source(stop)
      .arguments(arguments)
      .context(
        new TestGtfsGraphQLRequestContext(
          null,
          transitEnv.transitService(),
          null,
          flexTransfers,
          regularTransfers,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          RouteRequest.defaultValue()
        )
      )
      .build();
  }

  private static FlexTransferService flexTransfers(PathTransfer... transfers) {
    var repository = TransferServiceTestFactory.defaultTransferRepository();
    var byStop = ImmutableMultimap.<StopLocation, PathTransfer>builder();
    for (var transfer : transfers) {
      byStop.put(transfer.from, transfer);
    }
    repository.addAllTransfersByStops(byStop.build());
    return TransferServiceTestFactory.transferService(repository);
  }

  private static PathTransfer flexTransfer(StopLocation from, StopLocation to, double distance) {
    return new PathTransfer(from, to, distance, List.of(), EnumSet.of(StreetMode.WALK));
  }

  private static NearbyStop path(RegularStop to, double distance) {
    return new NearbyStop(to.getId(), distance, List.of(), null);
  }

  private static List<FeedScopedId> toStopIds(Iterable<NearbyStop> transfers) {
    return StreamSupport.stream(transfers.spliterator(), false).map(NearbyStop::stopId).toList();
  }
}
