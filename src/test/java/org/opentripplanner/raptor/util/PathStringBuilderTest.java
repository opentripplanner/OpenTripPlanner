package org.opentripplanner.raptor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor.api.path.PathStringBuilder;
import org.opentripplanner.raptor.spi.RaptorStopNameResolver;

public class PathStringBuilderTest {

  private static final RaptorStopNameResolver STOP_NAME_RESOLVER = RaptorStopNameResolver.nullSafe(
    null
  );
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
      "Walk 17s ~ oslo:1 Rental  2m",
      subject.walk(17).sep().stop("oslo:1").space().rental(120).toString()
    );
  }

  @Test
  public void walkWithDurationPadded() {
    assertEquals("Walk   17s", new PathStringBuilder(STOP_NAME_RESOLVER, true).walk(17).toString());
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
  public void flexZeroLength() {
    assertEquals("Flex 0s 0x", subject.flex(0, 0).toString());
  }

  @Test
  public void flexNoramlCase() {
    assertEquals("Flex 5m12s 2x", subject.flex(D_5_12, 2).toString());
  }

  @Test
  public void sep() {
    assertEquals("1 ~ 2", subject.stop(1).sep().stop(2).toString());
  }

  @Test
  public void path() {
    assertEquals(
      "Walk 37s ~ 227 ~ BUS 10:46:05 10:55 ~ 112 ~ Walk 1h37m7s",
      subject
        .walk(37)
        .sep()
        .stop(227)
        .sep()
        .transit(MODE, T_10_46_05, T_10_55)
        .sep()
        .stop(112)
        .sep()
        .walk(3600 + 37 * 60 + 7)
        .toString()
    );
  }

  @Test
  public void pathWithoutAccessAndEgress() {
    assertEquals(
      "227 ~ BUS 10:46:05 10:55 ~ 112",
      subject
        .accessEgress(TestAccessEgress.walk(227, 0, 0))
        .sep()
        .stop(227)
        .sep()
        .transit(MODE, T_10_46_05, T_10_55)
        .sep()
        .stop(112)
        .sep()
        .accessEgress(TestAccessEgress.walk(112, 0, 0))
        .toString()
    );
  }

  /* privet methods */

  private static int time(int hour, int min, int sec) {
    return 3600 * hour + 60 * min + sec;
  }
}
