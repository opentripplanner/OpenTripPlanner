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


otp.modules.calltaker.CallHistoryWidget = 
    otp.Class(otp.widgets.Widget, {

    module : null,

    header : null,
    queryList : null,
    
    startTime : null,
    lastSize : null,
    
    timer : null,
    
    callListView : null,
    queryListView : null,
    
    
    initialize : function(id, module) {
        var this_ = this;  
        
        this.module = module;

        otp.widgets.Widget.prototype.initialize.call(this, id, module, {
            cssClass : 'otp-callHistoryWidget',
            title : "Call History for user " + module.webapp.sessionManager.username,
            closeable : true,
            persistOnClose : true,
        });

        var buttonRow = $('<div class="otp-callHistory-buttonRow"></div>').appendTo(this.$());

        $('<input id="'+this.id+'-button" type="submit" value="New Call" style="float:left;" />').appendTo(buttonRow)
        .button().click(function() {
            if(this_.module.activeCall == null) {
                this_.module.startCall();
            }
            else {
                this_.module.endCall();
            }
        });
        
        this.timerText = $('<div class="otp-callHistory-buttonRowText">(No Active Call)</div>').appendTo(buttonRow);

        var callListContainer = $("<div class='otp-callHistory-callListContainer'>Recent Calls:</div>").appendTo(this.$());
        this.callList = $("<div id='"+this.id+"-callList' class='otp-callHistory-callList notDraggable'></div>").appendTo(callListContainer);

        this.callListView = new otp.widgets.CallListView(this.id+'-callList', this.module);

        var queryListContainer = $("<div class='otp-callHistory-queryListContainer'>Queries for selected call:</div>").appendTo(this.$());
        this.queryList = $("<div id='"+this.id+"-queryList' class='otp-callHistory-queryList notDraggable'></div>").appendTo(queryListContainer);

        this.queryListView = new otp.widgets.QueryListView(this.id+'-queryList', this.module);
             
        this.$().resizable({
            minWidth: 250,
            //alsoResize: "#"+this.id+"-callList,#"+this.id+"-queryList" //'this.callList, this.queryList', 
            resize: function( event, ui ) {
                if(this_.lastSize != null) {
                    var dh = ui.size.height - this_.lastSize.height;
                    var dw = ui.size.width - this_.lastSize.width;
                    this_.callList.height(this_.callList.height()+dh);
                    this_.queryList.height(this_.queryList.height()+dh);
                    this_.queryList.width(this_.queryList.width()+dw);
                }
                this_.lastSize = { width : ui.size.width, height : ui.size.height };
            }
        });
    },
    
    callStarted : function() {
        $('#'+this.id+'-button').val("End Call");
        this.queryList.empty();

        this.timerText.html("0:00");
        this.startTime = moment();

        var this_ = this;
        this.timer = setInterval(function() {
            this_.timerText.html(moment(moment()-this_.startTime).format("m:ss"));
        }, 1000);
    },
    
    callEnded : function() {
        clearInterval(this.timer);

        this.callListView.addCall(this.module.activeCall);  

        $('#'+this.id+'-button').val("New Call");
        this.timerText.html("No Active Call");
        this.queryList.empty();
    },
    
});


otp.widgets.CallView = Backbone.View.extend({
    
    events : {
        "click" : "clicked"
    },
    
    initialize: function() {
        _.bindAll(this, 'render'); // every function that uses 'this' as the current object should be in here
    },
    
    render: function() {
        var duration = moment(this.model.get('endTime')) - moment(this.model.get('startTime'));
        $(this.el).addClass('otp-callHistory-callListItem')
        .html('<span>'+moment(this.model.get('startTime')).format("h:mma")
            + ', ' + moment(this.model.get('startTime')).format("MMM D") + '<br>'
            + '(Length: '+otp.util.Time.secsToHrMinSec(duration/1000)+')</span>');
        return this; // for chainable calls, like .render().el
    }, 
    
    clicked : function() {
        console.log('call '+this.model.id+' clicked');
        this.module.callHistoryWidget.queryListView.fetchByCallId(this.model.id);
    }   

});
  
  
otp.widgets.CallListView = Backbone.View.extend({

    module: null,
    
    initialize: function(elName, module) {
        this.module = module;
        _.bindAll(this, 'render', 'addCall', 'appendCall'); // every function that uses 'this' as the current object should be in here

        this.el = $('#'+elName);
        this.collection = new otp.modules.calltaker.CallList();
        this.collection.bind('add', this.appendCall); // collection event binder
        this.collection.bind('reset', this.render);
        this.collection.fetch({ data: {
            sessionId: module.sessionManager.sessionId,
            limit: 10,
        }});
        this.counter = 0;
        this.render();
    },
    
    render: function(){
        var self = this;
        _(this.collection.models).each(function(item){ // in case collection is not empty
            self.appendCall(item);
        }, this);
    },
    
    
    addCall: function(call){
        console.log("addCall");
        this.collection.add(call);
        //this.collection.create(call);
    },

    appendCall: function(item) {
        var callView = new otp.widgets.CallView({
            model: item
        });
        callView.module = this.module;
        $(this.el).prepend(callView.render().el);
    }
});
    
    
otp.widgets.QueryView = Backbone.View.extend({

    events : {
        "click" : "clicked"
    },
    
    initialize: function() {
        _.bindAll(this, 'render'); // every function that uses 'this' as the current object should be in here
    },
    
    render: function() {
        var html = '<div class="otp-pastQueryRowTime">'+moment(this.model.get('timeStamp')).format("h:mm:ssa")+'</div>';
        html += '<div class="otp-pastQueryRowDesc">' + this.model.get('fromPlace') + ' to ' + this.model.get('toPlace') + '</div>';

        $(this.el).addClass('otp-callHistory-callListItem')
        .html(html);
        return this; // for chainable calls, like .render().el
    }, 

    getTimeAgoStr : function(ms) {
        if(ms < 1000) return "Just now";
        if(ms < 60000) return Math.round(ms/1000)+" sec ago";
        if(ms < 3600000) return Math.round(ms/60000) + " min ago";
        if(ms < 86400000) return Math.round(ms/3600000)+" hours ago";
        return Math.round(ms/86400000) + " days ago";
    },
        
    clicked : function() {
        this.module.restoreTrip(JSON.parse(this.model.get('queryParams')));
    },

});
  
  
otp.widgets.QueryListView = Backbone.View.extend({
   
    module: null,
    
    initialize: function(elName, module) {
        this.module = module;
        _.bindAll(this, 'render', 'addQuery', 'appendQuery'); // every function that uses 'this' as the current object should be in here

        this.el = $('#'+elName);
        this.collection = new otp.modules.calltaker.QueryList();
        this.collection.bind('add', this.appendQuery);
        this.collection.bind('reset', this.render);
        this.render();
    },
    
    render: function(){
        var this_ = this;
        _(this.collection.models).each(function(item){
            this_.appendQuery(item);
        }, this);
    },
    
    addQuery: function(query){
        this.collection.add(query);
    },

    appendQuery: function(query) {
        var view = new otp.widgets.QueryView({
            model: query
        });
        view.module = this.module;
        $(this.el).prepend(view.render().el);
    },
    
    fetchByCallId: function(callId) {
        this.el.empty();
        this.collection.fetch({ data: {
            sessionId : this.module.sessionManager.sessionId, 
            "call.id": callId
        }});
    }
});
    
