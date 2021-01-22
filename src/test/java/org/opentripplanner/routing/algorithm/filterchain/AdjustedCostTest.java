package org.opentripplanner.routing.algorithm.filterchain;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.filters.SortOnAdjustedCost;

import javax.annotation.Nonnull;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.A;
import static org.opentripplanner.model.plan.TestItineraryBuilder.B;
import static org.opentripplanner.model.plan.TestItineraryBuilder.E;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.raptor.util.TimeUtils.parseDuration;
import static org.opentripplanner.transit.raptor.util.TimeUtils.parseTimeCompact;

public class AdjustedCostTest {

  private static final int D2m = parseDuration("2m");
  private static final int D30m = parseDuration("30m");

  private static final int T10_00 = noonTime("10:00");
  private static final int T15_00 = noonTime("15:00");
  private static final int T15_01 = noonTime("15:01");
  private static final int T15_05 = noonTime("15:05");
  private static final int T15_15 = noonTime("15:15");
  private static final int T16_00 = noonTime("16:00");
  private static final int T16_05 = noonTime("16:05");
  private static final int T16_12 = noonTime("16:12");
  private static final int T16_24 = noonTime("16:24");
  private static final int T23_00 = noonTime("23:00");

  /* Trip ids */
  private static final int R1 = 1;
  private static final int R7 = 7;
  private static final int L2 = 2;
  private static final int L3 = 3;

  private static final Itinerary i1_0tx = newItinerary(A, T10_00)
      .rail(R1, T10_00, T16_24, E)
      .build();

  private static final Itinerary i2_1m = newItinerary(A, T10_00)
      .rail(R1, T10_00, T15_00, B)
      .bus(L2, T15_01, T16_05, E)
      .build();

  private static final Itinerary i3_10m = newItinerary(A, T10_00)
      .rail(R1, T10_00, T15_05, B)
      .bus(L3, T15_15, T16_12, E)
      .build();
  private static final Itinerary iL_1h = newItinerary(A, T10_00)
      .rail(R1, T10_00, T15_00, B)
      .bus(R7, T16_00, T23_00, E)
      .build();

  private static final int i1Cost = i1_0tx.generalizedCost;
  private static final int i2Cost = i2_1m.generalizedCost;
  private static final int i3Cost = i3_10m.generalizedCost;
  private static final int iLCost = iL_1h.generalizedCost;



  @SuppressWarnings("ConstantConditions")
  @Nonnull
  private final AdjustedCost subject = AdjustedCost.create(2.0);

  @Test
  public void testTestSetUp() {
    assertEquals(-120, T10_00);
    assertEquals(180, T15_00);
  }


  @Test
  public void calculate() {
    // No transfers, no extra cost
    assertEquals(0, subject.calculate(D30m, i1_0tx) - i1Cost);

    // For 1m the extra cost should be 60*2 = 120
    assertEquals(120, subject.calculate(D2m, i2_1m) - i2Cost);

    // 30m - 1m = 29m * 2 = 29*2*60 = 3 480
    assertEquals(3_480, subject.calculate(D30m, i2_1m) - i2Cost);

    // 30m - 10m = 20m * 2 = 20*2*60 = 2 400
    assertEquals(2_400, subject.calculate(D30m, i3_10m) - i3Cost);

    // Transfer time > 30m, no extra cost
    assertEquals(0, subject.calculate(D30m, iL_1h) - iLCost);
  }

  @Test
  public void minSafeTransferTime() {
    assertEquals(1_800,     subject.minSafeTransferTime(List.of(i1_0tx, i2_1m, i3_10m)));
  }

  @Test
  public void testToString() {
    assertEquals("AdjustedCost{minSafeTransferTimeFactor: 2.0}", subject.toString());
  }

  @Test
  public void create() {
    assertNull(AdjustedCost.create(-0.0001));
    assertNotNull(AdjustedCost.create(0.0001));
  }

  @Test
  public void sort() {
    var filter = new SortOnAdjustedCost(false, subject);
    assertEquals(
        toStr(List.of(i1_0tx, i3_10m, i2_1m)),
        toStr(filter.filter(List.of(i2_1m, i3_10m, i1_0tx)))
    );
  }

  /** Time relative to noon in minutes */
  private static int noonTime(String time) {
    return parseTimeCompact(time) / 60 - 12 * 60;
  }
}