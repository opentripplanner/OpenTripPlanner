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

otp.namespace("otp.widgets");

otp.widgets.StopTimesWidget = 
    otp.Class(otp.widgets.Widget, {

    initialize : function(id, stopID, routeName, stopTimes, highlightTime) {
    
        otp.widgets.Widget.prototype.initialize.apply(this, arguments);
        this.$().addClass('otp-stopTimesWidget');
        this.$().resizable();
        $('<div><b>Route</b>: '+routeName+'</div>').appendTo(this.$());
        $('<div><b>Stop</b>: '+stopID+'</div>').appendTo(this.$());
        var timesDiv = $('<div class="otp-stopTimes-list notDraggable"></div>');
        for(var i=0; i<stopTimes.length; i++) {
            $('<div>'+otp.util.Time.formatItinTime(stopTimes[i], "h:mma")+'</div>').appendTo(timesDiv);
        }
        timesDiv.appendTo(this.$());
        
    }
    
});
