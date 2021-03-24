package org.opentripplanner.netex.mapping;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.FareZone;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.netex.issues.QuayWithoutCoordinates;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.rutebanken.netex.model.Quay;

class StopMapper {

  private final DataImportIssueStore issueStore;

  private final FeedScopedIdFactory idFactory;

  StopMapper(
      FeedScopedIdFactory idFactory,
      DataImportIssueStore issueStore
  ) {
    this.idFactory = idFactory;
    this.issueStore = issueStore;
  }

  /**
   * Map Netex Quay to OTP Stop
   */
  @Nullable
  Stop mapQuayToStop(Quay quay, Station parentStation, List<FareZone> fareZones) {
    WgsCoordinate coordinate = WgsCoordinateMapper.mapToDomain(quay.getCentroid());

    if (coordinate == null) {
      issueStore.add(new QuayWithoutCoordinates(quay.getId()));
      return null;
    }

    Stop stop = new Stop(
        idFactory.createId(quay.getId()),
        parentStation.getName(),
        quay.getPublicCode(),
        null,
        WgsCoordinateMapper.mapToDomain(quay.getCentroid()),
        null,
        null,
        null, fareZones,
        null,
        null,
        null
    );
    stop.setParentStation(parentStation);

    return stop;
  }
}
