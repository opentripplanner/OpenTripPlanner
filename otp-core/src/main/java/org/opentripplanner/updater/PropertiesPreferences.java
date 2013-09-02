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

package org.opentripplanner.updater;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * A read-only and .properties-file backed implementation of standard Java "Preferences".
 * 
 * The generated Preference will have nodes based on the dotted-path of properties, like path and
 * filenames.
 * 
 */
public class PropertiesPreferences extends AbstractPreferences {

    private Map<String, String> root;

    private Map<String, PropertiesPreferences> children;

    public PropertiesPreferences(File file) throws IOException {
        this(new FileInputStream(file));
    }

    public PropertiesPreferences(InputStream inputStream) throws IOException {
        super(null, "");
        root = new TreeMap<String, String>();
        children = new TreeMap<String, PropertiesPreferences>();
        Properties properties = new Properties();
        properties.load(inputStream);
        initFromProperties(properties);
    }

    public PropertiesPreferences(Properties properties) {
        super(null, "");
        root = new TreeMap<String, String>();
        children = new TreeMap<String, PropertiesPreferences>();
        initFromProperties(properties);
    }

    private PropertiesPreferences(AbstractPreferences parent, String name, Properties properties) {
        super(parent, name);
        root = new TreeMap<String, String>();
        children = new TreeMap<String, PropertiesPreferences>();
        initFromProperties(properties);
    }

    private void initFromProperties(Properties properties) {
        if (properties == null)
            return;
        String path = getDottedPath();
        final Enumeration<?> pnen = properties.propertyNames();
        Set<String> childrenNodes = new HashSet<String>();
        while (pnen.hasMoreElements()) {
            String key = (String) pnen.nextElement();
            if (key.startsWith(path)) {
                String subKey = key.substring(path.length());
                int dotIndex = subKey.indexOf('.');
                if (dotIndex == -1) {
                    root.put(subKey, properties.getProperty(key));
                } else {
                    String childName = subKey.substring(0, dotIndex);
                    childrenNodes.add(childName);
                }
            }
        }
        for (String childName : childrenNodes) {
            children.put(childName, new PropertiesPreferences(this, childName, properties));
        }
    }

    private String getDottedPath() {
        Preferences cursor = this;
        StringBuffer retval = new StringBuffer();
        while (cursor != null) {
            if (cursor.name().length() > 0)
                retval.insert(0, cursor.name() + ".");
            cursor = cursor.parent();
        }
        return retval.toString();
    }

    @Override
    protected void putSpi(String key, String value) {
        throw new UnsupportedOperationException(
                "Read-only implementation of preferences: putSpi() not supported.");
    }

    @Override
    protected String getSpi(String key) {
        return root.get(key);
    }

    @Override
    protected void removeSpi(String key) {
        throw new UnsupportedOperationException(
                "Read-only implementation of preferences: removeSpi() not supported.");
    }

    @Override
    protected void removeNodeSpi() throws BackingStoreException {
        throw new UnsupportedOperationException(
                "Read-only implementation of preferences: removeNodeSpi() not supported.");
    }

    @Override
    protected String[] keysSpi() throws BackingStoreException {
        return root.keySet().toArray(new String[root.keySet().size()]);
    }

    @Override
    protected String[] childrenNamesSpi() throws BackingStoreException {
        return children.keySet().toArray(new String[children.keySet().size()]);
    }

    @Override
    protected PropertiesPreferences childSpi(String name) {
        PropertiesPreferences child = children.get(name);
        if (child == null || child.isRemoved()) {
            child = new PropertiesPreferences(this, name, null);
            children.put(name, child);
        }
        return child;
    }

    @Override
    protected void syncSpi() throws BackingStoreException {
        // Does nothing: read-only
    }

    protected void flushSpi() throws BackingStoreException {
        // Does nothing: read-only
    }
}
