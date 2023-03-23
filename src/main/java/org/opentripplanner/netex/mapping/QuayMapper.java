package org.opentripplanner.netex.mapping;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.netex.issues.QuayWithoutCoordinates;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.netex.mapping.support.NetexMainAndSubMode;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.framework.EntityById;
import org.opentripplanner.transit.model.site.FareZone;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Quay;

class QuayMapper {

  private final DataImportIssueStore issueStore;

  private final FeedScopedIdFactory idFactory;

  private final EntityById<RegularStop> regularStopIndex;

  private record MappingParameters(
    Quay quay,
    Station parentStation,
    Collection<FareZone> fareZones,
    NetexMainAndSubMode transitMode,
    Accessibility wheelchair
  ) {}

  QuayMapper(
    FeedScopedIdFactory idFactory,
    DataImportIssueStore issueStore,
    EntityById<RegularStop> regularStopIndex
  ) {
    this.idFactory = idFactory;
    this.issueStore = issueStore;
    this.regularStopIndex = regularStopIndex;
  }

  /**
   * Map Netex Quay to OTP Stop
   */
  @Nullable
  RegularStop mapQuayToStop(
    @Nonnull Quay quay,
    Station parentStation,
    Collection<FareZone> fareZones,
    NetexMainAndSubMode transitMode,
    Accessibility wheelchair
  ) {
    MappingParameters parameters = new MappingParameters(
      quay,
      parentStation,
      fareZones,
      transitMode,
      wheelchair
    );
    return regularStopIndex.computeIfAbsent(
      idFactory.createId(quay.getId()),
      ignored -> map(parameters)
    );
  }

  private RegularStop map(MappingParameters parameters) {
    WgsCoordinate coordinate = WgsCoordinateMapper.mapToDomain(parameters.quay.getCentroid());

    if (coordinate == null) {
      issueStore.add(new QuayWithoutCoordinates(parameters.quay.getId()));
      return null;
    }

    var builder = RegularStop
      .of(idFactory.createId(parameters.quay.getId()))
      .withParentStation(parameters.parentStation)
      .withName(parameters.parentStation.getName())
      .withPlatformCode(parameters.quay.getPublicCode())
      .withDescription(
        NonLocalizedString.ofNullable(
          parameters.quay.getDescription(),
          MultilingualString::getValue
        )
      )
      .withCoordinate(WgsCoordinateMapper.mapToDomain(parameters.quay.getCentroid()))
      .withWheelchairAccessibility(parameters.wheelchair)
      .withVehicleType(parameters.transitMode.mainMode())
      .withNetexVehicleSubmode(parameters.transitMode.subMode());

    builder.fareZones().addAll(parameters.fareZones);

    return builder.build();
  }
}
