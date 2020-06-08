package org.opentripplanner.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import external.service.ExternalGraphUpdater;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import org.junit.Test;
import org.opentripplanner.annotation.StaticClassComponent.FinalStaticClassComponent;
import org.opentripplanner.routing.impl.NycFareServiceFactory;
import org.opentripplanner.routing.services.FareServiceFactory;
import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.standalone.config.UpdaterConfig;
import org.opentripplanner.updater.GraphUpdater;

public class ComponentAnnotationConfiguratorTest {

  ComponentAnnotationConfigurator configurator = ComponentAnnotationConfigurator.getInstance();
  ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void testNormalGraphUpdaterComponent()
      throws InstantiationException, IllegalAccessException {
    configurator.scanPackages(Collections.singletonList("org.opentripplanner.annotation"));
    GraphUpdater component = configurator
        .getConstructorDescriptor("test.compoent", ServiceType.GraphUpdater).newInstance(null);
    assertNotNull(component);
    assertTrue(TestComponent.class.isAssignableFrom(component.getClass()));
  }

  @Test
  public void testStaticInnerClass() throws InstantiationException, IllegalAccessException {
    configurator.scanPackages(Collections.singletonList("org.opentripplanner.annotation"));
    GraphUpdater component = configurator
        .getConstructorDescriptor("test.staticClassComponent", ServiceType.GraphUpdater)
        .newInstance(null);
    assertNotNull(component);
    assertTrue(StaticClassComponent.class.isAssignableFrom(component.getClass()));
    assertTrue(FinalStaticClassComponent.class.isAssignableFrom(component.getClass()));
  }

  @Test
  public void testFromBuildConfig()
      throws InstantiationException, IllegalAccessException, IOException {
    JsonNode builderConfig = objectMapper
        .readTree(new File("./src/test/resources/", "build-config.json"));
    ComponentAnnotationConfigurator.getInstance().fromConfig(builderConfig);
    GraphUpdater component = configurator
        .getConstructorDescriptor("external.updater", ServiceType.GraphUpdater).newInstance(null);
    assertNotNull(component);
    assertTrue(GraphUpdater.class.isAssignableFrom(component.getClass()));
    assertTrue(ExternalGraphUpdater.class.isAssignableFrom(component.getClass()));

    FareServiceFactory factory = configurator
        .getConstructorDescriptor("new-york", ServiceType.ServiceFactory).newInstance(null);
    assertNotNull(factory);
    assertTrue(NycFareServiceFactory.class.isAssignableFrom(factory.getClass()));
    assertTrue(FareServiceFactory.class.isAssignableFrom(factory.getClass()));
  }

  @Test
  public void testUpdaterConfig() throws IOException {
    configurator.scanPackages(Collections.singletonList("org.opentripplanner.annotation"));

    JsonNode routerConfig = objectMapper
        .readTree(new File("./src/test/resources/", "router-config.json"));
    NodeAdapter adapter = new NodeAdapter(routerConfig,"test");

    UpdaterConfig config = new UpdaterConfig(adapter.path("updaters"));
    assertEquals(1,config.getTypes().size());
    assertEquals("test.compoent",config.getTypes().toArray()[0]);
  }
}
