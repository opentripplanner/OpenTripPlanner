package org.opentripplanner.model.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class ConstrainedTransferTest implements TransferTestData {

  private static final TransferConstraint NO_CONSTRAINS = TransferConstraint.create().build();
  private static final TransferConstraint GUARANTIED = TransferConstraint.create().guaranteed().build();

  private final ConstrainedTransfer TX_A_TO_B = new ConstrainedTransfer(null, STOP_POINT_A, STOP_POINT_B, NO_CONSTRAINS);
  private final ConstrainedTransfer TX_A_TO_R22 = new ConstrainedTransfer(null, STOP_POINT_A, ROUTE_POINT_22, NO_CONSTRAINS);
  private final ConstrainedTransfer TX_A_TO_T23 = new ConstrainedTransfer(null, STOP_POINT_A, TRIP_POINT_23, NO_CONSTRAINS);
  private final ConstrainedTransfer TX_R11_TO_B = new ConstrainedTransfer(null, ROUTE_POINT_11, STOP_POINT_B, NO_CONSTRAINS);
  private final ConstrainedTransfer TX_R11_TO_R22 = new ConstrainedTransfer(null, ROUTE_POINT_11, ROUTE_POINT_22, NO_CONSTRAINS);
  private final ConstrainedTransfer TX_T11_TO_R22 = new ConstrainedTransfer(null, TRIP_POINT_11, ROUTE_POINT_22, NO_CONSTRAINS);
  private final ConstrainedTransfer TX_T11_TO_T22 = new ConstrainedTransfer(null, TRIP_POINT_11, TRIP_POINT_23, NO_CONSTRAINS);

  private final ConstrainedTransfer TX_NO_CONSTRAINS = new ConstrainedTransfer(null, STOP_POINT_A, STOP_POINT_B, NO_CONSTRAINS);
  private final ConstrainedTransfer TX_GUARANTIED = new ConstrainedTransfer(null, TRIP_POINT_11, TRIP_POINT_23, GUARANTIED);

  @BeforeEach
  public void setup() {
    ROUTE_1.setShortName("L1");
    ROUTE_2.setShortName("L2");
    TRIP_1.setRoute(ROUTE_1);
    TRIP_2.setRoute(ROUTE_2);
    TRIP_1.setRoute(ROUTE_1);
    TRIP_2.setRoute(ROUTE_2);
  }

  @Test
  public void getSpecificityRanking() {
    assertEquals(0, TX_A_TO_B.getSpecificityRanking());
    assertEquals(1, TX_R11_TO_B.getSpecificityRanking());
    assertEquals(1, TX_A_TO_R22.getSpecificityRanking());
    assertEquals(2, TX_R11_TO_R22.getSpecificityRanking());
    assertEquals(2, TX_A_TO_T23.getSpecificityRanking());
    assertEquals(3, TX_T11_TO_R22.getSpecificityRanking());
    assertEquals(4, TX_T11_TO_T22.getSpecificityRanking());
  }

  @Test
  public void testOtherAccessors() {
    assertEquals(STOP_POINT_A, TX_A_TO_R22.getFrom());
    assertEquals(ROUTE_POINT_22, TX_A_TO_R22.getTo());
  }

  @Test
  public void noConstraints() {
    assertTrue(TX_NO_CONSTRAINS.noConstraints());
    assertFalse(TX_GUARANTIED.noConstraints());
  }

  @Test
  public void testToString() {
    assertEquals(
            "ConstrainedTransfer{from: <Stop F:A>, to: <Stop F:B>, constraint: {no constraints}}",
            TX_A_TO_B.toString()
    );
  }
}