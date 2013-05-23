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

package org.opentripplanner.graph_builder.impl;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LoggerAppenderProvider {

    private static org.slf4j.Logger _log = LoggerFactory.getLogger(PruneFloatingIslands.class);

    public static String createCsvFile4LoggerCat(String fileName, String categoryName) {
        if (fileName.isEmpty()) return null;
        try{
            PatternLayout layout =  new PatternLayout("%m\n");
            FileAppender appender = new RollingFileAppender(layout, fileName, false);
            Logger.getLogger(categoryName).addAppender(appender);
        }catch (IOException ioe){
            _log.warn("could not create file appender for " + fileName + " duo to: " + ioe.getMessage());
            return null;
        }
        return categoryName;
    }

}
