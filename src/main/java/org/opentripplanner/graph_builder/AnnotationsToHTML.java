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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.logging.Level;

import com.google.common.primitives.Ints;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.graph_builder.annotation.GraphBuilderAnnotation;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class generates nice HTML graph annotations reports 
 * 
 * They are created with the help of getHTMLMessage function in {@link GraphBuilderAnnotation} derived classes.
 * @author mabu
 */
public class AnnotationsToHTML implements GraphBuilderModule {

    private static Logger LOG = LoggerFactory.getLogger(AnnotationsToHTML.class); 

    //Path to output folder
    private File outPath;

    private int maxNumberOfAnnotationsPerFile;

    Set<String> classes;

    Multiset<String> classOccurences;

    List<HTMLWriter> writers;

    //Key is classname, value is annotation message
    //Multimap because there are multiple annotations for each classname
    private Multimap<String, String> annotations;
  
    public AnnotationsToHTML (File outpath, int maxNumberOfAnnotationsPerFile) {
        this.outPath = outpath;
        annotations = ArrayListMultimap.create();
        this.maxNumberOfAnnotationsPerFile = maxNumberOfAnnotationsPerFile;
        this.classes = new TreeSet<>();
        this.writers = new ArrayList<>();
        this.classOccurences = HashMultiset.create();
    }


    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {

        if (outPath == null) {
            LOG.error("Saving folder is empty!");
            return;
        }


        for (GraphBuilderAnnotation annotation : graph.getBuilderAnnotations()) {
            //writer.println("<p>" + annotation.getHTMLMessage() + "</p>");
            // writer.println("<small>" + annotation.getClass().getSimpleName()+"</small>");
            addAnnotation(annotation);

        }
        LOG.info("Creating Annotations log");

        Map<String, Collection<String>> annotationsMap = annotations.asMap();
        //saves list of annotation classes and counts
        List<T2<String, Integer>> counts = new ArrayList<>(annotationsMap.size());
        for (Map.Entry<String, Collection<String>> entry: annotationsMap.entrySet()) {
            counts.add(new T2<>(entry.getKey(), entry.getValue().size()));
        }

        //Orders annotations and counts of annotations usages by number of usages
        //from most used to the least
        Collections.sort(counts, (o1, o2) -> Ints.compare(o2.second, o1.second));

        int currentNumberOfAnnotationsPerFile = 0;
        Multimap<String, String> current_map = ArrayListMultimap.create();
        String last_added_key = null;

        //Annotations are grouped until the count of annotations is less then maxNumberOfAnnotationsPerFile
        //otherwise each class of annotations is different file.
        for (T2<String, Integer> count : counts) {
            LOG.info("Key: {} ({})", count.first, count.second);

            if ((currentNumberOfAnnotationsPerFile + count.second) <= maxNumberOfAnnotationsPerFile) {
                LOG.info("Increasing count: {}+{}={}", currentNumberOfAnnotationsPerFile, count.second, currentNumberOfAnnotationsPerFile+count.second);
                currentNumberOfAnnotationsPerFile+=count.second;
                current_map.putAll(count.first, annotationsMap.get(count.first));
                last_added_key = count.first;
            } else {
                LOG.info("Flush count:{}", currentNumberOfAnnotationsPerFile);
                if (currentNumberOfAnnotationsPerFile > 0) {
                    addAnnotations(last_added_key, current_map);
                }

                current_map = ArrayListMultimap.create();
                current_map.putAll(count.first, annotationsMap.get(count.first));
                last_added_key = count.first;
                currentNumberOfAnnotationsPerFile = count.second;
            }

        }

        LOG.info("Flush last count:{}", currentNumberOfAnnotationsPerFile);
        addAnnotations(last_added_key, current_map);


        //Actual writing to the file is made here since
        // this is the first place where actual number of files is known (because it depends on annotations count)
        for (HTMLWriter writer : writers) {
            writer.writeFile(classOccurences);
        }


        LOG.info("Annotated log is in {}", outPath);


    }

    private void addAnnotations(String last_added_key, Multimap<String, String> current_map) {
        try {
            HTMLWriter file_writer;
            if (current_map.keySet().size() == 1) {
                if (current_map.values().size() > 1.2*maxNumberOfAnnotationsPerFile) {
                    LOG.info("Number of annotations is very large. Splitting: {}", last_added_key);
                    List<String> allAnnonations = new ArrayList<>(current_map.values());
                    List<List<String>> partitions = Lists.partition(allAnnonations, maxNumberOfAnnotationsPerFile);
                    for (List<String> partition: partitions) {
                        classOccurences.add(last_added_key);
                        int labelCount = classOccurences.count(last_added_key);
                        file_writer =new HTMLWriter(last_added_key+Integer.toString(labelCount), partition);
                        writers.add(file_writer);
                    }

                } else {
                    classOccurences.add(last_added_key);
                    int labelCount = classOccurences.count(last_added_key);
                    file_writer = new HTMLWriter(last_added_key + Integer.toString(labelCount),
                        current_map);
                    writers.add(file_writer);
                }

            } else {
                classOccurences.add("rest");
                int labelCount = classOccurences.count("rest");
                file_writer = new HTMLWriter("rest" + labelCount, current_map);
                writers.add(file_writer);
            }


        } catch (FileNotFoundException ex) {
            LOG.error("Output folder not found:{} {}", outPath, ex);
            return;
        }
    }

    @Override
    public void checkInputs() {

    }

    private void addAnnotation(GraphBuilderAnnotation annotation) {
        String className = annotation.getClass().getSimpleName();
        annotations.put(className, annotation.getHTMLMessage());

    }

    class HTMLWriter {
        private PrintStream out;

        private Multimap<String, String> lannotations;

        private String current_class;

        public HTMLWriter(String key, Collection<String> annotations) throws FileNotFoundException {
            LOG.info("Making file: {}", key);
            File newFile = new File(outPath, key +".html");
            FileOutputStream fileOutputStream = new FileOutputStream(newFile);
            this.out = new PrintStream(fileOutputStream);
            lannotations = ArrayListMultimap.create();
            lannotations.putAll(key, annotations);
            current_class = key;
        }

        public HTMLWriter(String filename, Multimap<String, String> curMap)
            throws FileNotFoundException {
            LOG.info("Making file: {}", filename);
            File newFile = new File(outPath, filename +".html");
            FileOutputStream fileOutputStream = new FileOutputStream(newFile);
            this.out = new PrintStream(fileOutputStream);
            lannotations = curMap;
            current_class = filename;
        }

        private void writeFile(Multiset<String> classes) {
            println("<html><head><title>Graph report for " + outPath.getParentFile()
                + "Graph.obj</title>");
            println("\t<meta charset=\"utf-8\">");
            println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
            println("<script src='http://code.jquery.com/jquery-1.11.1.js'></script>");
            println(
                "<link rel='stylesheet' href='http://yui.yahooapis.com/pure/0.5.0/pure-min.css'>");
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
            println(css);
            println("</head><body>");
            println("<h1>OpenTripPlanner annotations log</h1>");
            println("<h2>Graph report for " + outPath.getParentFile() + "Graph.obj</h2>");
            println("<p>");
            //adds links to the other HTML files
            for (Multiset.Entry<String> htmlAnnotationClass: classes.entrySet()) {
                String label_name = htmlAnnotationClass.getElement();
                String label;
                int currentCount = 1;
                //it needs to add link to every file even if they are split
                while (currentCount <= htmlAnnotationClass.getCount()) {
                    label = label_name + currentCount;
                    if (label.equals(current_class)) {
                        println("<span>" + label + "</span><br />");
                    } else {
                        println("<a href=\"" + label + ".html\">" + label + "</a><br />");
                    }
                    currentCount++;
                }
            }
            println("</p>");
            println("<div id=\"buttons\"></div><ul id=\"log\"></ul>");



            println("\t<script>");

            writeJson();
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
            println(js);
            println("\t</script>");

            println("</body></html>");

            close();
        }

        private void println(String bodyhtml) {
            out.println(bodyhtml);
        }

        private void close() {
            out.close();
        }


        
        /**
         * Generates JSON from annotations variable which is used by Javascript
         * to display HTML report
         */
        private void writeJson() {
            try {
 
                out.print("\tvar data=");
                ObjectMapper mapper = new ObjectMapper();
                JsonGenerator jsonGenerator = mapper.getJsonFactory().createJsonGenerator(out);
                
                mapper.writeValue(jsonGenerator, lannotations.asMap());
                out.println(";");

            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(AnnotationsToHTML.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
