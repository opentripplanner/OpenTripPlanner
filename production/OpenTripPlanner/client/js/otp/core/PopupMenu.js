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


otp.core.PopupMenu = otp.Class({

    menu    : null,
    
    suppressHide : false,
    
    initialize : function() {
        var this_ = this;
        
        this.menu = $('<div class="otp-popupMenu"></div>');
        
        $(document).bind("click", function(event) {
            if(this_.suppressHide) {
                this_.suppressHide = false;
                return;
            }
            this_.menu.hide();
        });
    },
    
    getOffset : function(event) {
        return { 
            top: event.clientY,
            left: event.clientX
        };
    },
    
    addClass : function(className) {
        this.menu.addClass(className);
    },
    
    addItem : function(text, clickHandler) {
        $('<div class="otp-popupMenu-item">'+text+'</div>')
        .appendTo($(this.menu))
        .click(clickHandler);        
        return this; // for chaining
    },
    
    addSeparator : function(scrollable) {
        $("<hr />").appendTo($(this.menu));
    },
});    
