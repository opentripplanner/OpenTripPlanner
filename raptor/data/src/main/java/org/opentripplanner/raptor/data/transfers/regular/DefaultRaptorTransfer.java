package org.opentripplanner.raptor.data.transfers.regular;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.raptor.api.model.RaptorValueType;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.utils.time.DurationUtils;

class DefaultRaptorTransfer implements RaptorTransfer, Serializable {

  private final int toStop;
  private final int duration;
  private final int c1;

  public DefaultRaptorTransfer(int toStop, int duration, int c1) {
    this.toStop = toStop;
    this.duration = duration;
    this.c1 = c1;
  }

  @Override
  public int stop() {
    return toStop;
  }

  @Override
  public int durationInSeconds() {
    return duration;
  }

  @Override
  public int c1() {
    return c1;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DefaultRaptorTransfer that = (DefaultRaptorTransfer) o;
    return toStop == that.toStop && duration == that.duration && c1 == that.c1;
  }

  @Override
  public int hashCode() {
    return Objects.hash(toStop, duration, c1);
  }

  @Override
  public String toString() {
    return (
      "Transfer " +
      toStop +
      " ~ [" +
      DurationUtils.durationToStr(duration) +
      ", " +
      RaptorValueType.C1.format(c1) +
      "]"
    );
  }
}
