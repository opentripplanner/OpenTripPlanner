package org.opentripplanner.model.projectinfo;

import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.OtpConfig;
import org.opentripplanner.standalone.config.RouterConfig;

import java.io.Serializable;

public class OtpProjectInfo implements Serializable {

    private static final long serialVersionUID = 1;

    private static final OtpProjectInfo INSTANCE = OtpProjectInfoParser.loadFromProperties();


    static final String UNKNOWN = "UNKNOWN";

    /* Info derived from version string */
    public final MavenProjectVersion version;

    /* Other info from git-commit-id-plugin via otp-project-info.properties */
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

    public static OtpProjectInfo projectInfo() {
        return INSTANCE;
    }


    OtpProjectInfo() {
        this(
            "0.0.0-ParseFailure",
            new VersionControlInfo(
                UNKNOWN,
                UNKNOWN,
                UNKNOWN,
                UNKNOWN,
                true
            )
        );
    }
    
    public OtpProjectInfo(
            String version,
            VersionControlInfo versionControl
    ) {
        this.version = MavenProjectVersion.parse(version);
        this.versionControl = versionControl;
    }

    public long getUID() {
        return hashCode();
    }

    public String toString() {
        return "OTP " + getVersionString();
    }

    /**
     * Return a version string:
     * {@code version: 2.1.0, commit: 2121212.., branch: dev-2.x}
     */
    public String getVersionString() {
        String format = "version: %s, commit: %s, branch: %s";
        return String.format(format, version.version, versionControl.commit, versionControl.branch);
    }

    /**
     * This method compare the maven project version, an return {@code true} if both are
     * the same. Two different SNAPSHOT versions are considered the same - work in progress.
     */
    public boolean sameVersion(OtpProjectInfo other) {
        return this.version.sameVersion(other.version);
    }
}
