package org.opentripplanner.standalone.server;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.process.internal.RequestScope;
import org.junit.jupiter.api.Test;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.configure.RequestScopedFactory;
import org.opentripplanner.transit.service.TransitService;

class DaggerToJerseyBridgeTest {

  @Test
  void allAccessorsWithinOneRequestShareTheSameFactoryInstance() throws Exception {
    Supplier<RequestScopedFactory> factorySupplier = () -> {
      RequestScopedFactory factory = mock(RequestScopedFactory.class);
      when(factory.transitService()).thenReturn(mock(TransitService.class));
      when(factory.createServerContext()).thenReturn(mock(OtpServerRequestContext.class));
      return factory;
    };

    InjectionManager im = Injections.createInjectionManager();
    im.register(new DaggerToJerseyBridge(factorySupplier));
    im.completeRegistration();

    RequestScope requestScope = im.getInstance(RequestScope.class);

    RequestScopedFactory[] factoryFromRequestA = new RequestScopedFactory[1];
    requestScope.runInScope(() -> {
      RequestScopedFactory factory = im.getInstance(RequestScopedFactory.class);
      factoryFromRequestA[0] = factory;

      TransitService expectedTs = factory.transitService();
      OtpServerRequestContext expectedCtx = factory.createServerContext();

      TransitService ts = im.getInstance(TransitService.class);
      OtpServerRequestContext ctx = im.getInstance(OtpServerRequestContext.class);
      assertSame(expectedTs, ts);
      assertSame(expectedCtx, ctx);

      assertSame(factory, im.getInstance(RequestScopedFactory.class));
    });

    RequestScopedFactory[] factoryFromRequestB = new RequestScopedFactory[1];
    requestScope.runInScope(() -> {
      factoryFromRequestB[0] = im.getInstance(RequestScopedFactory.class);
    });

    assertNotSame(factoryFromRequestA[0], factoryFromRequestB[0]);
  }
}
