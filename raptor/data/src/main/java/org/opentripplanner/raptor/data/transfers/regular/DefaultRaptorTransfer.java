package org.opentripplanner.raptor.data.transfers.regular;

import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.utils.lang.OtpNumberFormat;
import org.opentripplanner.utils.time.DurationUtils;

/**
 * An immutable, materialized {@link RaptorTransfer}. This is what {@link RaptorTransfer#materialize()}
 * returns for the flyweight cursor over the packed transfers.
 *
 * @param stop              see {@link RaptorTransfer#stop()}
 * @param c1                see {@link RaptorTransfer#c1()}
 * @param durationInSeconds see {@link RaptorTransfer#durationInSeconds()}
 */
record DefaultRaptorTransfer(int stop, int c1, int durationInSeconds) implements RaptorTransfer {
  @Override
  public RaptorTransfer materialize() {
    return this;
  }

  @Override
  public String toString() {
    return (
      "Transfer " +
      stop +
      " ~ [" +
      DurationUtils.durationToStr(durationInSeconds) +
      ", " +
      OtpNumberFormat.formatCostCenti(c1) +
      "]"
    );
  }
}
