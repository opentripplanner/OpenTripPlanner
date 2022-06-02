package org.opentripplanner.transit.model.organization;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.basic.TransitEntityBuilder;

public class BrandingBuilder extends TransitEntityBuilder<Branding, BrandingBuilder> {

  private String shortName;
  private String name;
  private String url;
  private String description;
  private String image;

  public BrandingBuilder(FeedScopedId id) {
    super(id);
  }

  BrandingBuilder(@Nullable Branding original) {
    super(original);
  }

  public String getShortName() {
    return shortName;
  }

  public BrandingBuilder withShortName(String shortName) {
    this.shortName = shortName;
    return this;
  }

  public String getName() {
    return name;
  }

  public BrandingBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public BrandingBuilder withUrl(String url) {
    this.url = url;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public BrandingBuilder withDescription(String description) {
    this.description = description;
    return this;
  }

  public String getImage() {
    return image;
  }

  public BrandingBuilder withImage(String image) {
    this.image = image;
    return this;
  }

  @NotNull
  @Override
  protected Branding buildFromValues() {
    return new Branding(this);
  }

  @Override
  protected void updateLocal(@Nonnull Branding original) {
    this.shortName = original.getShortName();
    this.name = original.getName();
    this.url = original.getUrl();
    this.description = original.getDescription();
    this.image = original.getImage();
  }
}
