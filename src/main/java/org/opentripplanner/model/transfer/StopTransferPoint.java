package org.opentripplanner.model.transfer;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.model.Stop;

public class StopTransferPoint implements TransferPoint, Serializable {

  private static final long serialVersionUID = 1L;

  private final Stop stop;

  public StopTransferPoint(Stop stop) {
    this.stop = stop;
  }

  @Override
  public Stop getStop() {
    return stop;
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
