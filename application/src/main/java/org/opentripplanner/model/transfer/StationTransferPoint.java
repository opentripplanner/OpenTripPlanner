package org.opentripplanner.model.transfer;

import java.io.Serializable;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.utils.tostring.ValueObjectToStringBuilder;

public final class StationTransferPoint implements TransferPoint, Serializable {

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
  public boolean isStationTransferPoint() {
    return true;
  }

  public String toString() {
    return ValueObjectToStringBuilder.of()
      .addText("StationTP{")
      .addObj(station.getId())
      .addText("}")
      .toString();
  }
}
