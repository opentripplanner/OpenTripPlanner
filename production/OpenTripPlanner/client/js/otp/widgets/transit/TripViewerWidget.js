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

otp.widgets.transit.TripViewerWidget =
    otp.Class(otp.widgets.transit.RouteBasedWidget, {

    module : null,

    agency_id : null,

    activeLeg : null,
    timeIndex : null,

    routeLookup : [], // for retrieving route obj from numerical index in <select> element

    lastSize : null,
    //variantIndexLookup : null,

    initialize : function(id, module) {

        otp.widgets.transit.RouteBasedWidget.prototype.initialize.call(this, id, module, {
            title : _tr('Trip Viewer'),
            cssClass : 'otp-tripViewer',
            closeable : true,
            openInitially : false,
            persistOnClose : true,
        });

        this.module = module;

        var this_ = this;

        this.stopList = $('<div class="otp-tripViewer-stopList notDraggable" />').appendTo(this.mainDiv);

        this.scheduleLink = $('<div class="otp-tripViewer-scheduleLink notDraggable" />').appendTo(this.mainDiv);

        console.log("added sched link");
        this.mainDiv.resizable({
            minWidth: 200,
            alsoResize: this.stopList,
        });

    },


    clear : function() {
        this.stopList.empty();
    },

    variantSelected : function(variantData) {
        //console.log("var sel");
        //console.log(variantData);
        var this_ = this;
        this.stopList.empty();
        var selectedStopIndex = 0;
        for(var i=0; i<this.activeVariant.stops.length; i++) {
            var stop = this.activeVariant.stops[i];

            var row = $('<div class="otp-tripViewer-stopRow" />').appendTo(this.stopList);

            var stopIcon = $('<div style="width: 30px; height: 32px; overflow: hidden; float:left; margin-left: 2px;" />').appendTo(row);

            // use the appropriate line/stop graphic
            var lineImg;
            if(i == 0) {
                lineImg = $('<img src="images/widget-trip-stop-first.png" />');
            }
            else if(i == this.activeVariant.stops.length - 1) {
                lineImg = $('<img src="images/widget-trip-stop-last.png" />');
            }
            else {
                lineImg = $('<img src="images/widget-trip-stop-middle.png" />');
            }

            // append the arrow for the board/alight stop, if applicable
            if(this.activeLeg && i == this.activeLeg.from.stopIndex) {
                $('<img src="images/mode/arrow.png" style="margin: 8px 2px;" />').appendTo(stopIcon);
            }
            else if(this.activeLeg && i == this.activeLeg.to.stopIndex) {
                $('<img src="images/mode/arrow-left.png" style="margin: 8px 2px;" />').appendTo(stopIcon);
            }
            else {
                lineImg.css({ marginLeft : 12 });
            }

            lineImg.appendTo(stopIcon);

            // set up the stop name and id/links content
            var stopText = $('<div style="margin-left: 40px" />').appendTo(row);
            $('<div class="otp-tripViewer-stopRow-name"><b>'+(i+1)+'.</b> '+stop.name+'</div>').appendTo(stopText);
            var idLine = $('<div class="otp-tripViewer-stopRow-idLine" />').appendTo(stopText);
            var idHtml = '<span><i>';
            if(stop.url) idHtml += '<a href="'+stop.url+'" target="_blank">';
            idHtml += stop.id; //.agencyId+' #'+stop.id.id;
            if(stop.url) idHtml += '</a>';
            idHtml += '</i></span>'
            $(idHtml).appendTo(idLine);

            //TRANSLATORS: Recenter map on this stop (Shown at each stop in
            //Trip viewer
            $('<span>&nbsp;[<a href="#">' + _tr('Recenter') + '</a>]</span>').appendTo(idLine)
            .data("stop", stop)
            .click(function(evt) {
                var stop = $(this).data("stop");
                this_.module.webapp.map.lmap.panTo(new L.LatLng(stop.lat, stop.lon));
            });
            //TRANSLATORS: Link to Stop viewer (Shown at each stop in Trip
            //viewer)
            $('<span>&nbsp;[<a href="#">' + _tr('Viewer') + '</a>]</span>').appendTo(idLine)
            .data("stop", stop)
            .click(function(evt) {
                var stop = $(this).data("stop");
                if(!this_.module.stopViewerWidget) {
                    this_.module.stopViewerWidget = new otp.widgets.transit.StopViewerWidget("otp-"+this_.module.id+"-stopViewerWidget", this_.module);
                    this_.module.stopViewerWidget.mainDiv.offset({top: evt.clientY, left: evt.clientX});
                }
                this_.module.stopViewerWidget.show();
                //this_.module.stopViewerWidget.activeTime = leg.startTime;
                this_.module.stopViewerWidget.setStop(stop.id, stop.name);
                this_.module.stopViewerWidget.bringToFront();
            });

            // highlight the boarded stops
            if(this.activeLeg && i >= this.activeLeg.from.stopIndex && i <= this.activeLeg.to.stopIndex) {
                stopIcon.css({ background : '#bbb' });
            }

            // set up hover functionality (open popup over stop)
            row.data("stop", stop).hover(function(evt) {
                var stop = $(this).data("stop");
                var latLng = new L.LatLng(stop.lat, stop.lon);
                if(!this_.module.webapp.map.lmap.getBounds().contains(latLng)) return;
                var popup = L.popup()
                    .setLatLng(latLng)
                    .setContent(stop.name)
                    .openOn(this_.module.webapp.map.lmap);
            }, function(evt) {
                this_.module.webapp.map.lmap.closePopup();
            });

        }

        // scroll to the boarded segment, if applicable
        if(this.activeLeg) {
            var scrollY = this.stopList[0].scrollHeight * this.activeLeg.from.stopIndex / (this.activeVariant.stops.length - 1);
            this.stopList.scrollTop(scrollY);
        }

        // update the route link

        var url = variantData.route.url;
        var html = "";
        if(url) html += 'Link to: <a href="' + url + '" target="_blank">Route Info</a>';

        // TriMet-specific code
        if(url.indexOf('http://trimet.org') === 0) {
            var day = "w";
            if(this.activeLeg) {
                var dow = moment(this.activeLeg.startTime).add("h", -3).day();
                if(dow === 0) day = "h";
                if(dow === 6) day = "s";
            }
            var rte = url.substring(29, 32);
            var direction = variantData.id.split(':')[2];
            html += ' | <a href="http://trimet.org/schedules/' + day + '/t1' + rte + '_' + direction + '.htm" target="_blank">Timetable</a>';
        }

        this.scheduleLink.html(html);

    },

});
