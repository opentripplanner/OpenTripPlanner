package org.opentripplanner.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Properties;

public class ProjectInfo implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectInfo.class);
    public static final ProjectInfo INSTANCE = fromProperties();
    private static final long serialVersionUID = INSTANCE.getUID();
    private static final String UNKNOWN = "UNKNOWN";

    /* Info derived from version string */
    public final String version; 
    public final int major;
    public final int minor;
    public final int patch;
    public final String qualifier;
    
    /* Other info from git-commit-id-plugin via maven-version.properties */
    public final String commit;
    public final String branch;
    public final String describe;
    public final String commitTime;
    public final String buildTime;
    public final boolean dirty;

    private static ProjectInfo fromProperties() {
        final String FILE = "maven-version.properties";
        try {
            Properties props = new java.util.Properties();
            InputStream in = ProjectInfo.class.getClassLoader().getResourceAsStream(FILE);
            props.load(in);
            ProjectInfo version = new ProjectInfo(
                    props.getProperty("project.version"),
                    props.getProperty("git.commit.id"),
                    props.getProperty("git.commit.id.describe"),
                    props.getProperty("git.commit.time"),
                    props.getProperty("git.branch"),
                    props.getProperty("git.build.time"),
                    props.getProperty("git.dirty")
            );
            LOG.debug("Parsed Maven artifact version: {}", version.toStringVerbose());
            return version;
        } catch (Exception e) {
            LOG.error("Error reading version from properties file: {}", e.getMessage());
            return new ProjectInfo();
        }
    }
    
    private ProjectInfo() {
        // JAXB Marshalling requires classes to have a 0-arg constructor and mutable fields.
        // otherwise it throws a com.sun.xml.bind.v2.runtime.IllegalAnnotationsException.
        // It is protecting you against yourself, since you might someday want to
        // unmarshal that same object in Java.
        // The 'proper' way of handling this is to make a mutable equivalent of your class,
        // plus another adapter class that converts between the mutable and immutable 
        // versions. It would be absurd to produce so much boilerplate and verbosity in this
        // situation, so I am providing a 0-arg constructor with a totally different role: 
        // generating a default version when OTP encounters a problem parsing the
        // maven-version.properties file.
        this("0.0.0-ParseFailure", UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN);
    }
    
    public ProjectInfo(
            String version,
            String commit,
            String describe,
            String commitTime,
            String branch,
            String buildTime,
            String dirty
    ) {
        this.version = version;
        String [] fields = version.split("\\-");
        if (fields.length > 1)
            qualifier = fields[1];
        else
            qualifier = "";
        fields = fields[0].split("\\.");
        if (fields.length > 0)
            major = Integer.parseInt(fields[0]);
        else
            major = 0;
        if (fields.length > 1)
            minor = Integer.parseInt(fields[1]);
        else
            minor = 0;
        if (fields.length > 2)
            patch = Integer.parseInt(fields[2]);
        else
            patch = 0;
        this.commit = normalize(commit);
        this.describe = normalize(describe);
        this.commitTime = normalize(commitTime);
        this.branch = normalize(branch);
        this.buildTime = normalize(buildTime);
        this.dirty = "true".equalsIgnoreCase(dirty);
    }

    public long getUID() {
        return hashCode();
    }

    public String toString() {
        return String.format("MavenVersion(%d, %d, %d, %s, %s)", 
               major, minor, patch, qualifier, commit);
    }

    public String toStringVerbose() {
        return String.format("%s => %s UID=%d", version, this.toString(), getUID());
    }

    public String getShortVersionString() {
        return "OpenTripPlanner " + version + " " + commit;
    }

    public String getLongVersionString() {
        String format = "version: %s, commit: %s, branch: %s";
        return String.format(format, version, commit, branch);
    }

    /** @return "major.minor.patch". The SNAPSHOT `qualifier` is removed. */
    public String unqualifiedVersion() {
        return String.format("%d.%d.%d", major, minor, patch);
    }

    public int hashCode () {
        return (qualifier.equals("SNAPSHOT") ? -1 : +1) *
                (1000000 * major + 1000 * minor + patch);
    }

    public boolean equals (Object other) {
        if ( ! (other instanceof ProjectInfo))
            return false;
        ProjectInfo that = (ProjectInfo) other;
        return this.major == that.major &&
               this.minor == that.minor &&
               this.patch == that.patch &&
               this.qualifier.equals(that.qualifier);
    }

    private static String normalize(String text) {
        if(text == null || text.isBlank()) { return UNKNOWN; }
        if(text.startsWith("${") && text.endsWith("}")) { return UNKNOWN; }
        return text;
    }
}
