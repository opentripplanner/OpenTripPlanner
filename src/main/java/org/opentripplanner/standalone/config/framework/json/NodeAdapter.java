package org.opentripplanner.standalone.config.framework.json;

import static org.opentripplanner.standalone.config.framework.json.NodeInfo.SOURCETYPE_QUALIFIER;
import static org.opentripplanner.standalone.config.framework.json.NodeInfo.TYPE_QUALIFIER;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opentripplanner.framework.application.OtpAppException;

/**
 * This class wrap a {@link JsonNode} and decorate it with type-safe parsing of types used in OTP
 * like enums, date, time, URIs and so on. By wrapping the JsonNode we get consistent parsing rules
 * and the possibility to log unused parameters when the end of parsing a file. Also, the
 * configuration POJOs become cleaner because they do not have any parsing logic in them anymore.
 * <p>
 * This class has 100% test coverage - keep it that way, for the individual configuration POJOs a
 * smoke test is good enough.
 */
public class NodeAdapter {

  private final JsonNode json;

  /**
   * The source is the origin of the configuration. The source can be "DEFAULT", the name of the
   * JSON source files or the "SerializedGraph".
   */
  private final String source;

  /**
   * This class wrap a {@link JsonNode} which might be a child of another node. We keep the path
   * string for logging and debugging purposes
   */
  private final String contextPath;

  /**
   * The map of all children by their name. It is used to be able to produce a list of unused
   * parameters for all children after the parsing is complete.
   */
  private final Map<String, NodeAdapter> childrenByName = new HashMap<>();

  /**
   * A map of all configured parameters for this node. The current JSON document that is parsed
   * may or may not contain the parameter in this map. The map holds information about the
   * parameter and can be used for generating documentation and such.
   */
  private final Map<String, NodeInfo> parameters = new HashMap<>();

  /**
   * All warnings are collected during the file parsing and printed as one block at the
   * end.
   */
  private final List<String> warnings = new ArrayList<>();

  private boolean usedAsRaw = false;

  private final int level;

  private NodeAdapter(@Nonnull JsonNode node, String source, String contextPath, int level) {
    this.json = node;
    this.source = source;
    this.contextPath = contextPath;
    this.level = level;
  }

  public NodeAdapter(@Nonnull JsonNode node, String source) {
    this(node, source, null, 0);
  }

  /**
   * Constructor for nested configuration nodes.
   */
  private NodeAdapter(@Nonnull JsonNode node, @Nonnull NodeAdapter parent, String paramName) {
    this(node, parent.source, parent.fullPath(paramName), parent.level + 1);
    parent.childrenByName.put(paramName, this);
  }

  public String contextPath() {
    return contextPath;
  }

  public String source() {
    return source;
  }

  public boolean isNonEmptyArray() {
    return json.isArray() && json.size() > 0;
  }

  public boolean isObject() {
    return json.isObject() && json.size() > 0;
  }

  public List<NodeInfo> parametersSorted() {
    return parameters.values().stream().sorted().toList();
  }

  /** Get a child by name. The child must exist. */
  public NodeAdapter child(String paramName) {
    return childrenByName.get(paramName);
  }

  /** Create new parameter, a builder is returned. */
  public ParameterBuilder of(String paramName) {
    return new ParameterBuilder(this, paramName);
  }

  public boolean isEmpty() {
    return json.isMissingNode();
  }

  /** Delegates to {@link JsonNode#has(String)} */
  public boolean exist(String paramName) {
    return json.has(paramName);
  }

  /**
   * WARNING! Avoid using this method - it bypasses the build in typesafe parsing support. Only
   * use it to provide custom parsing.
   */
  public String asText() {
    return json.asText();
  }

  /** List all present parameters by name */
  public Iterator<String> parameterNames() {
    return json.fieldNames();
  }

  /**
   * List all children present in the JSON document.
   */
  public Iterator<String> listExistingChildNodes() {
    return json.fieldNames();
  }

  /**
   * List all children parsed - this includes arrays elements.
   */
  public List<String> listChildrenByName() {
    return childrenByName.keySet().stream().sorted().toList();
  }

  /**
   * Return the value of the type qualifier or throws an exception if it does not exist.
   */
  public String typeQualifier() {
    assertRequiredFieldExist(TYPE_QUALIFIER);
    return json.path(TYPE_QUALIFIER).asText();
  }

  public String sourceTypeQualifier() {
    assertRequiredFieldExist(SOURCETYPE_QUALIFIER);
    return json.path(SOURCETYPE_QUALIFIER).asText();
  }

  /**
   * Log unused parameters and other warnings for the entire configuration file/node tree. Only
   * call this method for the root adapter, once for each config file read.
   */
  public void logAllWarnings(Consumer<String> logger) {
    for (String p : unusedParams()) {
      logger.accept("Unexpected config parameter: '" + p + "' in '" + source + "'");
    }
    allWarnings().forEach(logger);
  }

  /**
   * Be careful when using this method - this bypasses the NodeAdaptor, and we loose
   * track of unused parameters and cannot generate documentation for the children.
   * <p>
   * OTP will no longer WARN about unused parameters for this node.
   */
  public JsonNode rawNode() {
    this.usedAsRaw = true;
    return json;
  }

  /**
   * Return the level for this node, relative to root of the document. Root is at level zero,
   * roots children are at level one, and so on.
   */
  public int level() {
    return level;
  }

  /**
   * Used by {@link ParameterBuilder} to skip one node in the node tree. This method
   * does work with the unused parameters.
   */
  JsonNode rawNode(String paramName) {
    parameters.put(paramName, NodeInfo.ofSkipChild(paramName));
    return json.path(paramName);
  }

  /** Return the node as a JSON string. */
  public String toJson() {
    return json.toString();
  }

  /** Return the node as a pretty JSON string. */
  public String toPrettyString() {
    return json.toPrettyString();
  }

  NodeAdapter path(String paramName, JsonNode node) {
    if (childrenByName.containsKey(paramName)) {
      return childrenByName.get(paramName);
    }
    return new NodeAdapter(node, this, paramName);
  }

  /**
   * Create an undocumented child, version is required.
   */
  NodeAdapter pathUndocumentedChild(String paramName, OtpVersion since) {
    return of(paramName).since(since).summary("NA").asObject();
  }

  /* private methods */

  /**
   * This method list all unused parameters(full path), also nested ones. It uses recursion to get
   * child nodes.
   */
  private List<String> unusedParams() {
    if (usedAsRaw) {
      return List.of();
    }

    List<String> unusedParams = new ArrayList<>();
    Iterator<String> it = json.fieldNames();
    Set<String> parameterNames = parameters.keySet();

    while (it.hasNext()) {
      String fieldName = it.next();
      if (!parameterNames.contains(fieldName)) {
        unusedParams.add(fullPath(fieldName) + ":" + json.get(fieldName));
      }
    }

    for (NodeAdapter c : childrenByName.values()) {
      // Recursive call to get child unused parameters
      unusedParams.addAll(c.unusedParams());
    }
    unusedParams.sort(String::compareTo);
    return unusedParams;
  }

  /**
   * This method validate the given parameter info and save it to the list of parameters.
   * The JSON node is for this parameter is returned, if the node do not exist a "missing node"
   * (node that returns true for isMissingNode) will be returned.
   */
  JsonNode addAndValidateParameterNode(NodeInfo info) {
    addParameterInfo(info);

    if (info.required()) {
      assertRequiredFieldExist(info.name());
    }
    return json.path(info.name());
  }

  public String fullPath(String paramName) {
    return contextPath == null ? paramName : concatPath(contextPath, paramName);
  }

  String concatPath(String a, String b) {
    return a + "." + b;
  }

  /**
   * Add a warning to the list of warnings logged after parsing a config file is
   * complete.
   */
  public void addWarning(String message, String paramName) {
    message += " Parameter: " + fullPath(paramName) + ".";
    message += " Source: " + source + ".";
    warnings.add(message);
  }

  /**
   * Return a {@link OtpAppException}. The full path and source is injected into the message if
   * replacing the placeholders {@code "{path}"}, if they exist.
   */
  public OtpAppException createException(String message, String paramName) {
    message += " Parameter: " + fullPath(paramName) + ".";
    message += " Source: " + source + ".";
    return new OtpAppException(message);
  }

  /**
   * Same as {@link #createException(String, String)} with the given cause is appended to the
   * message.
   */
  public OtpAppException createException(String message, String paramName, Exception cause) {
    message += " Details: " + cause.getMessage();
    return createException(message, paramName);
  }

  /* private methods */

  private Stream<String> allWarnings() {
    Stream<String> childrenWarnings = childrenByName
      .values()
      .stream()
      .flatMap(NodeAdapter::allWarnings);
    return Stream.concat(childrenWarnings, warnings.stream());
  }

  private void addParameterInfo(NodeInfo info) {
    if (parameters.containsKey(info.name())) {
      assertParameterInfoIsEqual(info);
    } else {
      parameters.put(info.name(), info);
    }
  }

  private void assertRequiredFieldExist(String paramName) {
    if (!exist(paramName)) {
      throw requiredFieldMissingException(paramName);
    }
  }

  private void assertParameterInfoIsEqual(NodeInfo info) {
    var other = parameters.get(info.name());
    if (!other.equals(info)) {
      throw new OtpAppException(
        "Two different parameter definitions exist: " + other + " != " + info
      );
    }
  }

  private OtpAppException requiredFieldMissingException(String paramName) {
    return new OtpAppException(
      "Required parameter '" + fullPath(paramName) + "' not found in '" + source + "'."
    );
  }
}
