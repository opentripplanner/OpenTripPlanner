package org.opentripplanner.transit.model.organization;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.basic.TransitEntity2;
import org.opentripplanner.util.lang.ToStringBuilder;

/**
 * OTP model for branding. Common for both NeTEx and GTFS.
 */
public class Branding extends TransitEntity2<Branding, BrandingBuilder> {

  private final String shortName;
  private final String name;
  private final String url;
  private final String description;
  private final String image;

  public Branding(BrandingBuilder builder) {
    super(builder.getId());
    this.shortName = builder.getShortName();
    this.name = builder.getName();
    this.url = builder.getUrl();
    this.description = builder.getDescription();
    this.image = builder.getImage();
  }

  public static BrandingBuilder of(FeedScopedId id) {
    return new BrandingBuilder(id);
  }

  @Nonnull
  public static BrandingBuilder of(@Nullable Branding original) {
    return new BrandingBuilder(original);
  }

  @Override
  public BrandingBuilder copy() {
    return new BrandingBuilder(this);
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
  public boolean sameValue(@Nonnull Branding other) {
    return (
      getId().equals(other.getId()) &&
      Objects.equals(shortName, other.shortName) &&
      Objects.equals(name, other.name) &&
      Objects.equals(url, other.url) &&
      Objects.equals(description, other.description) &&
      Objects.equals(image, other.image)
    );
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
