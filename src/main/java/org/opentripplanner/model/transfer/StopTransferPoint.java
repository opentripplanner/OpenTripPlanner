package org.opentripplanner.model.transfer;

import org.opentripplanner.model.StopLocation;

import java.io.Serializable;

public class StopTransferPoint implements TransferPoint, Serializable {

  private static final long serialVersionUID = 1L;

  private final StopLocation stop;


  public StopTransferPoint(StopLocation stop) {
    this.stop = stop;
  }

  public StopLocation getStop() {
    return stop;
  }

  @Override
  public boolean appliesToAllTrips() {
    return true;
  }

  @Override
  public int getSpecificityRanking() {
    return 1;
  }

  @Override
  public boolean isStopTransferPoint() { return true; }

  public String toString() {
    return "<Stop " + stop.getId() + ">";
  }
}
