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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
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
        writer.println("<html><head><title>Graph report for " + path + "</title></head><body>");

        for (GraphBuilderAnnotation annotation : graph.getBuilderAnnotations()) {
            writer.println("<p>" + annotation.getHTMLMessage() + "</p>");

        }
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

        public HTMLWriter(OutputStream out) {
            this.out = new PrintStream(out);
        }

        public HTMLWriter(String filePath) throws FileNotFoundException {
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            this.out = new PrintStream(fileOutputStream);
        }

        private void println(String bodyhtml) {
            out.println(bodyhtml);
        }

        private void close() {
            out.close();
        }
    }

}
