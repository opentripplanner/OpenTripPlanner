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

otp.namespace("otp.locale");

/**
  * @class
  */
otp.locale.Marathi = {

    config :
    {
        metricsSystem : "international",
        rightClickMsg : "प्रवास कुठून कुठपर्यंत करायचा आहे ते स्थान निवडण्याकरिता नकाशावर राईट क्लिक करा.",
        attribution   : {
            title   : "License Attribution",
            content : "Disclaimer goes here"  
        }
    },

    contextMenu : 
    {
        fromHere         : "कुठे बस पकडणार?",
        toHere           : "कुठे ऊतरणार?",
        intermediateHere : "Add intermediate point", // TODO localize

        centerHere       : "नकाशा बघण्यासाठी",
        zoomInHere       : "जुम इन करण्यासाठी(चित्र मोठे करण्यासाठी)",
        zoomOutHere      : "जुम आउट करण्यासाठी(चित्र लहान करण्यासाठी)",
        previous         : "आधी या ठिकाणी होतात",
        next             : "नंतरची जागा"
    },

    // TODO Localize Me
    bikeTriangle : 
    {
        safeName : "Bike friendly",
        safeSym  : "S",

        hillName : "Flat",
        hillSym  : "F",

        timeName : "Quick",
        timeSym  : "Q"
    },

    service : 
    {
        weekdays:  "आठवड्याचे वार",
        saturday:  "शनिवार ",
        sunday:    "रविवार",
        schedule:  "वेळापत्रक"
    },

    indicators : 
    {
        ok         : "ओ.के.",
        date       : "दिनांक",
        loading    : "माहिती येत आहे",
        searching  : "शोधत आहे...",
        qEmptyText : "पत्ता,जवळची खूण,बस थांबा..."
    },

    buttons: 
    {
// TODO
        reverse       : "Reverse",
        reverseTip    : "<b>Reverse directions</b><br/>Plan a return trip by reversing this trip's start and end points, and adjusting the time forward.",
        reverseMiniTip: "Reverse directions",

        edit          : "Edit",
        editTip       : "<b>Edit trip</b><br/>Return to the main trip planner input form with the details of this trip.",

        clear         : "Clear",
        clearTip      : "<b>Clear</b><br/>Clear the map and all active tools.",

        fullScreen    : "Full Screen",
        fullScreenTip : "<b>Full Screen</b><br/>Show -or- hide tool panels",

        print         : "Print",
        printTip      : "<b>Print</b><br/>Print friendly version of the trip plan (without map).",

        link          : "Link",
        linkTip      : "<b>Link</b><br/>Show link url for this trip plan.",

        feedback      : "Feedback",
        feedbackTip   : "<b>Feedback</b><br/>Send your thoughts or experiences with the map",

        submit       : "Submit",
        clearButton  : "Clear",
        ok           : "OK",
        cancel       : "Cancel",
        yes          : "Yes",
        no           : "No",
        showDetails  : "Show details...",
        hideDetails  : "Hide details..."
    },

    // note: keep these lower case (and uppercase via template / code if needed)
    directions : 
    {
        southeast:      "आग्न्येय",
        southwest:      "नैऋत्य",
        northeast:      "ईशान्य",
        northwest:      "वायव्य",
        north:          "उत्तर",
        west:           "पश्चिम",
        south:          "दक्षिण",
        east:           "पूर्व",
        bound:          "वाकडा",
        left:           "डावीकडे",
        right:          "उजवीकडे",
        slightly_left:  "थोड डावीकडे ",
        slightly_right: "थोड उजवीकडे",
        hard_left:      "अगदी डावीकडे ",
        hard_right:     "अगदी उजवीकडे",
        'continue':     "चालू ठेवा",
        to_continue:    "चालू ठेवा",
        becomes:        "becomes",
        at:             "at",

// TODO
        on:             "on",
        to:             "to",
        via:            "via",
        circle_counterclockwise: "take roundabout counterclockwise",
        circle_clockwise:        "take roundabout clockwise"
    },

    // see otp.planner.Templates for use ... these are used on the trip itinerary as well as forms and other places
    instructions :
    {
// TODO
        walk         : "Walk",
        walk_toward  : "Walk",
        walk_verb    : "Walk",
        bike         : "Bike",
        bike_toward  : "Bike",
        bike_verb    : "Bike",
        drive        : "Drive",
        drive_toward : "Drive",
        drive_verb   : "Drive",
        move         : "Proceed",
        move_toward  : "Proceed",

        transfer     : "transfer",
        transfers    : "transfers",

        continue_as  : "Continues as",
        stay_aboard  : "stay on board",

        depart       : "Depart",
        arrive       : "Arrive",

        start_at     : "Start at",
        end_at       : "End at"
    },

    // see otp.planner.Templates for use
    labels : 
    {
// TODO
        agency_msg   : "Service run by", // TODO
        agency_msg_tt: "Open agency website in separate window...", // TODO
        about        : "About",
        stop_id      : "Stop ID",
        trip_details : "Le Trip Details",
        fare         : "Fare",
        fare_symbol  : "रुपया ",
        travel       : "Travel",
        valid        : "Valid",
        trip_length  : "Time",
        with_a_walk  : "with a walk of",
        alert_for_rt : "Alert for route"
    },

    // see otp.planner.Templates for use -- one output are the itinerary leg headers
    modes : 
    {
// TODO
        WALK:           "WALK",
        BICYCLE:        "BICYCLE",
        CAR:            "CAR",
        TRAM:           "TRAM",
        SUBWAY:         "SUBWAY",
        RAIL:           "RAIL",
        BUS:            "BUS",
        FERRY:          "FERRY",
        CABLE_CAR:      "CABLE CAR",
        GONDOLA:        "GONDOLA",
        FUNICULAR:      "FUNICULAR"
    },

    ordinal_exit:
    {
// TODO
        1:  "to first exit",
        2:  "to second exit",
        3:  "to third exit",
        4:  "to fourth exit",
        5:  "to fifth exit",
        6:  "to sixth exit",
        7:  "to seventh exit",
        8:  "to eighth exit",
        9:  "to ninth exit",
        10: "to tenth exit"
    },

    time:
    {
        // TODO
        hour_abbrev    : "hour",
        hours_abbrev   : "hours",
        hour           : "hour",
        hours          : "hours",

// TODO
        format        : "D, j M H:i",
        date_format   : "d-m-Y",
        time_format   : "H:i",
        minute        : "मिनिट",
        minutes       : "मिनिटस",
        minute_abbrev : "मिनिट",
        minutes_abbrev: "मिनिटस",
        second_abbrev : "सेकंद",
        seconds_abbrev: "सेकंदस",
        months:         ['जानेवारी', 'फेब्रुवारी', 'मार्च', 'एप्रिल', 'मे', 'जून', 'जुलै', 'ऑगस्ट', 'सप्टेंबर', 'ऑक्टोबर', 'नोव्हेंबर', 'डिसेंबर']
    },
    
    systemmap :
    {
        labels :
        {
            panelTitle : "System Map"
        }
    },

    tripPlanner :
    {
        labels : 
        {
            panelTitle    : "ट्रीप प्लॅनर",
            tabTitle      : "ट्रीप प्लॅन करा ",
            inputTitle    : "ट्रीपची माहिती ",
            optTitle      : "ट्रीप क्रमाने (पर्यायी)",
            submitMsg     : "तुमची ट्रीप नियोजित करीत आहे...",
            optionalTitle : "",
            date          : "दिनांक",
            time          : "वेळ",
            when          : "कधी?",
            from          : "कुठून?",
            fromHere      : "या इथून",
            to            : "कुठ पर्यंत ",
            toHere        : "या इथपर्यंत ",
            intermediate  : "Intermediate Place",          // TODO
            minimize      : "दाखवा",
            maxWalkDistance: "पायी चालण्याचे अंतर ",
            maxBikeDistance: "पायी चालण्याचे  bike",              // TODO
            arriveDepart  : "पोहोचण्याचे ठिकाण/निघा",
            mode          : "या मार्गाने जाऊ शकता",
            wheelchair    : "व्हील चेअरचा वापर करता येईल अशी ट्रीप", 
            go            : "निघा",
            planTrip      : "तुमचा मार्ग शोधा",
            newTrip       : "नवी ट्रीप"
        },

        // see otp/config.js for where these values are used
        link : 
        {
            text           : "Link to this trip (OTP)",
            trip_separator : "This trip on other transit planners",
            bike_separator : "On other bike trip planners",
            walk_separator : "On other walking direction planners",
            google_transit : "Google Transit",
            google_bikes   : "Google Bike Directions",
            google_walk    : "Google Walking Directions",
            google_domain  : "http://www.google.co.in"
        },

        // see otp.planner.Forms for use
        geocoder:
        {
//TODO
            working      : "Looking up address ....",
            error        : "Did not receive any results",
            msg_title    : "नवी ट्रीप review trip plan",
            msg_content  : "नवी ट्रीप correct errors before planning your trip",
            select_result_title : "Please select a result",
            address_header : "Address"
        },

        error:
        {
            title        : 'ट्रीप प्लॅनर मध्ये काही गडबड आहे',
            deadMsg      : "ट्रीप प्लॅनर सध्या प्रतिसाद देत नाही,पुन्हा प्रयत्न करण्यासाठी कृपया वाट पहा किंवा दुसरी ट्रीप प्लान करा (खाली दिलेली लिंक पहा)",
            geoFromMsg   : "कृपया तुमच्या ट्रीप करता कुठून जायचे ते ठिकाण निवडा:",
            geoToMsg     : "कृपया जेथे जायचे ते ठिकाण निवडा: "
        },
        
        // default messages from server if a message was not returned
        msgcodes:
        {
            200: "प्लॅन ओ.के.",
            500: "सर्व्हर एरर आहे",
            400: "ट्रीप कमाल मर्यादे बाहेर",
            404: "मार्ग सापडत नाही",
            406: "जायच्या/यायच्या वेळा दिलेल्या नाहीत",
            408: "क्षमस्व,तुमची वेळ संपली आहे",
            413: "अपुरी माहिती",
            440: "जिथून निघायचे त्याचा जीओकोड मिळत नाही ",
            450: "जिथे जायचे त्याचा जीओकोड मिळत नाही",
            460: "कुठून कुठे जायचे त्याचा जीओकोड मिळत नाही",
            409: "खूप जवळचा मार्ग",
            340: "कुठून जायचे त्याचा जीओकोड सुस्पष्ट नाही",
            350: "कुठे जायचे त्याचा जीओकोड सुस्पष्ट नाही",
            360: "कुठून कुठे जायचे त्याचा जीओकोड सुस्पष्ट नाही"
        },

        options: 
        [
          ['TRANSFERS', 'कमीत कमी बस बदलुन'],
          ['QUICK',     'कमी वेळेत'],
          ['SAFE',      'सुरक्षित मार्ग'],
          ['TRIANGLE',  'Custom trip...']  // TODO localize
        ],
    
        arriveDepart: 
        [
          ['false', 'प्रस्थान'], 
          ['true',  'आगमन']
        ],
    
        maxWalkDistance : 
        [
            ['160',   '1/10 मैल'],
            ['420',   '1/4 मैल'],
            ['840',   '1/2 मैल'],
            ['1260',  '3/4 मैल'],
            ['1600',  '1 मैल'],
            ['3200',  '2 मैल'],
            ['4800',  '3 मैल'],
            ['6400',  '4 मैल'],
            ['7600',  '5 मैल'],
            ['7601',  '10 मैल'],
            ['7602',  '15 मैल'],
            ['7603',  '20 मैल'],
            ['7604',  '30 मैल'],
            ['7605',  '40 मैल'],
            ['7606',  '50 मैल'],
            ['7607',  '100 मैल']
        ],
    
        mode : 
        [
            ['TRANSIT,WALK', 'बस आणि पायी'],
           // ['BUSISH,TRAINISH,WALK', 'बस आणि रेल्वे'],
            ['BUSISH,WALK', 'फक्त बस'],
          //  ['TRAINISH,WALK', 'फक्त रेल्वे'],
            ['WALK', 'फक्त पायी']//,
         //   ['BICYCLE', 'सायकल'],
           // ['TRANSIT,BICYCLE', 'बदला व सायकलने']
        ],

        wheelchair :
        [
            ['false', 'गरज नाही'],
            ['true', 'गरज आहे']
        ]
    },

    CLASS_NAME : "otp.locale.Marathi"
};