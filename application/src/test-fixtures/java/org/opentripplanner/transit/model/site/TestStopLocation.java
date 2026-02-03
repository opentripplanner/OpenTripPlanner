package org.opentripplanner.transit.model.site;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.geometry.WgsCoordinate;

public class TestStopLocation implements StopLocation {

  private final FeedScopedId id;

  public TestStopLocation(FeedScopedId id) {
    this.id = id;
  }

  @Override
  public FeedScopedId getId() {
    return id;
  }

  @Override
  public int getIndex() {
    throw new NotImplementedException("Implementation doesn't support getIndex().");
  }

  @Override
  @Nullable
  public I18NString getName() {
    return I18NString.of(id.toString());
  }

  @Override
  @Nullable
  public I18NString getDescription() {
    return null;
  }

  @Override
  @Nullable
  public I18NString getUrl() {
    return null;
  }

  @Override
  public StopType getStopType() {
    return null;
  }

  @Override
  public WgsCoordinate getCoordinate() {
    return null;
  }

  @Override
  @Nullable
  public Geometry getGeometry() {
    return null;
  }

  @Override
  public boolean isPartOfStation() {
    return false;
  }

  @Override
  public boolean isPartOfSameStationAs(StopLocation alternativeStop) {
    return false;
  }
}
