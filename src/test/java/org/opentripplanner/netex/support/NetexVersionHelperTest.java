package org.opentripplanner.netex.support;

import static java.time.Month.MAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.netex.support.NetexVersionHelper.comparingVersion;
import static org.opentripplanner.netex.support.NetexVersionHelper.firstValidDateTime;
import static org.opentripplanner.netex.support.NetexVersionHelper.latestVersionIn;
import static org.opentripplanner.netex.support.NetexVersionHelper.latestVersionedElementIn;
import static org.opentripplanner.netex.support.NetexVersionHelper.versionOf;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.EntityInVersionStructure;
import org.rutebanken.netex.model.ValidBetween;

class NetexVersionHelperTest {

  private static final EntityInVersionStructure E_VER_1 = new EntityInVersionStructure()
    .withVersion("1");
  private static final EntityInVersionStructure E_VER_2 = new EntityInVersionStructure()
    .withVersion("2");
  private static final EntityInVersionStructure E_VER_ANY = new EntityInVersionStructure()
    .withVersion("any");

  @Test
  void versionOfTest() {
    assertEquals(1, versionOf(E_VER_1));
  }

  @Test
  void any() {
    assertEquals(-1, versionOf(E_VER_ANY));
  }

  @Test
  void latestVersionInTest() {
    assertEquals(2, latestVersionIn(Arrays.asList(E_VER_1, E_VER_2)));
    assertEquals(-1, latestVersionIn(Collections.emptyList()));
  }

  @Test
  void lastestVersionedElementInTest() {
    assertEquals(E_VER_2, latestVersionedElementIn(Arrays.asList(E_VER_1, E_VER_2)));
    assertNull(latestVersionedElementIn(Collections.emptyList()));
  }

  @Test
  void comparingVersionTest() {
    // Given a comparator (subject under test)
    Comparator<EntityInVersionStructure> subject = comparingVersion();
    // And a entity with version as the E_VER_1 entity
    EntityInVersionStructure sameVersionAs_E_VER_1 = new EntityInVersionStructure()
      .withVersion("1");

    // Then expect equals versions to return zero
    assertEquals(0, subject.compare(E_VER_1, sameVersionAs_E_VER_1));

    // Then expect lesser version to return less than zero
    assertTrue(subject.compare(E_VER_1, E_VER_2) < 0);

    // Then expect higher version to return more than zero
    assertTrue(subject.compare(E_VER_2, E_VER_1) > 0);
  }

  @Test
  void testFirstRelevantDateTime() {
    var may1st = LocalDateTime.of(2021, MAY, 1, 14, 0);
    var may2nd = LocalDateTime.of(2021, MAY, 2, 14, 0);
    var may3rd = LocalDateTime.of(2021, MAY, 3, 14, 0);
    var may4th = LocalDateTime.of(2021, MAY, 4, 14, 0);

    var pOpenEnded = new ValidBetween();
    var pToMay2nd = new ValidBetween().withToDate(may2nd);
    var pFromMay2nd = new ValidBetween().withFromDate(may2nd);
    var pFrom2ndTo3rd = new ValidBetween().withFromDate(may2nd).withToDate(may3rd);

    // Open ended periods always yield the input timestamp
    assertEquals(may1st, firstValidDateTime(List.of(), may1st));
    assertEquals(may1st, firstValidDateTime(List.of(pOpenEnded), may1st));
    assertEquals(may3rd, firstValidDateTime(List.of(pOpenEnded), may3rd));
    assertEquals(may1st, firstValidDateTime(List.of(pFromMay2nd, pOpenEnded), may1st));

    // Pick the best day for a period in the future
    assertEquals(may2nd, firstValidDateTime(List.of(pFromMay2nd), may1st));
    assertEquals(may2nd, firstValidDateTime(List.of(pFrom2ndTo3rd), may1st));

    // Get correct date-time for a period ending at may 2nd
    assertEquals(may1st, firstValidDateTime(List.of(pToMay2nd), may1st));
    assertEquals(may2nd, firstValidDateTime(List.of(pToMay2nd), may2nd));
    assertNull(firstValidDateTime(List.of(pToMay2nd), may3rd));

    // Get correct date-time for a fixed period
    assertEquals(may2nd, firstValidDateTime(List.of(pFrom2ndTo3rd), may1st));
    assertEquals(may2nd, firstValidDateTime(List.of(pFrom2ndTo3rd), may2nd));
    assertEquals(may3rd, firstValidDateTime(List.of(pFrom2ndTo3rd), may3rd));
    assertNull(firstValidDateTime(List.of(pFrom2ndTo3rd), may4th));
  }
}
