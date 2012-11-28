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


otp.widgets.PastQueriesWidget = 
    otp.Class(otp.widgets.Widget, {

    module : null,
    
    header : null,
    queryList : null,
    
    initialize : function(id, module) {
    
        otp.widgets.Widget.prototype.initialize.apply(this, arguments);
        this.module = module;
        this.$().addClass('otp-pastQueriesWidget');
        this.$().resizable();
        this.header = $("<div class='otp-pastQueriesHeader'>Past 10 Queries for user "+module.userName+"</div>").appendTo(this.$());
        this.queryList = $("<div id='"+this.id+"-queryList' class='otp-pastQueriesList'></div>").appendTo(this.$());
               
        this.$().resizable({
            alsoResize: this.queryList
        });
        this.$().draggable({
            cancel: '#'+this.id+'-queryList'
        });
    },
    
    updateQueries : function(queries) {
        var this_ = this;
        this.queryList.empty();
        queries.each(function(query) {
            query = query.toJSON();
            var d = Date.parse(query.timeStamp);
            var html = '<div class="otp-pastQueryRow">';
            html += '<div class="otp-pastQueryRowTime">'+this.getTimeAgoStr(Date.now()-d)+'</div>';
            html += '<div class="otp-pastQueryRowDesc">'+query.fromPlace+' to '+query.toPlace+'</div>';
            html += '</div>';
            $(html).appendTo(this.queryList).click(function() {
                console.log(query.queryParams);
                this_.module.restoreTrip(JSON.parse(query.queryParams));
            });
        }, this);        
    },
    
    getTimeAgoStr : function(ms) {
        if(ms < 1000) return "Just now";
        if(ms < 60000) return Math.round(ms/1000)+" sec ago";
        if(ms < 3600000) return Math.round(ms/60000) + " min ago";
        if(ms < 86400000) return Math.round(ms/3600000)+" hours ago";
        return Math.round(ms/86400000) + " days ago";
    }
});
      
