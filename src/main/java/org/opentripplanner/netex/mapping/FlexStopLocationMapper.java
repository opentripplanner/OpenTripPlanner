package org.opentripplanner.netex.mapping;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.model.FlexLocationGroup;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.rutebanken.netex.model.FlexibleArea;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.rutebanken.netex.model.KeyValueStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;

class FlexStopLocationMapper {

  private final FeedScopedIdFactory idFactory;

  private final HashGridSpatialIndex<Stop> stopsSpatialIndex;

  private static final Logger LOG = LoggerFactory.getLogger(FlexStopLocationMapper.class);

  /**
   * Key-value pair used until proper NeTEx support is added
   */
  private static final String FLEXIBLE_STOP_AREA_TYPE_KEY = "FlexibleStopAreaType";

  /**
   * Key-value pair used until proper NeTEx support is added
   */
  private static final String UNRESTRICTED_PUBLIC_TRANSPORT_AREAS_VALUE = "UnrestrictedPublicTransportAreas";

  FlexStopLocationMapper(FeedScopedIdFactory idFactory, Collection<Stop> stops) {
    this.idFactory = idFactory;
    this.stopsSpatialIndex = new HashGridSpatialIndex<>();
    for (Stop stop : stops) {
      Envelope env = new Envelope(stop.getCoordinate().asJtsCoordinate());
      this.stopsSpatialIndex.insert(env, stop);
    }
  }

  /**
   * Maps NeTEx FlexibleStopPlace to FlexStopLocation. The support for FlexLocationGroup is
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
      flexibleAreaType = flexibleStopPlace
          .getKeyList()
          .getKeyValue()
          .stream()
          .filter(k -> k.getKey().equals(FLEXIBLE_STOP_AREA_TYPE_KEY))
          .findFirst();
    }

    if (flexibleAreaType.isPresent()
        && flexibleAreaType.get().getValue().equals(UNRESTRICTED_PUBLIC_TRANSPORT_AREAS_VALUE)
    ) {
      return mapStopsInFlexArea(flexibleStopPlace, (FlexibleArea) area);
    }
    else {
      return mapFlexArea(flexibleStopPlace, (FlexibleArea) area);
    }
  }

  /**
   * Allows pickup / drop off along any eligible street inside the area
   */
  FlexStopLocation mapFlexArea(FlexibleStopPlace flexibleStopPlace, FlexibleArea area) {
    FlexStopLocation result = new FlexStopLocation(idFactory.createId(flexibleStopPlace.getId()));
    result.setName(flexibleStopPlace.getName().getValue());
    result.setGeometry(OpenGisMapper.mapGeometry(area.getPolygon()));
    return result;
  }

  /**
   * Allows pickup / drop off at any regular Stop inside the area
   */
  FlexLocationGroup mapStopsInFlexArea(FlexibleStopPlace flexibleStopPlace, FlexibleArea area) {
    FlexLocationGroup result = new FlexLocationGroup(idFactory.createId(flexibleStopPlace.getId()));
    result.setName(flexibleStopPlace.getName().getValue());
    Geometry geometry = OpenGisMapper.mapGeometry(area.getPolygon());

    for (Stop stop : stopsSpatialIndex.query(geometry.getEnvelopeInternal())) {
      Point p = GeometryUtils
          .getGeometryFactory()
          .createPoint(stop.getCoordinate().asJtsCoordinate());
      if (geometry.contains(p)) {
        result.addLocation(stop);
      }
    }

    return result;
  }
}
