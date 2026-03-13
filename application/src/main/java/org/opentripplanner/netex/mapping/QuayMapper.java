package org.opentripplanner.netex.mapping;

import static org.opentripplanner.transit.model.basic.TransitMode.RAIL;

import java.util.Collection;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.netex.mapping.support.NetexMainAndSubMode;
import org.opentripplanner.transit.model.site.FareZone;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.Quay;

class QuayMapper {

  private static final String RAIL_REPLACEMENT_BUS_VALUE =
    BusSubmodeEnumeration.RAIL_REPLACEMENT_BUS.value();

  private final DataImportIssueStore issueStore;

  private final FeedScopedIdFactory idFactory;

  private final SiteRepositoryBuilder siteRepositoryBuilder;

  QuayMapper(
    FeedScopedIdFactory idFactory,
    DataImportIssueStore issueStore,
    SiteRepositoryBuilder siteRepositoryBuilder
  ) {
    this.idFactory = idFactory;
    this.issueStore = issueStore;
    this.siteRepositoryBuilder = siteRepositoryBuilder;
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
    var id = idFactory.createId(quay.getId());
    return siteRepositoryBuilder.computeRegularStopIfAbsent(id, it ->
      map(it, quay, parentStation, fareZones, transitMode, wheelchair)
    );
  }

  private RegularStop map(
    FeedScopedId id,
    Quay quay,
    Station parentStation,
    Collection<FareZone> fareZones,
    NetexMainAndSubMode transitMode,
    Accessibility wheelchair
  ) {
    var coordinate = WgsCoordinateMapper.mapToDomain(quay.getCentroid());

    if (coordinate == null) {
      issueStore.add(
        "QuayWithoutCoordinates",
        "Quay %s does not contain any coordinates.",
        quay.getId()
      );
      return null;
    }

    String subMode = transitMode.subMode();
    boolean sometimesUsedRealtime = false;

    if (OTPFeature.IncludeStopsUsedRealTimeInTransfers.isOn()) {
      if (transitMode.mainMode() == RAIL) {
        sometimesUsedRealtime = true;
      }
      // We only consider rail and rail-replacement-bus stops when generating transfers. The reason
      // we do not include all stops is due to perfomance reasons. This should be replaced by
      // generating transfers as needed for realtime updates.
      else if (subMode != null && subMode.equals(RAIL_REPLACEMENT_BUS_VALUE)) {
        sometimesUsedRealtime = true;
      }
    }

    var builder = siteRepositoryBuilder
      .regularStop(id)
      .withParentStation(parentStation)
      .withName(parentStation.getName())
      .withPlatformCode(quay.getPublicCode() != null ? quay.getPublicCode().getValue() : null)
      .withDescription(
        NonLocalizedString.ofNullable(
          MultilingualStringMapper.nullableValueOf(quay.getDescription())
        )
      )
      .withCoordinate(WgsCoordinateMapper.mapToDomain(quay.getCentroid()))
      .withWheelchairAccessibility(wheelchair)
      .withVehicleType(transitMode.mainMode())
      .withNetexVehicleSubmode(subMode)
      .withSometimesUsedRealtime(sometimesUsedRealtime);

    builder.fareZones().addAll(fareZones);

    return builder.build();
  }
}
