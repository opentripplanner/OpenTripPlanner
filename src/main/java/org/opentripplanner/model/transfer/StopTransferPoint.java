package org.opentripplanner.model.transfer;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;

public class StopTransferPoint implements TransferPoint, Serializable {

  private static final long serialVersionUID = 1L;

  private final StopLocation stop;

  public StopTransferPoint(StopLocation stop) {
    this.stop = stop;
  }

  @Override
  public StopLocation getStop() {
    return stop;
  }

  @Override
  public boolean applyToAllTrips() {
    return true;
  }

  @Override
  public int getSpecificityRanking() {
    return 0;
  }

  @Override
  public String toString() {
    return "(stop: " + stop.getId() + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) { return true; }
    if (!(o instanceof StopTransferPoint)) { return false; }
    final StopTransferPoint that = (StopTransferPoint) o;
    return Objects.equals(stop.getId(), that.stop.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(stop.getId());
  }
}
