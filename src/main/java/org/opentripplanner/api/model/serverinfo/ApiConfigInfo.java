package org.opentripplanner.api.model.serverinfo;

import org.opentripplanner.model.projectinfo.ProjectInfo;

public class ApiConfigInfo {

  private final String otpConfigVersion;
  private final String buildConfigVersion;
  private final String routerConfigVersion;

  public ApiConfigInfo(ProjectInfo projectInfo) {
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
