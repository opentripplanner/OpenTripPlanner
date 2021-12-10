package org.opentripplanner.model;

import java.util.HashSet;
import java.util.Set;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.common.geometry.GeometryUtils;

/**
 * A group of stopLocations, which can share a common Stoptime
 */
public class FlexLocationGroup extends TransitEntity implements StopLocation {

  private static final long serialVersionUID = 1L;

  private String name;

  private final Set<StopLocation> stopLocations = new HashSet<>();

  private GeometryCollection geometry = new GeometryCollection(null, GeometryUtils.getGeometryFactory());

  private Point centroid;

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
  public String getDescription() {
    return null;
  }

  @Override
  public String getUrl() {
    return null;
  }

  /**
   * Returns the centroid of all stops and areas belonging to this location group.
   */
  @Override
  public WgsCoordinate getCoordinate() {
    return new WgsCoordinate(centroid.getY(), centroid.getX());
  }

  @Override
  public String getFirstZoneAsString() {
    return null;
  }

  @Override
  public Geometry getGeometry() {
    return geometry;
  }

  @Override
  public boolean isPartOfStation() {
    return false;
  }

  @Override
  public boolean isPartOfSameStationAs(StopLocation alternativeStop) {
    return false;
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
      newGeometries[numGeometries] = location.getGeometry();
    } else {
      throw new RuntimeException("Unknown location type");
    }
    geometry = new GeometryCollection(newGeometries, GeometryUtils.getGeometryFactory());
    centroid = geometry.getCentroid();
  }

  /**
   * Returns all the locations belonging to this location group.
   */
  public Set<StopLocation> getLocations() {
    return stopLocations;
  }
}