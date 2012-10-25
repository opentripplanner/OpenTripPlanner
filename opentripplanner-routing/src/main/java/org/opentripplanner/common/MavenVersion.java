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

package org.opentripplanner.common;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenVersion implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(MavenVersion.class);
    public static final MavenVersion VERSION = fromProperties();
    private static final long serialVersionUID = VERSION.getUID();

    public final String version; 
    public final int major;
    public final int minor;
    public final int incremental;
    public final String qualifier;
    public final String commit;
    
    private static MavenVersion fromProperties() {
        final String FILE = "maven-version.properties";
        try {
            Properties props = new java.util.Properties();
            InputStream in = MavenVersion.class.getClassLoader().getResourceAsStream(FILE);
            props.load(in);
            MavenVersion version = new MavenVersion(props.getProperty("project.version"), 
                                                    props.getProperty("git.commit.id"));
            LOG.info("Parsed Maven artifact version: {}", version.toStringVerbose());
            return version;
        } catch (Exception e) {
            LOG.error("Error reading version from properties file: {}", e.getMessage());
            return new MavenVersion();
        }
    }
    
    private MavenVersion () {
        // JAXB Marshalling requires classes to have a 0-arg constructor and mutable fields.
        // otherwise it throws a com.sun.xml.bind.v2.runtime.IllegalAnnotationsException.
        // It is protecting you against yourself, since you might someday want to
        // unmarshal that same object in Java.
        // The 'proper' way of handling this is to make a mutable equivalent of your class,
        // plus another adapter class that converts between the mutable and immutable 
        // versions. It would be absurd to produce so much boilerplate and verbosity in this
        // situation, so I am providing a 0-arg constructor with a totally different role: 
        // generating a default version when OTP encounters a problem parsing the
        // maven-version.properties file.
        this("0.0.0-ParseFailure", "0");
    }
    
    public MavenVersion (String v, String c) {
        version = v;
        String [] fields = v.split("\\-");
        if (fields.length > 1)
            qualifier = fields[1];
        else
            qualifier = "";
        fields = fields[0].split("\\.");
        if (fields.length > 0)
            major = Integer.parseInt(fields[0]);
        else
            major = 0;
        if (fields.length > 1)
            minor = Integer.parseInt(fields[1]);
        else
            minor = 0;
        if (fields.length > 2)
            incremental = Integer.parseInt(fields[2]);
        else
            incremental = 0;
        commit = c;
    }
    
    public long getUID() {
        return (long) hashCode();
    }

    public String toString() {
        return String.format("MavenVersion(%d, %d, %d, %s, %s)", 
               major, minor, incremental, qualifier, commit);
    }

    public String toStringVerbose() {
        return String.format("%s => %s UID=%d", version, this.toString(), getUID());
    }

    public int hashCode () {
        return (qualifier.equals("SNAPSHOT") ? -1 : +1) *
                (1000000 * major + 1000 * minor + incremental);
    }

    public boolean equals (Object other) {
        if ( ! (other instanceof MavenVersion))
            return false;
        MavenVersion that = (MavenVersion) other;
        return this.major == that.major &&
               this.minor == that.minor &&
               this.incremental == that.incremental &&
               this.qualifier.equals(that.qualifier);
    }
}
