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

        var salutation = $('#' + this.id + '-salutation option:selected').text();
        var firstname = $('#' + this.id + '-firstname').val();
        var lastname = $('#' + this.id + '-lastname').val();
        var address1 = $('#' + this.id + '-address1').val();
        var address2 = $('#' + this.id + '-address2').val();
        var city = $('#' + this.id + '-city').val();
        var state = $('#' + this.id + '-state').val();
        var zip = $('#' + this.id + '-zip').val();
    

        var doc = new jsPDF("p", "pt");
        doc.setFont("times");
        doc.setFontSize(12);
        var line_height = 14;


        var horizontal_margin = this.module.options.mailables_horizontal_margin || 108;
        var vertical_margin = this.module.options.mailables_vertical_margin || 72;

        var x_offset = horizontal_margin, y_offset = vertical_margin;
        var max_y = 792 - vertical_margin;


        // current date
        doc.text(x_offset, y_offset, moment().format("MMMM DD, YYYY"));


        // recipient address
        y_offset += 4 * line_height;
        doc.text(x_offset, y_offset, firstname + " " + lastname);
        y_offset += line_height;
        doc.text(x_offset, y_offset, address1);
        if(address2 && address2.length > 0) {
            y_offset += line_height;
            doc.text(x_offset, y_offset, address2);
        }
        y_offset += line_height;
        doc.text(x_offset, y_offset, city + ", " + state + " " + zip);


        // recipient salutation
        y_offset += line_height*2;
        doc.text(x_offset, y_offset, "Dear " + salutation + " " + lastname + ":");


        // introduction block        
        y_offset += line_height;
        var lines = doc.splitTextToSize(this.module.options.mailables_introduction, 612 - 2 * horizontal_margin);
        for(var l = 0; l < lines.length; l++) {
            y_offset += line_height;
            if(y_offset > max_y) {
                doc.addPage();
                y_offset = vertical_margin;
            }
            doc.text(x_offset, y_offset, lines[l]);
        }
                
                
        // table header 
        if(y_offset + line_height * 7 > max_y) {
            doc.addPage();
            y_offset = vertical_margin;
        } 

        y_offset += line_height * 2;
        doc.setFontType("bold");
        doc.text(x_offset, y_offset, "SUMMARY BY ITEM");

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
            
            var lines = doc.splitTextToSize(name, 612 - 2 * horizontal_margin - 108);

            if(y_offset + lines.length * line_height + 4 > max_y) {
                doc.addPage();
                y_offset = vertical_margin;
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
                y_offset = vertical_margin;
            }
            doc.text(x_offset, y_offset, lines[l]);
        }
        

        // "Sincerely.." block        
        if(y_offset + line_height * 4.5 + this.module.options.mailables_signature_graphic_height > max_y) {
            doc.addPage();
            y_offset = vertical_margin;
        } 

        y_offset += line_height * 2;
        doc.text(x_offset, y_offset, "Sincerely,");
        y_offset += line_height  / 2;
        doc.addImage(this.module.options.mailables_signature_graphic_data,
                     "JPEG", x_offset, y_offset,
                     this.module.options.mailables_signature_graphic_width,
                     this.module.options.mailables_signature_graphic_height);

        y_offset += this.module.options.mailables_signature_graphic_height + line_height;
        doc.text(x_offset, y_offset, this.module.options.mailables_signature_name);
        y_offset += line_height;
        doc.text(x_offset, y_offset, this.module.options.mailables_signature_title);


        doc.save('mailables_letter.pdf');

    },
});
