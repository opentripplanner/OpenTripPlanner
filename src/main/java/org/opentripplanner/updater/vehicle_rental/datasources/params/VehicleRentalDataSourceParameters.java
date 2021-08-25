package org.opentripplanner.updater.vehicle_rental.datasources.params;

import org.opentripplanner.updater.DataSourceType;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Map;

public class VehicleRentalDataSourceParameters {
  private final DataSourceType sourceType;
  private final String url;
  private final String network;
  private final Map<String, String> httpHeaders;

  public VehicleRentalDataSourceParameters(
      DataSourceType sourceType,
      String url,
      String network,
      @NotNull
      Map<String, String> httpHeaders
  ) {
    this.sourceType = sourceType;
    this.url = url;
    this.network = network;
    this.httpHeaders = httpHeaders;
  }

  @NotNull
  public String getUrl() { return url; }

  @NotNull
  public DataSourceType getSourceType() {
    return sourceType;
  }

  /**
   * Each updater can be assigned a unique network ID in the configuration to prevent
   * returning bikes at stations for another network.
   * TODO shouldn't we give each updater a unique network ID by default?
   */
  @Nullable
  public String getNetwork(String defaultValue) {
    return network == null || network.isEmpty() ? defaultValue : network;
  }

  @NotNull
  public Map<String, String> getHttpHeaders() {
    return httpHeaders;
  }
}
