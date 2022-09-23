package org.opentripplanner.netex.mapping;

import java.util.Collection;
import javax.annotation.Nullable;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.netex.issues.QuayWithoutCoordinates;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.NonLocalizedString;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.basic.WgsCoordinate;
import org.opentripplanner.transit.model.site.FareZone;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Quay;

class QuayMapper {

  private final DataImportIssueStore issueStore;

  private final FeedScopedIdFactory idFactory;

  QuayMapper(FeedScopedIdFactory idFactory, DataImportIssueStore issueStore) {
    this.idFactory = idFactory;
    this.issueStore = issueStore;
  }

  /**
   * Map Netex Quay to OTP Stop
   */
  @Nullable
  RegularStop mapQuayToStop(
    Quay quay,
    Station parentStation,
    Collection<FareZone> fareZones,
    T2<TransitMode, String> transitMode,
    Accessibility wheelchair
  ) {
    WgsCoordinate coordinate = WgsCoordinateMapper.mapToDomain(quay.getCentroid());

    if (coordinate == null) {
      issueStore.add(new QuayWithoutCoordinates(quay.getId()));
      return null;
    }

    var builder = RegularStop
      .of(idFactory.createId(quay.getId()))
      .withParentStation(parentStation)
      .withName(parentStation.getName())
      .withPlatformCode(quay.getPublicCode())
      .withDescription(
        NonLocalizedString.ofNullable(quay.getDescription(), MultilingualString::getValue)
      )
      .withCoordinate(WgsCoordinateMapper.mapToDomain(quay.getCentroid()))
      .withWheelchairAccessibility(wheelchair)
      .withVehicleType(transitMode.first)
      .withNetexVehicleSubmode(transitMode.second);

    builder.fareZones().addAll(fareZones);

    return builder.build();
  }
}
