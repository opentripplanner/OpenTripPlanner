package org.opentripplanner.model.transfer;

import java.io.Serializable;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.base.ValueObjectToStringBuilder;

public final class StationTransferPoint implements TransferPoint, Serializable {

  private static final long serialVersionUID = 1L;

  private final Station station;


  public StationTransferPoint(Station station) {
    this.station = station;
  }

  public Station getStation() {
    return station;
  }

  @Override
  public boolean appliesToAllTrips() {
    return true;
  }

  @Override
  public int getSpecificityRanking() {
    return 0;
  }

  @Override
  public boolean isStationTransferPoint() { return true; }

  public String toString() {
    return ValueObjectToStringBuilder.of()
            .addText("<Station ")
            .addObj(station.getId())
            .addText(">")
            .toString();
  }
}
