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

otp.namespace("otp.core");


otp.core.WidgetManagerMenu =
    otp.Class(otp.core.PopupMenu, {

    webapp : null,
    
    widgetItems : null,
    
    initialize : function(webapp) {
        
        otp.core.PopupMenu.prototype.initialize.call(this);
        this.webapp = webapp;

        this.menu.addClass('otp-widgetManagerMenu').hide();

        
        var this_ = this;
        this.addItem(_tr("Minimize all"), function() {
            var widgets = this_.webapp.activeModule.widgets;
            for(infoWidgetId in this_.webapp.infoWidgets) widgets.push(this_.webapp.infoWidgets[infoWidgetId]);
            for(var i = 0; i < widgets.length; i++) {
                var widget = widgets[i];
                if(widget.isOpen && widget.minimizable) widget.minimize();
            }
        });
        this.addItem(_tr("Unminimize all"), function() {
            var widgets = this_.webapp.activeModule.widgets;
            for(infoWidgetId in this_.webapp.infoWidgets) widgets.push(this_.webapp.infoWidgets[infoWidgetId]);
            for(var i = 0; i < widgets.length; i++) {
                var widget = widgets[i];
                if(widget.isMinimized) widget.unminimize();
            }        
        });
        
        this.addSeparator();
        
        this.widgetItems = $("<div />").appendTo(this.menu);
    },
    

    show : function() {
        this.refreshWidgets();
        this.suppressHide = true;
        this.menu.show().appendTo('body');
    },
    
    refreshWidgets : function() {
        this.clearWidgetItems();
        for(var i = 0; i < this.webapp.activeModule.widgets.length; i++) {
            var widget = this.webapp.activeModule.widgets[i];
            if(widget.isOpen || widget.persistOnClose) this.addWidgetItem(widget);
        }          
    },

    addWidgetItem : function(widget) {
        var this_ = this;
        $('<div class="otp-popupMenu-item">'+widget.title+'</div>')
        .appendTo($(this.widgetItems))
        .click(function() {
            if(!widget.isOpen) widget.show();
            if(widget.isMinimized) widget.unminimize();
            widget.bringToFront();
        });
        return this; // for chaining
    },
    
    clearWidgetItems : function() {
        this.widgetItems.empty();
    }
    
});
