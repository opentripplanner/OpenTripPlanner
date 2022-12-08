package org.opentripplanner.transit.model.site;

import java.util.Collection;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class MultiModalStationBuilder
  extends AbstractEntityBuilder<MultiModalStation, MultiModalStationBuilder> {

  private Collection<Station> childStations;

  private I18NString name;

  private WgsCoordinate coordinate;

  private String code;

  private String description;

  private I18NString url;

  MultiModalStationBuilder(FeedScopedId id) {
    super(id);
  }

  MultiModalStationBuilder(@Nonnull MultiModalStation original) {
    super(original);
    this.childStations = original.getChildStations();
    this.name = original.getName();
    this.coordinate = original.getCoordinate();
    this.code = original.getCode();
    this.description = original.getDescription();
    this.url = original.getUrl();
  }

  public MultiModalStationBuilder withName(I18NString name) {
    this.name = name;
    return this;
  }

  public I18NString name() {
    return name;
  }

  public MultiModalStationBuilder withChildStations(Collection<Station> childStations) {
    this.childStations = childStations;
    return this;
  }

  public Set<Station> childStations() {
    return Set.copyOf(this.childStations);
  }

  public MultiModalStationBuilder withCoordinate(WgsCoordinate coordinate) {
    this.coordinate = coordinate;
    return this;
  }

  public WgsCoordinate coordinate() {
    return coordinate;
  }

  public MultiModalStationBuilder withCode(String code) {
    this.code = code;
    return this;
  }

  public String code() {
    return code;
  }

  public MultiModalStationBuilder withDescription(String description) {
    this.description = description;
    return this;
  }

  public String description() {
    return description;
  }

  public MultiModalStationBuilder withUrl(I18NString url) {
    this.url = url;
    return this;
  }

  public I18NString url() {
    return url;
  }

  @Override
  protected MultiModalStation buildFromValues() {
    return new MultiModalStation(this);
  }
}
