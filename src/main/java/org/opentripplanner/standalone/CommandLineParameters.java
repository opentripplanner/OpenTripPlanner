package org.opentripplanner.standalone;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jersey.repackaged.com.google.common.collect.Lists;

import org.opentripplanner.routing.impl.GraphServiceImpl;

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
public class CommandLineParameters implements Cloneable {

    private static final int DEFAULT_PORT = 8080;
    private static final int DEFAULT_SECURE_PORT = 8081;
    private static final String DEFAULT_GRAPH_DIRECTORY  = "/var/otp/graphs";
    private static final String DEFAULT_CACHE_DIRECTORY  = "/var/otp/cache";
    private static final String DEFAULT_POINTSET_DIRECTORY  = "/var/otp/pointsets";
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

    @Parameter(names = { "-e", "--elevation"},
            description = "download and use elevation data for the graph")
    boolean elevation;
    
    @Parameter(names = { "-m", "--inMemory"},
    description = "pass the graph to the server in-memory after building it, without saving to disk")
    public boolean inMemory;
 
    @Parameter(names = { "--preFlight"},
    description = "pass the graph to the server in-memory after building it, and saving to disk")
    boolean preFlight;

    @Parameter(names = {"--noTransit"},
    description = "skip all transit input files (GTFS)")
    boolean noTransit;

    @Parameter(names = {"--useTransfersTxt"},
    description = "use transfers.txt file for the gtfsBundle (GTFS)")
    boolean useTransfersTxt;
    
    @Parameter(names = {"--noParentStopLinking"},
    description = "skip linking of stops to parent stops (GTFS)")
    boolean noParentStopLinking;

    @Parameter(names = {"--useStreetsForLinking"},
    description = "use street network to link stops to each other")
    boolean useStreetsForLinking;

    @Parameter(names = {"--parentStationTransfers"},
    description = "create direct transfers between the constituent stops of each parent station")
    boolean parentStationTransfers = false;

    @Parameter(names = {"--noStreets"},
    description = "skip all street input files (OSM)")
    boolean noStreets;

    @Parameter(names = {"--noEmbedConfig"},
    description = "Skip embedding config in graph (Embed.properties)")
    boolean noEmbedConfig = false;

    @Parameter(names = { "--skipVisibility"},
            description = "skip area visibility calculations, which are often time consuming.")
    boolean skipVisibility;

    /* Options for the server sub-task. */

    @Parameter( names = { "-a", "--analyst"}, 
            description = "enable OTP Analyst extensions")
    boolean analyst;

    @Parameter( names = {"--bindAddress"},
            description = "the address of the network interface to bind to. defaults to all interfaces.")
    String bindAddress = "0.0.0.0";

    @Parameter( names = { "--securePort"}, validateWith = AvailablePort.class,
            description = "server port")
    Integer securePort;

    @Parameter( names = { "-f", "--graphConfigFile"}, validateWith = ReadableFile.class,
            description = "path to graph configuration file")
    String graphConfigFile;

    // --basePath (rather than --graphs). Maybe just eliminate most short options.
    @Parameter( names = { "-g", "--graphs"}, validateWith = ReadableDirectory.class,
            description = "path to graph directory")
    String graphDirectory;

    @Parameter(names = { "--autoScan" }, description = "auto-scan for graphs to register in graph directory.")
    boolean autoScan = false;

    @Parameter(names = { "--autoReload" }, description = "auto-reload registered graphs when source data is modified.")
    boolean autoReload = false;

    @Parameter( names = { "-l", "--longDistance"}, 
            description = "use an algorithm tailored for long-distance routing")
    boolean longDistance = false;

    @Parameter( names = { "-p", "--port"}, validateWith = AvailablePort.class, 
    description = "server port")
    Integer port;

    @Parameter( names = { "-P", "--pointSet"}, validateWith =  ReadableDirectory.class, 
    		description = "path to pointSet directory")
    String pointSetDirectory;
    
    @Parameter( names = { "-r", "--router"}, validateWith = RouterId.class,
    		description = "Router ID, first one being the default")
    List<String> routerIds;

    @Parameter( names = { "-s", "--server"}, 
            description = "run a server")
    boolean server = false;
    
    @Parameter( names = { "-z", "--visualize"}, 
    description = "open a debugging graph visualizer")
    boolean visualize;

    @Parameter( validateWith = ReadableFile.class, // the remaining parameters in one array
    description = "files") 
    List<File> files = new ArrayList<File>();
    
    @Parameter( names = {"--insecure"},
            description = "allow unauthenticated access to sensitive resources, e.g. /routers")
    boolean insecure = false;

    /** Set some convenience parameters based on other parameters' values. */
    public void infer () {
        server |= ( inMemory || preFlight || port != null );
        if (graphDirectory  == null) graphDirectory  = DEFAULT_GRAPH_DIRECTORY;
        if (routerIds == null) {
            if (autoScan || inMemory || preFlight)
                routerIds = Collections.emptyList();
            else
                routerIds = Arrays.asList(DEFAULT_ROUTER_ID);
        }
        if (cacheDirectory == null) cacheDirectory = DEFAULT_CACHE_DIRECTORY;
        if (pointSetDirectory == null) pointSetDirectory = DEFAULT_POINTSET_DIRECTORY;
        if (server && port == null) {
            port = DEFAULT_PORT;
            new AvailablePort().validate(port);
        }
        if (server && securePort == null) {
            securePort = DEFAULT_SECURE_PORT;
            new AvailablePort().validate(securePort);
        }
    }

    public CommandLineParameters clone () {
        CommandLineParameters ret;
        try {
            ret = (CommandLineParameters) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
        
        if (this.build != null) {
            ret.build = Lists.newArrayList();
            ret.build.addAll(this.build);
        }
        
        if (this.routerIds != null) {
            ret.routerIds = Lists.newArrayList();
            ret.routerIds.addAll(this.routerIds);
        }
        
        return ret;
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
            Pattern routerIdPattern = GraphServiceImpl.routerIdPattern;
            Matcher m = routerIdPattern.matcher(value);
            if ( ! m.matches()) {
                String msg = String.format("%s: '%s' is not a valid router ID.", name, value);
                throw new ParameterException(msg);
            }
        }
    }
}

