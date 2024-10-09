package org.opentripplanner.transit.model.organization;

import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class BrandingBuilder extends AbstractEntityBuilder<Branding, BrandingBuilder> {

  private String shortName;
  private String name;
  private String url;
  private String description;
  private String image;

  BrandingBuilder(FeedScopedId id) {
    super(id);
  }

  BrandingBuilder(Branding original) {
    super(original);
    this.shortName = original.getShortName();
    this.name = original.getName();
    this.url = original.getUrl();
    this.description = original.getDescription();
    this.image = original.getImage();
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

  @Override
  protected Branding buildFromValues() {
    return new Branding(this);
  }
}
