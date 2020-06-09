package org.opentripplanner.annotation;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.util.ConstructorDescriptor;
import org.opentripplanner.util.PackageScanner;

public class ComponentAnnotationConfigurator {

  private static Map<ServiceType, Map<String, ConstructorDescriptor>> componentMaps = new HashMap<>();
  private static ComponentAnnotationConfigurator instance = new ComponentAnnotationConfigurator();
  public static final String COMPONENTS_PACKAGE = "components.packages";

  public static ComponentAnnotationConfigurator getInstance() {
    return instance;
  }

  private ComponentAnnotationConfigurator() {

  }

  public void fromConfig(JsonNode config) {
    Set<String> packages = Sets
        .newHashSet("org.opentripplanner.updater", "org.opentripplanner.routing");
    if (config != null && config.has(COMPONENTS_PACKAGE) && config.path(COMPONENTS_PACKAGE)
        .isArray()) {
      config.path(COMPONENTS_PACKAGE).forEach(node -> packages.add(node.asText()));
    }
    scanPackages(packages);
  }

  public void scanPackages(Collection<String> packages) {
    packages.stream().map(PackageScanner::getClasses).flatMap(Collection::stream)
        .forEach(this::setupRegisteredComponent);
  }

  public ConstructorDescriptor getConstructorDescriptor(String key, ServiceType type) {
    if (componentMaps.containsKey(type) && componentMaps.get(type).containsKey(key)) {
      return componentMaps.get(type).get(key);
    }
    return null;
  }

  private void setupRegisteredComponent(Class<?> clazz) {
    ConstructorDescriptor descriptor = ConstructorDescriptor.getConstructorDescriptor(clazz);
    if (descriptor != null) {
      Map<String, ConstructorDescriptor> classMap;
      if (!componentMaps.containsKey(descriptor.getType())) {
        classMap = new HashMap<>();
      } else {
        classMap = componentMaps.get(descriptor.getType());
      }
      classMap.put(descriptor.getKey(), descriptor);
      componentMaps.put(descriptor.getType(), classMap);
    }
  }
}
