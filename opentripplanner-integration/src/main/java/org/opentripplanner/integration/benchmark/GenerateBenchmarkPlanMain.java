package org.opentripplanner.integration.benchmark;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Parser;
import org.json.JSONException;
import org.json.JSONObject;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.GenericMutableDao;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

/**
 * Generate a benchmark plan (see {@link RunBenchmarkPlanMain} from a GTFS feed
 * by randomly picking pairs of Stops from the feed and generating from-to trip
 * requests between the pairs. The resulting trip plan file can be fed into
 * {@link RunBenchmarkPlanMain} to run a benchmark of those plans.
 * 
 * @author bdferris
 * 
 */
public class GenerateBenchmarkPlanMain {

  private static final String ARG_TIME = "time";

  private static final String ARG_SAMPLES = "samples";

  private static final String ARG_LAT_NOISE = "latNoise";

  private static final String ARG_LON_NOISE = "lonNoise";

  private static final String ARG_ENVELOPE = "envelope";

  public static void main(String[] args) throws Exception {

    Options options = new Options();
    options.addOption(ARG_TIME, true, "time to plan trip");
    options.addOption(ARG_SAMPLES, true, "number of samples to generate");
    options.addOption(ARG_LAT_NOISE, true, "lat noise");
    options.addOption(ARG_LON_NOISE, true, "lon noise");
    options.addOption(ARG_ENVELOPE, true, "envelope");

    Parser parser = new GnuParser();
    CommandLine cli = parser.parse(options, args);
    args = cli.getArgs();

    if (args.length != 2) {
      System.err.println("usage: input_gtfs output_file");
      System.exit(-1);
    }

    GenerateBenchmarkPlanMain task = new GenerateBenchmarkPlanMain();
    task.setInputPath(new File(args[0]));
    task.setOutputPath(new File(args[1]));

    if (cli.hasOption(ARG_TIME)) {
      Date time = DateLibrary.getIso8601StringAsDate(cli.getOptionValue(ARG_TIME));
      task.setTime(time);
    }

    if (cli.hasOption(ARG_SAMPLES)) {
      int samples = Integer.parseInt(cli.getOptionValue(ARG_SAMPLES));
      task.setSamples(samples);
    }

    if (cli.hasOption(ARG_LAT_NOISE))
      task.setLatNoise(Double.parseDouble(cli.getOptionValue(ARG_LAT_NOISE)));

    if (cli.hasOption(ARG_LON_NOISE))
      task.setLatNoise(Double.parseDouble(cli.getOptionValue(ARG_LON_NOISE)));

    if (cli.hasOption(ARG_ENVELOPE)) {
      String value = cli.getOptionValue(ARG_ENVELOPE);
      String[] tokens = value.split(",");
      if (tokens.length != 4) {
        System.err.println("usage: -envelope latMin,lonMin,latMax,lonMax");
        System.exit(-1);
      }
      Coordinate c1 = new Coordinate(Double.parseDouble(tokens[1]),
          Double.parseDouble(tokens[0]));
      Coordinate c2 = new Coordinate(Double.parseDouble(tokens[3]),
          Double.parseDouble(tokens[2]));
      task.setEnvelope(new Envelope(c1, c2));
    }

    task.run();
  }

  private static Random _random = new Random();

  private File _inputPath;

  private File _outputPath;

  private Date _time = new Date();

  private int _samples = 100;

  private double _latNoise = 0.0002;

  private double _lonNoise = 0.002;

  private Envelope _envelope = null;

  public void setInputPath(File file) {
    _inputPath = file;
  }

  public void setOutputPath(File file) {
    _outputPath = file;
  }

  public void setTime(Date time) {
    _time = time;
  }

  public void setSamples(int samples) {
    _samples = samples;
  }

  public void setLatNoise(double latNoise) {
    _latNoise = latNoise;
  }

  public void setLonNoise(double lonNoise) {
    _lonNoise = lonNoise;
  }

  public void setEnvelope(Envelope envelope) {
    _envelope = envelope;
  }

  public void run() throws IOException, JSONException {

    List<Stop> stops = readStopsFromGtfs();

    String timeAsString = DateLibrary.getTimeAsIso8601String(_time);

    PrintWriter out = new PrintWriter(_outputPath);

    for (int i = 0; i < _samples; i++) {
      JSONObject from = getRandomLocationNearTransitStop(stops);
      JSONObject to = getRandomLocationNearTransitStop(stops);
      JSONObject row = new JSONObject();
      row.put("from", from);
      row.put("to", to);
      row.put("time", timeAsString);
      out.println(row.toString());
    }

    out.close();
  }

  private List<Stop> readStopsFromGtfs() throws IOException {

    GtfsReader reader = new GtfsReader();
    reader.setDefaultAgencyId("agency");
    reader.setInputLocation(_inputPath);

    // We only want to read in stops
    List<Class<?>> classes = reader.getEntityClasses();
    classes.clear();
    classes.add(Stop.class);

    reader.run();

    GenericMutableDao dao = reader.getEntityStore();
    Collection<Stop> stops = dao.getAllEntitiesForType(Stop.class);
    List<Stop> stopsToInclude = new ArrayList<Stop>();

    for (Stop stop : stops) {
      if (_envelope != null) {
        Coordinate c = new Coordinate(stop.getLon(), stop.getLat());
        if (!_envelope.contains(c))
          continue;
      }
      stopsToInclude.add(stop);
    }

    return stopsToInclude;
  }

  private JSONObject getRandomLocationNearTransitStop(List<Stop> stops)
      throws JSONException {

    int index = _random.nextInt(stops.size());
    Stop stop = stops.get(index);

    double lat = stop.getLat() + _random.nextGaussian() * _latNoise;
    double lon = stop.getLon() + _random.nextGaussian() * _lonNoise;

    JSONObject obj = new JSONObject();
    obj.put("lat", lat);
    obj.put("lon", lon);
    return obj;
  }
}
