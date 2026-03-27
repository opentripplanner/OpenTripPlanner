package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flex;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walk;
import static org.opentripplanner.raptor.api.request.RaptorProfile.MULTI_CRITERIA;
import static org.opentripplanner.raptor.spi.SearchDirection.FORWARD;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.request.RaptorViaLocation;
import org.opentripplanner.raptor.rangeraptor.transit.AccessPaths;
import org.opentripplanner.raptor.rangeraptor.transit.EgressPaths;
import org.opentripplanner.raptor.rangeraptor.transit.ViaConnections;

class StopsWithArriveByTransitCriteriaResolverTest implements RaptorTestConstants {

  private static final int ITERATION_STEP = 60;

  private static AccessPaths accessPaths(Collection<RaptorAccessEgress> paths) {
    return AccessPaths.create(ITERATION_STEP, paths, MULTI_CRITERIA, FORWARD);
  }

  private static EgressPaths egressPaths(Collection<RaptorAccessEgress> paths) {
    return EgressPaths.create(paths, MULTI_CRITERIA);
  }

  @Test
  void onBoardAccessStopIsIncluded() {
    var result = StopsWithArriveByTransitCriteriaResolver.resolve(
      accessPaths(List.of(flex(STOP_A, 300))),
      egressPaths(List.of()),
      null
    );
    assertTrue(result.contains(STOP_A));
  }

  @Test
  void onStreetAccessStopIsNotIncluded() {
    var result = StopsWithArriveByTransitCriteriaResolver.resolve(
      accessPaths(List.of(walk(STOP_A, 300))),
      egressPaths(List.of()),
      null
    );
    assertTrue(result.isEmpty());
  }

  @Test
  void nonFreeEgressStopIsIncluded() {
    var result = StopsWithArriveByTransitCriteriaResolver.resolve(
      accessPaths(List.of()),
      egressPaths(List.of(walk(STOP_B, 120))),
      null
    );
    assertTrue(result.contains(STOP_B));
  }

  @Test
  void freeEgressStopIsIncluded() {
    // A free egress has stopReachedOnBoard=false, so it only fires on transit arrivals —
    // the same rule as a walk egress. The stop therefore needs the 4D comparator.
    var result = StopsWithArriveByTransitCriteriaResolver.resolve(
      accessPaths(List.of()),
      egressPaths(List.of(TestAccessEgress.walk(STOP_B, D30_s))),
      null
    );
    assertTrue(result.contains(STOP_B));
  }

  @Test
  void viaTransferFromStopIsIncluded() {
    var viaLocation = RaptorViaLocation.via("Via", Duration.ZERO)
      .addViaTransfer(STOP_C, TestTransfer.transfer(STOP_D, 120))
      .build();
    var viaConnections = new ViaConnections(viaLocation.connections());

    var result = StopsWithArriveByTransitCriteriaResolver.resolve(
      accessPaths(List.of()),
      egressPaths(List.of()),
      viaConnections
    );
    assertTrue(result.contains(STOP_C));
  }

  @Test
  void viaSameStopConnectionIsNotIncluded() {
    var viaLocation = RaptorViaLocation.via("Via", Duration.ZERO).addViaStop(STOP_C).build();
    var viaConnections = new ViaConnections(viaLocation.connections());

    var result = StopsWithArriveByTransitCriteriaResolver.resolve(
      accessPaths(List.of()),
      egressPaths(List.of()),
      viaConnections
    );
    assertTrue(result.isEmpty());
  }

  @Test
  void nullViaConnectionsIsHandled() {
    var result = StopsWithArriveByTransitCriteriaResolver.resolve(
      accessPaths(List.of()),
      egressPaths(List.of()),
      null
    );
    assertTrue(result.isEmpty());
  }

  @Test
  void allThreeSourcesContributeDistinctStops() {
    var viaLocation = RaptorViaLocation.via("Via", Duration.ZERO)
      .addViaTransfer(STOP_C, TestTransfer.transfer(STOP_D, 90))
      .build();
    var viaConnections = new ViaConnections(viaLocation.connections());

    var result = StopsWithArriveByTransitCriteriaResolver.resolve(
      accessPaths(List.of(flex(STOP_A, 300))),
      egressPaths(List.of(walk(STOP_B, 120))),
      viaConnections
    );
    assertEquals(3, result.size());
    assertTrue(result.contains(STOP_A));
    assertTrue(result.contains(STOP_B));
    assertTrue(result.contains(STOP_C));
  }

  @Test
  void sameStopFromMultipleSourcesIsIncludedOnce() {
    var viaLocation = RaptorViaLocation.via("Via", Duration.ZERO)
      .addViaTransfer(STOP_A, TestTransfer.transfer(STOP_D, 90))
      .build();
    var viaConnections = new ViaConnections(viaLocation.connections());

    var result = StopsWithArriveByTransitCriteriaResolver.resolve(
      accessPaths(List.of(flex(STOP_A, 300))),
      egressPaths(List.of(walk(STOP_A, 120))),
      viaConnections
    );
    assertEquals(1, result.size());
    assertTrue(result.contains(STOP_A));
  }
}
