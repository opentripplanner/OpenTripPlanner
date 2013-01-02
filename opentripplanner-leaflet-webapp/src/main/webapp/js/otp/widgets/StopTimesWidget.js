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

    initialize : function(id, widgetManager) {
    
        otp.widgets.Widget.prototype.initialize.call(this, id, widgetManager);
        this.$().addClass('otp-stopTimesWidget');
        this.$().css('display','none');
        
        this.routeLabel = $('<div></div>').appendTo(this.$());
        this.stopLabel = $('<div></div>').appendTo(this.$());
        
        this.timesDiv = $('<div class="otp-stopTimes-list notDraggable"></div>').appendTo(this.$());
        
        this.$().resizable({
            alsoResize: this.timesDiv,
        });

        var this_ = this;
        $('<div class="otp-stopTimes-close">[<a href="#">CLOSE</a>]</div>')
        .appendTo(this.$())
        .click(function() {
            this_.$().hide();
        });
        
    },
    
    update : function(stopID, routeName, stopTimes, highlightTime) {
        this.routeLabel.html('<b>Route</b>: '+routeName);
        this.stopLabel.html('<b>Stop</b>: '+stopID);

        this.timesDiv.empty();
        var highlightIndex = 0;
        for(var i=0; i<stopTimes.length; i++) {
            var html = '<div class="otp-stopTimes-list-item">';
            if(stopTimes[i] == highlightTime) html += "<b>";
            html += otp.util.Time.formatItinTime(stopTimes[i], "h:mma")
            html += '</div>';
            if(stopTimes[i] == highlightTime) html += "</b>";
            $(html).appendTo(this.timesDiv);
            if(stopTimes[i] == highlightTime) highlightIndex = i;
        }

        console.log("scr h="+this.timesDiv[0].scrollHeight);
        console.log(this.timesDiv[0]);
        console.log("pct="+highlightIndex/stopTimes.length);

        var scrollY = this.timesDiv[0].scrollHeight*highlightIndex/stopTimes.length
        if(highlightIndex>0) scrollY = scrollY - this.timesDiv.height()/2 + 10;
        this.timesDiv.scrollTop(scrollY);
    }
    
});
