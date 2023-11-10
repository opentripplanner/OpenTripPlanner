package org.opentripplanner.transit.model.site;

import java.util.HashSet;
import java.util.Set;
import java.util.function.IntSupplier;
import javax.annotation.Nonnull;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class GroupStopBuilder extends AbstractEntityBuilder<GroupStop, GroupStopBuilder> {

  private final IntSupplier indexCounter;

  private I18NString name;

  private Set<StopLocation> stopLocations = new HashSet<>();

  private GeometryCollection geometry = new GeometryCollection(
    null,
    GeometryUtils.getGeometryFactory()
  );

  private WgsCoordinate centroid;

  GroupStopBuilder(FeedScopedId id, IntSupplier indexCounter) {
    super(id);
    this.indexCounter = indexCounter;
  }

  GroupStopBuilder(@Nonnull GroupStop original) {
    super(original);
    this.indexCounter = original::getIndex;
    // Optional fields
    this.name = original.getName();
    this.stopLocations = new HashSet<>(original.getLocations());
    this.geometry = (GeometryCollection) original.getGeometry();
    this.centroid = original.getCoordinate();
  }

  @Override
  protected GroupStop buildFromValues() {
    return new GroupStop(this);
  }

  public GroupStopBuilder withName(I18NString name) {
    this.name = name;
    return this;
  }

  public I18NString name() {
    return name;
  }

  public GroupStopBuilder addLocation(StopLocation location) {
    stopLocations.add(location);

    int numGeometries = geometry.getNumGeometries();
    Geometry[] newGeometries = new Geometry[numGeometries + 1];
    for (int i = 0; i < numGeometries; i++) {
      newGeometries[i] = geometry.getGeometryN(i);
    }
    if (location instanceof RegularStop) {
      WgsCoordinate coordinate = location.getCoordinate();
      Envelope envelope = new Envelope(coordinate.asJtsCoordinate());
      double xscale = Math.cos(coordinate.latitude() * Math.PI / 180);
      envelope.expandBy(100 / xscale, 100);
      newGeometries[numGeometries] = GeometryUtils.getGeometryFactory().toGeometry(envelope);
    } else if (location instanceof AreaStop) {
      newGeometries[numGeometries] = location.getGeometry();
    } else {
      throw new RuntimeException("Unknown location type");
    }
    geometry = new GeometryCollection(newGeometries, GeometryUtils.getGeometryFactory());
    centroid = new WgsCoordinate(geometry.getCentroid());

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

  int createIndex() {
    return indexCounter.getAsInt();
  }
}
