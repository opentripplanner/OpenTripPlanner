/* 
 This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/
package org.opentripplanner.standalone;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.regex.Pattern;

public class NetexParameters {

    private static final String MODULE_FILE_PATTERN = ".*-netex-no\\.zip";

    private static final String SHARED_FILE_PATTHER = "shared-data\\.xml";

    private static final String SHARED_GROUP_FILE_PATTERN = "(\\w{3})-.*-shared\\.xml";

    private static final String GROUP_FILE_PATTERN = "(\\w{3})-.*\\.xml";

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
     * This field is used to match <em>shared files</em> in the module file(zip file entries).
     * Shared files are loaded first. Then the rest of the files are grouped and loaded.
     * <p>
     * The pattern <code>'shared-data\.xml'</code> matches <code>'shared-data.xml'</code>
     * <p>
     * File names are matched in the following order - and treated accordingly to the first match:
     * <ol>
     *     <li>Shared files pattern</li>
     *     <li>Shared group files pattern</li> see {@link #sharedGroupFilePattern}
     *     <li>Group files pattern</li> see {@link #groupFilePattern}
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

    public NetexParameters(JsonNode config) {
        if (config == null) {
            moduleFilePattern = Pattern.compile(MODULE_FILE_PATTERN);
            sharedFilePattern = Pattern.compile(SHARED_FILE_PATTHER);
            sharedGroupFilePattern = Pattern.compile(SHARED_GROUP_FILE_PATTERN);
            groupFilePattern = Pattern.compile(GROUP_FILE_PATTERN);
        } else {
            moduleFilePattern = pattern("moduleFilePattern", MODULE_FILE_PATTERN, config);
            sharedFilePattern = pattern("sharedFilePattern", SHARED_FILE_PATTHER, config);
            sharedGroupFilePattern = pattern("sharedGroupFilePattern", SHARED_GROUP_FILE_PATTERN, config);
            groupFilePattern = pattern("groupFilePattern", GROUP_FILE_PATTERN, config);
        }
    }

    private static Pattern pattern(String path, String defaultValue, JsonNode config) {
        return Pattern.compile(config.path(path).asText(defaultValue));
    }

    public boolean moduleFileMatches(String name) {
        return moduleFilePattern.matcher(name).matches();
    }
}
