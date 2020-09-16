package org.opentripplanner.netex.loader.mapping;

import net.opengis.gml._3.AbstractRingPropertyType;
import net.opengis.gml._3.LinearRingType;
import net.opengis.gml._3.PolygonType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.api.resource.CoordinateArrayListSequence;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.model.FlexStopLocation;
import org.rutebanken.netex.model.FlexibleArea;
import org.rutebanken.netex.model.FlexibleStopPlace;

import java.util.ArrayList;
import java.util.List;

public class FlexStopLocationMapper {

  private final FeedScopedIdFactory idFactory;

  public FlexStopLocationMapper(FeedScopedIdFactory idFactory) {
    this.idFactory = idFactory;
  }

  /**
   * Maps NeTEx FlexibleStopPlace to FlexStopLocation. This currently does not support
   * FlexLocationGroup, as an equivalent is not defined in the NeTEx Nordic profile.
   */
  public FlexStopLocation map(FlexibleStopPlace flexibleStopPlace) {
    FlexStopLocation result = new FlexStopLocation();
    result.setId(idFactory.createId(flexibleStopPlace.getId()));
    result.setName(flexibleStopPlace.getName().getValue());

    Object flexibleAreaOrFlexibleAreaRefOrHailAndRideArea = flexibleStopPlace
        .getAreas()
        .getFlexibleAreaOrFlexibleAreaRefOrHailAndRideArea()
        .get(0); // Only one area allowed in NeTEx Nordic profile.

    if (flexibleAreaOrFlexibleAreaRefOrHailAndRideArea instanceof FlexibleArea) {
      result.setGeometry(mapGeometry((
          (FlexibleArea) flexibleAreaOrFlexibleAreaRefOrHailAndRideArea
      ).getPolygon()));
    }
    else {
      throw new IllegalArgumentException("Hail and ride areas are not currently supported.");
    }
    return result;
  }

  private Geometry mapGeometry(PolygonType polygonType) {
    return new Polygon(
        new LinearRing(mapCoordinateSequence(polygonType.getExterior()),
            GeometryUtils.getGeometryFactory()
        ),
        polygonType
            .getInterior()
            .stream()
            .map(c -> new LinearRing(mapCoordinateSequence(c), GeometryUtils.getGeometryFactory()))
            .toArray(LinearRing[]::new),
        GeometryUtils.getGeometryFactory()
    );
  }

  private CoordinateSequence mapCoordinateSequence(
      AbstractRingPropertyType abstractRingPropertyType
  ) {
    List<Double> posList = ((LinearRingType) abstractRingPropertyType.getAbstractRing().getValue())
        .getPosList()
        .getValue();

    // Convert a single list of alternating lat/lon values into coordinates
    ArrayList<Coordinate> coordinates = new ArrayList<>();
    for (int i = 0; i < posList.size(); i += 2) {
      coordinates.add(new Coordinate(posList.get(i), posList.get(i + 1)));
    }

    return new CoordinateArrayListSequence(coordinates);
  }
}
