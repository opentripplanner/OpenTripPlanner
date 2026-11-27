package org.opentripplanner.standalone.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.actuator.ActuatorAPI;
import org.opentripplanner.standalone.config.RouterConfig;

class OTPWebApplicationTest {

  private static final OTPWebApplicationParameters PARAMETERS = RouterConfig.DEFAULT.server();

  @Test
  void additionalComponentsAreRegistered() {
    var singleton = new Object();
    var subject = new OTPWebApplication(
      PARAMETERS,
      () -> null,
      new JerseyComponents(Set.of(TestResource.class), Set.of(singleton))
    );

    assertTrue(subject.getClasses().contains(TestResource.class));
    assertTrue(subject.getSingletons().contains(singleton));
  }

  @Test
  void actuatorAPIIsNotRegisteredByDefault() {
    var subject = new OTPWebApplication(PARAMETERS, () -> null, JerseyComponents.EMPTY);

    assertFalse(subject.getClasses().contains(ActuatorAPI.class));
  }

  @Path("/test")
  public static class TestResource {

    @GET
    public String get() {
      return "test";
    }
  }
}
