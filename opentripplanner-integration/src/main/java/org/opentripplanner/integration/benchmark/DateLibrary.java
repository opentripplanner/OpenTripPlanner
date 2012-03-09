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

package org.opentripplanner.integration.benchmark;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateLibrary {

  private static DateLibrary _instance;
// Something very close to ISO 8601 time format
  private final SimpleDateFormat _format = new SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ssZ");

  public static String getTimeAsIso8601String(Date date) {
    String timeString = getInstance()._format.format(date);
    return timeString.substring(0, timeString.length() - 2) + ":"
        + timeString.substring(timeString.length() - 2);
  }

  public static Date getIso8601StringAsDate(String value)
      throws java.text.ParseException {
    int index = value.lastIndexOf(':');
    if (index == value.length() - 3)
      value = value.substring(0, index) + value.substring(index + 1);
    return getInstance()._format.parse(value);
  }

  private static synchronized DateLibrary getInstance() {
    if (_instance == null) {
        _instance = new DateLibrary();
    }
    return _instance;
  }

}
