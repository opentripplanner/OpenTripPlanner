$(document).ready(function() {
    console.log("init form");
    
    $.support.cors = true;
    $("#travelDate").datepicker();
    
    $("#submitBtn").button().click(function() {
        console.log("submit: " + $("#teacherName").val());
        
        var data = {
            userName : 'admin',
            password : 'secret',
            'request.teacherName' : $("#teacherName").val(),
            'request.schoolName' : $("#schoolName").val(),
            'request.createdBy' : this.userName,
        };

        $.ajax('http://localhost:9000/fieldTrip/newRequest', {
            type: 'POST',
            
            data: data,
                
            success: function(data) {
                if((typeof data) == "string") data = jQuery.parseJSON(data);
                console.log('success');
            },
            
            error: function(data) {
                console.log("error saving trip");
                console.log(data);
            }
        });
                
    });

});
