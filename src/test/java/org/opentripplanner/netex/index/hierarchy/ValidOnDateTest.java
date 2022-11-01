package org.opentripplanner.netex.index.hierarchy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.Month;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.TariffZone;
import org.rutebanken.netex.model.ValidBetween;

class ValidOnDateTest {

  private final LocalDateTime D1 = LocalDateTime.of(2021, Month.MAY, 1, 12, 0);
  private final LocalDateTime D2 = LocalDateTime.of(2021, Month.MAY, 2, 12, 0);
  private final LocalDateTime D3 = LocalDateTime.of(2021, Month.MAY, 3, 12, 0);
  private final LocalDateTime D4 = LocalDateTime.of(2021, Month.MAY, 4, 12, 0);
  private final LocalDateTime D5 = LocalDateTime.of(2021, Month.MAY, 5, 12, 0);

  private final TariffZone E_NO_PERIODS = entity(1, null, null);
  private final TariffZone E_END_D2 = entity(2, null, D2);
  private final TariffZone E_D3_TO_D4 = entity(3, D3, D4);

  @Test
  void isValid() {
    // Valid
    assertTrue(new ValidOnDate<>(E_NO_PERIODS, D1).isValid());
    assertTrue(new ValidOnDate<>(E_END_D2, D1).isValid());
    assertTrue(new ValidOnDate<>(E_END_D2, D2).isValid());
    assertTrue(new ValidOnDate<>(E_D3_TO_D4, D4).isValid());

    // Not valid
    assertFalse(new ValidOnDate<>(E_END_D2, D3).isValid());
    assertFalse(new ValidOnDate<>(E_D3_TO_D4, D5).isValid());
  }

  @Test
  void bestVersion() {
    var timeLimit = D2;
    var ok = new ValidOnDate<>(E_NO_PERIODS, timeLimit);
    var best = new ValidOnDate<>(E_END_D2, timeLimit);
    var notValid = new ValidOnDate<>(E_D3_TO_D4, timeLimit);

    assertFalse(ok.bestVersion(best));
    assertTrue(best.bestVersion(ok));

    assertTrue(ok.bestVersion(notValid));
    assertFalse(notValid.bestVersion(ok));

    assertTrue(best.bestVersion(notValid));
    assertFalse(notValid.bestVersion(best));
  }

  private TariffZone entity(int version, LocalDateTime from, LocalDateTime to) {
    var e = new TariffZone().withVersion(Integer.toString(version));
    if (from == null && to == null) {
      return e;
    }

    var v = new ValidBetween();
    if (from != null) {
      v.withFromDate(from);
    }
    if (to != null) {
      v.withToDate(to);
    }

    return e.withValidBetween(v);
  }
}
