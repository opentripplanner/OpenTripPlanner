package org.opentripplanner.ext.datastore.aws;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record S3Object(String bucket, String name) {
  static final String S3_SCHEMA_PREFIX = "s3";

  private static final Pattern GS_URL_PATTERN = Pattern.compile(
    "//([\\p{Lower}\\d.-]{3,63})/([^\\p{Cntrl}]+)?"
  );

  public String toUriString() {
    return toUriString(bucket, name);
  }

  public S3Object child(String childName) {
    return new S3Object(bucket, name + '/' + childName);
  }

  static String toUriString(String bucket, String objectName) {
    return S3_SCHEMA_PREFIX + "://" + bucket + "/" + objectName;
  }

  static URI toUri(String bucket, String objectName) {
    try {
      return new URI(toUriString(bucket, objectName));
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e.getLocalizedMessage(), e);
    }
  }

  boolean isRoot() {
    return "".equals(name);
  }

  /**
   * GCS URL pattern for the Scheme Specific Part, without the 'gs:' prefix Not all rules are
   * validated here, but the following is:
   * <ul>
   *     <li>Bucket names must contain only lowercase letters, numbers, dashes (-),
   *     underscores (_), and dots (.)
   *     <li>Bucket names must contain 3 to 63 characters.
   *     <li>Object names must be at least one character.
   *     <li>Object names should avoid using control characters
   *     this is enforced here, and is strictly just a strong recommendation.
   * </ul>
   * One exception is allowed, the object name may be an empty string({@code ""}), this is used to
   * create a virtual root directory.
   */

  static S3Object toS3Object(URI uri) {
    Matcher m = GS_URL_PATTERN.matcher(uri.getSchemeSpecificPart());

    if (m.matches()) {
      return new S3Object(m.group(1), dirName(m.group(2)));
    }
    throw new IllegalArgumentException(
      "The '" +
      uri +
      "' is not a legal Google Cloud Storage " +
      "URL on format: '" +
      S3_SCHEMA_PREFIX +
      "://bucket-name/object-name'."
    );
  }

  /* private methods */

  private static String dirName(String objectDir) {
    return objectDir == null ? "" : objectDir;
  }
}
