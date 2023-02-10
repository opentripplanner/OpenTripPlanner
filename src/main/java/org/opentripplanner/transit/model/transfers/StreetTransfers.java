package org.opentripplanner.transit.model.transfers;

import java.util.Iterator;
import org.opentripplanner.raptor.api.model.RaptorTransfer;

public class StreetTransfers implements Iterator<RaptorTransfer>, RaptorTransfer {

  private final int[] values;

  private final int[] fromStopIndex;

  private int currentIndex = 0;
  private int currentIndexEnd = 0;

  public StreetTransfers() {
    // TODO RTM
    this.values = new int[0];
    this.fromStopIndex = new int[0];
  }

  public Iterator<? extends RaptorTransfer> withStop(int stopIndex) {
    this.currentIndex = fromStopIndex[stopIndex] - 3;
    this.currentIndexEnd = fromStopIndex[stopIndex + 1];
    return this;
  }

  @Override
  public boolean hasNext() {
    currentIndex += 3;
    return currentIndex < currentIndexEnd;
  }

  @Override
  public RaptorTransfer next() {
    return this;
  }

  @Override
  public int stop() {
    return values[currentIndex];
  }

  @Override
  public int durationInSeconds() {
    return values[currentIndex + 1];
  }

  @Override
  public int generalizedCost() {
    return values[currentIndex + 2];
  }
}
