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
    
    activeLeg : null,
    timeIndex : null,
    
        
    initialize : function(id, module) {
    
        otp.widgets.Widget.prototype.initialize.call(this, id, module.webapp.widgetManager);
        module.addWidget(this);
        
        this.module = module;
        
        this.$().addClass('otp-stopViewer');
        this.$().css('display','none');
        
        this.minimizable = true;
        this.addHeader("Stop Viewer");
        
        var this_ = this;

        this.activeTime = moment();
          
        this.stopInfo = $('<div class="otp-stopViewer-infoRow" />').appendTo(this.$());

        var dateRow = $('<div class="otp-stopViewer-dateRow notDraggable" />').appendTo(this.$()).append('<span>Date: </span>');
        var currentDate = new Date();
        this.lastDate = currentDate;
        this.datePicker = $('<input type="text" style="width:100px;" />').datepicker({
            onSelect: function(date) {
                console.log(date);
                console.log(moment(date)-moment(this_.lastDate));
                
                this_.clearTimes();
                this_.runTimesQuery();
            }
        }).appendTo(dateRow);
        this.datePicker.datepicker("setDate", currentDate);



        this.timesDiv = $("<div class='otp-stopViewer-stopTimes notDraggable'></div>");
        this.timesDiv.appendTo(this.$());        

        $('<div class="otp-stopViewer-stopTimes-advancer" style="left:0px;">&laquo;</div>')
        .appendTo(this.timesDiv)
        .click(function(evt) {
            this_.updateTimes(-1);
        });

        $('<div class="otp-stopViewer-stopTimes-advancer" style="right:0px;">&raquo;</div>')
        .appendTo(this.timesDiv)
        .click(function(evt) {
            this_.updateTimes(1);
        });


        $('<div class="otp-stopTimes-close">[<a href="#">CLOSE</a>]</div>')
        .appendTo(this.$())
        .click(function() {
            this_.$().hide();
        });
        
    },
    
    
    clearTimes : function() {
        this.times = null;
        this.timeIndex = null;
        if(this.rightTime) this.rightTime.remove();
        if(this.centerTime) this.centerTime.remove();
        if(this.leftTime) this.leftTime.remove();    
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
        this.module.webapp.transitIndex.runStopTimesQuery2(this.agencyId, this.stopId, startTime, startTime+86400000, this, function(data) {
            this_.times = [];
            for(var i=0; i < data.stopTimes.length; i++) {
                var time = data.stopTimes[i];
                if(time.phase == "departure") {
                    this_.times.push(time.time*1000);
                }
            }
            this_.updateTimes();
        });        
    },
            
    updateTimes : function(delta) {
        //console.log("uT delta="+delta);
        if(!this.timeIndex) {
            var bestTimeDiff = 1000000000;
            for(var i=0; i<this.times.length; i++) {
                var timeDiff = Math.abs(this.activeTime - this.times[i]);
                if(timeDiff < bestTimeDiff) {
                    this.timeIndex = i;
                    bestTimeDiff = timeDiff;
                }
            }
        }
         
        if(delta && delta == 1) {
            this.timeIndex++;
            this.leftTime.remove();
            this.centerTime.animate({
                left: 40,
                width: 60,
                'font-size': 14,
                'padding-top': 13
            });
            this.leftTime = this.centerTime;
            
            this.rightTime.animate({
                left: 110,
                width: 80,
                'font-size': 20,
                'padding-top': 10
            });
            this.centerTime = this.rightTime;

            this.rightTime = $('<div class="otp-stopViewer-stopTimes-timeBox">' + this.getTime(this.timeIndex+1) + '<div>')
            .css({
                left: 300,
                width: 60,
                'font-size': 14,
                'padding-top': 13
            })
            .appendTo(this.timesDiv)
            .animate({
                left: 200
            });
        }
        else if(delta && delta == -1) {
            this.timeIndex--;
            this.rightTime.remove();
            this.centerTime.animate({
                left: 200,
                width: 60,
                'font-size': 14,
                'padding-top': 13
            });
            this.rightTime = this.centerTime;
            
            this.leftTime.animate({
                left: 110,
                width: 80,
                'font-size': 20,
                'padding-top': 10
            });
            this.centerTime = this.leftTime;

            this.leftTime = $('<div class="otp-stopViewer-stopTimes-timeBox">' + this.getTime(this.timeIndex-1) + '<div>')
            .css({
                left: -60,
                width: 60,
                'font-size': 14,
                'padding-top': 13
            })
            .appendTo(this.timesDiv)
            .animate({
                left: 40
            });
        }
        else {
            this.leftTime = $('<div class="otp-stopViewer-stopTimes-timeBox">' + this.getTime(this.timeIndex-1) + '<div>')
            .css({            
                left: 40,
                width: 60,
                'font-size': 14,
                'padding-top': 13
            })
            .appendTo(this.timesDiv);
           
            this.centerTime = $('<div class="otp-stopViewer-stopTimes-timeBox">' + this.getTime(this.timeIndex) + '<div>')
            .css({            
                left: 110,
                width: 80,
                'font-size': 20,
                'padding-top': 10
            })
            .appendTo(this.timesDiv);
     
            this.rightTime = $('<div class="otp-stopViewer-stopTimes-timeBox">' + this.getTime(this.timeIndex+1) + '<div>')
            .css({
                left: 200,
                width: 60,
                'font-size': 14,
                'padding-top': 13
            })
            .appendTo(this.timesDiv);
        }
    },
    
    getTime : function(index) {
        if(index < 0 || index >= this.times.length) return "";
        return otp.util.Time.formatItinTime(this.times[index], "h:mma");
    }
    
});
