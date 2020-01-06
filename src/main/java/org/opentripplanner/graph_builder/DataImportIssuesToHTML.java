package org.opentripplanner.graph_builder;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import org.apache.commons.io.FileUtils;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * This class generates a nice HTML graph import data issue report.
 * 
 * They are created with the help of getHTMLMessage function in {@link DataImportIssue} derived classes.
 * @author mabu
 */
public class DataImportIssuesToHTML implements GraphBuilderModule {

    private static Logger LOG = LoggerFactory.getLogger(DataImportIssuesToHTML.class);

    //Path to output folder
    private File outPath;

    //If there are more then this number of issues the report are split into multiple files
    //This is because browsers aren't made for giant HTML files which can be made with 500k lines
    private int maxNumberOfIssuesPerFile;


    //This counts all occurrences of HTML issue type
    //If one issue type is split into two files it has two entries in this Multiset
    //IT is used to show numbers in HTML files name and links
    private Multiset<String> issueTypeOccurrences;

    //List of writers which are used for actual writing issues to HTML
    List<HTMLWriter> writers;

    //Key is classname, value is issue message
    //Multimap because there are multiple issues for each classname
    private Multimap<String, String> issues;
  
    public DataImportIssuesToHTML(File outpath, int maxNumberOfIssuesPerFile) {
        this.outPath = outpath;
        this.issues = ArrayListMultimap.create();
        this.maxNumberOfIssuesPerFile = maxNumberOfIssuesPerFile;
        this.writers = new ArrayList<>();
        this.issueTypeOccurrences = HashMultiset.create();
    }


    @Override
    public void buildGraph(
            Graph graph,
            HashMap<Class<?>, Object> extra,
            DataImportIssueStore issueStore
    ) {

        if (outPath == null) {
            LOG.error("Saving folder is empty!");
            return;
        }

        outPath = new File(outPath, "report");
        if (outPath.exists()) {
            //Removes all files from report directory
            try {
                FileUtils.cleanDirectory(outPath);
            } catch (IOException e) {
                LOG.error("Failed to clean HTML report directory: " + outPath.toString() + ". HTML report won't be generated!", e);
                return;
            }
        } else {
            //Creates report directory if it doesn't exist yet
            try {
                FileUtils.forceMkdir(outPath);
            } catch (IOException e) {
                e.printStackTrace();
                LOG.error("Failed to create HTML report directory: " + outPath.toString() + ". HTML report won't be generated!", e);
                return;
            }
        }



        //Groups issues in multimap according to issue type
        for (DataImportIssue it : issueStore.getIssues()) {
            //writer.println("<p>" + it.getHTMLMessage() + "</p>");
            // writer.println("<small>" + it.getClass().getSimpleName()+"</small>");
            addIssue(it);

        }
        LOG.info("Creating data import issue log");


        //Creates list of HTML writers. Each writer has whole class of HTML issues
        //Or multiple HTML writers can have parts of one class of HTML issues if number
        // of issues is larger than maxNumberOfIssuesPerFile.
        for (Map.Entry<String, Collection<String>> entry: issues.asMap().entrySet()) {
            List<String> issueList;
            if (entry.getValue() instanceof List) {
                issueList = (List<String>) entry.getValue();
            } else {
                issueList = new ArrayList<>(entry.getValue());
            }
            addIssues(entry.getKey(), issueList);
        }

        //Actual writing to the file is made here since
        // this is the first place where actual number of files is known (because it depends on
        // the issue count)
        for (HTMLWriter writer : writers) {
            writer.writeFile(issueTypeOccurrences, false);
        }

        try {
            HTMLWriter indexFileWriter = new HTMLWriter("index", (Multimap<String, String>)null);
            indexFileWriter.writeFile(issueTypeOccurrences, true);
        } catch (FileNotFoundException e) {
            LOG.error("Index file coudn't be created:{}", e);
        }

        LOG.info("Data import issue logs are in {}", outPath);


    }

    /**
     * Creates file with given type of issues
     *
     * If number of issues is larger then 'maxNumberOfIssuesPerFile' multiple files are generated.
     * And named issueClassName1,2,3 etc.
     *
     * @param issueTypeName name of import data issue class and then also filename
     * @param issues list of all import data issue with that class
     */
    private void addIssues(String issueTypeName, List<String> issues) {
        try {
            HTMLWriter file_writer;
            if (issues.size() > 1.2* maxNumberOfIssuesPerFile) {
                LOG.debug("Number of issues is very large. Splitting: {}", issueTypeName);
                List<List<String>> partitions = Lists.partition(issues,
                    maxNumberOfIssuesPerFile
                );
                for (List<String> partition: partitions) {
                    issueTypeOccurrences.add(issueTypeName);
                    int labelCount = issueTypeOccurrences.count(issueTypeName);
                    file_writer = new HTMLWriter(issueTypeName + labelCount, partition);
                    writers.add(file_writer);
                }

            } else {
                issueTypeOccurrences.add(issueTypeName);
                int labelCount = issueTypeOccurrences.count(issueTypeName);
                file_writer = new HTMLWriter(issueTypeName + labelCount,
                    issues);
                writers.add(file_writer);
            }
        } catch (FileNotFoundException ex) {
            LOG.error("Output folder not found:{} {}", outPath, ex);
        }
    }

    @Override
    public void checkInputs() {

    }

    /**
     * Groups issues according to issue type, using the classname as type name.
     *
     * All issues are saved together in multimap where key is issue classname
     * and values are list of issue with that class
     */
    private void addIssue(DataImportIssue issue) {
        String issueTypeName = issue.getClass().getSimpleName();
        issues.put(issueTypeName, issue.getHTMLMessage());

    }

    class HTMLWriter {
        private PrintStream out;

        private Multimap<String, String> writerIssues;

        private String issueTypeName;

        public HTMLWriter(String key, Collection<String> issues) throws FileNotFoundException {
            LOG.debug("Making file: {}", key);
            File newFile = new File(outPath, key +".html");
            FileOutputStream fileOutputStream = new FileOutputStream(newFile);
            this.out = new PrintStream(fileOutputStream);
            writerIssues = ArrayListMultimap.create();
            writerIssues.putAll(key, issues);
            issueTypeName = key;
        }

        public HTMLWriter(String filename, Multimap<String, String> curMap)
            throws FileNotFoundException {
            LOG.debug("Making file: {}", filename);
            File newFile = new File(outPath, filename +".html");
            FileOutputStream fileOutputStream = new FileOutputStream(newFile);
            this.out = new PrintStream(fileOutputStream);
            writerIssues = curMap;
            issueTypeName = filename;
        }

        private void writeFile(Multiset<String> classes, boolean isIndexFile) {
            println("<html><head><title>Graph report for " + outPath.getParentFile()
                + "graph.obj</title>");
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
            println(String.format("<h1>OpenTripPlanner data import issue log for %s</h1>", issueTypeName));
            println("<h2>Graph report for " + outPath.getParentFile() + "graph.obj</h2>");
            println("<p>");
            //adds links to the other HTML files
            for (Multiset.Entry<String> htmlIssueType : classes.entrySet()) {
                String label_name = htmlIssueType.getElement();
                String label;
                int currentCount = 1;
                //it needs to add link to every file even if they are split
                while (currentCount <= htmlIssueType.getCount()) {
                    label = label_name + currentCount;
                    if (label.equals(issueTypeName)) {
                        println(String.format("<button class='pure-button pure-button-disabled button-%s'>%s</button>",
                            label_name.toLowerCase(), label));
                    } else {
                        println(String.format("<a class='pure-button button-%s' href=\"%s.html\">%s</a>",
                            label_name.toLowerCase(), label, label));
                    }
                    currentCount++;
                }
            }
            println("</p>");
            if (!isIndexFile) {
                println("<ul id=\"log\">");
                writeIssues();
                println("</ul>");
            }

            println("</body></html>");

            close();
        }

        /**
         * Writes issues as LI html elements
         */
        private void writeIssues() {
            String FMT = "<li>%s</li>";
            for (Map.Entry<String, String> it: writerIssues.entries()) {
                print(String.format(FMT, it.getValue()));
            }
        }

        private void println(String bodyhtml) {
            out.println(bodyhtml);
        }

        private void print(String bodyhtml) {
            out.print(bodyhtml);
        }

        private void close() {
            out.close();
        }


        
        /**
         * Generates JSON from issue variable which is used by Javascript
         * to display HTML report
         */
        private void writeJson() {
            try {
 
                out.print("\tvar data=");
                ObjectMapper mapper = new ObjectMapper();
                JsonGenerator jsonGenerator = mapper.getJsonFactory().createJsonGenerator(out);
                
                mapper.writeValue(jsonGenerator, writerIssues.asMap());
                out.println(";");

            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(DataImportIssuesToHTML.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
