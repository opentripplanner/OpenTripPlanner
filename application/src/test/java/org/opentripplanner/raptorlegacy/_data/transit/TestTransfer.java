package org.opentripplanner.raptorlegacy._data.transit;

import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.utils.lang.OtpNumberFormat;
import org.opentripplanner.utils.time.DurationUtils;

public record TestTransfer(int stop, int durationInSeconds, int c1) implements RaptorTransfer {
  public TestTransfer reverseOf(int stop) {
    return new TestTransfer(stop, durationInSeconds, c1);
  }

  @Override
  public String toString() {
    String duration = DurationUtils.durationToStr(durationInSeconds());
    return String.format("WALK %s %s ~ %d", duration, OtpNumberFormat.formatCostCenti(c1), stop());
  }
}
