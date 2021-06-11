package org.opentripplanner.updater.bike_rental.datasources.params;

import org.opentripplanner.updater.DataSourceType;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Map;

public class BikeRentalDataSourceParameters {
  private final DataSourceType sourceType;
  private final String url;
  private final String network;
  private final String apiKey;
  private final Map<String, String> httpHeaders;

  public BikeRentalDataSourceParameters(
      DataSourceType sourceType,
      String url,
      String network,
      String apiKey,
      Map<String, String> httpHeaders
  ) {
    this.sourceType = sourceType;
    this.url = url;
    this.network = network;
    this.apiKey = apiKey;
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

  @Nullable
  public String getApiKey() {
    return apiKey;
  }

  public Map<String, String> getHttpHeaders() {
    return httpHeaders;
  }
}
