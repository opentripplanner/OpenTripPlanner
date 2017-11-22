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

otp.namespace("otp.modules.alerts");


otp.modules.alerts.EditAlertView = Backbone.View.extend({

    events : {
        'keyup textarea' : 'descriptionTextChanged',
        'click #addRangeButton' : 'addRangeButtonClicked',
        'click .otp-alerts-editAlert-deleteRangeButton' : 'deleteRangeButtonClicked',
        'click .otp-alerts-editAlert-deleteEntityButton' : 'deleteEntityButtonClicked',
    },    
    
    render : function() {
        var this_ = this;

        
        var context = this.options.widget.module.prepareAlertTemplateContext(this.model); //_.clone(this.model.attributes);
        var rangeIndex = entityIndex = 0;
        context = _.extend(context, {
            widgetId : this.options.widget.id,
            renderDate : function() {
                return function(date, render) { return moment(parseInt(render(date))).format(otp.config.dateFormat+' '+otp.config.timeFormat); }
            },
            rangeIndex: function() { return rangeIndex++; },
            entityIndex: function() { return entityIndex++; },
        });
        

        
        this.$el.html(ich['otp-alerts-alertEditor'](context));

        // set up the date/time pickers for the 'create new timerange' input
        $("#"+this.options.widget.id+'-rangeStartInput').datetimepicker({
            timeFormat: otp.config.timeFormat, 
        }).datepicker("setDate", new Date());

        $("#"+this.options.widget.id+'-rangeEndInput').datetimepicker({
            timeFormat: otp.config.timeFormat, 
        }).datepicker("setDate", new Date());

        // allow the entities list to accept route/stop elements via drag & drop
        $("#"+this.options.widget.id+'-entitiesList').droppable({
            accept: '.otp-alerts-entitiesWidget-entityRow',
            hoverClass: 'otp-alerts-editAlert-entitiesList-dropHover',
            drop: function(event, ui) { this_.handleEntityDrop(event, ui); }
        });

    },

    descriptionTextChanged : function(event) {
        var text = $('#'+this.options.widget.id+'-descriptionText').val();
        this.model.set('descriptionText', text);
    },
    
    addRangeButtonClicked : function(event) {
        var start = 1000*moment($("#"+this.options.widget.id+'-rangeStartInput').val(), "MM/DD/YYYY "+otp.config.timeFormat).unix();
        var end = 1000*moment($("#"+this.options.widget.id+'-rangeEndInput').val(), "MM/DD/YYYY "+otp.config.timeFormat).unix();
        console.log("new range: "+start+" to "+end);
        
        this.model.attributes.timeRanges.push({
            startTime: start,
            endTime: end
        });
        this.render();
    },

    deleteRangeButtonClicked : function(event) {
        var index = parseInt(event.target.id.split('-').pop());
        this.model.attributes.timeRanges.splice(index, 1);
        this.render();
    },

    deleteEntityButtonClicked : function(event) {
        var index = parseInt(event.target.id.split('-').pop());
        this.model.attributes.informedEntities.splice(index, 1);
        this.render();
    },
    
    handleEntityDrop : function(event, ui) {
        
        var routeIdObj = $(ui.draggable.context).data('routeId');
        if(typeof routeIdObj !== 'undefined' && routeIdObj !== null) {
            this.model.attributes.informedEntities.push({
                agencyId : routeIdObj.agencyId,
                routeId : routeIdObj.id,
            });
        }

        var stopIdObj = $(ui.draggable.context).data('stopId');
        if(typeof stopIdObj !== 'undefined' && stopIdObj !== null) {
            this.model.attributes.informedEntities.push({
                agencyId : stopIdObj.agencyId,
                stopId : stopIdObj.id,
            });
        }

        this.render();
    }
    
});


otp.modules.alerts.EditAlertWidget = 
    otp.Class(otp.widgets.Widget, {
    
    module : null,
    
    alertObj : null,
    
    affectedRoutes : [],
    affectedStops : [],

    initialize : function(id, module, alertObj) {
        var this_ = this;
        otp.widgets.Widget.prototype.initialize.call(this, id, module, {
            title : (alertObj.get('id') == null) ? 'Create Alert' : 'Edit Alert #'+alertObj.get('id'),
            cssClass : 'otp-alerts-editAlertWidget',
            closeable: true
        });
        
        this.module = module;
        this.alertObj = alertObj;
        
        // set up the view 
        var view = new otp.modules.alerts.EditAlertView({
            el: $('<div />').appendTo(this.mainDiv),
            model: alertObj,
            widget: this,
        });
        
        
        // create the save and delete buttons
        var buttonRow = $('<div>').addClass('otp-alerts-entitiesWidget-buttonRow').appendTo(this.mainDiv)
        
        $(Mustache.render(otp.templates.button, { text : "Save"}))
        .button().appendTo(buttonRow).click(function() {
            this_.module.saveAlert(this_.alertObj);
            this_.close();
        });

        $(Mustache.render(otp.templates.button, { text : "Delete"}))
        .button().appendTo(buttonRow).click(function() {
            this_.module.deleteAlert(this_.alertObj);
            this_.close();
        });
        

        view.render();
    },
    
    onClose : function() {
        delete this.module.openEditAlertWidgets[this.alertObj.get('id')];
    }
    
});

