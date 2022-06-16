package org.opentripplanner.netex.mapping.support;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.util.Assert;
import org.rutebanken.netex.model.ValidBetween;

public class ValidityComparatorTest {

  private static final int EXP_EQ = 0;
  private static final int EXP_LT = -1;
  private static final int EXP_GT = 1;

  private final ValidityComparator comparator = new ValidityComparator();

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

    Assert.equals(EXP_EQ, comparator.compare(List.of(validNull), List.of(validNull)));
    Assert.equals(EXP_EQ, comparator.compare(List.of(validNull), List.of(validNow1)));
    Assert.equals(EXP_EQ, comparator.compare(List.of(validNow1), List.of(validNow2)));
    Assert.equals(EXP_LT, comparator.compare(List.of(validNow1), List.of(validPast2)));
    Assert.equals(EXP_LT, comparator.compare(List.of(validNow1), List.of(validFuture2)));
    Assert.equals(EXP_EQ, comparator.compare(List.of(validPast1), List.of(validPast1)));
    Assert.equals(EXP_GT, comparator.compare(List.of(validPast1), List.of(validNow2)));
    Assert.equals(EXP_LT, comparator.compare(List.of(validPast1), List.of(validPast2)));
    Assert.equals(EXP_GT, comparator.compare(List.of(validPast1), List.of(validFuture2)));
    Assert.equals(EXP_EQ, comparator.compare(List.of(validFuture1), List.of(validFuture1)));
    Assert.equals(EXP_GT, comparator.compare(List.of(validFuture1), List.of(validNow2)));
    Assert.equals(EXP_LT, comparator.compare(List.of(validFuture1), List.of(validPast2)));
    Assert.equals(EXP_LT, comparator.compare(List.of(validFuture1), List.of(validFuture2)));
  }
}
