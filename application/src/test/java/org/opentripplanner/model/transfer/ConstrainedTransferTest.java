package org.opentripplanner.model.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.transfer.TransferTestData.ROUTE_POINT_1A;
import static org.opentripplanner.model.transfer.TransferTestData.ROUTE_POINT_1S;
import static org.opentripplanner.model.transfer.TransferTestData.ROUTE_POINT_2B;
import static org.opentripplanner.model.transfer.TransferTestData.ROUTE_POINT_2S;
import static org.opentripplanner.model.transfer.TransferTestData.STATION_POINT;
import static org.opentripplanner.model.transfer.TransferTestData.STOP_POINT_A;
import static org.opentripplanner.model.transfer.TransferTestData.STOP_POINT_B;
import static org.opentripplanner.model.transfer.TransferTestData.TRIP_POINT_11_1;
import static org.opentripplanner.model.transfer.TransferTestData.TRIP_POINT_21_3;

import org.junit.jupiter.api.Test;

public class ConstrainedTransferTest {

  private static final TransferConstraint NO_CONSTRAINS = TransferConstraint.of().build();
  private static final TransferConstraint GUARANTEED = TransferConstraint.of().guaranteed().build();

  private final ConstrainedTransfer TX_STATION_TO_STATION = noConstTx(STATION_POINT, STATION_POINT);
  private final ConstrainedTransfer TX_STATION_TO_B = noConstTx(STATION_POINT, STOP_POINT_B);
  private final ConstrainedTransfer TX_STATION_TO_R2B = noConstTx(STATION_POINT, ROUTE_POINT_2B);
  private final ConstrainedTransfer TX_STATION_TO_R2S = noConstTx(STATION_POINT, ROUTE_POINT_2S);
  private final ConstrainedTransfer TX_STATION_TO_T23 = noConstTx(STATION_POINT, TRIP_POINT_21_3);

  private final ConstrainedTransfer TX_A_TO_STATION = noConstTx(STOP_POINT_A, STATION_POINT);
  private final ConstrainedTransfer TX_A_TO_B = noConstTx(STOP_POINT_A, STOP_POINT_B);
  private final ConstrainedTransfer TX_A_TO_R2B = noConstTx(STOP_POINT_A, ROUTE_POINT_2B);
  private final ConstrainedTransfer TX_A_TO_R2S = noConstTx(STOP_POINT_A, ROUTE_POINT_2S);
  private final ConstrainedTransfer TX_A_TO_T23 = noConstTx(STOP_POINT_A, TRIP_POINT_21_3);

  private final ConstrainedTransfer TX_R1S_TO_STATION = noConstTx(ROUTE_POINT_1S, STATION_POINT);
  private final ConstrainedTransfer TX_R1S_TO_B = noConstTx(ROUTE_POINT_1S, STOP_POINT_B);
  private final ConstrainedTransfer TX_R1S_TO_R2B = noConstTx(ROUTE_POINT_1S, ROUTE_POINT_2B);
  private final ConstrainedTransfer TX_R1S_TO_R2S = noConstTx(ROUTE_POINT_1S, ROUTE_POINT_2S);
  private final ConstrainedTransfer TX_R1S_TO_T23 = noConstTx(ROUTE_POINT_1S, TRIP_POINT_21_3);

  private final ConstrainedTransfer TX_R1A_TO_STATION = noConstTx(ROUTE_POINT_1A, STATION_POINT);
  private final ConstrainedTransfer TX_R1A_TO_B = noConstTx(ROUTE_POINT_1A, STOP_POINT_B);
  private final ConstrainedTransfer TX_R1A_TO_R2B = noConstTx(ROUTE_POINT_1A, ROUTE_POINT_2B);
  private final ConstrainedTransfer TX_R1A_TO_R2S = noConstTx(ROUTE_POINT_1A, ROUTE_POINT_2S);
  private final ConstrainedTransfer TX_R1A_TO_T23 = noConstTx(ROUTE_POINT_1A, TRIP_POINT_21_3);

  private final ConstrainedTransfer TX_T11_TO_STATION = noConstTx(TRIP_POINT_11_1, STATION_POINT);
  private final ConstrainedTransfer TX_T11_TO_B = noConstTx(TRIP_POINT_11_1, STOP_POINT_B);
  private final ConstrainedTransfer TX_T11_TO_R2B = noConstTx(TRIP_POINT_11_1, ROUTE_POINT_2B);
  private final ConstrainedTransfer TX_T11_TO_R2S = noConstTx(TRIP_POINT_11_1, ROUTE_POINT_2S);
  private final ConstrainedTransfer TX_T11_TO_T23 = noConstTx(TRIP_POINT_11_1, TRIP_POINT_21_3);

  private final ConstrainedTransfer TX_NO_CONSTRAINS = noConstTx(STOP_POINT_A, STOP_POINT_B);

  private final ConstrainedTransfer TX_GUARANTEED = new ConstrainedTransfer(
    null,
    TRIP_POINT_11_1,
    TRIP_POINT_21_3,
    GUARANTEED
  );

  @Test
  public void getSpecificityRanking() {
    assertEquals(0, TX_STATION_TO_STATION.getSpecificityRanking());
    assertEquals(10, TX_STATION_TO_B.getSpecificityRanking());
    assertEquals(20, TX_STATION_TO_R2S.getSpecificityRanking());
    assertEquals(30, TX_STATION_TO_R2B.getSpecificityRanking());
    assertEquals(40, TX_STATION_TO_T23.getSpecificityRanking());

    assertEquals(11, TX_A_TO_STATION.getSpecificityRanking());
    assertEquals(21, TX_A_TO_B.getSpecificityRanking());
    assertEquals(31, TX_A_TO_R2S.getSpecificityRanking());
    assertEquals(41, TX_A_TO_R2B.getSpecificityRanking());
    assertEquals(51, TX_A_TO_T23.getSpecificityRanking());

    assertEquals(22, TX_R1S_TO_STATION.getSpecificityRanking());
    assertEquals(32, TX_R1S_TO_B.getSpecificityRanking());
    assertEquals(42, TX_R1S_TO_R2S.getSpecificityRanking());
    assertEquals(52, TX_R1S_TO_R2B.getSpecificityRanking());
    assertEquals(62, TX_R1S_TO_T23.getSpecificityRanking());

    assertEquals(33, TX_R1A_TO_STATION.getSpecificityRanking());
    assertEquals(43, TX_R1A_TO_B.getSpecificityRanking());
    assertEquals(53, TX_R1A_TO_R2S.getSpecificityRanking());
    assertEquals(63, TX_R1A_TO_R2B.getSpecificityRanking());
    assertEquals(73, TX_R1A_TO_T23.getSpecificityRanking());

    assertEquals(44, TX_T11_TO_STATION.getSpecificityRanking());
    assertEquals(54, TX_T11_TO_B.getSpecificityRanking());
    assertEquals(64, TX_T11_TO_R2S.getSpecificityRanking());
    assertEquals(74, TX_T11_TO_R2B.getSpecificityRanking());
    assertEquals(84, TX_T11_TO_T23.getSpecificityRanking());
  }

  @Test
  public void testOtherAccessors() {
    assertEquals(STOP_POINT_A, TX_A_TO_R2B.getFrom());
    assertEquals(ROUTE_POINT_2B, TX_A_TO_R2B.getTo());
  }

  @Test
  public void noConstraints() {
    assertTrue(TX_NO_CONSTRAINS.noConstraints());
    assertFalse(TX_GUARANTEED.noConstraints());
  }

  @Test
  public void testToString() {
    assertEquals(
      "ConstrainedTransfer{from: StopTP{F:A}, to: StopTP{F:B}, constraint: (no constraints)}",
      TX_A_TO_B.toString()
    );
  }

  private static ConstrainedTransfer noConstTx(TransferPoint s, TransferPoint t) {
    return new ConstrainedTransfer(null, s, t, NO_CONSTRAINS);
  }
}
