package org.opentripplanner.model.projectinfo;

import java.io.Serializable;
import java.util.Objects;

/**
 * This class encapsulate a simplified version of  Mave version number. It has logic to parse any
 * version string that follow the Maven standard.
 */
public class MavenProjectVersion implements Serializable {

  /** The maven version string "as is" */
  public final String version;

  public final int major;

  public final int minor;

  public final int patch;

  /**
   * The qualifier, snapshot or build number part of the version. This is what is after the first
   * '-'. Maven distinguish between qualifier, snapshots or build-number to be able to sort versions
   * in the correct order, but we do not need to 'sort', hence the simplification treating all 3 of
   * these as a 'qualifier'.
   */
  public final String qualifier;

  private MavenProjectVersion(String version, int major, int minor, int patch, String qualifier) {
    this.version = version;
    this.major = major;
    this.minor = minor;
    this.patch = patch;
    this.qualifier = qualifier;
  }

  /** @return "major.minor.patch". The SNAPSHOT `qualifier` is removed. */
  public String unqualifiedVersion() {
    return String.format("%d.%d.%d", major, minor, patch);
  }

  /**
   * Compare version and check if they are the same. Note the version might be two different
   * SNAPSHOTs of the same version - here considered equals.
   */
  public boolean sameVersion(MavenProjectVersion other) {
    return version.equals(other.version);
  }

  @Override
  public String toString() {
    return version;
  }

  static MavenProjectVersion parse(String version) {
    Objects.requireNonNull(version);
    int major = 0;
    int minor = 0;
    int patch = 0;
    String qualifier = "";
    String[] numFields;

    int pos = version.indexOf('-');

    if (pos > 0) {
      if (pos < version.length()) {
        qualifier = version.substring(pos + 1);
      }
      numFields = splitInotNumFields(version.substring(0, pos));
    } else {
      numFields = splitInotNumFields(version);
    }

    if (numFields.length > 0) {
      major = parseNumField(numFields[0]);
    }
    if (numFields.length > 1) {
      minor = parseNumField(numFields[1]);
    }
    if (numFields.length > 2) {
      patch = parseNumField(numFields[2]);
    }

    return new MavenProjectVersion(version, major, minor, patch, qualifier);
  }

  private static String[] splitInotNumFields(String v) {
    return v.split("\\.");
  }

  /** Parse numeric field, if not numeric return 0 */
  private static int parseNumField(String field) {
    try {
      return Integer.parseInt(field);
    } catch (NumberFormatException ignore) {
      return 0;
    }
  }
}
