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

otp.namespace("otp.widgets.transit");

otp.widgets.transit.StopViewerWidget = 
    otp.Class(otp.widgets.Widget, {

    module : null,

    agency_id : null,
    
    timeIndex : null,
    
        
    initialize : function(id, module) {
    
        otp.widgets.Widget.prototype.initialize.call(this, id, module, {
            title : 'Stop Viewer',
            cssClass : 'otp-stopViewer',
            closeable : true,
            resizable : true,
            openInitially : false,
            persistOnClose : true,
        });
        
        this.module = module;
        
        var this_ = this;

        this.activeTime = moment();
          
        ich['otp-stopViewer']({}).appendTo(this.mainDiv);
        
        this.timeList = this.mainDiv.find(".otp-stopViewer-timeList");
        this.stopInfo = this.mainDiv.find(".otp-stopViewer-stopInfo");
        
        var currentDate = new Date();
        this.datePicker = this.mainDiv.find(".otp-stopViewer-dateInput");
        this.datePicker.datepicker({
            onSelect: function(date) {
                this_.activeTime = moment(date).unix()*1000 + moment(this_.activeTime).hours()*3600000 + moment(this_.activeTime).minutes()*60000;
                this_.clearTimes();
                this_.runTimesQuery();
            }
        });
        this.datePicker.datepicker("setDate", currentDate);
        
        
    },

    clearTimes : function() {
        this.times = null;
        this.timeIndex = null;
        this.timeList.empty();
    },
            
    setStop : function(agencyId, stopId, stopName) {
        this.agencyId = agencyId;
        this.stopId = stopId;
        this.clearTimes();
        this.stopInfo.html("<b>Stop:</b> " + stopName + " (" + agencyId + " #" + stopId + ")");
        this.runTimesQuery();
    },
    
    runTimesQuery : function() {
        var this_ = this;
        var startTime = moment(this.datePicker.val()).add("hours", -otp.config.timeOffset).unix()*1000;
        this.module.webapp.transitIndex.runStopTimesQuery2(this.agencyId, this.stopId, startTime+10800000, startTime+97200000, this, function(data) {
            this_.times = [];
            for(var i=0; i < data.stopTimes.length; i++) {
                var time = data.stopTimes[i];
                if(time.phase == "departure") {
                    this_.times.push(time);
                }
            }
            this_.updateTimes();
        });        
    },
      
    updateTimes : function() {
        var minDiff = 1000000000;
        var bestIndex = 0;
        for(var i = 0; i < this.times.length; i++) {
            var time = this.times[i];
            time.formattedTime = otp.util.Time.formatItinTime(time.time * 1000, "h:mma");
            ich['otp-stopViewer-timeListItem'](time).appendTo(this.timeList);
            var diff = Math.abs(this.activeTime - time.time*1000);
            if(diff < minDiff) {
                minDiff = diff;
                bestIndex = i;
            }
        }
        this.timeList.scrollTop(((bestIndex/this.times.length) * this.timeList[0].scrollHeight) - this.timeList.height()/2 + $(this.timeList.find(".otp-stopViewer-timeListItem")[0]).height()/2);
    },
    
    setActiveTime : function(activeTime) {
        this.activeTime = activeTime;
        this.datePicker.datepicker("setDate", new Date(activeTime));
    },
    
});
