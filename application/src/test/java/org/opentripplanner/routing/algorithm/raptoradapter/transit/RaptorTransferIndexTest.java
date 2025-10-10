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

  private static final List<List<Transfer>> data = List.of(
    List.of(t1, t2, t5),
    List.of(t3),
    List.of(),
    List.of(t4)
  );

  @Test
  void testForwardWalk() {
    var streetSearchRequest = StreetSearchRequest.of().withMode(WALK).build();
    performTestOnBothImplementations(streetSearchRequest, index -> {
      assertThat(index.getForwardTransfers(0)).containsExactlyElementsIn(
        getForwardRaptorTransfers(streetSearchRequest, t1, t2)
      );
      assertThat(index.getForwardTransfers(1)).containsExactlyElementsIn(
        getForwardRaptorTransfers(streetSearchRequest, t3)
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
        getForwardRaptorTransfers(streetSearchRequest, t2, t5)
      );
      assertThat(index.getForwardTransfers(1)).isEmpty();
      assertThat(index.getForwardTransfers(2)).isEmpty();
      assertThat(index.getForwardTransfers(3)).containsExactlyElementsIn(
        getForwardRaptorTransfers(streetSearchRequest, t4)
      );
    });
  }

  @Test
  void testReverseWalk() {
    var streetSearchRequest = StreetSearchRequest.of().withMode(WALK).build();
    performTestOnBothImplementations(streetSearchRequest, index -> {
      assertThat(index.getReversedTransfers(0)).containsExactlyElementsIn(
        getReversedRaptorTransfers(streetSearchRequest, 1, t3)
      );
      assertThat(index.getReversedTransfers(1)).isEmpty();
      assertThat(index.getReversedTransfers(2)).containsExactlyElementsIn(
        getReversedRaptorTransfers(streetSearchRequest, 0, t1)
      );
      assertThat(index.getReversedTransfers(3)).containsExactlyElementsIn(
        getReversedRaptorTransfers(streetSearchRequest, 0, t2)
      );
    });
  }

  @Test
  void testReverseBike() {
    var streetSearchRequest = StreetSearchRequest.of().withMode(BIKE).build();
    performTestOnBothImplementations(streetSearchRequest, index -> {
      assertThat(index.getReversedTransfers(0)).containsExactlyElementsIn(
        getReversedRaptorTransfers(streetSearchRequest, 3, t4)
      );
      assertThat(index.getReversedTransfers(1)).isEmpty();
      assertThat(index.getReversedTransfers(2)).containsExactlyElementsIn(
        getReversedRaptorTransfers(streetSearchRequest, 0, t5)
      );
      assertThat(index.getReversedTransfers(3)).containsExactlyElementsIn(
        getReversedRaptorTransfers(streetSearchRequest, 0, t2)
      );
    });
  }

  private void performTestOnBothImplementations(
    StreetSearchRequest streetSearchRequest,
    Consumer<RaptorTransferIndex> verifier
  ) {
    for (var index : List.of(
      RaptorTransferIndex.createInitialSetup(data, streetSearchRequest),
      RaptorTransferIndex.createRequestScope(data, streetSearchRequest)
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
        t.asRaptorTransfer(streetSearchRequest).map(x -> x.reverseOf(fromStopIndex)).stream()
      )
      .toList();
  }
}
