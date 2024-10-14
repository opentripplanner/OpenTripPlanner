package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.framework.collection.MapUtils;
import org.opentripplanner.model.ShapePoint;

/** Responsible for mapping GTFS ShapePoint into the OTP model. */
class ShapePointMapper {

  private final Map<org.onebusaway.gtfs.model.ShapePoint, ShapePoint> mappedShapePoints = new HashMap<>();

  Collection<ShapePoint> map(Collection<org.onebusaway.gtfs.model.ShapePoint> allShapePoints) {
    return MapUtils.mapToList(allShapePoints, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  ShapePoint map(org.onebusaway.gtfs.model.ShapePoint orginal) {
    return orginal == null ? null : mappedShapePoints.computeIfAbsent(orginal, this::doMap);
  }

  private ShapePoint doMap(org.onebusaway.gtfs.model.ShapePoint rhs) {
    ShapePoint lhs = new ShapePoint();

    lhs.setShapeId(AgencyAndIdMapper.mapAgencyAndId(rhs.getShapeId()));
    lhs.setSequence(rhs.getSequence());
    lhs.setLat(rhs.getLat());
    lhs.setLon(rhs.getLon());
    lhs.setDistTraveled(rhs.getDistTraveled());

    // Skip mapping of proxy
    // private transient StopTimeProxy proxy;
    if (rhs.getProxy() != null) {
      throw new IllegalStateException("Did not expect proxy to be set! Data: " + rhs);
    }

    return lhs;
  }
}
