package org.opentripplanner.model;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.common.geometry.GeometryUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * A group of stopLocations, which can share a common Stoptime
 */
public class FlexLocationGroup extends TransitEntity implements StopLocation {

  private static final long serialVersionUID = 1L;

  private String name;

  private final Set<StopLocation> stopLocations = new HashSet<>();

  private GeometryCollection geometry = new GeometryCollection(null, GeometryUtils.getGeometryFactory());

  public FlexLocationGroup(FeedScopedId id) {
    super(id);
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getCode() {
    return null;
  }

  /**
   * Returns the centroid of all stops and areas belonging to this location group.
   */
  @Override
  public WgsCoordinate getCoordinate() {
    Point centroid = geometry.getCentroid();
    return new WgsCoordinate(centroid.getY(), centroid.getX());
  }

  /**
   * Adds a new location to the location group.
   * This should ONLY be used during the graph build process.
   */
  public void addLocation(StopLocation location) {
    stopLocations.add(location);

    int numGeometries = geometry.getNumGeometries();
    Geometry[] newGeometries = new Geometry[numGeometries + 1];
    for (int i = 0; i < numGeometries; i++) {
      newGeometries[i] = geometry.getGeometryN(i);
    }
    if (location instanceof Stop) {
      WgsCoordinate coordinate = location.getCoordinate();
      Envelope envelope = new Envelope(coordinate.asJtsCoordinate());
      double xscale = Math.cos(coordinate.latitude() * Math.PI / 180);
      envelope.expandBy(100 / xscale, 100);
      newGeometries[numGeometries] = GeometryUtils.getGeometryFactory().toGeometry(envelope);
    } else if (location instanceof FlexStopLocation) {
      newGeometries[numGeometries] = ((FlexStopLocation) location).getGeometry();
    } else {
      throw new RuntimeException("Unknown location type");
    }
    geometry = new GeometryCollection(newGeometries, GeometryUtils.getGeometryFactory());
  }

  /**
   * Returns all the locations belonging to this location group.
   */
  public Set<StopLocation> getLocations() {
    return stopLocations;
  }
}