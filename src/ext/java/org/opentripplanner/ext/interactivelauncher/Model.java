package org.opentripplanner.ext.interactivelauncher;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import org.opentripplanner.ext.interactivelauncher.logging.LogModel;
import org.opentripplanner.ext.interactivelauncher.startup.StartupModel;

public class Model implements Serializable {

  private static final File MODEL_FILE = new File("interactive_otp_main.json");

  private StartupModel startupModel;
  private LogModel logModel;

  public Model() {}

  public static Model load() {
    var model = MODEL_FILE.exists() ? readFromFile() : createNew();
    // Setup callbacks
    model.logModel.init(model::save);

    return model;
  }

  public StartupModel getStartupModel() {
    return startupModel;
  }

  public LogModel getLogModel() {
    return logModel;
  }

  public void save() {
    try {
      new ObjectMapper().writeValue(MODEL_FILE, this);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private static Model createNew() {
    var model = new Model();
    model.logModel = new LogModel();
    model.logModel.initFromConfig();
    model.setupCallbacks();
    return model;
  }

  private static Model readFromFile() {
    try {
      var model = new ObjectMapper().readValue(MODEL_FILE, Model.class);
      model.setupCallbacks();
      return model;
    } catch (IOException e) {
      System.err.println(
        "Unable to read the InteractiveOtpMain state cache. If the model changed this " +
        "is expected, and it will work next time. Cause: " +
        e.getMessage()
      );
      return createNew();
    }
  }

  private void setupCallbacks() {
    logModel.init(this::save);
  }
}
