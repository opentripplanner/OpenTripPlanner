package org.opentripplanner.netex.mapping;

import java.util.Collection;
import java.util.Optional;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.HashGridSpatialIndex;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.GroupStopBuilder;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.rutebanken.netex.model.FlexibleArea;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.rutebanken.netex.model.KeyValueStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FlexStopsMapper {

  private static final Logger LOG = LoggerFactory.getLogger(FlexStopsMapper.class);
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

  FlexStopsMapper(FeedScopedIdFactory idFactory, Collection<RegularStop> stops) {
    this.idFactory = idFactory;
    this.stopsSpatialIndex = new HashGridSpatialIndex<>();
    for (RegularStop stop : stops) {
      Envelope env = new Envelope(stop.getCoordinate().asJtsCoordinate());
      this.stopsSpatialIndex.insert(env, stop);
    }
  }

  /**
   * Maps NeTEx FlexibleStopPlace to FlexStopLocation. The support for GroupStop is
   * dependent on a key/value in the NeTEx file, until proper NeTEx support is added.
   */
  StopLocation map(FlexibleStopPlace flexibleStopPlace) {
    Object area = flexibleStopPlace
      .getAreas()
      .getFlexibleAreaOrFlexibleAreaRefOrHailAndRideArea()
      .get(0);
    if (!(area instanceof FlexibleArea)) {
      LOG.warn(
        "FlexibleStopPlace {} not mapped. Hail and ride areas are not currently supported.",
        flexibleStopPlace.getId()
      );
      return null;
    }

    Optional<KeyValueStructure> flexibleAreaType = Optional.empty();
    if (flexibleStopPlace.getKeyList() != null) {
      flexibleAreaType =
        flexibleStopPlace
          .getKeyList()
          .getKeyValue()
          .stream()
          .filter(k -> k.getKey().equals(FLEXIBLE_STOP_AREA_TYPE_KEY))
          .findFirst();
    }

    if (
      flexibleAreaType.isPresent() &&
      flexibleAreaType.get().getValue().equals(UNRESTRICTED_PUBLIC_TRANSPORT_AREAS_VALUE)
    ) {
      return mapStopsInFlexArea(flexibleStopPlace, (FlexibleArea) area);
    } else {
      return mapFlexArea(flexibleStopPlace, (FlexibleArea) area);
    }
  }

  /**
   * Allows pickup / drop off along any eligible street inside the area
   */
  AreaStop mapFlexArea(FlexibleStopPlace flexibleStopPlace, FlexibleArea area) {
    var name = new NonLocalizedString(flexibleStopPlace.getName().getValue());
    return AreaStop
      .of(idFactory.createId(flexibleStopPlace.getId()))
      .withName(name)
      .withGeometry(OpenGisMapper.mapGeometry(area.getPolygon()))
      .build();
  }

  /**
   * Allows pickup / drop off at any regular Stop inside the area
   */
  GroupStop mapStopsInFlexArea(FlexibleStopPlace flexibleStopPlace, FlexibleArea area) {
    GroupStopBuilder result = GroupStop
      .of(idFactory.createId(flexibleStopPlace.getId()))
      .withName(new NonLocalizedString(flexibleStopPlace.getName().getValue()));

    Geometry geometry = OpenGisMapper.mapGeometry(area.getPolygon());

    for (RegularStop stop : stopsSpatialIndex.query(geometry.getEnvelopeInternal())) {
      Point p = GeometryUtils
        .getGeometryFactory()
        .createPoint(stop.getCoordinate().asJtsCoordinate());
      if (geometry.contains(p)) {
        result.addLocation(stop);
      }
    }

    return result.build();
  }
}
