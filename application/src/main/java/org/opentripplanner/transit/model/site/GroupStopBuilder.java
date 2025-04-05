package org.opentripplanner.transit.model.site;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
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

  private List<StopLocation> stopLocations = new ArrayList<>();

  private GeometryCollection geometry = new GeometryCollection(
    null,
    GeometryUtils.getGeometryFactory()
  );

  private GeometryCollection encompassingAreaGeometry = null;

  private WgsCoordinate centroid;

  GroupStopBuilder(FeedScopedId id, IntSupplier indexCounter) {
    super(id);
    this.indexCounter = indexCounter;
  }

  GroupStopBuilder(GroupStop original) {
    super(original);
    this.indexCounter = original::getIndex;
    // Optional fields
    this.name = original.getName();
    this.stopLocations = new ArrayList<>(original.getChildLocations());
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

  public GroupStopBuilder withEncompassingAreaGeometries(List<Geometry> geometries) {
    this.encompassingAreaGeometry = new GeometryCollection(
      geometries.toArray(new Geometry[0]),
      GeometryUtils.getGeometryFactory()
    );
    return this;
  }

  public I18NString name() {
    return name;
  }

  public GroupStopBuilder addLocation(StopLocation location) {
    if (
      !(location.getStopType() == StopType.REGULAR ||
        location.getStopType() == StopType.FLEXIBLE_AREA)
    ) {
      throw new RuntimeException(
        String.format(
          "Unsupported location for %s. Must be %s or %s.",
          GroupStop.class.getSimpleName(),
          StopType.REGULAR,
          StopType.FLEXIBLE_AREA
        )
      );
    }

    stopLocations.add(location);

    int numGeometries = geometry.getNumGeometries();
    Geometry[] newGeometries = new Geometry[numGeometries + 1];
    for (int i = 0; i < numGeometries; i++) {
      newGeometries[i] = geometry.getGeometryN(i);
    }
    newGeometries[numGeometries] = location.getGeometry();

    geometry = new GeometryCollection(newGeometries, GeometryUtils.getGeometryFactory());
    centroid = new WgsCoordinate(geometry.getCentroid());

    return this;
  }

  public List<StopLocation> stopLocations() {
    return List.copyOf(stopLocations);
  }

  public GeometryCollection geometry() {
    return geometry;
  }

  public GeometryCollection encompassingAreaGeometry() {
    return encompassingAreaGeometry;
  }

  public WgsCoordinate centroid() {
    return centroid;
  }

  int createIndex() {
    return indexCounter.getAsInt();
  }
}
