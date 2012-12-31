/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

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
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.GenericMutableDao;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.opentripplanner.routing.graph.Vertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

/**
 * Generate a benchmark plan (see {@link RunBenchmarkPlanMain} from a Graph randomly picking pairs of points near intersections and generating from-to
 * trip requests between the pairs. The resulting trip plan file can be fed into {@link RunBenchmarkPlanMain} to run a benchmark of those plans.
 * 
 * @author flamholz
 */
public class GenerateGraphBenchmarkPlanMain {

    private static final String ARG_TIME = "time";

    private static final String ARG_SAMPLES = "samples";

    private static final String ARG_LAT_NOISE = "latNoise";

    private static final String ARG_LON_NOISE = "lonNoise";

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        options.addOption(ARG_TIME, true, "time to plan trip");
        options.addOption(ARG_SAMPLES, true, "number of samples to generate");
        options.addOption(ARG_LAT_NOISE, true, "lat noise");
        options.addOption(ARG_LON_NOISE, true, "lon noise");

        Parser parser = new GnuParser();
        CommandLine cli = parser.parse(options, args);
        args = cli.getArgs();

        if (args.length != 2) {
            System.err.println("usage: input_graph output_file");
            System.exit(-1);
        }

        GenerateGraphBenchmarkPlanMain task = new GenerateGraphBenchmarkPlanMain();
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

        if (cli.hasOption(ARG_LAT_NOISE)) {
            task.setLatNoise(Double.parseDouble(cli.getOptionValue(ARG_LAT_NOISE)));
        }

        if (cli.hasOption(ARG_LON_NOISE)) {
            task.setLatNoise(Double.parseDouble(cli.getOptionValue(ARG_LON_NOISE)));
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

    ObjectMapper mapper = new ObjectMapper();

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

    public void run() throws IOException, ClassNotFoundException {

        Graph g = readGraphFromFile();
        List<Vertex> vertices = new ArrayList<Vertex>(g.getVertices());

        String timeAsString = DateLibrary.getTimeAsIso8601String(_time);

        PrintWriter out = new PrintWriter(_outputPath);

        for (int i = 0; i < _samples; i++) {
            ObjectNode from = getRandomLocationNearVertex(vertices);
            ObjectNode to = getRandomLocationNearVertex(vertices);
            ObjectNode row = mapper.createObjectNode();
            row.put("from", from);
            row.put("to", to);
            row.put("time", timeAsString);
            out.println(row.toString());
        }

        out.close();
    }

    private Graph readGraphFromFile() throws IOException, ClassNotFoundException {
        Graph g = Graph.load(_inputPath, LoadLevel.FULL);
        return g;
    }

    private ObjectNode getRandomLocationNearVertex(List<Vertex> vertices) {

        int index = _random.nextInt(vertices.size());
        Vertex v = vertices.get(index);

        double lat = v.getY() + _random.nextGaussian() * _latNoise;
        double lon = v.getX() + _random.nextGaussian() * _lonNoise;

        ObjectNode obj = mapper.createObjectNode();
        obj.put("lat", lat);
        obj.put("lon", lon);
        return obj;
    }
}
