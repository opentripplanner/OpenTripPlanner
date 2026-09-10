package org.opentripplanner.raptor.data.transfers.regular;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.spi.RaptorTransfer;

class RaptorTransfersTest {

  private static final int STOP_A = 0;
  private static final int STOP_B = 1;
  private static final int STOP_C = 2;
  private static final int TIME_AB = 100;
  private static final int TIME_AC = 200;
  private static final int C1_AB = 10;
  private static final int C1_AC = 20;
  private static final TxEntry TX_AB = new TxEntry(STOP_B, TIME_AB, C1_AB);
  private static final TxEntry TX_AC = new TxEntry(STOP_C, TIME_AC, C1_AC);
  private static final TxEntry TX_BA = new TxEntry(STOP_A, TIME_AB, C1_AB);
  private static final TxEntry TX_CA = new TxEntry(STOP_A, TIME_AC, C1_AC);

  @Test
  void simpleCase() {
    RaptorTransfers subject = RaptorTransfers.of(3)
      .addTransfer(STOP_A, STOP_B,  TIME_AB, C1_AB)
      .addTransfer(STOP_A, STOP_C,  TIME_AC, C1_AC)
      .build();

    assertThat(collect(subject.getTransfersFromStop(STOP_A))).containsExactly(TX_AB, TX_AC);
    assertThat(collect(subject.getTransfersFromStop(STOP_B))).isEmpty();
    assertThat(collect(subject.getTransfersFromStop(STOP_C))).isEmpty();

    assertThat(collect(subject.getTransfersToStop(STOP_A))).isEmpty();
    assertThat(collect(subject.getTransfersToStop(STOP_B))).containsExactly(TX_BA);
    assertThat(collect(subject.getTransfersToStop(STOP_C))).containsExactly(TX_CA);
  }

  @Test
  void testIteratorHasNextForEmptySet() {
    RaptorTransfers subject = RaptorTransfers.of(3)
      .addTransfer(STOP_A, STOP_B, 100, 10)
      .build();

    assertThat(subject.getTransfersFromStop(STOP_C).hasNext()).isFalse();
  }

  private static List<TxEntry> collect(Iterator<? extends RaptorTransfer> it) {
    List<TxEntry> result = new ArrayList<>();
    while (it.hasNext()) {
      RaptorTransfer t = it.next();
      result.add(new TxEntry(t.stop(), t.durationInSeconds(), t.c1()));
    }
    return result;
  }

  private record TxEntry(int stop, int durationInSeconds, int c1) {}
}
