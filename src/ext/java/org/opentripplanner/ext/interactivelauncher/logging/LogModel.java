package org.opentripplanner.ext.interactivelauncher.logging;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Responsible for storing the selected loggers to debug. This is
 * serialized to store the user preferences between runs.
 */
public class LogModel implements Serializable {

  private final Set<String> activeLoggers = new HashSet<>();

  @JsonIgnore
  private Runnable saveCallback;

  public LogModel() {}

  /** Need to set this manually to support JSON serialization. */
  public void init(Runnable saveCallback) {
    this.saveCallback = saveCallback;
  }

  /** Needed to do JSON serialization. */
  public Collection<String> getActiveLoggers() {
    return List.copyOf(activeLoggers);
  }

  /** Needed to do JSON serialization. */
  public void setActiveLoggers(Collection<String> loggers) {
    this.activeLoggers.clear();
    this.activeLoggers.addAll(loggers);
  }

  public void initFromConfig() {
    activeLoggers.addAll(DebugLoggingSupport.getDebugLoggers());
  }

  boolean isLoggerEnabled(String name) {
    return activeLoggers.contains(name);
  }

  void turnLoggerOnOff(String name, boolean enable) {
    if (enable) {
      activeLoggers.add(name);
    } else {
      activeLoggers.remove(name);
    }
    DebugLoggingSupport.configureDebugLogging(name, enable);
    saveCallback.run();
  }
}
