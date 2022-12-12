package org.opentripplanner.routing.bike_rental;

import java.util.Collections;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

public class GeofencingZones {

  private final Set<GeofencingZone> zones;
  private final GeometryFactory geometryFactory = new GeometryFactory();

  public GeofencingZones() {
    this.zones = Collections.emptySet();
  }

  public GeofencingZones(Set<GeofencingZone> zones) {
    this.zones = zones;
  }

  public boolean canDropOffVehicle(Geometry g) {
    // if there are no rules, this means that you can drop off free-floating bikes anywhere (in the world!)
    if (isEmpty()) {
      return true;
    } else {
      return (
        zones.stream().anyMatch(zone -> zone.dropOffAllowed(g)) &&
        zones.stream().noneMatch(zone -> zone.dropOffProhibited(g))
      );
    }
  }

  public boolean canDropOffVehicle(Coordinate c) {
    var point = geometryFactory.createPoint(c);
    return canDropOffVehicle(point);
  }

  public int size() {
    return zones.size();
  }

  public boolean isEmpty() {
    return zones.isEmpty();
  }

  public boolean nonEmpty() {
    return !isEmpty();
  }

  public Envelope getEnvelope() {
    var envelope = new Envelope();
    zones.forEach(z -> envelope.expandToInclude(z.geometry.getEnvelopeInternal()));
    return envelope;
  }

  public static class GeofencingZone {

    final Geometry geometry;
    final boolean dropOffAllowed;

    public GeofencingZone(Geometry geometry, boolean dropOffAllowed) {
      this.geometry = geometry;
      this.dropOffAllowed = dropOffAllowed;
    }

    boolean dropOffAllowed(Geometry p) {
      return geometry.intersects(p) && dropOffAllowed;
    }

    boolean dropOffProhibited(Geometry p) {
      return geometry.intersects(p) && !dropOffAllowed;
    }
  }
}
