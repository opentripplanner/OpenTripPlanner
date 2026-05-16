package org.opentripplanner.ext.interactivelauncher;

import java.io.File;
import java.io.Serializable;
import org.opentripplanner.ext.interactivelauncher.debug.logging.LogModel;
import org.opentripplanner.ext.interactivelauncher.debug.raptor.RaptorDebugModel;
import org.opentripplanner.ext.interactivelauncher.startup.StartupModel;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

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
      var mapper = new JsonMapper();
      return mapper.readValue(MODEL_FILE, Model.class).initSubModels();
    } catch (JacksonException e) {
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
      var mapper = JsonMapper.builder()
        .configure(SerializationFeature.INDENT_OUTPUT, true)
        // TODO writeValue was removed from JsonMapper in Jackson 3.
        .writeValue(MODEL_FILE, this)
        .build();
    } catch (JacksonException e) {
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
