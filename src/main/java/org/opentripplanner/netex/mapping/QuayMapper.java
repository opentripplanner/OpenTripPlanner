package org.opentripplanner.netex.mapping;

import java.util.Collection;
import javax.annotation.Nullable;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.netex.issues.QuayWithoutCoordinates;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.netex.mapping.support.NetexMainAndSubMode;
import org.opentripplanner.transit.model.basic.Accessibility;
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
    NetexMainAndSubMode transitMode,
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
      .withVehicleType(transitMode.mainMode())
      .withNetexVehicleSubmode(transitMode.subMode());

    builder.fareZones().addAll(fareZones);

    return builder.build();
  }
}
