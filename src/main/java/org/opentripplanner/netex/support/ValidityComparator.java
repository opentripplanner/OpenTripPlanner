package org.opentripplanner.netex.support;

import org.rutebanken.netex.model.ValidBetween;

import javax.ws.rs.NotSupportedException;
import java.util.Collection;
import java.util.Comparator;

/**
 * This comparator is used for choosing the StopPlace with the most relevant validity period when there are
 * multiple versions of the same StopPlace. This way of sorting is specific to how versioning is handled in the
 * <a href="https://github.com/entur/tiamat">Entur stop place register</a>. StopPlaces are chosen according to the
 * current import date, even though the user can search for trips in the past or future when other StopPlaces might be
 * valid. This is a simplification chosen both to avoid problems with older data sets and because OTP does not support
 * validity information on stops.
 *
 * Stop places are compared according to the following criteria in order:
 * 1. Valid now (or no validity information)
 * 2. Future valid period with earliest start date
 * 3. Past valid period with latest end date
 */
public class ValidityComparator implements Comparator<Collection<ValidBetween>> {

    @Override
    public int compare(Collection<ValidBetween> v1collection, Collection<ValidBetween> v2collection) {
        if (v1collection.size() > 1 || v2collection.size() > 1) {
            throw new NotSupportedException("More than one validity period not supported");
        }

        ValidBetween v1 = v1collection.stream().findFirst().orElse(null);
        ValidBetween v2 = v2collection.stream().findFirst().orElse(null);

        Boolean validNow1 = ValidityHelper.isValidNow(v1);
        Boolean validNow2 = ValidityHelper.isValidNow(v2);
        if (validNow1 && !validNow2) return -1;
        if (validNow2 && !validNow1) return 1;
        if (validNow1 && validNow2) return 0;

        Boolean validFuture1 = ValidityHelper.isValidFuture(v1);
        Boolean validFuture2 = ValidityHelper.isValidFuture(v2);
        if (validFuture1 && !validFuture2) return -1;
        if (validFuture2 && !validFuture1) return 1;
        if (validFuture1 && validFuture2) {
            return v1.getFromDate().compareTo(v2.getFromDate());
        }

        Boolean validPast1 = ValidityHelper.isValidPast(v1);
        Boolean validPast2 = ValidityHelper.isValidPast(v2);
        if (validPast1 && !validPast2) return -1;
        if (validPast2 && !validPast1) return 1;
        if (validPast1 && validPast2) {
            return v2.getToDate().compareTo(v1.getToDate());
        }

        return 0;
    }
}