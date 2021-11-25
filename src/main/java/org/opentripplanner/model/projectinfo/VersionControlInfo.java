package org.opentripplanner.model.projectinfo;

import org.opentripplanner.model.base.ToStringBuilder;

import java.io.Serializable;

import static org.opentripplanner.model.projectinfo.OtpProjectInfo.UNKNOWN;

public class VersionControlInfo implements Serializable {
  private static final long serialVersionUID = 1L;

  public final String commit;
  public final String branch;
  public final String commitTime;
  public final String buildTime;
  public final boolean dirty;

  public VersionControlInfo() {
    this(UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, true);
  }

  public VersionControlInfo(
      String commit,
      String branch,
      String commitTime,
      String buildTime,
      boolean dirty
  ) {
    this.commit = commit;
    this.branch = branch;
    this.commitTime = commitTime;
    this.buildTime = buildTime;
    this.dirty = dirty;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(VersionControlInfo.class)
        .addStr("commit", commit)
        .addStr("branch", branch)
        .addStr("commitTime", commitTime)
        .addStr("buildTime", buildTime)
        .addBool("dirty", dirty)
        .toString();
  }
}
