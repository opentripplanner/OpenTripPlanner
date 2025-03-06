package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.util.Objects;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.utils.lang.OtpNumberFormat;
import org.opentripplanner.utils.time.DurationUtils;

public final class DefaultRaptorTransfer implements RaptorTransfer {

  private final int stop;
  private final int durationInSeconds;
  private final int c1;
  private final Transfer transfer;

  public DefaultRaptorTransfer(int stop, int durationInSeconds, int c1, Transfer transfer) {
    this.stop = stop;
    this.durationInSeconds = durationInSeconds;
    this.c1 = c1;
    this.transfer = transfer;
  }

  public int stop() {
    return stop;
  }

  public int durationInSeconds() {
    return durationInSeconds;
  }

  public int c1() {
    return c1;
  }

  public Transfer transfer() {
    return transfer;
  }

  public DefaultRaptorTransfer reverseOf(int fromStopIndex) {
    return new DefaultRaptorTransfer(fromStopIndex, durationInSeconds, c1, transfer);
  }

  @Override
  public boolean equals(Object o) {
    // Note! The 'transfer' is not part of the equals method, two paths with the same cost and
    // dutation is equal, we should just pick one.
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    var that = (DefaultRaptorTransfer) o;
    return stop == that.stop && durationInSeconds == that.durationInSeconds && c1 == that.c1;
  }

  @Override
  public int hashCode() {
    // See implementation note in equals(..)
    return Objects.hash(stop, durationInSeconds, c1);
  }

  @Override
  public String toString() {
    String duration = DurationUtils.durationToStr(durationInSeconds());
    return String.format(
      "%s %s %s ~ %d",
      transfer.modesAsString(),
      duration,
      OtpNumberFormat.formatCostCenti(c1),
      stop()
    );
  }
}
