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

package org.opentripplanner.graph_builder;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.opentripplanner.gbannotation.GraphBuilderAnnotation;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotationsToHTML {

    private static Logger LOG = LoggerFactory.getLogger(AnnotationsToHTML .class); 

    @Parameter(names = { "-h", "--help"}, description = "Print this help message and exit", help = true)
    private boolean help;

    @Parameter(names = { "-g", "--graph"}, description = "path to the graph file", required = true)
    private String graphPath;

    @Parameter(names = { "-o", "--out"}, description = "output file")
    private String outPath;

    private AnnotationEndpoints annotationEndpoints = new AnnotationEndpoints();

    private JCommander jc;

    private Graph graph;

    private HTMLWriter writer;

    public static void main(String[] args) throws IOException {
        // FIXME turn off all logging to avoid mixing log entries and HTML
//        @SuppressWarnings("unchecked")
//        List<Logger> loggers = Collections.<Logger>list(LogManager.getCurrentLoggers());
//        loggers.add(LogManager.getRootLogger());
//        for ( Logger logger : loggers ) {
//            logger.setLevel(Level.OFF);
//        }
        AnnotationsToHTML annotationsToHTML = new AnnotationsToHTML(args);
        annotationsToHTML.run();
        
    }

    public AnnotationsToHTML(String[] args) {
        jc = new JCommander(this);
        jc.addCommand(annotationEndpoints);

        try {
            jc.parse(args);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            jc.usage();
            System.exit(1);
        }

        if (help || jc.getParsedCommand() == null) {
            jc.usage();
            System.exit(0);
        }
    }


    private void run() {

        try {
            graph = Graph.load(new File(graphPath), Graph.LoadLevel.DEBUG);
        } catch (Exception e) {
            LOG.error("Exception while loading graph from " + graphPath);
            e.printStackTrace();
            return;
        }
        LOG.info("done loading graph.");

        if (outPath != null) {
            try {
                writer = new HTMLWriter(outPath);
            } catch (FileNotFoundException ex) {
                java.util.logging.Logger.getLogger(AnnotationsToHTML.class.getName()).log(Level.SEVERE, null, ex);
                LOG.error("Exception while opening output file {}:{}", outPath, ex.getMessage());
                return;
            }
        } else {
            writer = new HTMLWriter(System.out);
        }

        String command = jc.getParsedCommand();
        if (command.equals("annotate")) {
            annotationEndpoints.run();
        }

        if (outPath != null) {
            LOG.info("HTML is in {}", outPath);
        }

        writer.close();
    }

    private void process(Graph graph, String path) {
        writer.println("<html><head><title>Graph report for " + path + "</title>");
        writer.println("\t<meta charset=\"utf-8\">");
        writer.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
        writer.println("<script src='http://code.jquery.com/jquery-1.11.1.js'></script>");
        writer.println("<link rel='stylesheet' href='http://yui.yahooapis.com/pure/0.5.0/pure-min.css'>");
        String css = "\t\t<style>\n"
                + "\n"
                + "\t\t\tbutton.pure-button {\n"
                + "\t\t\t\tmargin:5px;\n"
                + "\t\t\t}\n"
                + "\n"
                + "\t\t\tspan.pure-button {\n"
                + "\t\t\t\tcursor:default;\n"
                + "\t\t\t}\n"
                + "\n"
                + "\t\t\t.button-graphwide,\n"
                + "\t\t\t.button-parkandrideunlinked,\n"
                + "\t\t\t.button-graphconnectivity,\n"
                + "\t\t\t.button-turnrestrictionbad\t{\n"
                + "\t\t\t\tcolor:white;\n"
                + "\t\t\t\ttext-shadow: 0 1px 1px rgba(0, 0, 0, 0.2);\n"
                + "\t\t\t}\n"
                + "\n"
                + "\t\t\t.button-graphwide {\n"
                + "\t\t\t\tbackground: rgb(28, 184, 65); /* this is a green */\n"
                + "\t\t\t}\n"
                + "\n"
                + "\t\t\t.button-parkandrideunlinked {\n"
                + "\t\t\t\tbackground: rgb(202, 60, 60); /* this is a maroon */\n"
                + "\t\t\t}\n"
                + "\n"
                + "\t\t\t.button-graphconnectivity{\n"
                + "\t\t\t\tbackground: rgb(223, 117, 20); /* this is an orange */\n"
                + "\t\t\t}\n"
                + "\n"
                + "\t\t\t.button-turnrestrictionbad {\n"
                + "\t\t\t\tbackground: rgb(66, 184, 221); /* this is a light blue */\n"
                + "\t\t\t}\n"
                + "\n"
                + "\t\t</style>\n"
                + "";
        writer.println(css);
        writer.println("</head><body>");
        writer.println("<h1>OpenTripPlanner annotations log</h1>");
        writer.println("<div id=\"buttons\"></div><ul id=\"log\"></ul>");

        for (GraphBuilderAnnotation annotation : graph.getBuilderAnnotations()) {
            //writer.println("<p>" + annotation.getHTMLMessage() + "</p>");
           // writer.println("<small>" + annotation.getClass().getSimpleName()+"</small>");
            writer.addAnnotation(annotation);

        }
        
        writer.println("\t<script>");
        writer.writeJson();
        String js = "\t\tvar selected = {};\n"
                + "\n"
                + "\t\t//select/deselects filters and correctly styles buttons\n"
                + "\t\tfunction refilter(item) {\n"
                + "\t\t\tconsole.debug(item.data.name);\n"
                + "\t\t\tvar annotation = item.data.name;\n"
                + "\t\t\tvar keyLower = \"button-\"+annotation.toLowerCase();\n"
                + "\t\t\tif (annotation in selected) {\n"
                + "\t\t   \t\tdelete selected[annotation];\t\n"
                + "\t\t\t\t$(\"button.\"+keyLower).removeClass(keyLower);\n"
                + "\t\t\t} else {\n"
                + "\t\t\t\tselected[annotation] = true;\n"
                + "\t\t\t\t$(\"button:contains('\"+annotation+\"')\").addClass(keyLower);\n"
                + "\t\t\t}\n"
                + "\t\t\tresetView();\n"
                + "\n"
                + "\t\t}\n"
                + "\n"
                + "\t\t//draws list based on selected buttons\n"
                + "\t\tfunction resetView() {\n"
                + "\t\t\t$(\"#log\").empty();\n"
                + "\t\t\t$.each(data, function(key, value) {\n"
                + "\t\t\t\t//console.log(key);\n"
                + "\t\t\t\tif (key in selected) {\n"
                + "\t\t\t\t\tvar keyLower = \"button-\"+key.toLowerCase();\n"
                + "\t\t\t\t\t$.each(value, function(index, log) {\n"
                + "\t\t\t\t\t$(\"#log\").append(\"<li>\"+ \"<span class='pure-button \" + keyLower + \"'>\"+ key + \"</span>\" + log+\"</li>\");\n"
                + "\t\t\t\t\t});\n"
                + "\t\t\t\t}\n"
                + "\t\t\t});\n"
                + "\t\t}\n"
                + "\n"
                + "\t\t//creates buttons\n"
                + "\t\t$.each(data, function(key, value) {\n"
                + "\t\t\t//console.info(key);\n"
                + "\t\t\t//console.info(value);\n"
                + "\t\t\tvar keyLower = \"button-\"+key.toLowerCase();\n"
                + "\t\t\tselected[key] = true;\n"
                + "\t\t\tlen = value.length;\n"
                + "\t\t\t$(\"#buttons\").append(\"<button class='pure-button \" + keyLower + \"'>\"+key+ \" (\" + len + \")</button>\");\n"
                + "\t\t\t$(\".\"+keyLower).on(\"click\", {name: key}, refilter);\n"
                + "\t\t});\n"
                + "\n"
                + "\t\tresetView();\n"
                + "";
        writer.println(js);
        writer.println("\t</script>");
        
        writer.println("</body></html>");
    }

    @Parameters(commandNames = "annotate", commandDescription = "Dumps annotations in HTML")
    class AnnotationEndpoints {

        public void run() {
            LOG.info("Annotating log");
            process(graph, graphPath);
            LOG.info("Done annotating log");
        }
    }

    class HTMLWriter {
        private PrintStream out;
        private Multimap<String, String> annotations;

        public HTMLWriter(OutputStream out) {
            this.out = new PrintStream(out);
            annotations = ArrayListMultimap.create();
        }

        public HTMLWriter(String filePath) throws FileNotFoundException {
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            this.out = new PrintStream(fileOutputStream);
            annotations = ArrayListMultimap.create();
        }

        private void println(String bodyhtml) {
            out.println(bodyhtml);
        }

        private void close() {
            out.close();
        }

        private void addAnnotation(GraphBuilderAnnotation annotation) {
            String className = annotation.getClass().getSimpleName();
            annotations.put(className, annotation.getHTMLMessage());
            
        }
        
        private void writeJson() {
            try {
 
                out.print("\tvar data=");
                //StringWriter wr = new StringWriter();
                ObjectMapper mapper = new ObjectMapper();
                JsonGenerator jsonGenerator = mapper.getJsonFactory().createJsonGenerator(out);
                //jsonGenerator.setPrettyPrinter(new DefaultPrettyPrinter());
                
                mapper.writeValue(jsonGenerator, annotations.asMap());
                out.println(";");

                //writer.println(wr.toString());
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(AnnotationsToHTML.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
