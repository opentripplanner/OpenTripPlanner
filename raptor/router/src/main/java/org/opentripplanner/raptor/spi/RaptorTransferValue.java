package org.opentripplanner.raptor.spi;

import org.opentripplanner.utils.time.DurationUtils;

/**
 * An immutable, materialized {@link RaptorTransfer}. Use this to keep a transfer after the iterator
 * step it was read in, for example in a path or in the transfer optimization. Transfers returned by
 * {@link RaptorTransferDataProvider#getTransfersFromStop(int)} may be flyweights and must NOT be
 * retained, copy them with {@link #copyOf(RaptorTransfer)}.
 *
 * @param stop              see {@link RaptorTransfer#stop()}
 * @param c1                see {@link RaptorTransfer#c1()}
 * @param durationInSeconds see {@link RaptorTransfer#durationInSeconds()}
 */
public record RaptorTransferValue(
  int stop,
  int c1,
  int durationInSeconds
) implements RaptorTransfer {
  public static RaptorTransferValue copyOf(RaptorTransfer transfer) {
    return new RaptorTransferValue(transfer.stop(), transfer.c1(), transfer.durationInSeconds());
  }

  @Override
  public String toString() {
    return (
      "Transfer " +
      stop +
      " ~ [" +
      DurationUtils.durationToStr(durationInSeconds) +
      ", " +
      "$" +
      RaptorCostConverter.toOtpDomainCost(c1) +
      "]"
    );
  }
}
