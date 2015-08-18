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
            //TRANSLATORS: widget title
            title : _tr('Stop Viewer'),
            cssClass : 'otp-stopViewer',
            closeable : true,
            resizable : true,
            openInitially : false,
            persistOnClose : true,
        });

        this.module = module;

        var this_ = this;

        this.activeTime = moment().unix() * 1000;

        this.stopFinder = new otp.widgets.transit.StopFinderWidget(this.module.id + "-stopFinder", this.module, this);

        var translated_template = {
            //TRANSLATORS: Date: date chooser (In stop viewer)
            date: _tr('Date'),
            //TRANSLATORS: Button
            find_stops: _tr('Find Stops'),
            //TRANSLATORS: When no public transit stops were selected in stop viewer
            no_stops_selected: _tr('(No Stop Selected)'),

        }

        ich['otp-stopViewer'](translated_template).appendTo(this.mainDiv);

        this.timeList = this.mainDiv.find(".otp-stopViewer-timeList");
        this.stopInfo = this.mainDiv.find(".otp-stopViewer-stopInfo");

        var currentDate = new Date();
        this.datePicker = this.mainDiv.find(".otp-stopViewer-dateInput");
        this.datePicker.datepicker({
            onSelect: function(date) {
                var hrs = moment(this_.activeTime).hours();
                var mins = moment(this_.activeTime).minutes();
                this_.activeTime = moment(date).add('hours', hrs).add('minutes', mins).unix() * 1000;
                this_.clearTimes();
                this_.runTimesQuery();
            }
        });
        this.datePicker.datepicker("setDate", currentDate);

        this.mainDiv.find(".otp-stopViewer-findButton").click(function() {
            this_.stopFinder.show();
            this_.stopFinder.bringToFront();
        });

    },

    clearTimes : function() {
        this.times = null;
        this.timeIndex = null;
        this.timeList.empty();
    },

    setStop : function(stopId, stopName) {
        this.stopId = stopId;
        this.clearTimes();
        //TRANSLATORS: Public transport <Stop> (stop name)
        this.stopInfo.html("<b>" + _tr("Stop") + ":</b> " + stopName + " ("  + stopId + ")");
        this.runTimesQuery();
    },

    runTimesQuery : function() {
        var this_ = this;
        //var startTime = moment(this.datePicker.val(), otp.config.locale.time.date_format).add("hours", -otp.config.timeOffset).unix();
        this.module.webapp.indexApi.runStopTimesQuery(this.stopId, this.datePicker.datepicker("getDate"), this, function(data) {
            this_.times = [];
            // rearrange stoptimes, flattening and sorting;
            _.each(data, function(stopTime){
                //extract routeId from pattern.id, which is in the form of agency_d:route_id:direction_id:count
                var parts = stopTime.pattern.id.split(":");
                var routeId = parts[0] + ":" + parts[1];
                _.each(stopTime.times,function(time){
                    var pushTime = {};
                    pushTime.routeShortName = this_.module.webapp.indexApi.routes[routeId].routeData.shortName;
                    pushTime.routeLongName = this_.module.webapp.indexApi.routes[routeId].routeData.longName;
                    pushTime.time = time.realtimeDeparture;
                    pushTime.serviceDay = time.serviceDay;
                    this_.times.push(pushTime);
                });
            });
            this_.times.sort(function(a,b){return a.time-b.time});
            this_.updateTimes();
        });
    },

    updateTimes : function() {
        var minDiff = 1000000000;
        var bestIndex = 0;
        var to_trans = pgettext('bus_direction', " to ");
        //TRANSLATORS: Trip block (A block consists of two or more
        //sequential trips made using the same vehicle, where a passenger
        //can transfer from one trip to the next just by staying in the
        //vehicle.)
        var block_trans = _tr('Block');

        for(var i = 0; i < this.times.length; i++) {
            var time = this.times[i];
            //time.formattedTime = otp.util.Time.formatItinTime(time.time*1000, otp.config.locale.time.time_format);
            time.formattedTime = moment.utc(time.time*1000).format(otp.config.locale.time.time_format);
            //FIXME: There is probably a better way to translate to and block
            //then in each call separately
            time.to = to_trans;
            time.block = block_trans;
            ich['otp-stopViewer-timeListItem'](time).appendTo(this.timeList);
            var diff = Math.abs(this.activeTime - (time.time + time.serviceDay)*1000);
            if(diff < minDiff) {
                minDiff = diff;
                bestIndex = i;
            }
        }
        this.timeList.scrollTop(this.timeList.find(".otp-stopViewer-timeListItem")[bestIndex].offsetTop);
    },

    setActiveTime : function(activeTime) {
        this.activeTime = activeTime;
        this.datePicker.datepicker("setDate", new Date(activeTime));
    },

});
