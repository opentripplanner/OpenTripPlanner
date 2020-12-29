package org.opentripplanner.api.model.serverinfo;

import org.opentripplanner.model.projectinfo.OtpProjectInfo;

public class ApiConfigInfo {

  private final String otpConfigVersion;
  private final String buildConfigVersion;
  private final String routerConfigVersion;

  public ApiConfigInfo(OtpProjectInfo projectInfo) {
    this.otpConfigVersion = projectInfo.otpConfigVersion;
    this.buildConfigVersion = projectInfo.buildConfigVersion;
    this.routerConfigVersion = projectInfo.routerConfigVersion;
  }

  public String getOtpConfigVersion() {
    return otpConfigVersion;
  }

  public String getBuildConfigVersion() {
    return buildConfigVersion;
  }

  public String getRouterConfigVersion() {
    return routerConfigVersion;
  }
}
