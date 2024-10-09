package org.opentripplanner.api.model.serverinfo;

import org.opentripplanner.model.projectinfo.VersionControlInfo;

public class ApiVersionControlInfo {

  private final VersionControlInfo versionControl;

  public ApiVersionControlInfo(VersionControlInfo versionControl) {
    this.versionControl = versionControl;
  }

  public String getCommit() {
    return versionControl.commit;
  }

  public String getBranch() {
    return versionControl.branch;
  }

  public String getCommitTime() {
    return versionControl.commitTime;
  }

  public String getBuildTime() {
    return versionControl.buildTime;
  }

  public boolean isDirty() {
    return versionControl.dirty;
  }
}
