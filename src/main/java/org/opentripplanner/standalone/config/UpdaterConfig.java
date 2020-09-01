package org.opentripplanner.standalone.config;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.annotation.ComponentAnnotationConfigurator;
import org.opentripplanner.annotation.ServiceType;
import org.opentripplanner.util.ConstructorDescriptor;
import org.opentripplanner.util.OtpAppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

/**
 * This class maps between the JSON array of updaters and the concrete class implementations of each
 * updater parameters. Some updaters use the same parameters, so a map is kept between the JSON
 * updater type strings and the appropriate updater parameter class.
 */
public class UpdaterConfig {

  private static final Logger LOG = LoggerFactory.getLogger(UpdaterConfig.class);

  private final Multimap<String, Object> configList = ArrayListMultimap.create();

  public UpdaterConfig(NodeAdapter updaterConfigList) {
    LOG.info("There are {} updaters", updaterConfigList.asList().size());
    for (NodeAdapter conf : updaterConfigList.asList()) {
      String type = conf.asText("type");
      LOG.info("Creating updater in type '{}'", type);

      ConstructorDescriptor descriptor = ComponentAnnotationConfigurator.getInstance()
          .getConstructorDescriptor(type, ServiceType.GraphUpdater);
      Class<?> paramClazz = descriptor.getInitialClass();
      if (paramClazz != null) {
        try {
          configList.put(type, paramClazz.getConstructor(NodeAdapter.class).newInstance(conf));
        } catch (Exception e) {
          throw new OtpAppException("The updater config type is unknown: " + type);
        }
      }
    }
  }


  public Set<String> getTypes() {
    return configList.keySet();
  }

  public Collection<Object> getParameters(String key) {
    return configList.get(key);
  }
}
