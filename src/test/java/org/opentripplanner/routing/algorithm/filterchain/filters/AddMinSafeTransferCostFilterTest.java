package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;

import javax.annotation.Nonnull;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.A;
import static org.opentripplanner.model.plan.TestItineraryBuilder.B;
import static org.opentripplanner.model.plan.TestItineraryBuilder.C;
import static org.opentripplanner.model.plan.TestItineraryBuilder.E;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.raptor.util.TimeUtils.parseDuration;
import static org.opentripplanner.transit.raptor.util.TimeUtils.parseTimeCompact;

public class AddMinSafeTransferCostFilterTest {

  private static final int D2m = parseDuration("2m");
  private static final int D24m = parseDuration("24m");
  private static final int D40m = parseDuration("40m");

  private static final int T10_00 = noonTime("10:00");
  private static final int T15_00 = noonTime("15:00");
  private static final int T15_01 = noonTime("15:01");
  private static final int T15_10 = noonTime("15:10");
  private static final int T16_00 = noonTime("16:00");
  private static final int T16_11 = noonTime("16:11");
  private static final int T16_30 = noonTime("16:30");
  private static final int T23_00 = noonTime("23:00");

  /* Trip ids */
  private static final int R1 = 1;
  private static final int R7 = 7;
  private static final int L2 = 2;
  private static final int L3 = 3;

  private static final Itinerary i1_0tx = newItinerary(A, T10_00)
      .rail(R1, T10_00, T16_00, E)
      .build();

  private static final Itinerary i2_1m = newItinerary(A, T10_00)
      .walk(D2m, B)
      .rail(R1, T10_00, T15_00, C)
      .bus(L2, T15_01, T16_11, E)
      .build();

  private static final Itinerary i3_10m = newItinerary(A, T10_00)
      .rail(R1, T10_00, T15_00, B)
      .bus(L3, T15_10, T16_30, E)
      .build();


  @SuppressWarnings("ConstantConditions")
  @Nonnull
  private final AddMinSafeTransferCostFilter subject = new AddMinSafeTransferCostFilter(2.0);

  @Test
  public void testTestSetUp() {
    assertEquals(-120, T10_00);
    assertEquals(180, T15_00);
  }

  @Test
  public void noTransfersNoAdditionalCost() {
    assertEquals(0, subject.calculateAdditionalCost(D40m, i1_0tx));
  }

  @Test
  public void additionalCostForOneMinuteOfUnsafeTransfer() {
    // For 1m the extra cost should be 60*2 = 120
    assertEquals(120, subject.calculateAdditionalCost(D2m, i2_1m));
  }

  @Test
  public void additionalCostFor20MinutesOfUnsafeTransfer() {
    // 40m - 10m = 30m * 2 = 30*2*60 = 3 600
    assertEquals(3_600, subject.calculateAdditionalCost(D40m, i3_10m));
  }

  @Test
  public void testNoExtraCostForTransfersAboveMinSafeTransferTime() {
    var i1 = newItinerary(A, T10_00)
        .rail(R1, T10_00, T15_00, B)
        .bus(R7, T16_00, T23_00, E)
        .build();

    // Transfer time > 30m, no extra cost
    assertEquals(0, subject.calculateAdditionalCost(D40m, i1));
  }

  @Test
  public void minSafeTransferTime() {
    // Min total travel time is 6h: 6h * 6.666% = 24m
    assertEquals(D24m, subject.minSafeTransferTime(List.of(i1_0tx)));
    assertEquals(D24m, subject.minSafeTransferTime(List.of(i1_0tx, i2_1m, i3_10m)));
  }

  @Test
  public void minSafeTransferTimeUpperBound() {
    assertEquals(D40m, subject.minSafeTransferTime(
        List.of(newItinerary(A).rail(R1, T10_00, noonTime("20:00"), E).build())
    ));
    assertEquals(D40m, subject.minSafeTransferTime(
        List.of(newItinerary(A).rail(R1, T10_00, noonTime("20:01"), E).build())
    ));
    assertEquals(D40m, subject.minSafeTransferTime(
        List.of(newItinerary(A).rail(R1, T10_00, noonTime("22:00"), E).build())
    ));
  }

  @Test
  public void testToString() {
    assertEquals(
        "add-min-safe-transfer-cost-filter{minSafeTransferTimeFactor: 2.0}",
        subject.toString()
    );
  }

  /** Time relative to noon in minutes */
  private static int noonTime(String time) {
    return parseTimeCompact(time) / 60 - 12 * 60;
  }
}