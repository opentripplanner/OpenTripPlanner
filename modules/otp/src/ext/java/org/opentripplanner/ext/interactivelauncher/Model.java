package org.opentripplanner.ext.interactivelauncher;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import org.opentripplanner.ext.interactivelauncher.debug.logging.LogModel;
import org.opentripplanner.ext.interactivelauncher.debug.raptor.RaptorDebugModel;
import org.opentripplanner.ext.interactivelauncher.startup.StartupModel;

public class Model implements Serializable {

  private static final File MODEL_FILE = new File("interactive_otp_main.json");

  private StartupModel startupModel;
  private LogModel logModel;
  private RaptorDebugModel raptorDebugModel;

  public Model() {}

  public static Model load() {
    return MODEL_FILE.exists() ? readFromFile() : createNew();
  }

  public StartupModel getStartupModel() {
    return startupModel;
  }

  public LogModel getLogModel() {
    return logModel;
  }

  public RaptorDebugModel getRaptorDebugModel() {
    return raptorDebugModel;
  }

  private static Model createNew() {
    return new Model().initSubModels();
  }

  private static Model readFromFile() {
    try {
      var mapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      return mapper.readValue(MODEL_FILE, Model.class).initSubModels();
    } catch (IOException e) {
      System.err.println(
        "Unable to read the InteractiveOtpMain state cache. If the model changed this " +
        "is expected, and it will work next time. Cause: " +
        e.getMessage()
      );
      return createNew();
    }
  }

  void save() {
    try {
      var mapper = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);
      mapper.writeValue(MODEL_FILE, this);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private Model initSubModels() {
    if (startupModel == null) {
      startupModel = new StartupModel();
    }
    if (logModel == null) {
      logModel = LogModel.createFromConfig();
    }
    if (raptorDebugModel == null) {
      raptorDebugModel = new RaptorDebugModel();
    }
    logModel.init(this::save);
    raptorDebugModel.init(this::save);
    return this;
  }
}
