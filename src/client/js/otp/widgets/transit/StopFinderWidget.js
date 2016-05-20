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

otp.widgets.transit.StopFinderWidget =
    otp.Class(otp.widgets.Widget, {

    module : null,

    timeIndex : null,


    initialize : function(id, module, stopViewer) {

        otp.widgets.Widget.prototype.initialize.call(this, id, module, {
            //TRANSLATORS: Widget title
            title : _tr('Stop Finder'),
            cssClass : 'otp-stopFinder',
            closeable : true,
            resizable : true,
            openInitially : false,
            persistOnClose : true,
        });

        this.module = module;
        this.stopViewer = stopViewer;

        var this_ = this;

        this.activeTime = moment();

        var translated_template = {
            //TRANSLATORS: [Public transport] : Selector for GTFS feed
            feed: _tr('Feed'),
            //TRANSLATORS: Search for Stops by ID
            by_id: _tr('By ID'),
            //TRANSLATORS: Search for Stops by Name
            by_name: _tr('By Name'),
            //TRANSLATORS: Search for Stops by ID/by Name
            search: _tr('Search')
        }

        ich['otp-stopFinder'](translated_template).appendTo(this.mainDiv);

        this.feedSelect = this.mainDiv.find('.otp-stopFinder-feedSelect');
        this.module.webapp.indexApi.loadFeeds(this, function() {
            var feeds = this.module.webapp.indexApi.feeds
            for(var i = 0; i < feeds.length; i++) {
                $("<option />").html(feeds[i]).appendTo(this_.feedSelect);
            }
        });

        this.stopList = this.mainDiv.find('.otp-stopFinder-stopList');

        this.mainDiv.find('.otp-stopFinder-idButton').click(function() {
            var feedId = this_.feedSelect.val();
            var id = this_.mainDiv.find('.otp-stopFinder-idField').val();
            if(!id || id.length === 0) return;
            this_.module.webapp.indexApi.loadStopById(feedId, id, this, function(data) {
                this_.updateStops(data === null ? [] : [data]);
            });
        });

        this.mainDiv.find('.otp-stopFinder-nameButton').click(function() {
            var feedId = this_.feedSelect.val();
            var name = this_.mainDiv.find('.otp-stopFinder-nameField').val();
            if(!name || name.length === 0) return;
            this_.module.webapp.indexApi.loadStopsByName(feedId, name, this, function(data) {
                this_.updateStops(data);
            });
        });

        this.center();
    },

    updateStops : function(stops) {
        this.stopList.empty();

        var this_ = this;

        if(!stops || stops.length === 0) {
            this.stopList.html(_tr("No Stops Found"));
            return;
        }

        for(var i = 0; i < stops.length; i++) {
            var stop = stops[i];
            $('<div />')
                .addClass('otp-stopFinder-stopRow')
                .html(stop.name)
                .appendTo(this.stopList)
                .data('stop', stop)
                .click(function() {
                    var s = $(this).data('stop');
                    this_.stopViewer.show();
                    this_.stopViewer.setStop(s.id, s.name);
                    this_.stopViewer.bringToFront();
                });
        }
    },
});
