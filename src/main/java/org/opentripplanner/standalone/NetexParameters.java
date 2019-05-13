package org.opentripplanner.standalone;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.regex.Pattern;

public class NetexParameters {

    private static final String EMPTY_STRING_PATTERN = "$^";

    private static final String MODULE_FILE_PATTERN = ".*-netex-no\\.zip";

    private static final String IGNORE_FILE_PATTERN = EMPTY_STRING_PATTERN;

    private static final String SHARED_FILE_PATTERN = "shared-data\\.xml";

    private static final String SHARED_GROUP_FILE_PATTERN = "(\\w{3})-.*-shared\\.xml";

    private static final String GROUP_FILE_PATTERN = "(\\w{3})-.*\\.xml";

    private static final String NETEX_FEED_ID = "DefaultFeed";

    /**
     * This field is used to identify the specific NeTEx feed. It is used instead of the feed_id field in GTFS file
     * feed_info.txt.
     */

    public final String netexFeedId;

    /**
     * This field is used to identify Netex module (zip) files. The format is
     * a regular expression. The regular expression should match the name
     * of the file including file extension, but not the file path.
     * <p>
     * The pattern <code>'.*-netex-no\.zip'</code> matches <code>'norway-aggregated-netex-no.zip'</code>
     * <p>
     * Default value is <code>'.*-netex-no\.zip'</code>
     */
    public final Pattern moduleFilePattern;

    /**
     * This field is used to exclude matching <em>files</em> in the module file(zip file entries).
     * The <em>ignored</em> files are <em>not</em> loaded.
     * <p>
     * Default value is <code>'$^'</code> witch matches empty stings (not a valid file name).
     * @see #sharedFilePattern
     */
    public final Pattern ignoreFilePattern;

    /**
     * This field is used to match <em>shared files</em>(zip file entries) in the module file.
     * Shared files are loaded first. Then the rest of the files are grouped and loaded.
     * <p>
     * The pattern <code>'shared-data\.xml'</code> matches <code>'shared-data.xml'</code>
     * <p>
     * File names are matched in the following order - and treated accordingly to the first match:
     * <ol>
     *     <li>{@link #ignoreFilePattern}</li>
     *     <li>Shared file pattern (this)</li>
     *     <li>{@link #sharedGroupFilePattern}.</li>
     *     <li>{@link #groupFilePattern}.</li>
     * </ol>
     * <p>
     * Default value is <code>'shared-data\.xml'</code>
     */
    public final Pattern sharedFilePattern;

    /**
     * This field is used to match <em>shared group files</em> in the module file(zip file entries).
     * Typically this is used to group all files from one agency together.
     * <p>
     * <em>Shared group files</em> are loaded after shared files, but before the matching group
     * files. Each <em>group</em> of files are loaded as a unit, followed by next group.
     * <p>
     * Files are grouped together by the first group pattern in the regular expression.
     * <p>
     * The pattern <code>'(\w{3})-.*-shared\.xml'</code> matches <code>'RUT-shared.xml'</code> with
     * group <code>'RUT'</code>.
     * <p>
     * Default value is <code>'(\w{3})-.*-shared\.xml'</code>
     * @see #sharedFilePattern
     * @see #groupFilePattern
     */
    public final Pattern sharedGroupFilePattern;

    /**
     * This field is used to match <em>group files</em> in the module file(zip file entries).
     * <em>group files</em> are loaded right the after <em>shared group files</em> are loaded.
     * <p>
     * Files are grouped together by the first group pattern in the regular expression.
     * <p>
     * The pattern <code>'(\w{3})-.*\.xml'</code> matches <code>'RUT-Line-208-Hagalia-Nevlunghavn.xml'</code> with
     * group <code>'RUT'</code>.
     * <p>
     * Default value is <code>'(\w{3})-.*\.xml'</code>
     * @see #sharedFilePattern
     * @see #sharedGroupFilePattern
     */
    public final Pattern groupFilePattern;

    NetexParameters(JsonNode config) {
        moduleFilePattern = pattern("moduleFilePattern", MODULE_FILE_PATTERN, config);
        ignoreFilePattern = pattern("ignoreFilePattern", IGNORE_FILE_PATTERN, config);
        sharedFilePattern = pattern("sharedFilePattern", SHARED_FILE_PATTERN, config);
        sharedGroupFilePattern = pattern("sharedGroupFilePattern", SHARED_GROUP_FILE_PATTERN, config);
        groupFilePattern = pattern("groupFilePattern", GROUP_FILE_PATTERN, config);
        netexFeedId = text("netexFeedId", NETEX_FEED_ID, config);
    }

    private static Pattern pattern(String path, String defaultValue, JsonNode config) {
        return Pattern.compile(text(path, defaultValue, config));
    }

    private static String text(String path, String defaultValue, JsonNode config) {
        return config == null ? defaultValue : config.path(path).asText(defaultValue);
    }

    public boolean moduleFileMatches(String name) {
        return moduleFilePattern.matcher(name).matches();
    }
}
