package org.opentripplanner.api.model.serverinfo;

import org.opentripplanner.model.projectinfo.MavenProjectVersion;

public class ApiProjectVersion {

  private final MavenProjectVersion projectVersion;

  public ApiProjectVersion(MavenProjectVersion version) {
    this.projectVersion = version;
  }

  public String getVersion() {
    return projectVersion.version;
  }

  public int getMajor() {
    return projectVersion.major;
  }

  public int getMinor() {
    return projectVersion.minor;
  }

  public int getPatch() {
    return projectVersion.patch;
  }

  public String getQualifier() {
    return projectVersion.qualifier;
  }
}
