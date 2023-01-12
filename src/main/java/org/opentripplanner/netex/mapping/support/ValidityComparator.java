package org.opentripplanner.netex.mapping.support;

import jakarta.ws.rs.NotSupportedException;
import java.util.Collection;
import java.util.Comparator;
import org.rutebanken.netex.model.ValidBetween;

/**
 * This comparator is used for choosing the StopPlace with the most relevant validity period when
 * there are multiple versions of the same StopPlace. This way of sorting is specific to how
 * versioning is handled in the <a href="https://github.com/entur/tiamat">Entur stop place
 * register</a>. StopPlaces are chosen according to the current import date, even though the user
 * can search for trips in the past or future when other StopPlaces might be valid. This is a
 * simplification chosen both to avoid problems with older data sets and because OTP does not
 * support validity information on stops.
 * <p>
 * Stop places are compared according to the following criteria in order: 1. Valid now (or no
 * validity information) 2. Future valid period with earliest start date 3. Past valid period with
 * latest end date
 */
class ValidityComparator implements Comparator<Collection<ValidBetween>> {

  @Override
  public int compare(Collection<ValidBetween> v1List, Collection<ValidBetween> v2List) {
    if (v1List.size() > 1 || v2List.size() > 1) {
      throw new NotSupportedException("More than one validity period not supported");
    }

    ValidBetween v1 = v1List.stream().findFirst().orElse(null);
    ValidBetween v2 = v2List.stream().findFirst().orElse(null);

    // Check NOW
    {
      boolean validNow1 = ValidityHelper.isValidNow(v1);
      boolean validNow2 = ValidityHelper.isValidNow(v2);

      if (validNow1 || validNow2) {
        if (validNow1 && validNow2) {
          return 0;
        }
        return validNow1 ? -1 : 1;
      }
    }

    // Check FUTURE
    {
      boolean validFuture1 = ValidityHelper.isValidFuture(v1);
      boolean validFuture2 = ValidityHelper.isValidFuture(v2);

      if (validFuture1 || validFuture2) {
        if (validFuture1 && validFuture2) {
          //noinspection ConstantConditions
          return v1.getFromDate().compareTo(v2.getFromDate());
        }
        return validFuture1 ? -1 : 1;
      }
    }

    // Check PAST
    {
      boolean validPast1 = ValidityHelper.isValidPast(v1);
      boolean validPast2 = ValidityHelper.isValidPast(v2);

      if (validPast1 || validPast2) {
        if (validPast1 && validPast2) {
          //noinspection ConstantConditions
          return v2.getToDate().compareTo(v1.getToDate());
        }
        return validPast1 ? -1 : 1;
      }
    }
    return 0;
  }
}
