package org.opentripplanner.model.transfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.model.transfer.TransferConstraint.MAX_WAIT_TIME_NOT_SET;
import static org.opentripplanner.model.transfer.TransferPriority.ALLOWED;

import org.junit.Before;
import org.junit.Test;

public class ConstrainedTransferTest implements TransferTestData {

  private static final TransferConstraint NO_CONSTRAINS = new TransferConstraint(ALLOWED, false, false, MAX_WAIT_TIME_NOT_SET);
  private static final TransferConstraint GUARANTIED = new TransferConstraint(ALLOWED, false, true, MAX_WAIT_TIME_NOT_SET);

  private final ConstrainedTransfer TX_A_TO_B = new ConstrainedTransfer(STOP_POINT_A, STOP_POINT_B, NO_CONSTRAINS);
  private final ConstrainedTransfer TX_A_TO_R22 = new ConstrainedTransfer(STOP_POINT_A, ROUTE_POINT_22, NO_CONSTRAINS);
  private final ConstrainedTransfer TX_A_TO_T23 = new ConstrainedTransfer(STOP_POINT_A, TRIP_POINT_23, NO_CONSTRAINS);
  private final ConstrainedTransfer TX_R11_TO_B = new ConstrainedTransfer(ROUTE_POINT_11, STOP_POINT_B, NO_CONSTRAINS);
  private final ConstrainedTransfer TX_R11_TO_R22 = new ConstrainedTransfer(ROUTE_POINT_11, ROUTE_POINT_22, NO_CONSTRAINS);
  private final ConstrainedTransfer TX_T11_TO_R22 = new ConstrainedTransfer(TRIP_POINT_11, ROUTE_POINT_22, NO_CONSTRAINS);
  private final ConstrainedTransfer TX_T11_TO_T22 = new ConstrainedTransfer(TRIP_POINT_11, TRIP_POINT_23, NO_CONSTRAINS);

  private final ConstrainedTransfer TX_NO_CONSTRAINS = new ConstrainedTransfer(STOP_POINT_A, STOP_POINT_B, NO_CONSTRAINS);
  private final ConstrainedTransfer TX_GUARANTIED = new ConstrainedTransfer(TRIP_POINT_11, TRIP_POINT_23, GUARANTIED);

  @Before
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
  public void priorityCost() {
    assertEquals(NO_CONSTRAINS.priorityCost(), ConstrainedTransfer.priorityCost(null));
    assertEquals(NO_CONSTRAINS.priorityCost(), ConstrainedTransfer.priorityCost(TX_NO_CONSTRAINS));
    assertEquals(GUARANTIED.priorityCost(), ConstrainedTransfer.priorityCost(TX_GUARANTIED));
  }

  @Test
  public void noConstraints() {
    assertTrue(TX_NO_CONSTRAINS.noConstraints());
    assertFalse(TX_GUARANTIED.noConstraints());
  }

  @Test
  public void testToString() {
    assertEquals(
            "ConstrainedTransfer{from: (stop: F:A), to: (stop: F:B), constraint: { NONE }}",
            TX_A_TO_B.toString()
    );
  }
}