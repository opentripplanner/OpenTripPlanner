package org.opentripplanner.netex.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.HashGridSpatialIndex;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.api.Issue;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.StopModelBuilder;
import org.rutebanken.netex.model.FlexibleArea;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.rutebanken.netex.model.KeyListStructure;
import org.rutebanken.netex.model.KeyValueStructure;

class FlexStopsMapper {

  /**
   * Key-value pair used until proper NeTEx support is added
   */
  private static final String FLEXIBLE_STOP_AREA_TYPE_KEY = "FlexibleStopAreaType";
  /**
   * Key-value pair used until proper NeTEx support is added
   */
  private static final String UNRESTRICTED_PUBLIC_TRANSPORT_AREAS_VALUE =
    "UnrestrictedPublicTransportAreas";
  private final FeedScopedIdFactory idFactory;
  private final HashGridSpatialIndex<RegularStop> stopsSpatialIndex;
  private final StopModelBuilder stopModelBuilder;
  private final DataImportIssueStore issueStore;
  private final TransportModeMapper transportModeMapper = new TransportModeMapper();

  FlexStopsMapper(
    FeedScopedIdFactory idFactory,
    Collection<RegularStop> stops,
    StopModelBuilder stopModelBuilder,
    DataImportIssueStore issueStore
  ) {
    this.idFactory = idFactory;
    this.stopsSpatialIndex = new HashGridSpatialIndex<>();
    for (RegularStop stop : stops) {
      Envelope env = new Envelope(stop.getCoordinate().asJtsCoordinate());
      this.stopsSpatialIndex.insert(env, stop);
    }
    this.stopModelBuilder = stopModelBuilder;
    this.issueStore = issueStore;
  }

  /**
   * Maps NeTEx FlexibleStopPlace to FlexStopLocation. The support for GroupStop is
   * dependent on a key/value in the NeTEx file, until proper NeTEx support is added.
   */
  StopLocation map(FlexibleStopPlace flexibleStopPlace) {
    List<StopLocation> stops = new ArrayList<>();
    TransitMode flexibleStopTransitMode = mapTransitMode(flexibleStopPlace);
    var areas = flexibleStopPlace.getAreas().getFlexibleAreaOrFlexibleAreaRefOrHailAndRideArea();
    List<Geometry> areaGeometries = new ArrayList<>();
    for (var area : areas) {
      if (!(area instanceof FlexibleArea flexibleArea)) {
        issueStore.add(
          Issue.issue(
            "UnsupportedFlexibleStopPlaceAreaType",
            "FlexibleStopPlace %s contains an unsupported area %s.",
            flexibleStopPlace.getId(),
            area
          )
        );
        continue;
      }

      Geometry flexibleAreaGeometry = mapGeometry(flexibleArea);
      areaGeometries.add(flexibleAreaGeometry);

      if (shouldAddStopsFromArea(flexibleArea, flexibleStopPlace)) {
        stops.addAll(
          findStopsInFlexArea(flexibleArea, flexibleAreaGeometry, flexibleStopTransitMode)
        );
      } else {
        AreaStop areaStop = mapFlexArea(
          flexibleArea,
          flexibleAreaGeometry,
          flexibleStopPlace.getName().getValue()
        );
        if (areaStop != null) {
          stops.add(areaStop);
        }
      }
    }

    if (stops.isEmpty()) {
      return null;
    } else {
      // We create a new GroupStop, even if the stop place consists of a single area, in order to
      // get the ids for the area and stop place correct
      var builder = stopModelBuilder
        .groupStop(idFactory.createId(flexibleStopPlace.getId()))
        .withName(new NonLocalizedString(flexibleStopPlace.getName().getValue()))
        .withEncompassingAreaGeometries(areaGeometries);
      stops.forEach(builder::addLocation);
      return builder.build();
    }
  }

  /**
   * Allows pickup / drop off along any eligible street inside the area
   */
  AreaStop mapFlexArea(FlexibleArea area, Geometry geometry, String backupName) {
    if (geometry == null) {
      return null;
    }

    var areaName = area.getName();
    return stopModelBuilder
      .areaStop(idFactory.createId(area.getId()))
      .withName(new NonLocalizedString(areaName != null ? areaName.getValue() : backupName))
      .withGeometry(geometry)
      .build();
  }

  /**
   * Allows pickup / drop off at all regular stops inside the area that match the transit mode of the
   * flexible stop place.
   */
  List<RegularStop> findStopsInFlexArea(
    FlexibleArea area,
    @Nullable Geometry geometry,
    @Nullable TransitMode flexibleStopTransitMode
  ) {
    if (geometry == null || flexibleStopTransitMode == null) {
      return List.of();
    }
    List<RegularStop> stops = stopsSpatialIndex
      .query(geometry.getEnvelopeInternal())
      .stream()
      .filter(stop -> flexibleStopTransitMode == stop.getGtfsVehicleType())
      .filter(stop -> geometry.contains(stop.getGeometry()))
      .toList();

    if (stops.isEmpty()) {
      issueStore.add(
        Issue.issue(
          "MissingStopsInUnrestrictedPublicTransportAreas",
          "FlexibleArea %s with type UnrestrictedPublicTransportAreas does not contain any regular stop.",
          area.getId()
        )
      );
      return List.of();
    }

    return stops;
  }

  private Geometry mapGeometry(FlexibleArea area) {
    try {
      return OpenGisMapper.mapGeometry(area.getPolygon());
    } catch (Exception e) {
      issueStore.add(
        Issue.issue(
          "InvalidFlexAreaGeometry",
          "FlexibleArea %s has an invalid geometry.",
          area.getId()
        )
      );
      return null;
    }
  }

  @Nullable
  private TransitMode mapTransitMode(FlexibleStopPlace flexibleStopPlace) {
    try {
      return transportModeMapper.mapAllVehicleModesOfTransport(
        flexibleStopPlace.getTransportMode()
      );
    } catch (TransportModeMapper.UnsupportedModeException e) {
      return null;
    }
  }

  private boolean shouldAddStopsFromArea(FlexibleArea flexibleArea, FlexibleStopPlace parentStop) {
    var flexibleAreaType = getFlexibleStopAreaType(flexibleArea.getKeyList());
    var parentStopType = getFlexibleStopAreaType(parentStop.getKeyList());

    if (UNRESTRICTED_PUBLIC_TRANSPORT_AREAS_VALUE.equals(flexibleAreaType)) {
      return true;
    } else {
      return (
        UNRESTRICTED_PUBLIC_TRANSPORT_AREAS_VALUE.equals(parentStopType) && flexibleAreaType == null
      );
    }
  }

  private static String getFlexibleStopAreaType(KeyListStructure keyListStructure) {
    String flexibleStopAreaType = null;
    if (keyListStructure != null) {
      for (KeyValueStructure k : keyListStructure.getKeyValue()) {
        if (k.getKey().equals(FLEXIBLE_STOP_AREA_TYPE_KEY)) {
          flexibleStopAreaType = k.getValue();
          break;
        }
      }
    }
    return flexibleStopAreaType;
  }
}
