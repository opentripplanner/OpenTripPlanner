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


otp.modules.calltaker.MailablesWidget = 
    otp.Class(otp.widgets.Widget, {

    module : null,
    
    initialize : function(id, module) {
        this.module = module;

        otp.widgets.Widget.prototype.initialize.call(this, id, module, {
            cssClass : 'otp-calltaker-mailablesWidget',
            title : "Mailables",
            resizable: true,
            closeable : true,
            persistOnClose : true,
            openInitially : false,            
        });

        var index = 0;
        ich['otp-calltaker-mailablesWidget']({
            widgetId : this.id,
            mailables : this.module.options.mailables,
            index : function() {
                return index++;
            },
        }).appendTo(this.mainDiv);
        this.center();
        
        for(var i = 0; i < this.module.options.mailables.length; i++) {
            var elt = $('#' + this.id + '-listItem-' + i);
            elt.data('mailable', this.module.options.mailables[i])
            .draggable({
                helper: 'clone',
                appendTo : 'body',
                zIndex: 10000,
                revert: 'invalid',
                drag: function(event,ui){ 
                    $(ui.helper).css({
                        border : '2px solid gray',
                        width : elt.css('width'),
                    });
                }                    
            });
        }
        
        // allow the selected list to accept elements from full list via drag & drop
        $("#"+this.id+'-selectedList').droppable({
            accept: '.otp-calltaker-mailables-fullListItem',
            hoverClass: 'otp-calltaker-mailables-selectedListHover',
            drop: $.proxy(function(event, ui) {
                var mailable = $(ui.draggable.context).data('mailable');
                this.mailableDropped($(ui.draggable.context).data('mailable'));
            }, this),
        });
        
        $('#' + this.id + '-createLetterButton').button().click($.proxy(this.createLetter, this));
        
    },
    
    mailableDropped : function(mailable) {
        var selectedList = $('#' + this.id + '-selectedList');
        var elt = ich['otp-calltaker-selectedMailable'](mailable).appendTo(selectedList);

        elt.find('.otp-calltaker-mailables-selectedListItem-closeButton').click(function() {
            elt.remove();
        })
        
        selectedList.scrollTop(selectedList[0].scrollHeight)
    },
    
    createLetter : function() {

        var img = new Image();
        var this_ = this;
        img.onload = function() {
            var canvas = document.createElement('canvas');
            canvas.width = img.width;
            canvas.height = img.height;

            var ctx = canvas.getContext("2d");
            ctx.drawImage(img, 0, 0);

            var data = canvas.toDataURL("image/jpeg");
            this_.headerImageData = data;
            this_.headerImageWidth = img.width;
            this_.headerImageHeight = img.height;
            
            this_.writePDF();

        }
        img.src = this.module.options.mailables_header_graphic;
    
    },
    
    writePDF : function() {

        var firstname = $('#' + this.id + '-firstname').val().toUpperCase();
        var lastname = $('#' + this.id + '-lastname').val().toUpperCase();
        var address1 = $('#' + this.id + '-address1').val().toUpperCase();
        var address2 = $('#' + this.id + '-address2').val().toUpperCase();
        var city = $('#' + this.id + '-city').val().toUpperCase();
        var state = $('#' + this.id + '-state').val().toUpperCase();
        var zip = $('#' + this.id + '-zip').val().toUpperCase();
    

        var doc = new jsPDF("p", "pt", "letter");
        var line_height = 14;


        var horizontal_margin = this.module.options.mailables_horizontal_margin || 108;
        var top_margin = this.module.options.mailables_top_margin || 108;
        var bottom_margin = this.module.options.mailables_bottom_margin || 72;

        var x_offset = horizontal_margin, y_offset = top_margin;
        var max_y = 792 - bottom_margin;


        this.preparePage(doc);
        
        
        // current date
        y_offset += line_height;
        doc.text(x_offset, y_offset, moment().format("MMMM D, YYYY"));


        // recipient address
        y_offset += 3 * line_height;
        doc.text(x_offset, y_offset, firstname + " " + lastname);
        y_offset += line_height;
        doc.text(x_offset, y_offset, address1);
        if(address2 && address2.length > 0) {
            y_offset += line_height;
            doc.text(x_offset, y_offset, address2);
        }
        y_offset += line_height;
        doc.text(x_offset, y_offset, city + ", " + state + " " + zip);


        // introduction block        
        y_offset += 2 * line_height;
        var lines = doc.splitTextToSize(this.module.options.mailables_introduction, 612 - 2 * horizontal_margin);
        for(var l = 0; l < lines.length; l++) {
            y_offset += line_height;
            if(y_offset > max_y) {
                doc.addPage();
                this.preparePage(doc);
                y_offset = top_margin;
            }
            doc.text(x_offset, y_offset, lines[l]);
        }
                
                
        // table header 
        if(y_offset + line_height * 7 > max_y) {
            doc.addPage();
            this.preparePage(doc);
            y_offset = top_margin;
        } 

        y_offset += line_height * 2;
        doc.setFontType("bold");
        var summaryText = "SUMMARY BY ITEM"
        var w = doc.getStringUnitWidth(summaryText, {fontType : 'bold'}) * 12;
        doc.text(306 - w/2, y_offset, summaryText);


        y_offset += line_height * 2;
        doc.text(x_offset, y_offset, "Item");
        doc.text(612 - horizontal_margin - 72, y_offset, "Quantity");


        // table rows
        y_offset += line_height * 2;
        doc.setFontType("normal");
        var items = $("#"+this.id+'-selectedList').children();
        for(var i = 0; i < items.length; i++) {
            var item = $(items[i]);
            var name = item.find('.otp-calltaker-mailables-selectedListItem-name').html();
            var largePrint = item.find('.otp-calltaker-mailables-selectedListItem-largePrint');
            var quantity = item.find('.otp-calltaker-mailables-selectedListItem-quantity').val();
            if(largePrint && largePrint.is(':checked')) {
                name += " (LARGE PRINT)";
            }

            name = jQuery('<div>' + name + '</div>').text();
            
            var lines = doc.splitTextToSize(name, 612 - 2 * horizontal_margin - 108);

            if(y_offset + lines.length * line_height + 4 > max_y) {
                doc.addPage();
                this.preparePage(doc);                
                y_offset = top_margin;
            }
            doc.text(x_offset, y_offset, lines);
            doc.text(612 - horizontal_margin - 72, y_offset, quantity);
            y_offset += lines.length * line_height + 4;
        }

        // conclusion block        
        var lines = doc.splitTextToSize(this.module.options.mailables_conclusion, 612 - 2 * horizontal_margin);
        for(var l = 0; l < lines.length; l++) {
            y_offset += line_height;
            if(y_offset > max_y) {
                doc.addPage();
                this.preparePage(doc);
                y_offset = top_margin;
            }
            doc.text(x_offset, y_offset, lines[l]);
        }
        

        doc.save('mailables_letter.pdf');

    },
    
    preparePage : function(doc) {

        if(this.headerImageData) {
            var imgWidth = this.module.options.mailables_header_graphic_width || 72 * this.headerImageWidth/300;
            var imgHeight =  this.module.options.mailables_header_graphic_height || 72 * this.headerImageHeight/300;
            doc.addImage(this.headerImageData, "JPEG", 306 - imgWidth/2, 54, imgWidth, imgHeight);
        }
                
        if(this.module.options.mailables_footer) {
            var fontSize = 9;
            doc.setFontSize(fontSize);
            var width = doc.getStringUnitWidth(this.module.options.mailables_footer) * fontSize;
            doc.text(306 - width/2, 750, this.module.options.mailables_footer);
        }

        doc.setFontSize(12);     
    },

    clearForm : function() {
        $('#' + this.id + '-firstname').val('');
        $('#' + this.id + '-lastname').val('');
        $('#' + this.id + '-address1').val('');
        $('#' + this.id + '-address2').val('');
        $('#' + this.id + '-city').val('');
        $('#' + this.id + '-state').val('');
        $('#' + this.id + '-zip').val('');
        $("#"+this.id+'-selectedList').empty();
    },

    onClose : function() {
        this.clearForm();
    }
});
