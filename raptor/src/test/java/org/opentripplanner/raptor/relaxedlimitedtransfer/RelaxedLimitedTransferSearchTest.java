ackage org.opentripplanner.raptor.relaxedlimitedtransfer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RelaxedLimitedTransferSearchTest {

  @Test
  void calculateIterationDepartureTime() {
    assertEquals(0, iterationDeparture(0, 59, 0));
    assertEquals(60, iterationDeparture(0, 60, 0));
    assertEquals(60, iterationDeparture(0, 61, 0));

    assertEquals(0, iterationDeparture(0, 60, 1));
    assertEquals(0, iterationDeparture(1, 60, 0));

    assertEquals(240, iterationDeparture(60, 360, 60));
    assertEquals(180, iterationDeparture(60, 360, 120));
    assertEquals(60, iterationDeparture(0, 61, 0));
  }

  private int iterationDeparture(int accessDuration, int boardTime, int boardSlack) {
    return RelaxedLimitedTransferSearch.calculateIterationDepartureTime(
      accessDuration,
      boardTime,
      boardSlack
    );
  }
}
