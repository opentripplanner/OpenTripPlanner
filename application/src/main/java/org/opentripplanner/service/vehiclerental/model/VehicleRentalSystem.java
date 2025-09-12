package org.opentripplanner.service.vehiclerental.model;

import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;
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
  private final I18NString name;

  @Nullable
  private final I18NString shortName;

  @Nullable
  private final I18NString operator;

  @Nullable
  private final String url;

  private VehicleRentalSystem() {
    this.systemId = null;
    this.name = null;
    this.shortName = null;
    this.operator = null;
    this.url = null;
  }

  private VehicleRentalSystem(Builder builder) {
    this.systemId = Objects.requireNonNull(builder.systemId);
    this.name = builder.name;
    this.shortName = builder.shortName;
    this.operator = builder.operator;
    this.url = builder.url;
  }

  public VehicleRentalSystem(
    String systemId,
    I18NString name,
    I18NString shortName,
    I18NString operator,
    String url
  ) {
    this.systemId = Objects.requireNonNull(systemId);
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

  public String systemId() {
    return systemId;
  }

  public I18NString name() {
    return name;
  }

  @Nullable
  public I18NString shortName() {
    return shortName;
  }

  @Nullable
  public I18NString operator() {
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
      Objects.equals(name, that.name) &&
      Objects.equals(shortName, that.shortName) &&
      Objects.equals(operator, that.operator) &&
      Objects.equals(url, that.url)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(systemId, name, shortName, operator, url);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(VehicleRentalSystem.class)
      .addStr("systemId", systemId, DEFAULT.systemId)
      .addObj("name", name, DEFAULT.name)
      .addObj("shortName", shortName, DEFAULT.shortName)
      .addObj("operator", operator, DEFAULT.operator)
      .addStr("url", url, DEFAULT.url)
      .toString();
  }

  public static class Builder {

    private final VehicleRentalSystem original;
    private String systemId;
    private I18NString name;
    private I18NString shortName;
    private I18NString operator;
    private String url;

    private Builder(VehicleRentalSystem original) {
      this.original = original;
      this.systemId = original.systemId;
      this.name = original.name;
      this.shortName = original.shortName;
      this.operator = original.operator;
      this.url = original.url;
    }

    public Builder withSystemId(String systemId) {
      this.systemId = systemId;
      return this;
    }

    public I18NString name() {
      return name;
    }

    public Builder withName(I18NString name) {
      this.name = name;
      return this;
    }

    public @Nullable I18NString shortName() {
      return shortName;
    }

    public Builder withShortName(@Nullable I18NString shortName) {
      this.shortName = shortName;
      return this;
    }

    public @Nullable I18NString operator() {
      return operator;
    }

    public Builder withOperator(@Nullable I18NString operator) {
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
