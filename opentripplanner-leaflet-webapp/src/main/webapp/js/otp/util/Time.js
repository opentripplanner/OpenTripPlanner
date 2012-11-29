/* This program is free software: you can redistribute it and/or
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

otp.namespace("otp.util");

/**
 * Utility routines for date/time conversion
 */
 
otp.util.Time = {

    msToHrMin : function(ms) {
        var hrs = Math.floor(ms / 3600000);
        var mins = Math.floor(ms / 60000) % 60;
        
        // TODO: localization
        var str = (hrs > 0 ? (hrs +" hr, ") : "") + mins + " min";
    
        return str;
    },
    
    formatItinTime : function(timestamp, formatStr) {
        formatStr = formatStr || otp.config.timeFormat+", "+otp.config.dateFormat;
        return moment(timestamp).add("hours", otp.config.timeOffset).format(formatStr);
    },

    CLASS_NAME: "otp.util.Time"
};
