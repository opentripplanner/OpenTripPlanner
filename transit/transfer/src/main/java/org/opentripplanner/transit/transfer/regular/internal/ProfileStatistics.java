package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Locale;
import javax.annotation.Nullable;

/**
 * The size of one profile after the regular transfers are generated.
 *
 * @param id                the id of the profile
 * @param preferences       the user preferences of the profile, for the log
 * @param parentId          the id of the parent of the profile, {@code null} if it has none
 * @param transfers         the number of transfers of the profile, also the ones that use a path of
 *                          the parent
 * @param sharedWithParent  the number of transfers that use the path of the parent, so the path is
 *                          stored once for both profiles
 */
record ProfileStatistics(
  String id,
  String preferences,
  @Nullable String parentId,
  int transfers,
  long sharedWithParent
) {
  /** The paths that only this profile stores. */
  long streetPaths() {
    return transfers - sharedWithParent;
  }

  String message() {
    var message = "Profile '%s' paths: %s".formatted(id, format(transfers));
    if (parentId != null) {
      long percentage = transfers == 0 ? 0 : Math.round((100.0 * sharedWithParent) / transfers);
      message += ", %s (%d%%) reused '%s'".formatted(
        format(sharedWithParent),
        percentage,
        parentId
      );
    }
    return message + " - " + preferences + ".";
  }

  /** Format a number with a space between each group of three digits, for example 1 234 567. */
  static String format(long number) {
    return String.format(Locale.ROOT, "%,d", number).replace(',', ' ');
  }
}
