package org.opentripplanner.transfer.regular.index;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.street.model.StreetMode.BIKE;
import static org.opentripplanner.street.model.StreetMode.WALK;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.transfer.regular.model.DefaultRaptorTransfer;
import org.opentripplanner.transfer.regular.model.PathTransfer;
import org.opentripplanner.transit.model.site.RegularStop;

class RaptorTransferIndexTest {

  private static final PathTransfer T1 = transfer(0, 2, 100, EnumSet.of(StreetMode.WALK));
  private static final PathTransfer T2 = transfer(
    0,
    3,
    200,
    EnumSet.of(StreetMode.WALK, StreetMode.BIKE)
  );
  private static final PathTransfer T3 = transfer(1, 0, 500, EnumSet.of(StreetMode.WALK));
  private static final PathTransfer T4 = transfer(3, 0, 1000, EnumSet.of(StreetMode.BIKE));
  private static final PathTransfer T5 = transfer(
    0,
    2,
    200,
    EnumSet.of(StreetMode.WALK, StreetMode.BIKE)
  );

  private static final List<List<PathTransfer>> DATA = List.of(
    List.of(T1, T2, T5),
    List.of(T3),
    List.of(),
    List.of(T4)
  );

  private static PathTransfer transfer(
    int fromStop,
    int toStop,
    int meters,
    EnumSet<StreetMode> streetModes
  ) {
    var from = RegularStop.of(FeedScopedId.of("F", "S"), () -> fromStop).build();
    var to = RegularStop.of(FeedScopedId.of("F", "S"), () -> toStop).build();
    return new PathTransfer(from, to, meters, List.of(), streetModes);
  }

  @Test
  void testForwardWalk() {
    var streetSearchRequest = StreetSearchRequest.of().withMode(WALK).build();
    performTestOnBothImplementations(streetSearchRequest, index -> {
      assertThat(index.getForwardTransfers(0)).containsExactlyElementsIn(
        getForwardRaptorTransfers(streetSearchRequest, T1, T2)
      );
      assertThat(index.getForwardTransfers(1)).containsExactlyElementsIn(
        getForwardRaptorTransfers(streetSearchRequest, T3)
      );
      assertThat(index.getForwardTransfers(2)).isEmpty();
      assertThat(index.getForwardTransfers(3)).isEmpty();
    });
  }

  @Test
  void testForwardBike() {
    var streetSearchRequest = StreetSearchRequest.of().withMode(BIKE).build();
    performTestOnBothImplementations(streetSearchRequest, index -> {
      assertThat(index.getForwardTransfers(0)).containsExactlyElementsIn(
        getForwardRaptorTransfers(streetSearchRequest, T2, T5)
      );
      assertThat(index.getForwardTransfers(1)).isEmpty();
      assertThat(index.getForwardTransfers(2)).isEmpty();
      assertThat(index.getForwardTransfers(3)).containsExactlyElementsIn(
        getForwardRaptorTransfers(streetSearchRequest, T4)
      );
    });
  }

  @Test
  void testReverseWalk() {
    var streetSearchRequest = StreetSearchRequest.of().withMode(WALK).build();
    performTestOnBothImplementations(streetSearchRequest, index -> {
      assertThat(index.getReversedTransfers(0)).containsExactlyElementsIn(
        getReversedRaptorTransfers(streetSearchRequest, 1, T3)
      );
      assertThat(index.getReversedTransfers(1)).isEmpty();
      assertThat(index.getReversedTransfers(2)).containsExactlyElementsIn(
        getReversedRaptorTransfers(streetSearchRequest, 0, T1)
      );
      assertThat(index.getReversedTransfers(3)).containsExactlyElementsIn(
        getReversedRaptorTransfers(streetSearchRequest, 0, T2)
      );
    });
  }

  @Test
  void testReverseBike() {
    var streetSearchRequest = StreetSearchRequest.of().withMode(BIKE).build();
    performTestOnBothImplementations(streetSearchRequest, index -> {
      assertThat(index.getReversedTransfers(0)).containsExactlyElementsIn(
        getReversedRaptorTransfers(streetSearchRequest, 3, T4)
      );
      assertThat(index.getReversedTransfers(1)).isEmpty();
      assertThat(index.getReversedTransfers(2)).containsExactlyElementsIn(
        getReversedRaptorTransfers(streetSearchRequest, 0, T5)
      );
      assertThat(index.getReversedTransfers(3)).containsExactlyElementsIn(
        getReversedRaptorTransfers(streetSearchRequest, 0, T2)
      );
    });
  }

  private void performTestOnBothImplementations(
    StreetSearchRequest streetSearchRequest,
    Consumer<RaptorTransferIndex> verifier
  ) {
    for (var index : List.of(
      new PreCachedRaptorTransferIndex(DATA, streetSearchRequest, false),
      new OnDemandRaptorTransferIndex(DATA, streetSearchRequest)
    )) {
      verifier.accept(index);
    }
  }

  private Iterable<DefaultRaptorTransfer> getForwardRaptorTransfers(
    StreetSearchRequest streetSearchRequest,
    PathTransfer... transfers
  ) {
    return Arrays.stream(transfers)
      .flatMap(t -> t.asRaptorTransfer(streetSearchRequest).stream())
      .toList();
  }

  private Iterable<DefaultRaptorTransfer> getReversedRaptorTransfers(
    StreetSearchRequest streetSearchRequest,
    int fromStopIndex,
    PathTransfer... transfers
  ) {
    return Arrays.stream(transfers)
      .flatMap(t ->
        t
          .asRaptorTransfer(streetSearchRequest)
          .map(x -> x.reverseOf(fromStopIndex))
          .stream()
      )
      .toList();
  }
}
