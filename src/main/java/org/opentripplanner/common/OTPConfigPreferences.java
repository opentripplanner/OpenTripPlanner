package org.opentripplanner.common;

import com.beust.jcommander.internal.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Stack;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

/**
 * A read-only implementation of the Java Preferences API (hierarchically structured configuration information).
 * The preferences are loaded from a text configuration file where every line is either a key-value pair (where the key
 * is separated from the value by whitespace characters), a child node name followed by whitespace and an opening
 * bracket, or a single closing bracket. For example:
 *
 * <pre>
 * # This is a comment line.
 * # Note that keys may not contain whitespace but values can.
 * name The OpenTripPlanner Project
 * frequency 87.65
 * size {
 *     height 12.3
 *     width  45.6
 *     depth  78.9
 * }
 * color {
 *     red   20
 *     green 20
 *     blue  40
 * }
 * </pre>
 *
 * @author abyrd
 */
public class OTPConfigPreferences extends AbstractPreferences {

    private static final Logger LOG = LoggerFactory.getLogger(OTPConfigPreferences.class);

    OTPConfigPreferences parent;
    String name;
    private Map<String, String> entries = Maps.newHashMap();
    private Map<String, OTPConfigPreferences> children = Maps.newHashMap();

    private static void parseError(String msg, String filename, int line) {
        String longmsg = String.format("parse error in config file '%s' at line %d:\n    %s", filename, line, msg);
        LOG.error(longmsg);
    }

    private OTPConfigPreferences (OTPConfigPreferences parent, String name) {
        super(parent, name);
    }

    public static OTPConfigPreferences fromFile (String filename) {
        OTPConfigPreferences curr = new OTPConfigPreferences(null, "");
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            Stack<OTPConfigPreferences> stack = new Stack<OTPConfigPreferences>();
            String line;
            int nline = 0;
            while ((line = br.readLine()) != null) {
                nline++;
                String[] tokens = line.trim().split("\\s+", 2); // split on one or more whitespace characters
                // System.out.println(Arrays.asList(tokens));
                if (tokens.length == 0 || tokens[0].length() == 0 || tokens[0].startsWith("#")) {
                    continue;
                }
                if (tokens.length > 2) {
                    parseError("line must contain a key-value pair, a child name and an opening bracket, " +
                            "or a single closing bracket.", filename, nline);
                    return null;
                }
                String key = tokens[0].trim();
                String val = "NONE";
                if (tokens.length > 1) val = tokens[1].trim();
                if (key.equals("}")) {
                    if (tokens.length > 1) {
                        parseError("Closing bracket should appear alone on a line.", filename, nline);
                        return null;
                    }
                    if (stack.isEmpty()) {
                        parseError("Bracket mismatch (too many closing brackets).", filename, nline);
                        return null;
                    }
                    curr = stack.pop();
                    continue;
                }
                if (key.endsWith("{")) {
                    parseError("An opening bracket should be preceded by a child name and whitespace.", filename, nline);
                    return null;
                }
                if (val.startsWith("{") && val.length() > 1) {
                    parseError("The opening bracket should be the last element on a line.", filename, nline);
                    return null;
                }
                if ("{".equals(val)) {
                    OTPConfigPreferences child = curr.children.get(key);
                    if (child != null) {
                        parseError(String.format("Multiple definitions of child '%s'.", key), filename, nline);
                        return null;
                    }
                    child = new OTPConfigPreferences(curr, key);
                    curr.children.put(key, child);
                    stack.push(curr);
                    curr = child;
                } else {
                    if (curr.entries.get(key) != null) {
                        parseError(String.format("Multiple definitions of key '%s'.", key), filename, nline);
                        return null;
                    }
                    curr.entries.put(key, val);
                }
            }
            if ( ! stack.isEmpty()) {
                parseError("Bracket mismatch (not enough closing brackets).", filename, nline);
                return null;
            }
            return curr;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void putSpi(String s, String s2) {
        throw new UnsupportedOperationException("read-only preferences implementation.");
    }

    @Override
    protected void removeSpi(String s) {
        throw new UnsupportedOperationException("Read-only preferences implementation.");
    }

    @Override
    protected void removeNodeSpi() throws BackingStoreException {
        throw new UnsupportedOperationException("Read-only preferences implementation.");
    }

    @Override
    protected String[] keysSpi() throws BackingStoreException {
        return entries.keySet().toArray(new String[entries.keySet().size()]);
    }

    @Override
    protected String[] childrenNamesSpi() throws BackingStoreException {
        return children.keySet().toArray(new String[children.keySet().size()]);
    }

    @Override
    protected String getSpi(String s) {
        return entries.get(s);
    }

    @Override
    protected AbstractPreferences childSpi(String name) {
        OTPConfigPreferences child = children.get(name);
        if (child == null || child.isRemoved()) {
            child = new OTPConfigPreferences(this, name);
            children.put(name, child);
        }
        return child;
    }

    @Override
    protected void syncSpi() throws BackingStoreException {
        // Does nothing: read-only
    }

    @Override
    protected void flushSpi() throws BackingStoreException {
        // Does nothing: read-only
    }

    @Override
    public String toString() {
        return toString(0);
    }

    private static int INDENT_WIDTH = 2;
    private String toString (int depth) {
        char[] indent = new char[depth * INDENT_WIDTH];
        Arrays.fill(indent, ' ');
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            sb.append(indent);
            sb.append(entry.getKey());
            sb.append(" ");
            sb.append(entry.getValue());
            sb.append("\n");
        }
        for (Map.Entry<String, OTPConfigPreferences> child : children.entrySet()) {
            sb.append(indent);
            sb.append(child.getKey());
            sb.append(" {\n");
            sb.append(child.getValue().toString(depth + 1));
            sb.append(indent);
            sb.append("}\n");
        }
        return sb.toString();
    }
}
