package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE;
import static org.opentripplanner.routing.api.request.StreetMode.WALK;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;

class RaptorTransferIndexTest {

  private static final Transfer t1 = new Transfer(2, 100, EnumSet.of(StreetMode.WALK));
  private static final Transfer t2 = new Transfer(
    3,
    200,
    EnumSet.of(StreetMode.WALK, StreetMode.BIKE)
  );
  private static final Transfer t3 = new Transfer(0, 500, EnumSet.of(StreetMode.WALK));
  private static final Transfer t4 = new Transfer(0, 1000, EnumSet.of(StreetMode.BIKE));
  private static final Transfer t5 = new Transfer(
    2,
    200,
    EnumSet.of(StreetMode.WALK, StreetMode.BIKE)
  );

  static List<List<Transfer>> data = List.of(
    List.of(t1, t2, t5),
    List.of(t3),
    List.of(),
    List.of(t4)
  );

  @Test
  void testForwardWalk() {
    var streetSearchRequest = StreetSearchRequest.of().withMode(WALK).build();
    for (var index : List.of(
      RaptorTransferIndex.createInitialSetup(data, streetSearchRequest),
      RaptorTransferIndex.createRequestScope(data, streetSearchRequest)
    )) {
      assertEquals(
        Stream.of(t1, t2)
          .flatMap(t -> t.asRaptorTransfer(streetSearchRequest).stream())
          .collect(Collectors.toSet()),
        new HashSet<>(index.getForwardTransfers(0))
      );
      assertEquals(
        t3.asRaptorTransfer(streetSearchRequest).stream().collect(Collectors.toSet()),
        new HashSet<>(index.getForwardTransfers(1))
      );
      assertEquals(Set.of(), new HashSet<>(index.getForwardTransfers(2)));
      assertEquals(Set.of(), new HashSet<>(index.getForwardTransfers(3)));
    }
  }

  @Test
  void testForwardBike() {
    var streetSearchRequest = StreetSearchRequest.of().withMode(BIKE).build();
    for (var index : List.of(
      RaptorTransferIndex.createInitialSetup(data, streetSearchRequest),
      RaptorTransferIndex.createRequestScope(data, streetSearchRequest)
    )) {
      assertEquals(
        Stream.of(t2, t5)
          .flatMap(t -> t.asRaptorTransfer(streetSearchRequest).stream())
          .collect(Collectors.toSet()),
        new HashSet<>(index.getForwardTransfers(0))
      );
      assertEquals(Set.of(), new HashSet<>(index.getForwardTransfers(1)));
      assertEquals(Set.of(), new HashSet<>(index.getForwardTransfers(2)));
      assertEquals(
        t4.asRaptorTransfer(streetSearchRequest).stream().collect(Collectors.toSet()),
        new HashSet<>(index.getForwardTransfers(3))
      );
    }
  }

  @Test
  void testReverseWalk() {
    var streetSearchRequest = StreetSearchRequest.of().withMode(WALK).build();
    for (var index : List.of(
      RaptorTransferIndex.createInitialSetup(data, streetSearchRequest),
      RaptorTransferIndex.createRequestScope(data, streetSearchRequest)
    )) {
      assertEquals(
        t3
          .asRaptorTransfer(streetSearchRequest)
          .map(r -> r.reverseOf(1))
          .stream()
          .collect(Collectors.toSet()),
        new HashSet<>(index.getReversedTransfers(0))
      );
      assertEquals(Set.of(), new HashSet<>(index.getReversedTransfers(1)));
      assertEquals(
        t1
          .asRaptorTransfer(streetSearchRequest)
          .map(r -> r.reverseOf(0))
          .stream()
          .collect(Collectors.toSet()),
        new HashSet<>(index.getReversedTransfers(2))
      );
      assertEquals(
        t2
          .asRaptorTransfer(streetSearchRequest)
          .map(r -> r.reverseOf(0))
          .stream()
          .collect(Collectors.toSet()),
        new HashSet<>(index.getReversedTransfers(3))
      );
    }
  }

  @Test
  void testReverseBike() {
    var streetSearchRequest = StreetSearchRequest.of().withMode(BIKE).build();
    for (var index : List.of(
      RaptorTransferIndex.createInitialSetup(data, streetSearchRequest),
      RaptorTransferIndex.createRequestScope(data, streetSearchRequest)
    )) {
      assertEquals(
        t4
          .asRaptorTransfer(streetSearchRequest)
          .map(r -> r.reverseOf(3))
          .stream()
          .collect(Collectors.toSet()),
        new HashSet<>(index.getReversedTransfers(0))
      );
      assertEquals(Set.of(), new HashSet<>(index.getReversedTransfers(1)));
      assertEquals(
        t5
          .asRaptorTransfer(streetSearchRequest)
          .map(r -> r.reverseOf(0))
          .stream()
          .collect(Collectors.toSet()),
        new HashSet<>(index.getReversedTransfers(2))
      );
      assertEquals(
        t2
          .asRaptorTransfer(streetSearchRequest)
          .map(r -> r.reverseOf(0))
          .stream()
          .collect(Collectors.toSet()),
        new HashSet<>(index.getReversedTransfers(3))
      );
    }
  }
}
