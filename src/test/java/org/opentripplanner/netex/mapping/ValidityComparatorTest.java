package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.locationtech.jts.util.Assert;
import org.opentripplanner.netex.support.ValidityComparator;
import org.rutebanken.netex.model.ValidBetween;

import java.time.LocalDateTime;
import java.util.Collections;

public class ValidityComparatorTest {
    private ValidityComparator validityComparator = new ValidityComparator();

    @Test
    public void testValidityComparator() {
        ValidBetween validNull = new ValidBetween();

        ValidBetween validNow1 = new ValidBetween();
        validNow1.setFromDate(LocalDateTime.now().minusDays(3));
        validNow1.setToDate(LocalDateTime.now().plusDays(3));

        ValidBetween validPast1 = new ValidBetween();
        validPast1.setFromDate(LocalDateTime.now().minusDays(7));
        validPast1.setToDate(LocalDateTime.now().minusDays(5));

        ValidBetween validFuture1 = new ValidBetween();
        validFuture1.setFromDate(LocalDateTime.now().plusDays(5));
        validFuture1.setToDate(LocalDateTime.now().plusDays(7));

        ValidBetween validNow2 = new ValidBetween();
        validNow2.setFromDate(LocalDateTime.now().minusDays(4));
        validNow2.setToDate(LocalDateTime.now().plusDays(4));

        ValidBetween validPast2 = new ValidBetween();
        validPast2.setFromDate(LocalDateTime.now().minusDays(8));
        validPast2.setToDate(LocalDateTime.now().minusDays(6));

        ValidBetween validFuture2 = new ValidBetween();
        validFuture2.setFromDate(LocalDateTime.now().plusDays(6));
        validFuture2.setToDate(LocalDateTime.now().plusDays(8));

        Assert.equals(0, validityComparator.compare(
                Collections.singletonList(validNull), Collections.singletonList(validNull)));
        Assert.equals(0, validityComparator.compare(
                Collections.singletonList(validNull), Collections.singletonList(validNow1)));
        Assert.equals(0, validityComparator.compare(
                Collections.singletonList(validNow1), Collections.singletonList(validNow2)));
        Assert.equals(-1, validityComparator.compare(
                Collections.singletonList(validNow1), Collections.singletonList(validPast2)));
        Assert.equals(-1, validityComparator.compare(
                Collections.singletonList(validNow1), Collections.singletonList(validFuture2)));
        Assert.equals(0, validityComparator.compare(
                Collections.singletonList(validPast1), Collections.singletonList(validPast1)));
        Assert.equals(1, validityComparator.compare(
                Collections.singletonList(validPast1), Collections.singletonList(validNow2)));
        Assert.equals(-1, validityComparator.compare(
                Collections.singletonList(validPast1), Collections.singletonList(validPast2)));
        Assert.equals(1, validityComparator.compare(
                Collections.singletonList(validPast1), Collections.singletonList(validFuture2)));
        Assert.equals(0, validityComparator.compare(
                Collections.singletonList(validFuture1), Collections.singletonList(validFuture1)));
        Assert.equals(1, validityComparator.compare(
                Collections.singletonList(validFuture1), Collections.singletonList(validNow2)));
        Assert.equals(-1, validityComparator.compare(
                Collections.singletonList(validFuture1), Collections.singletonList(validPast2)));
        Assert.equals(-1, validityComparator.compare(
                Collections.singletonList(validFuture1), Collections.singletonList(validFuture2)));
    }
}
