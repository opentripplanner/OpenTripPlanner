package org.opentripplanner.transit.model.site;

import java.util.Objects;
import java.util.function.IntSupplier;
import javax.annotation.Nonnull;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class AreaStopBuilder extends AbstractEntityBuilder<AreaStop, AreaStopBuilder> {

  private final IntSupplier indexCounter;
  private I18NString name;
  private boolean hasFallbackName;
  private I18NString description;

  private Geometry geometry;
  private WgsCoordinate centroid;
  private I18NString url;
  private String zoneId;

  AreaStopBuilder(FeedScopedId id, IntSupplier indexCounter) {
    super(id);
    this.indexCounter = Objects.requireNonNull(indexCounter);
  }

  AreaStopBuilder(@Nonnull AreaStop original) {
    super(original);
    this.indexCounter = original::getIndex;
    // Optional fields
    this.name = original.getName();
    this.hasFallbackName = original.hasFallbackName();
    this.url = original.getUrl();
    this.description = original.getDescription();
    this.zoneId = original.getFirstZoneAsString();
    this.geometry = original.getGeometry();
    this.centroid = original.getCoordinate();
  }

  public AreaStopBuilder withZoneId(String zoneId) {
    this.zoneId = zoneId;
    return this;
  }

  public boolean hasFallbackName() {
    return hasFallbackName;
  }

  @Override
  protected AreaStop buildFromValues() {
    return new AreaStop(this);
  }

  public AreaStopBuilder withName(I18NString name) {
    this.name = name;
    return this;
  }

  public AreaStopBuilder withDescription(I18NString description) {
    this.description = description;
    return this;
  }

  public AreaStopBuilder withUrl(I18NString url) {
    this.url = url;
    return this;
  }

  public AreaStopBuilder withGeometry(Geometry geometry) {
    this.geometry = geometry;
    this.centroid = new WgsCoordinate(geometry.getCentroid());
    return this;
  }

  public I18NString name() {
    return name;
  }

  public I18NString description() {
    return description;
  }

  public I18NString url() {
    return url;
  }

  public String zoneId() {
    return zoneId;
  }

  public Geometry geometry() {
    return geometry;
  }

  public WgsCoordinate centroid() {
    return centroid;
  }

  int createIndex() {
    return indexCounter.getAsInt();
  }
}
