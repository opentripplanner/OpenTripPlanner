package org.opentripplanner.service.vehiclerental.model;

import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Based on https://github.com/NABSA/gbfs/blob/master/gbfs.md#system_informationjson
 * <p>
 */
public final class VehicleRentalSystem {

  public static final VehicleRentalSystem DEFAULT = new VehicleRentalSystem();

  @Nullable
  private final String systemId;

  @Nullable
  private final String language;

  @Nullable
  private final String name;

  @Nullable
  private final String shortName;

  @Nullable
  private final String operator;

  @Nullable
  private final String url;

  private VehicleRentalSystem() {
    this.systemId = null;
    this.language = null;
    this.name = null;
    this.shortName = null;
    this.operator = null;
    this.url = null;
  }

  private VehicleRentalSystem(Builder builder) {
    this.systemId = builder.systemId;
    this.language = builder.language;
    this.name = builder.name;
    this.shortName = builder.shortName;
    this.operator = builder.operator;
    this.url = builder.url;
  }

  public VehicleRentalSystem(
    String systemId,
    String language,
    String name,
    String shortName,
    String operator,
    String url
  ) {
    this.systemId = systemId;
    this.language = language;
    this.name = name;
    this.shortName = shortName;
    this.operator = operator;
    this.url = url;
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  @Nullable
  public String systemId() {
    return systemId;
  }

  @Nullable
  public String language() {
    return language;
  }

  @Nullable
  public String name() {
    return name;
  }

  @Nullable
  public String shortName() {
    return shortName;
  }

  @Nullable
  public String operator() {
    return operator;
  }

  @Nullable
  public String url() {
    return url;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VehicleRentalSystem that = (VehicleRentalSystem) o;
    return (
      Objects.equals(systemId, that.systemId) &&
      Objects.equals(language, that.language) &&
      Objects.equals(name, that.name) &&
      Objects.equals(shortName, that.shortName) &&
      Objects.equals(operator, that.operator) &&
      Objects.equals(url, that.url)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(systemId, language, name, shortName, operator, url);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(VehicleRentalSystem.class)
      .addStr("systemId", systemId, DEFAULT.systemId)
      .addStr("language", language, DEFAULT.language)
      .addStr("name", name, DEFAULT.name)
      .addStr("shortName", shortName, DEFAULT.shortName)
      .addStr("operator", operator, DEFAULT.operator)
      .addStr("url", url, DEFAULT.url)
      .toString();
  }

  public static class Builder {

    private final VehicleRentalSystem original;
    private String systemId;
    private String language;
    private String name;
    private String shortName;
    private String operator;
    private String url;

    private Builder(VehicleRentalSystem original) {
      this.original = original;
      this.systemId = original.systemId;
      this.language = original.language;
      this.name = original.name;
      this.shortName = original.shortName;
      this.operator = original.operator;
      this.url = original.url;
    }

    public Builder withSystemId(@Nullable String systemId) {
      this.systemId = systemId;
      return this;
    }

    public String language() {
      return language;
    }

    public Builder withLanguage(@Nullable String language) {
      this.language = language;
      return this;
    }

    public String name() {
      return name;
    }

    public Builder withName(@Nullable String name) {
      this.name = name;
      return this;
    }

    public String shortName() {
      return shortName;
    }

    public Builder withShortName(@Nullable String shortName) {
      this.shortName = shortName;
      return this;
    }

    public String operator() {
      return operator;
    }

    public Builder withOperator(@Nullable String operator) {
      this.operator = operator;
      return this;
    }

    public String url() {
      return url;
    }

    public Builder withUrl(@Nullable String url) {
      this.url = url;
      return this;
    }

    public VehicleRentalSystem build() {
      var value = new VehicleRentalSystem(this);
      return original.equals(value) ? original : value;
    }
  }
}
