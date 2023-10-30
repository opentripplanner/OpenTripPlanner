package org.opentripplanner.ext.reportapi.configure;

import dagger.Subcomponent;
import org.opentripplanner.ext.reportapi.model.ReportService;
import org.opentripplanner.framework.di.OtpServerRequest;
import org.opentripplanner.routing.api.request.RouteRequest;

@OtpServerRequest
@Subcomponent
public interface ReportFactory {
  @OtpServerRequest
  ReportService reportService();

  @OtpServerRequest
  RouteRequest defaultRequest();

  @Subcomponent.Builder
  interface Builder {
    ReportFactory build();
  }
}
