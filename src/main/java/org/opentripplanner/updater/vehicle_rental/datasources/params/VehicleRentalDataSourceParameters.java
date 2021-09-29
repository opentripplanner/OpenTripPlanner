package org.opentripplanner.updater.vehicle_rental.datasources.params;

import org.opentripplanner.updater.DataSourceType;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Map;

public class VehicleRentalDataSourceParameters {
  private final DataSourceType sourceType;
  private final String url;
  private final Map<String, String> httpHeaders;

  public VehicleRentalDataSourceParameters(
      DataSourceType sourceType,
      String url,
      @NotNull
      Map<String, String> httpHeaders
  ) {
    this.sourceType = sourceType;
    this.url = url;
    this.httpHeaders = httpHeaders;
  }

  @NotNull
  public String getUrl() { return url; }

  @NotNull
  public DataSourceType getSourceType() {
    return sourceType;
  }

  @NotNull
  public Map<String, String> getHttpHeaders() {
    return httpHeaders;
  }
}
