package org.opentripplanner.transit.speed_test;

import java.io.File;
import java.net.URI;
import javax.annotation.Nullable;
import org.opentripplanner.datastore.OtpDataStore;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.SerializedGraphObject;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.standalone.config.OtpConfigLoader;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.speed_test.options.SpeedTestCmdLineOpts;

/**
 * A package-private helper class for setting up speed tests.
 */
class SetupHelper {

  static LoadModel loadGraph(File baseDir, @Nullable URI path) {
    File file = path == null
      ? OtpDataStore.graphFile(baseDir)
      : path.isAbsolute() ? new File(path) : new File(baseDir, path.getPath());
    SerializedGraphObject serializedGraphObject = SerializedGraphObject.load(file);
    Graph graph = serializedGraphObject.graph;

    if (graph == null) {
      throw new IllegalStateException(
        "Could not find graph at %s".formatted(file.getAbsolutePath())
      );
    }

    TimetableRepository timetableRepository = serializedGraphObject.timetableRepository;
    timetableRepository.index();
    graph.index();
    return new LoadModel(graph, timetableRepository, serializedGraphObject.buildConfig);
  }

  static void loadOtpFeatures(SpeedTestCmdLineOpts opts) {
    ConfigModel.initializeOtpFeatures(new OtpConfigLoader(opts.rootDir()).loadOtpConfig());
  }
}
