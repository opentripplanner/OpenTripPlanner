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


otp.core.ContextMenu = 
    otp.Class(otp.core.PopupMenu, {

    parent  : null,
    
    initialize : function(parent, menuClicked) {
        otp.core.PopupMenu.prototype.initialize.apply(this, arguments);
        
        var this_ = this;
        
        this.parent = parent;
        
        parent.on('contextmenu', function(event) {
            if(event.preventDefault) event.preventDefault();
            this_.menu.show();
            this_.menu.offset(this_.getOffset(event)).appendTo("body");

            if(menuClicked) menuClicked.call(this, event);
        });
    },
});    
