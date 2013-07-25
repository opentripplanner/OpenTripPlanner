package org.opentripplanner.standalone;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
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
 * @author abyrd
 */
public class CommandLineParameters {

    /* Options for the command itself, rather than build or server sub-tasks. */
    
    @Parameter(names = { "-h", "--help"}, help = true,
    description = "Print this help message and exit")
    boolean help;
    
    @Parameter(names = { "-v", "--verbose" }, 
    description = "Verbose output")
    boolean verbose = false;
   
    /* Options for the graph builder sub-task. */

    @Parameter(names = {"-b", "--build"}, variableArity = true, validateWith = ReadableDirectory.class, 
    description = "build graphs at specified paths")
    public List<File> foo = new ArrayList<File>();
    
    @Parameter(names = { "-s", "--serialize"}, 
    description = "whether to serialize the graph after building it")
    boolean serialize = true;
    
    /* Options for the server sub-task. */

    @Parameter( names = { "-p", "--port"}, validateWith = AvailablePort.class, 
    description = "server port")
    int port = 8080;

    @Parameter( names = { "-g", "--graphs"}, validateWith = ReadableDirectory.class, 
    description = "path to graph directory")
    String graphDirectory = "/var/otp/graphs";
    
    @Parameter( names = { "-r", "--router"}, validateWith = RouterId.class,
    description = "default router ID")
    String defaultRouterId = "";

    @Parameter( names = { "-t", "--static"}, 
    description = "path to static content")
    String staticDirectory = "/var/otp/static";

    @Parameter( validateWith = ReadableFile.class, // the remaining parameters in one array
    description = "files") 
    List<File> files = new ArrayList<File>();

    private static class ReadableFile implements IParameterValidator {
        @Override
        public void validate(String name, String value) throws ParameterException {
            File file = new File(value);
            if ( ! file.isFile()) {
                String msg = String.format("Parameter '%s': '%s' is not a file.", name, value);
                throw new ParameterException(msg);
            }
            if ( ! file.canRead()) {
                String msg = String.format("Parameter '%s': file '%s' is not readable.", name, value);
                throw new ParameterException(msg);
            }
        }
    }
    
    private static class ReadableDirectory implements IParameterValidator {
        @Override
        public void validate(String name, String value) throws ParameterException {
            File file = new File(value);
            if ( ! file.isDirectory()) {
                String msg = String.format("Parameter '%s': '%s' is not a directory.", name, value);
                throw new ParameterException(msg);
            }
            if ( ! file.canRead()) {
                String msg = String.format("Parameter '%s': directory '%s' is not readable.", name, value);
                throw new ParameterException(msg);
            }
        }
    }
    
    private static class PositiveInteger implements IParameterValidator {
        @Override
        public void validate(String name, String value) throws ParameterException {
            Integer i = Integer.parseInt(value);
            if ( i <= 0 ) {
                String msg = String.format("Parameter '%s' must be a positive integer.", name);
                throw new ParameterException(msg);
            }
        }
    }

    private static class AvailablePort implements IParameterValidator {
        @Override
        public void validate(String name, String value) throws ParameterException {
            new PositiveInteger().validate(name, value);
            Integer port = Integer.parseInt(value);
            ServerSocket socket = null;
            boolean portUnavailable = false;
            try {
                socket = new ServerSocket(port);
            } catch (IOException e) {
                portUnavailable = true;
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
                String msg = String.format("Port %d is not available.", port);
                throw new ParameterException(msg);
            }
        }
    }
    
    private static class RouterId implements IParameterValidator {
        @Override
        public void validate(String name, String value) throws ParameterException {
            Pattern routerIdPattern = GraphServiceFileImpl.routerIdPattern;
            Matcher m = routerIdPattern.matcher(value);
            if ( ! m.matches()) {
                String msg = String.format("Parameter '%s': '%s' is not a valid router ID.", name, value);
                throw new ParameterException(msg);
            }
        }
    }

}

