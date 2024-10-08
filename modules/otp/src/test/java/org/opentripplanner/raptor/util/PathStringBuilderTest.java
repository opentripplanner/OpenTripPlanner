package org.opentripplanner.raptor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flex;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.free;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.api.path.PathStringBuilder;
import org.opentripplanner.raptor.api.path.RaptorStopNameResolver;

public class PathStringBuilderTest {

  private static final RaptorStopNameResolver STOP_NAME_RESOLVER = RaptorStopNameResolver.nullSafe(
    null
  );
  private static final int ANY_STOP = 7;
  private static final String MODE = "BUS";
  private static final int T_10_46_05 = time(10, 46, 5);
  private static final int T_10_55 = time(10, 55, 0);
  private static final int D_5_12 = time(0, 5, 12);

  private final PathStringBuilder subject = new PathStringBuilder(STOP_NAME_RESOLVER);

  @Test
  public void walkSomeMinutesAndSeconds() {
    assertEquals("Walk 5m12s", subject.walk(D_5_12).toString());
  }

  @Test
  public void walkSomeSeconds() {
    assertEquals("Walk 17s", subject.walk(17).toString());
  }

  @Test
  public void walkThenRent() {
    assertEquals(
      "Walk 17s ~ oslo:1 Rental 2m",
      subject.walk(17).pickupRental("oslo:1", 120).toString()
    );
  }

  @Test
  public void transit() {
    assertEquals("BUS 10:46:05 10:55", subject.transit(MODE, T_10_46_05, T_10_55).toString());
  }

  @Test
  public void stop() {
    assertEquals("5000", subject.stop(5000).toString());
  }

  @Test
  public void ignoreFreeLeg() {
    assertEquals("Walk 1s", subject.accessEgress(free(ANY_STOP)).walk(1).toString());
  }

  @Test
  public void flexNormalCase() {
    assertEquals("Flex 5m12s 2x", subject.accessEgress(flex(ANY_STOP, D_5_12, 2)).toString());
  }

  @Test
  public void summary() {
    int START_TIME = time(12, 35, 0);
    int END_TIME = time(13, 45, 0);
    assertEquals(
      "[12:35 13:45 1h10m Tₓ1 C₁1.23 C₂5]",
      subject.summary(START_TIME, END_TIME, 1, 123, 5).toString()
    );
  }

  @Test
  public void summaryGeneralizedCostOnly() {
    assertEquals("[C₁0.01 C₂7]", subject.summary(1, 7).toString());
  }

  @Test
  public void path() {
    int egressDuration = 3600 + 37 * 60 + 7;
    assertEquals(
      "Walk 37s ~ 227 ~ BUS 10:46:05 10:55 ~ 112 ~ Walk 1h37m7s [10:44 12:33 1h49m Tₓ0 C₁567 C₂7]",
      subject
        .walk(37)
        .stop(227)
        .transit(MODE, T_10_46_05, T_10_55)
        .stop(112)
        .walk(egressDuration)
        .summary(time(10, 44, 0), time(12, 33, 0), 0, 56700, 7)
        .toString()
    );
  }

  @Test
  public void pathWithoutAccessAndEgress() {
    assertEquals(
      "227 ~ BUS 10:46:05 10:55 ~ 112 [10:46:05 10:55 8m55s Tₓ0 C₁60 C₂9 3pz]",
      subject
        .accessEgress(free(227))
        .stop(227)
        .transit(MODE, T_10_46_05, T_10_55)
        .stop(112)
        .accessEgress(free(112))
        .summary(T_10_46_05, T_10_55, 0, 6000, 9, b -> b.text("3pz"))
        .toString()
    );
  }

  /* privet methods */

  private static int time(int hour, int min, int sec) {
    return 3600 * hour + 60 * min + sec;
  }
}
