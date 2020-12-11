package org.opentripplanner.api.model.serverinfo;

import org.opentripplanner.model.projectinfo.ProjectInfo;

public class ApiServerInfo {
  public final String cpuName;
  public final int nCores;
  public final ApiProjectVersion version;
  public final ApiVersionControlInfo versionControl;
  public final ApiConfigInfo config;

  public ApiServerInfo(String cpuName, int nCores, ProjectInfo projectInfo) {
    this.cpuName = cpuName;
    this.nCores = nCores;
    this.version = new ApiProjectVersion(projectInfo.version);
    this.versionControl = new ApiVersionControlInfo(projectInfo.versionControl);
    this.config = new ApiConfigInfo(projectInfo);
  }

}
