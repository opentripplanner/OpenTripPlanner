package org.opentripplanner.standalone;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

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

    private static final int    DEFAULT_PORT         = 8080;
    private static final int    DEFAULT_SECURE_PORT  = 8081;
    private static final String DEFAULT_CACHE_PATH   = "/var/otp/cache";
    private static final String DEFAULT_BIND_ADDRESS = "0.0.0.0";

    /* Options for the command itself, rather than build or server sub-tasks. */

    @Parameter(names = {"--help"}, help = true,
            description = "Print this help message and exit.")
    public boolean help;

    @Parameter(names = {"--version"}, description = "Print the version, and then exit.")
    public boolean version = false;

    /* Options for graph building and loading. */

    @Parameter(names = {"--build"}, description = "Build graphs from inputs in the specified directory.")
    public boolean build;

    @Parameter(names = {"--cache"}, validateWith = ReadWriteDirectory.class,
            description = "The directory under which to cache OSM and NED tiles. Default is BASE_PATH/cache.")
    public File cacheDirectory = new File(DEFAULT_CACHE_PATH);

    @Parameter(names = {"--inMemory"},
            description = "Do not save the graph after building, just start the server.")
    public boolean inMemory;

    @Parameter(names = {"--load"}, description = "Load the Graph.obj in the specified directory.")
    public boolean load;

    /* Options for the server sub-task. */

    @Parameter(names = {"--bindAddress"},
            description = "Specify which network interface to bind to by address. 0.0.0.0 means all interfaces.")
    public String bindAddress = DEFAULT_BIND_ADDRESS;

    @Parameter(names = {"--clientFiles"}, validateWith = ReadableDirectory.class,
            description = "Path to directory containing local client files to serve.")
    public File clientDirectory = null;

    @Parameter(names = {"--disableFileCache"}, description = "Disable HTTP server static file cache (for development).")
    public boolean disableFileCache = false;

    @Parameter(names = {"--insecure"},
            description = "Allow unauthenticated access to sensitive API resources, e.g. /routers")
    public boolean insecure = false;

    @Parameter(names = {"--maxThreads"}, description = "The maximum number of HTTP handler threads.")
    public Integer maxThreads;

    @Parameter(names = {"--port"}, validateWith = PositiveInteger.class,
            description = "Server port for plain HTTP.")
    public Integer port = DEFAULT_PORT;

    @Parameter(names = {"--serve"},
            description = "Run an OTP API server. Implied by --load, and optional with --build.")
    public boolean serve = false;

    @Parameter(names = {"--securePort"}, validateWith = PositiveInteger.class,
            description = "Server port for HTTPS.")
    public Integer securePort = DEFAULT_SECURE_PORT;

    @Parameter(names = {"--visualize"},
            description = "Open a graph visualizer window for debugging.")
    public boolean visualize;

    /* The remaining single parameter after the switches is the directory with graph files. */

    @Parameter(validateWith = ReadableDirectory.class, description = "/graph/or/inputs/directory")
    public List<File> graphDirectory;

    /**
     * Set some convenience parameters based on other parameters' values.
     * Default values are validated even when no command line option is specified, and we will not bind ports unless
     * a server is started. Therefore we only validate that port parameters are positive integers, and we check that
     * ports are available only when a server will be started.
     */
    public void inferAndValidate () {
        if (inMemory) {
            serve = true;
        }
        if (serve) {
            checkPortAvailable(port);
            checkPortAvailable(securePort);
        }
    }

    /**
     * Workaround for bug https://github.com/cbeust/jcommander/pull/390
     * The main non-switch parameter has to be a list. Return the first one.
     */
    public File getGraphDirectory () {
        if (graphDirectory == null || graphDirectory.size() != 1) {
            throw new ParameterException("You must supply a single directory name.");
        }
        return graphDirectory.get(0);
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

    /**
     * @param port a port that we plan to bind to
     * @throws ParameterException if that port is not available
     */
    public static void checkPortAvailable (int port) throws ParameterException {
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

