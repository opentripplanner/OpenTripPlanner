package org.opentripplanner.ext.datastore.gs;

import com.google.cloud.storage.BlobId;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class help with mapping from an URI to String and  GCS {@link BlobId}.
 */
class GsHelper {

  /**
   * GCS URL pattern for the Scheme Specific Part, without the 'gs:' prefix Not all rules are
   * validated here, but the following is:
   * <ul>
   *     <li>Bucket names must contain only lowercase letters, numbers, dashes (-),
   *     underscores (_), and dots (.)
   *     <li>Bucket names must contain 3 to 222 characters.
   *     <li>Object names must be at least one character.
   *     <li>Object names should avoid using control characters
   *     this is enforced here, and is strictly just a strong recommendation.
   * </ul>
   * One exception is allowed, the object name may be an empty string({@code ""}), this is used to
   * create a virtual root directory.
   */
  private static final Pattern GS_URL_PATTERN = Pattern.compile(
    "//([\\p{Lower}\\d_.-]{3,222})/([^\\p{Cntrl}]+)?"
  );

  /** This is a utility class with static methods only; hence this constructor is private */
  private GsHelper() {}

  static BlobId toBlobId(URI uri) {
    Matcher m = GS_URL_PATTERN.matcher(uri.getSchemeSpecificPart());

    if (m.matches()) {
      return BlobId.of(m.group(1), dirName(m.group(2)));
    }
    throw new IllegalArgumentException(
      "The '" +
      uri +
      "' is not a legal Google Cloud Storage " +
      "URL on format: 'gs://bucket-name/object-name'."
    );
  }

  static String toUriString(BlobId blobId) {
    return toUriString(blobId.getBucket(), blobId.getName());
  }

  static String toUriString(String bucket, String objectName) {
    return "gs://" + bucket + "/" + objectName;
  }

  static URI toUri(String bucket, String objectName) {
    try {
      return new URI(toUriString(bucket, objectName));
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e.getLocalizedMessage(), e);
    }
  }

  static boolean isRoot(BlobId blobId) {
    return "".equals(blobId.getName());
  }

  /* private methods */

  private static String dirName(String objectDir) {
    return objectDir == null ? "" : objectDir;
  }
}
