package org.opentripplanner.transit.model.organization;

import org.opentripplanner.transit.model.basic.FeedScopedId;

public class BrandingBuilder {

  private FeedScopedId id;
  private String shortName;
  private String name;
  private String url;
  private String description;
  private String image;

  public BrandingBuilder(FeedScopedId id) {
    this.id = id;
  }

  BrandingBuilder(Branding domain) {
    this.id = domain.getId();
    this.shortName = domain.getShortName();
    this.name = domain.getName();
    this.url = domain.getUrl();
    this.description = domain.getDescription();
    this.image = domain.getImage();
  }

  public Branding build() {
    return new Branding(this);
  }

  public FeedScopedId getId() {
    return id;
  }

  public BrandingBuilder setId(FeedScopedId id) {
    this.id = id;
    return this;
  }

  public String getShortName() {
    return shortName;
  }

  public BrandingBuilder setShortName(String shortName) {
    this.shortName = shortName;
    return this;
  }

  public String getName() {
    return name;
  }

  public BrandingBuilder setName(String name) {
    this.name = name;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public BrandingBuilder setUrl(String url) {
    this.url = url;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public BrandingBuilder setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getImage() {
    return image;
  }

  public BrandingBuilder setImage(String image) {
    this.image = image;
    return this;
  }
}
