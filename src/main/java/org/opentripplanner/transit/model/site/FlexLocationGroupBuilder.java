package org.opentripplanner.transit.model.site;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.opentripplanner.transit.model.basic.I18NString;
import org.opentripplanner.transit.model.basic.WgsCoordinate;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.util.geometry.GeometryUtils;

public class FlexLocationGroupBuilder
  extends AbstractEntityBuilder<FlexLocationGroup, FlexLocationGroupBuilder> {

  private I18NString name;

  private Set<StopLocation> stopLocations = new HashSet<>();

  private GeometryCollection geometry = new GeometryCollection(
    null,
    GeometryUtils.getGeometryFactory()
  );

  private WgsCoordinate centroid;

  FlexLocationGroupBuilder(FeedScopedId id) {
    super(id);
  }

  FlexLocationGroupBuilder(@Nonnull FlexLocationGroup original) {
    super(original);
    // Optional fields
    this.name = original.getName();
    this.stopLocations = new HashSet<>(original.getLocations());
    this.geometry = (GeometryCollection) original.getGeometry();
    this.centroid = original.getCoordinate();
  }

  @Override
  protected FlexLocationGroup buildFromValues() {
    return new FlexLocationGroup(this);
  }

  public FlexLocationGroupBuilder withName(I18NString name) {
    this.name = name;
    return this;
  }

  public I18NString name() {
    return name;
  }

  public FlexLocationGroupBuilder addLocation(StopLocation location) {
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
    centroid = new WgsCoordinate(geometry.getCentroid().getY(), geometry.getCentroid().getX());

    return this;
  }

  public Set<StopLocation> stopLocations() {
    return Set.copyOf(stopLocations);
  }

  public GeometryCollection geometry() {
    return geometry;
  }

  public WgsCoordinate centroid() {
    return centroid;
  }
}
