package org.opentripplanner.standalone;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opentripplanner.routing.impl.GraphServiceFileImpl;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

/**
 * This is a JCommander-annotated class that holds parameters for OTP stand-alone mode.
 * These parameters can be parsed from the command line, or provided in a file using Jcommander's
 * at-symbol syntax (see http://jcommander.org/#Syntax). When stand-alone OTP is started as a 
 * daemon, parameters are loaded from such a file, located by default in '/etc/opentripplanner.cfg'.
 * 
 * Note that JCommander-annotated parameters can be any type that can be constructed from a string.
 * This module also contains classes for validating parameters. 
 * See: http://jcommander.org/#Parameter_validation
 * 
 * Some parameter fields are not initialized so when inferring other parameters, we can check for 
 * null and see whether they were specified on the command line.
 * 
 * @author abyrd
 */
public class CommandLineParameters {

    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_STATIC_DIRECTORY = "/var/otp/static";
    private static final String DEFAULT_GRAPH_DIRECTORY  = "/var/otp/graphs";
    private static final String DEFAULT_CACHE_DIRECTORY  = "/var/otp/cache";
    private static final String DEFAULT_ROUTER_ID = "";

    /* Options for the command itself, rather than build or server sub-tasks. */
    
    @Parameter(names = { "-h", "--help"}, help = true,
    description = "Print this help message and exit")
    boolean help;
    
    @Parameter(names = { "-v", "--verbose" }, 
    description = "Verbose output")
    boolean verbose;
   
    /* Options for the graph builder sub-task. */

    @Parameter(names = {"-b", "--build"}, validateWith = ReadableDirectory.class, 
    description = "build graphs at specified paths", variableArity = true)
    public List<File> build;
    
    @Parameter(names = { "-c", "--cache"}, validateWith = ReadWriteDirectory.class,
            description = "the directory under which to cache OSM and NED tiles")
    String cacheDirectory;

    @Parameter( names = { "--congestion"},
            description = "supply a congestion CSV file")
    String congestionCsv;

    @Parameter(names = { "-e", "--elevation"},
            description = "download and use elevation data for the graph")
    boolean elevation;
    
    @Parameter(names = { "-m", "--inMemory"},
    description = "pass the graph to the server in-memory after building it, without saving to disk")
    boolean inMemory;
    
    @Parameter(names = {"--noTransit"},
    description = "skip all transit input files (GTFS)")
    boolean noTransit;

    @Parameter(names = {"--useTransfersTxt"},
    description = "use transfers.txt file for the gtfsBundle (GTFS)")
    boolean useTransfersTxt;
    
    @Parameter(names = {"--noParentStopLinking"},
    description = "skip linking of stops to parent stops (GTFS)")
    boolean noParentStopLinking;

    @Parameter(names = {"--parentStationTransfers"},
    description = "create direct transfers between the constituent stops of each parent station")
    boolean parentStationTransfers = false;

    @Parameter(names = {"--noStreets"},
    description = "skip all street input files (OSM)")
    boolean noStreets;

    @Parameter(names = {"--noEmbedConfig"},
    description = "Skip embedding config in graph (Embed.properties)")
    boolean noEmbedConfig = false;

    @Parameter(names = {"--transitIndex"},
    description = "build a transit index for GTFS data")
    boolean transitIndex;

    /* Options for the server sub-task. */

    @Parameter( names = { "-a", "--analyst"}, 
            description = "enable OTP Analyst extensions")
    boolean analyst;
    
    @Parameter( names = { "-g", "--graphs"}, validateWith = ReadableDirectory.class, 
            description = "path to graph directory")
    String graphDirectory;
    
    @Parameter( names = { "-l", "--longDistance"}, 
            description = "use an algorithm tailored for long-distance routing")
    boolean longDistance = false;

    @Parameter( names = { "-p", "--port"}, validateWith = AvailablePort.class, 
    description = "server port")
    Integer port;

    @Parameter( names = { "-r", "--router"}, validateWith = RouterId.class,
    description = "Router ID, first one being the default")
    List<String> routerIds;

    @Parameter( names = { "-s", "--server"}, 
            description = "run a server")
    boolean server = false;
    
    @Parameter( names = { "-t", "--static"}, 
    description = "path to static content")
    String staticDirectory;

    @Parameter( names = { "-z", "--visualize"},
    description = "open a debugging graph visualizer")
    boolean visualize;

    @Parameter( validateWith = ReadableFile.class, // the remaining parameters in one array
    description = "files") 
    List<File> files = new ArrayList<File>();

    /** Set some convenience parameters based on other parameters' values. */
    public void infer () {
        server |= ( inMemory || port != null );
        if (graphDirectory  == null) graphDirectory  = DEFAULT_GRAPH_DIRECTORY;
        if (routerIds == null) routerIds = Arrays.asList(DEFAULT_ROUTER_ID);
        if (staticDirectory == null) staticDirectory = DEFAULT_STATIC_DIRECTORY;        
        if (cacheDirectory == null)  cacheDirectory  = DEFAULT_CACHE_DIRECTORY;        
        if (server && port == null) {
            port = DEFAULT_PORT;
            new AvailablePort().validate(port);
        }
    }
    
    public static class ReadableFile implements IParameterValidator {
        @Override
        public void validate(String name, String value) throws ParameterException {
            File file = new File(value);
            if ( ! file.isFile()) {
                String msg = String.format("%s: '%s' is not a file.", name, value);
                throw new ParameterException(msg);
            }
            if ( ! file.canRead()) {
                String msg = String.format("%s: file '%s' is not readable.", name, value);
                throw new ParameterException(msg);
            }
        }
    }
    
    public static class ReadableDirectory implements IParameterValidator {
        @Override
        public void validate(String name, String value) throws ParameterException {
            File file = new File(value);
            if ( ! file.isDirectory()) {
                String msg = String.format("%s: '%s' is not a directory.", name, value);
                throw new ParameterException(msg);
            }
            if ( ! file.canRead()) {
                String msg = String.format("%s: directory '%s' is not readable.", name, value);
                throw new ParameterException(msg);
            }
        }
    }
    
    public static class ReadWriteDirectory implements IParameterValidator {
        @Override
        public void validate(String name, String value) throws ParameterException {
            new ReadableDirectory().validate(name, value);
            File file = new File(value);
            if ( ! file.canWrite()) {
                String msg = String.format("%s: directory '%s' is not writable.", name, value);
                throw new ParameterException(msg);
            }
        }
    }

    public static class PositiveInteger implements IParameterValidator {
        @Override
        public void validate(String name, String value) throws ParameterException {
            Integer i = Integer.parseInt(value);
            if ( i <= 0 ) {
                String msg = String.format("%s must be a positive integer.", name);
                throw new ParameterException(msg);
            }
        }
    }

    public static class AvailablePort implements IParameterValidator {

        @Override
        public void validate(String name, String value) throws ParameterException {
            new PositiveInteger().validate(name, value);
            int port = Integer.parseInt(value);
            this.validate(port);
        }
        
        public void validate(int port) throws ParameterException {
            ServerSocket socket = null;
            boolean portUnavailable = false;
            String reason = null;
            try {
                socket = new ServerSocket(port);
            } catch (IOException e) {
                portUnavailable = true;
                reason = e.getMessage();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) { 
                        // will not be thrown
                    }
                }
            }
            if ( portUnavailable ) {
                String msg = String.format(": port %d is not available. %s.", port, reason);
                throw new ParameterException(msg);
            }
        }
    }
    
    public static class RouterId implements IParameterValidator {
        @Override
        public void validate(String name, String value) throws ParameterException {
            Pattern routerIdPattern = GraphServiceFileImpl.routerIdPattern;
            Matcher m = routerIdPattern.matcher(value);
            if ( ! m.matches()) {
                String msg = String.format("%s: '%s' is not a valid router ID.", name, value);
                throw new ParameterException(msg);
            }
        }
    }
}

