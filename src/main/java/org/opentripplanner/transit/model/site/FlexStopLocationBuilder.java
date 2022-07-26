package org.opentripplanner.transit.model.site;

import javax.annotation.Nonnull;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.transit.model.basic.I18NString;
import org.opentripplanner.transit.model.basic.WgsCoordinate;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class FlexStopLocationBuilder
  extends AbstractEntityBuilder<FlexStopLocation, FlexStopLocationBuilder> {

  private boolean hasFallbackName;
  private I18NString name;

  private Geometry geometry;
  private WgsCoordinate centroid;

  FlexStopLocationBuilder(FeedScopedId id) {
    super(id);
  }

  FlexStopLocationBuilder(@Nonnull FlexStopLocation original) {
    super(original);
    // Optional fields
    this.name = original.getName();
    this.hasFallbackName = original.hasFallbackName();
  }

  @Override
  protected FlexStopLocation buildFromValues() {
    return new FlexStopLocation(this);
  }

  public boolean hasFallbackName() {
    return hasFallbackName;
  }

  public FlexStopLocationBuilder withName(I18NString name) {
    this.name = name;
    return this;
  }

  public FlexStopLocationBuilder withGeometry(Geometry geometry) {
    this.geometry = geometry;
    this.centroid = new WgsCoordinate(geometry.getCentroid().getY(), geometry.getCentroid().getX());
    return this;
  }

  public I18NString name() {
    return name;
  }

  public Geometry geometry() {
    return geometry;
  }

  public WgsCoordinate centroid() {
    return centroid;
  }
}
