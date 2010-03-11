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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Parser;
import org.json.JSONException;
import org.json.JSONObject;
import org.opentripplanner.model.GraphBundle;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.impl.SerializedGraphFactoryBean;
import org.opentripplanner.routing.services.PathService;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;

public class RunBenchmarkPlanMain {

  private static final String ARG_REPETITIONS = "repetitions";

  public static void main(String[] args) throws Exception {

    Options options = new Options();
    options.addOption(ARG_REPETITIONS, true, "repetitions");

    Parser parser = new GnuParser();
    CommandLine cli = parser.parse(options, args);
    args = cli.getArgs();

    if (args.length != 2) {
      System.err.println("usage: graph_path benchmark_path");
      System.exit(-1);
    }

    RunBenchmarkPlanMain task = new RunBenchmarkPlanMain();
    task.setGraphPath(new File(args[0]));
    task.setBenchmarkPlan(new File(args[1]));

    if (cli.hasOption(ARG_REPETITIONS))
      task.setRepetitions(Integer.parseInt(cli.getOptionValue(ARG_REPETITIONS)));

    task.run();
  }

  private File _graphPath;

  private File _benchmarkPlan;

  private int _repetitions = 1;

  public void setGraphPath(File path) {
    _graphPath = path;
  }

  public void setBenchmarkPlan(File benchmarkPlan) {
    _benchmarkPlan = benchmarkPlan;
  }

  public void setRepetitions(int repetitions) {
    _repetitions = repetitions;
  }

  public void run() throws Exception {
    GenericApplicationContext context = getApplicationContext();
    PathService service = (PathService) context.getBean("pathServiceImpl");

    List<Plan> plans = readPlans();

    long tTotal = 0;
    for (int i = 0; i < _repetitions; i++) {
      for (Plan plan : plans) {

        String from = plan.latFrom + " " + plan.lonFrom;
        String to = plan.latTo + " " + plan.lonTo;

        try {
          long t0 = System.currentTimeMillis();
          service.plan(from, to, plan.time, new TraverseOptions());
          long t1 = System.currentTimeMillis();

          System.out.println("t=" + (t1 - t0));
          tTotal += t1 - t0;
        } catch (VertexNotFoundException ex) {
          System.out.println("no vertex: from=" + from + " to=" + to);
        }
      }
    }
    System.out.println("totalTime=" + tTotal);
  }

  private List<Plan> readPlans() throws FileNotFoundException, IOException,
      JSONException, ParseException {
    BufferedReader reader = new BufferedReader(new FileReader(_benchmarkPlan));
    String line = null;
    List<Plan> plans = new ArrayList<Plan>();

    while ((line = reader.readLine()) != null) {
      JSONObject obj = new JSONObject(line);
      plans.add(getJsonObjectAsPlan(obj));
    }
    return plans;
  }

  private GenericApplicationContext getApplicationContext() {

    GenericApplicationContext ctx = new GenericApplicationContext();
    XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
    xmlReader.loadBeanDefinitions(new ClassPathResource(
        "org/opentripplanner/application-context.xml"));

    Map<String, BeanDefinition> additionalBeans = getAdditionalBeans();
    for (Map.Entry<String, BeanDefinition> entry : additionalBeans.entrySet())
      ctx.registerBeanDefinition(entry.getKey(), entry.getValue());

    ctx.refresh();
    ctx.registerShutdownHook();
    return ctx;
  }

  private Map<String, BeanDefinition> getAdditionalBeans() {

    Map<String, BeanDefinition> additionalBeans = new HashMap<String, BeanDefinition>();

    BeanDefinitionBuilder bundlePath = BeanDefinitionBuilder.genericBeanDefinition(GraphBundle.class);
    bundlePath.addPropertyValue("path", _graphPath);
    additionalBeans.put("graphBundle", bundlePath.getBeanDefinition());

    BeanDefinitionBuilder graph = BeanDefinitionBuilder.genericBeanDefinition(SerializedGraphFactoryBean.class);
    graph.addPropertyReference("graphBundle", "graphBundle");
    additionalBeans.put("graph", graph.getBeanDefinition());

    return additionalBeans;
  }

  private Plan getJsonObjectAsPlan(JSONObject obj) throws JSONException,
      ParseException {

    Plan plan = new Plan();

    JSONObject from = obj.getJSONObject("from");
    plan.latFrom = from.getDouble("lat");
    plan.lonFrom = from.getDouble("lon");

    JSONObject to = obj.getJSONObject("to");
    plan.latTo = to.getDouble("lat");
    plan.lonTo = to.getDouble("lon");

    plan.time = DateLibrary.getIso8601StringAsDate(obj.getString("time"));
    return plan;
  }

  private static class Plan {
    public double latFrom;
    public double lonFrom;
    public double latTo;
    public double lonTo;
    public Date time;
  }
}
