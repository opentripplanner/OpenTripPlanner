package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.units.qual.C;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/** Responsible for mapping GTFS ShapePoint into the OTP model. */
class ShapePointMapper {

  private final IdFactory idFactory;

  ShapePointMapper(IdFactory idFactory) {
    this.idFactory = idFactory;
  }

  Map<FeedScopedId, CompactShape> map(
    Collection<org.onebusaway.gtfs.model.ShapePoint> allShapePoints
  ) {
    var ret = new HashMap<FeedScopedId, CompactShape>();
    for (var shapePoint : allShapePoints) {
      var shapeId = idFactory.createId(shapePoint.getShapeId(), "shape point");
      var shape = ret.computeIfAbsent(shapeId, id -> new CompactShape());
      shape.addPoint(shapePoint);
    }
    return ret;
  }
}
