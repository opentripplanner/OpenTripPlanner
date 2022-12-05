package org.opentripplanner.model.projectinfo;

import java.io.Serializable;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.OtpConfig;
import org.opentripplanner.standalone.config.RouterConfig;

public class OtpProjectInfo implements Serializable {

  static final String UNKNOWN = "UNKNOWN";
  private static final OtpProjectInfo INSTANCE = OtpProjectInfoParser.loadFromProperties();
  /** Info derived from version string */
  public final MavenProjectVersion version;

  /**
   * The graph file header expected for this instance of OTP.
   */
  public final GraphFileHeader graphFileHeaderInfo;

  /** Other info from git-commit-id-maven-plugin via otp-project-info.properties */
  public final VersionControlInfo versionControl;

  // ** Config file versions **
  // Each config file may have a "configVersion". This version is made available to the entire
  // application here - the config might not be available. For example, the build-config is not
  // available in the router and to the router APIs.

  /** See {@link OtpConfig#configVersion} */
  public String otpConfigVersion;

  /** See {@link BuildConfig#configVersion} */
  public String buildConfigVersion;

  /** See {@link RouterConfig#getConfigVersion()} */
  public String routerConfigVersion;

  OtpProjectInfo() {
    this("0.0.0-ParseFailure", new GraphFileHeader(), new VersionControlInfo());
  }

  public OtpProjectInfo(
    String version,
    GraphFileHeader graphFileHeaderInfo,
    VersionControlInfo versionControl
  ) {
    this.version = MavenProjectVersion.parse(version);
    this.graphFileHeaderInfo = graphFileHeaderInfo;
    this.versionControl = versionControl;
  }

  public static OtpProjectInfo projectInfo() {
    return INSTANCE;
  }

  public long getUID() {
    return hashCode();
  }

  /**
   * Return {@code true} if the graph file and the running instance of OTP is the same instance. If
   * the running instance of OTP or the Graph.obj serialization id is unknown, then {@code true} is
   * returned.
   */
  public boolean matchesRunningOTPInstance(GraphFileHeader graphFileHeader) {
    if (graphFileHeader.isUnknown()) {
      return true;
    }
    if (this.graphFileHeaderInfo.isUnknown()) {
      return true;
    }
    return this.graphFileHeaderInfo.equals(graphFileHeader);
  }

  public String toString() {
    return "OTP " + getVersionString();
  }

  /**
   * Return a version string: {@code version: 2.2.0, ser.ver.id: 7, commit: 2121212.., branch:
   * dev-2.x}
   */
  public String getVersionString() {
    String format = "version: %s, ser.ver.id: %s, commit: %s, branch: %s";
    return String.format(
      format,
      version.version,
      getOtpSerializationVersionId(),
      versionControl.commit,
      versionControl.branch
    );
  }

  /**
   * This method compare the maven project version, an return {@code true} if both are the same. Two
   * different SNAPSHOT versions are considered the same - work in progress.
   */
  public boolean sameVersion(OtpProjectInfo other) {
    return this.version.sameVersion(other.version);
  }

  /**
   * The OTP Serialization version id is used to determine if OTP and a serialized blob(Graph.obj)
   * of the otp internal model are compatible. This filed is writen into the Graph.obj file header
   * and checked when loading the graph later.
   */
  public String getOtpSerializationVersionId() {
    return graphFileHeaderInfo.otpSerializationVersionId();
  }
}
