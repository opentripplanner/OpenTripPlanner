package org.opentripplanner.transit.model.organization;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.LogInfo;

/**
 * OTP model for branding. Common for both NeTEx and GTFS.
 */
public class Branding extends AbstractTransitEntity<Branding, BrandingBuilder> implements LogInfo {

  private final String name;
  private final String shortName;
  private final String url;
  private final String description;
  private final String image;

  Branding(BrandingBuilder builder) {
    super(builder.getId());
    // Required fields - id only

    // Optional fields
    this.name = builder.getName();
    this.shortName = builder.getShortName();
    this.url = builder.getUrl();
    this.description = builder.getDescription();
    this.image = builder.getImage();
  }

  public static BrandingBuilder of(@Nonnull FeedScopedId id) {
    return new BrandingBuilder(id);
  }

  @Nullable
  public String getName() {
    return logName();
  }

  @Override
  @Nullable
  public String logName() {
    return name;
  }

  @Nullable
  public String getShortName() {
    return shortName;
  }

  @Nullable
  public String getUrl() {
    return url;
  }

  @Nullable
  public String getImage() {
    return image;
  }

  @Nullable
  public String getDescription() {
    return description;
  }

  @Override
  @Nonnull
  public BrandingBuilder copy() {
    return new BrandingBuilder(this);
  }

  @Override
  public boolean sameAs(@Nonnull Branding other) {
    return (
      getId().equals(other.getId()) &&
      Objects.equals(name, other.name) &&
      Objects.equals(shortName, other.shortName) &&
      Objects.equals(url, other.url) &&
      Objects.equals(description, other.description) &&
      Objects.equals(image, other.image)
    );
  }
}
