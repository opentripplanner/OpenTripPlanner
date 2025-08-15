package org.opentripplanner.service.vehiclerental.model;

import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Based on the field rental_apps in {@ https://github.com/NABSA/gbfs/blob/master/gbfs.md#system_informationjson
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class VehicleRentalSystemAppInformation {

  public static final VehicleRentalSystemAppInformation DEFAULT =
    new VehicleRentalSystemAppInformation();

  @Nullable
  private final String storeUri;

  @Nullable
  private final String discoveryUri;

  private VehicleRentalSystemAppInformation() {
    this.storeUri = null;
    this.discoveryUri = null;
  }

  private VehicleRentalSystemAppInformation(Builder builder) {
    this.storeUri = builder.storeUri;
    this.discoveryUri = builder.discoveryUri;
  }

  public VehicleRentalSystemAppInformation(String storeUri, String discoveryUri) {
    this.storeUri = storeUri;
    this.discoveryUri = discoveryUri;
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  @Nullable
  public String storeUri() {
    return storeUri;
  }

  @Nullable
  public String discoveryUri() {
    return discoveryUri;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VehicleRentalSystemAppInformation that = (VehicleRentalSystemAppInformation) o;
    return (
      Objects.equals(storeUri, that.storeUri) && Objects.equals(discoveryUri, that.discoveryUri)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(storeUri, discoveryUri);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(VehicleRentalSystemAppInformation.class)
      .addStr("storeUri", storeUri, DEFAULT.storeUri)
      .addStr("discoveryUri", discoveryUri, DEFAULT.discoveryUri)
      .toString();
  }

  public static class Builder {

    private final VehicleRentalSystemAppInformation original;
    private String storeUri;
    private String discoveryUri;

    private Builder(VehicleRentalSystemAppInformation original) {
      this.original = original;
      this.storeUri = original.storeUri;
      this.discoveryUri = original.discoveryUri;
    }

    public Builder withStoreUri(@Nullable String storeUri) {
      this.storeUri = storeUri;
      return this;
    }

    public Builder withDiscoveryUri(@Nullable String discoveryUri) {
      this.discoveryUri = discoveryUri;
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public VehicleRentalSystemAppInformation build() {
      var value = new VehicleRentalSystemAppInformation(this);
      return original.equals(value) ? original : value;
    }
  }
}
