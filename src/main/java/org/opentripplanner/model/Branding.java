package org.opentripplanner.model;

import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.basic.ToStringBuilder;
import org.opentripplanner.transit.model.basic.TransitEntity;

/**
 * OTP model for branding. Common for both NeTEx and GTFS.
 */
public class Branding extends TransitEntity {

  private final String shortName;
  private final String name;
  private final String url;
  private final String description;
  private final String image;

  public Branding(
    FeedScopedId id,
    String shortName,
    String name,
    String url,
    String description,
    String image
  ) {
    super(id);
    this.shortName = shortName;
    this.name = name;
    this.url = url;
    this.description = description;
    this.image = image;
  }

  public String getShortName() {
    return shortName;
  }

  public String getName() {
    return name;
  }

  public String getUrl() {
    return url;
  }

  public String getImage() {
    return image;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(this.getClass())
      .addObj("id", this.getId())
      .addStr("shortName", this.getShortName())
      .addStr("name", this.getName())
      .addStr("url", this.getUrl())
      .addStr("description", this.getDescription())
      .addStr("image", this.getImage())
      .toString();
  }
}
