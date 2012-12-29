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

    initialize : function(id, stopID, routeName, stopTimes, highlightTime, module) {
    
        otp.widgets.Widget.prototype.initialize.call(this, id, module);
        this.$().addClass('otp-stopTimesWidget');
        
        $('<div><b>Route</b>: '+routeName+'</div>').appendTo(this.$());
        $('<div><b>Stop</b>: '+stopID+'</div>').appendTo(this.$());
        
        var timesDiv = $('<div class="otp-stopTimes-list notDraggable"></div>');
        var highlightIndex = 0;
        for(var i=0; i<stopTimes.length; i++) {
            var html = '<div class="otp-stopTimes-list-item">';
            if(stopTimes[i] == highlightTime) html += "<b>";
            html += otp.util.Time.formatItinTime(stopTimes[i], "h:mma")
            html += '</div>';
            if(stopTimes[i] == highlightTime) html += "</b>";
            $(html).appendTo(timesDiv);
            if(stopTimes[i] == highlightTime) highlightIndex = i;
        }
        timesDiv.appendTo(this.$());
        console.log("scr h="+timesDiv[0].scrollHeight);
        console.log(timesDiv[0]);
        console.log("pct="+highlightIndex/stopTimes.length);
        var scrollY = timesDiv[0].scrollHeight*highlightIndex/stopTimes.length
        if(highlightIndex>0) scrollY = scrollY - timesDiv.height()/2 + 10;
        timesDiv.scrollTop(scrollY);
        
        this.$().resizable({
            alsoResize: timesDiv,
        });

        var this_ = this;
        $('<div class="otp-stopTimes-close">[<a href="#">CLOSE</a>]</div>')
        .appendTo(this.$())
        .click(function() {
            this_.$().hide();
        });
        
    }
    
});
