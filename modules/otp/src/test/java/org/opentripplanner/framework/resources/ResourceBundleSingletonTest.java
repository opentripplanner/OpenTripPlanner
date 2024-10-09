package org.opentripplanner.framework.resources;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.opentripplanner.api.common.Message;

class ResourceBundleSingletonTest {

  @Test
  void localize() {
    var msg = Message.SYSTEM_ERROR.get(Locale.GERMAN);
    assertEquals(
      "Es tut uns leid, leider steht der Trip-Planer momentan nicht zur Verfügung. Bitte versuchen Sie es zu einem späteren Zeitpunkt nochmal.",
      msg
    );
  }
}
