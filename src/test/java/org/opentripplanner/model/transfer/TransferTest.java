package org.opentripplanner.model.transfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.model.transfer.Transfer.MAX_WAIT_TIME_NOT_SET;
import static org.opentripplanner.model.transfer.TransferPriority.ALLOWED;
import static org.opentripplanner.model.transfer.TransferPriority.PREFERRED;
import static org.opentripplanner.model.transfer.TransferPriority.RECOMMENDED;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.util.time.DurationUtils;

public class TransferTest implements TransferTestData {

  public static final int MAX_WAIT_TIME = DurationUtils.duration("1h");
  private final Transfer TX_A_TO_B     = transfer(STOP_POINT_A, STOP_POINT_B);
  private final Transfer TX_A_TO_R22 = transfer(STOP_POINT_A, ROUTE_POINT_22);
  private final Transfer TX_A_TO_T23 = transfer(STOP_POINT_A, TRIP_POINT_23);
  private final Transfer TX_R11_TO_B = transfer(ROUTE_POINT_11, STOP_POINT_B);
  private final Transfer TX_R11_TO_R22 = transfer(ROUTE_POINT_11, ROUTE_POINT_22);
  private final Transfer TX_T11_TO_R22 = transfer(TRIP_POINT_11, ROUTE_POINT_22);
  private final Transfer TX_T11_TO_T22 = transfer(TRIP_POINT_11, TRIP_POINT_23);

  private final Transfer TX_NO_CONSTRAINS = new Transfer(STOP_POINT_A, STOP_POINT_B, ALLOWED, false, false, MAX_WAIT_TIME_NOT_SET);
  private final Transfer TX_RECOMMENDED = new Transfer(STOP_POINT_A, STOP_POINT_B, RECOMMENDED, false, false, MAX_WAIT_TIME_NOT_SET);
  private final Transfer TX_STAY_SEATED = new Transfer(TRIP_POINT_11, TRIP_POINT_23, ALLOWED, true, false, MAX_WAIT_TIME_NOT_SET);
  private final Transfer TX_GUARANTIED = new Transfer(TRIP_POINT_11, TRIP_POINT_23, ALLOWED, false, true, MAX_WAIT_TIME_NOT_SET);
  private final Transfer TX_MAX_WAIT_TIME = new Transfer(TRIP_POINT_11, TRIP_POINT_23, ALLOWED, false, false, MAX_WAIT_TIME);
  private final Transfer TX_EVERYTHING = new Transfer(TRIP_POINT_11, TRIP_POINT_23, PREFERRED, true, true, MAX_WAIT_TIME);

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
    assertEquals(ALLOWED, TX_A_TO_B.getPriority());
    assertEquals(RECOMMENDED, TX_RECOMMENDED.getPriority());
    assertTrue(TX_STAY_SEATED.isStaySeated());
    assertTrue(TX_GUARANTIED.isGuaranteed());
    assertFalse(TX_NO_CONSTRAINS.isStaySeated());
    assertFalse(TX_NO_CONSTRAINS.isGuaranteed());
    assertEquals(ALLOWED, TX_NO_CONSTRAINS.getPriority());
    assertEquals(MAX_WAIT_TIME, TX_MAX_WAIT_TIME.getMaxWaitTime());
  }

  @Test
  public void priorityCost() {
    assertEquals(0, Transfer.priorityCost(null));
    assertEquals(0, Transfer.priorityCost(TX_NO_CONSTRAINS));
    assertEquals(-1, Transfer.priorityCost(TX_RECOMMENDED));
    assertEquals(-10, Transfer.priorityCost(TX_GUARANTIED));
    assertEquals(-100, Transfer.priorityCost(TX_STAY_SEATED));
    assertEquals(-112, Transfer.priorityCost(TX_EVERYTHING));
  }

  @Test
  public void noConstraints() {
    assertTrue(TX_NO_CONSTRAINS.noConstraints());
    assertFalse(TX_STAY_SEATED.noConstraints());
    assertFalse(TX_GUARANTIED.noConstraints());
    assertFalse(TX_RECOMMENDED.noConstraints());
    assertFalse(TX_MAX_WAIT_TIME.noConstraints());
    assertFalse(TX_EVERYTHING.noConstraints());
  }

  @Test
  public void testToString() {
    assertEquals(
            "Transfer{from: (stop: F:A), to: (stop: F:B)}",
            TX_A_TO_B.toString()
    );
    assertEquals(
            "Transfer{from: (trip: T:1, stopPos: 1), to: (trip: T:2, stopPos: 3), priority: PREFERRED, maxWaitTime: 1h, staySeated, guaranteed}",
            TX_EVERYTHING.toString()
    );
  }

  private static Transfer transfer(TransferPoint from, TransferPoint to) {
    return new Transfer(from, to, ALLOWED, false, false, MAX_WAIT_TIME_NOT_SET);
  }

}