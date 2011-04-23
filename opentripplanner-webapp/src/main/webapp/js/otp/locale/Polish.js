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
otp.locale.Polish = {

    config : 
    {
        metricsSystem : "international",
        rightClickMsg : "TODO - localize me - Right-click on the map to designate the start and end of your trip.",
        attribution   : {
            title   : "License Attribution",
            content : "Disclaimer goes here"
        }
    },

    contextMenu : 
    {
        fromHere         : "Rozpocznij podró¿ tutaj",
        toHere           : "Zakoñcz podró¿ tutaj",

        centerHere       : "Centruj mapê tutaj",
        zoomInHere       : "Przybli¿ tutaj",
        zoomOutHere      : "Oddal st¹d",
        previous         : "Poprzednia pozycja na mapie",
        next             : "Nastêpna pozycja na mapie"
    },

    service : 
    {
        weekdays:  "Dni robocze",
        saturday:  "Sobota",
        sunday:    "Niedziela",
        schedule:  "Rozk³ad"
    },

    indicators : 
    {
        ok         : "OK",
        date       : "Data",
        loading    : "£adowanie",
        searching  : "Szukanie...",
        qEmptyText : "Adres, skrzy¿owanie, obiekt lub ID przystanku..."
    },

    buttons: 
    {
        reverse       : "Odwróæ",
        reverseTip    : "<b>Odwróæ kierunki</b><br/>Zaplanuj podró¿ powrotn¹ poprzez zamianê miejscami punktu startowego i koñcowego podró¿y i przeskok czasu do przodu.",
        reverseMiniTip: "Odwróæ kierunki",

        edit          : "Edytuj",
        editTip       : "<b>Edytuj podró¿</b><br/>Powróæ do planowania podró¿y z detalami tej podró¿y.",

        clear         : "Wyczyœæ",
        clearTip      : "<b>Wyczyœæ</b><br/>Wyczyœæ mapê i wszystkie aktywne narzêdzia.",

        fullScreen    : "Pe³en ekran",
        fullScreenTip : "<b>Pe³en ekran</b><br/>Poka¿ lub ukryj panele narzêdzi",

        print         : "Drukuj",
        printTip      : "<b>Drukuj</b><br/>Wydrukuj plan podró¿y (bez mapy).",

        link          : "Link",
        linkTip      : "<b>Link</b><br/>Poka¿ link do tego planu podró¿y.",

        feedback      : "Opinie",
        feedbackTip   : "<b>Opinie</b><br/>Wyœlij swoje uwagi i doœwiadczenia z narzêdzia",

        submit       : "Wyœlij",
        clearButton  : "Wyczyœæ",
        ok           : "OK",
        cancel       : "Anuluj",
        yes          : "Tak",
        no           : "Nie"
    },

    // note: keep these lower case (and uppercase via template / code if needed)
    directions : 
    {
        southEast:      "po³udniowy wschód",
        southWest:      "po³udniowy zachód",
        northEast:      "pó³nocny wschód",
        northWest:      "pó³nocny zachód",
        north:          "pó³noc",
        west:           "zachód",
        south:          "po³udnie",
        east:           "wschód",
        bound:          "w kierunku",
        left:           "lewo",
        right:          "prawo",
        slightly_left:  "lekko w lewo",
        slightly_right: "lekko w prawo",
        hard_left:      "mocno w lewo",
        hard_right:     "mocno w prawo",
        'continue':     "kontynuuj",
        to_continue:    "kontynuowaæ",
        becomes:        "zmienia siê w",
        at:             "o"
    },

    time:
    {
        minute_abbrev:  "min",
        minutes_abbrev: "minut",
        second_abbrev: "sek",
        seconds_abbrev: "sekund",
        months:         ['Sty', 'Lut', 'Mar', 'Kwi', 'Maj', 'Cze', 'Lip', 'Sie', 'Wrz', 'Paz', 'Lis', 'Gru']
    },
    
    systemmap :
    {
        labels :
        {
            panelTitle : "Mapa systemowa"
        }
    },

    tripPlanner :
    {
        labels : 
        {
            panelTitle    : "Planer podró¿y",
            tabTitle      : "Zaplanuj podró¿",
            inputTitle    : "Szczegó³y podró¿y",
            optTitle      : "Preferencje podró¿y (opcjonalne)",
            submitMsg     : "Planuje Twoj¹ podró¿...",
            optionalTitle : "",
            date          : "Data",
            time          : "Godzina",
            when          : "Kiedy",
            from          : "Z",
            fromHere      : "Sk¹d",
            to            : "Do",
            toHere        : "Dok¹d",
            minimize      : "Poka¿",
            maxWalkDistance: "Maksymalny spacer",
            arriveDepart  : "Dojazd/odjazd o",
            mode          : "Podró¿uj",
            wheelchair    : "Podró¿ dostêpna dla niepe³nosprawnych", 
            go            : "IdŸ",
            planTrip      : "Planuj swoj¹ podró¿",
            newTrip       : "Nowa podró¿"
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
            google_domain  : "http://www.google.com"
        },

        error:
        {
            title        : 'B³ad planera podró¿y',
            deadMsg      : "Planer podró¿y nie odpowiada. Odczekaj kilka minut i spróbuj ponownie, lub spróbuj wersji tekstowej planera (zobacz link poni¿ej).",
            geoFromMsg   : "Wybierz lokalizacjê 'Z' dla Twojej podró¿y: ",
            geoToMsg     : "Wybierz lokalizacjê 'Do' dla Twojej podró¿y: "
        },
        
        // default messages from server if a message was not returned
        msgcodes:
        {
            200: "Plan OK",
            500: "B³¹d serwera",
            400: "Podró¿ poza obs³ugiwanym obszarem",
            404: "Trasa nieodnaleziona",
            406: "Brak czasów w rozk³adzie",
            408: "Limit czasu osi¹gniêty",
            413: "Niew³aœciwy parametr",
            440: "Geokod Z nieodnaleziony",
            450: "Geokod Do nieodnaleziony",
            460: "Geokody Z i Do nieodnalezione",
            409: "Zbyt blisko",
            340: "Geokod Z niejednoznaczny",
            350: "Geokod Do niejednoznaczny",
            360: "Geokody Z i Do niejednoznaczne"
        },

        options: 
        [
          ['TRANSFERS', 'Ma³o przesiadek'],
          ['QUICK',     'Najszybsza podró¿'],
          ['SAFE',      'Najbezpieczniejsza podró¿']
        ],
    
        arriveDepart: 
        [
          ['false', 'Odjazd'], 
          ['true',  'Przyjazd']
        ],
    
        maxWalkDistance : 
        [
            ['200',   '200 m'],
            ['500',   '500 m'],
            ['1000',   '1 km'],
            ['1500',  '1,5 km'],
            ['5000',  '5 km'],
            ['10000',  '10 km'],
        ],
    
        mode : 
        [
            ['TRANSIT,WALK', 'Transport publiczny'],
            ['BUSISH,TRAINISH,WALK', 'Autobus i tramwaj'],
            ['BUSISH,WALK', 'Tylko autobus'],
            ['TRAINISH,WALK', 'Tylko tramwaj'],
            ['WALK', 'Tylko spacer'],
            ['BICYCLE', 'Rower'],
            ['TRANSIT,BICYCLE', 'Transport publiczny i rower']
        ],

        wheelchair :
        [
            ['false', 'Niewymagane'],
            ['true', 'Wymagane']
        ]
    },

    CLASS_NAME : "otp.locale.Polish"
};
