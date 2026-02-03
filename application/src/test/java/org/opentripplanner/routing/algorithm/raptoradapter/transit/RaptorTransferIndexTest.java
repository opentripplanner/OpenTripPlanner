package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE;
import static org.opentripplanner.routing.api.request.StreetMode.WALK;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;

class RaptorTransferIndexTest {

  private static final Transfer T1 = new Transfer(2, 100, EnumSet.of(StreetMode.WALK));
  private static final Transfer T2 = new Transfer(
    3,
    200,
    EnumSet.of(StreetMode.WALK, StreetMode.BIKE)
  );
  private static final Transfer T3 = new Transfer(0, 500, EnumSet.of(StreetMode.WALK));
  private static final Transfer T4 = new Transfer(0, 1000, EnumSet.of(StreetMode.BIKE));
  private static final Transfer T5 = new Transfer(
    2,
    200,
    EnumSet.of(StreetMode.WALK, StreetMode.BIKE)
  );

  private static final List<List<Transfer>> DATA = List.of(
    List.of(T1, T2, T5),
    List.of(T3),
    List.of(),
    List.of(T4)
  );

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
      RaptorTransferIndex.createInitialSetup(DATA, streetSearchRequest),
      RaptorTransferIndex.createRequestScope(DATA, streetSearchRequest)
    )) {
      verifier.accept(index);
    }
  }

  private Iterable<DefaultRaptorTransfer> getForwardRaptorTransfers(
    StreetSearchRequest streetSearchRequest,
    Transfer... transfers
  ) {
    return Arrays.stream(transfers)
      .flatMap(t -> t.asRaptorTransfer(streetSearchRequest).stream())
      .toList();
  }

  private Iterable<DefaultRaptorTransfer> getReversedRaptorTransfers(
    StreetSearchRequest streetSearchRequest,
    int fromStopIndex,
    Transfer... transfers
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
