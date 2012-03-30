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
        rightClickMsg : "Kliknij prawym przyciskiem myszy i wybierz początke i koniec podróży.",
        attribution   : {
            title   : "License Attribution",
            content : "Disclaimer goes here"
        }
    },

    contextMenu : 
    {
        fromHere         : "Rozpocznij podróż tutaj",
        toHere           : "Zakończ podróż tutaj",
        intermediateHere : "Dodaj punkt pośredni",  // TODO localize

        centerHere       : "Centruj mapę tutaj",
        zoomInHere       : "Przybliż tutaj",
        zoomOutHere      : "Oddal stąd",
        previous         : "Poprzednia pozycja na mapie",
        next             : "Następna pozycja na mapie"
    },

    // TODO Localize Me
    bikeTriangle : 
    {
        safeName : "Najbezpieczniejsza",
        safeSym  : "S",

        hillName : "Najbardziej płaska",
        hillSym  : "F",

        timeName : "Najszybsza",
        timeSym  : "Q"
    },

    service : 
    {
        weekdays:  "Dni robocze",
        saturday:  "Sobota",
        sunday:    "Niedziela",
        schedule:  "Rozkład"
    },

    indicators : 
    {
        ok         : "OK",
        date       : "Data",
        loading    : "Ładowanie",
        searching  : "Szukanie...",
        qEmptyText : "Adres, skrzyżowanie, obiekt lub ID przystanku..."
    },

    buttons: 
    {
        reverse       : "Odwróć",
        reverseTip    : "<b>Odwróć kierunki</b><br/>Zaplanuj podróż powrotną poprzez zamianę miejscami punktu startowego i końcowego podróży i przeskok czasu do przodu.",
        reverseMiniTip: "Odwróć kierunki",

        edit          : "Edytuj",
        editTip       : "<b>Edytuj podróż</b><br/>Powróć do planowania podróży z detalami tej podróży.",

        clear         : "Wyczyść",
        clearTip      : "<b>Wyczyść</b><br/>Wyczyść mapę i wszystkie aktywne narzędzia.",

        fullScreen    : "Pełen ekran",
        fullScreenTip : "<b>Pełen ekran</b><br/>Pokaż lub ukryj panele narzędzi",

        print         : "Drukuj",
        printTip      : "<b>Drukuj</b><br/>Wydrukuj plan podróży (bez mapy).",

        link          : "Link",
        linkTip      : "<b>Link</b><br/>Pokaż link do tego planu podróży.",

        feedback      : "Opinie",
        feedbackTip   : "<b>Opinie</b><br/>Wyślij swoje uwagi i doświadczenia z narzędzia",

        submit       : "Wyślij",
        clearButton  : "Wyczyść",
        ok           : "OK",
        cancel       : "Anuluj",
        yes          : "Tak",
        no           : "Nie",
// TODO
        showDetails  : "Show details...",
        hideDetails  : "Hide details..."
    },

    // note: keep these lower case (and uppercase via template / code if needed)
    directions : 
    {
        southeast:      "południowy wschód",
        southwest:      "południowy zachód",
        northeast:      "północny wschód",
        northwest:      "północny zachód",
        north:          "północ",
        west:           "zachód",
        south:          "południe",
        east:           "wschód",
        bound:          "w kierunku",
        left:           "lewo",
        right:          "prawo",
        slightly_left:  "lekko w lewo",
        slightly_right: "lekko w prawo",
        hard_left:      "mocno w lewo",
        hard_right:     "mocno w prawo",
        'continue':     "kontynuuj",
        to_continue:    "kontynuować",
        becomes:        "zmienia się w",
        at:             "o",
        on:             "na",
        to:             "do",
        via:            "przez",
        circle_counterclockwise: "okrąż rondo przeciwnie do kierunku wskazówek zegara",
        circle_clockwise:        "okrąż rondo zgodnie ze wskazówkami zegara"
    },

    // see otp.planner.Templates for use ... these are used on the trip itinerary as well as forms and other places
    instructions :
    {
// TODO
        walk         : "Idź",
        walk_toward  : "Idż",
        walk_verb    : "Idź",
        bike         : "Jedź",
        bike_toward  : "Jedź",
        bike_verb    : "Jedź",
        drive        : "Jedź",
        drive_toward : "Jedź",
        drive_verb   : "Jedź",
        move         : "Podążaj",
        move_toward  : "Podążaj",

        transfer     : "przesiadka",
        transfers    : "przesiadek",

        continue_as  : "Kontynuuje jako",
        stay_aboard  : "pozostań w pojeździe",

        depart       : "Odjeżdza",
        arrive       : "Przyjeżdza",

        start_at     : "Rozpocznij",
        end_at       : "Skończ"
    },

    // see otp.planner.Templates for use
    labels : 
    {
        agency_msg   : "Linia obsługiwana przez", 
        agency_msg_tt: "Otwórz stronę przewoźnika w nowym oknie...", 
        about        : "Informacje",
        stop_id      : "ID Przystanku",
        trip_details : "Szczegóły trasy",
        fare         : "Opłata",
        fare_symbol  : "zł",
        travel       : "Początek podróży",
        valid        : "Ważny",
        trip_length  : "Czasowy",
        with_a_walk  : "wraz z dojściem",
        alert_for_rt : "Wiadomość dla linii"
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
        1:  "do pierwszego wyjśćia",
        2:  "do drugiego wyjśćia",
        3:  "do trzeciego wyjśćia",
        4:  "do czwartego wyjśćia",
        5:  "do piątego wyjśćia",
        6:  "do szóstego wyjśćia",
        7:  "do siódmego wyjśćia",
        8:  "do ósmego wyjśćia",
        9:  "do dziewiątego wyjśćia",
        10: "do dziesiątego wyjśćia"
    },

    time:
    {
// TODO
        format        : "D, j M H:i",
        date_format   : "d-m-Y",
        time_format   : "H:i",
        minute        : "minuta",
        minutes       : "minut",
        minute_abbrev : "min",
        minutes_abbrev: "minut",
        second_abbrev : "sek",
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
            panelTitle    : "Planer podróży",
            tabTitle      : "Zaplanuj podróż",
            inputTitle    : "Szczegóły podróży",
            optTitle      : "Preferencje podróży (opcjonalne)",
            submitMsg     : "Planuje Twoją podróż...",
            optionalTitle : "",
            date          : "Data",
            time          : "Godzina",
            when          : "Kiedy",
            from          : "Z",
            fromHere      : "Skąd",
            to            : "Do",
            toHere        : "Dokąd",
            intermediate  : "Intermediate Place",            // TODO
            minimize      : "Pokaż",
            maxWalkDistance: "Maksymalny spacer",
            maxBikeDistance: "Maksymalny bike",              // TODO
            arriveDepart  : "Dojazd/odjazd o",
            mode          : "Podróżuj",
            wheelchair    : "Podróż dostępna dla niepełnosprawnych", 
            go            : "Idź",
            planTrip      : "Planuj swoją podróż",
            newTrip       : "Nowa podróż"
        },

        // see otp/config.js for where these values are used
        link : 
        {
// TODO
            text           : "Link do tej podróży (OTP)",
            trip_separator : "This trip on other transit planners",
            bike_separator : "On other bike trip planners",
            walk_separator : "On other walking direction planners",
            google_transit : "Google Transit",
            google_bikes   : "Google Bike Directions",
            google_walk    : "Google Walking Directions",
            google_domain  : "http://www.google.pl"
        },

        // see otp.planner.Forms for use
        geocoder:
        {
//TODO
            working      : "Poszukuje adresu ....",
            error        : "Brak pasujących wyników",
            msg_title    : "Czy chciałbyś ocenić zaproponowana podróż",
            msg_content  : "Les correct errors before planning your trip",
            select_result_title : "Wybierz adres",
            address_header : "Adres"
        },

        error:
        {
            title        : 'Bład planera podróży',
            deadMsg      : "Planer podróży nie odpowiada. Odczekaj kilka minut i spróbuj ponownie, lub spróbuj wersji tekstowej planera (zobacz link poniżej).",
            geoFromMsg   : "Wybierz lokalizację 'Z' dla Twojej podróży: ",
            geoToMsg     : "Wybierz lokalizację 'Do' dla Twojej podróży: "
        },
        
        // default messages from server if a message was not returned
        msgcodes:
        {
            200: "Plan OK",
            500: "Błąd serwera",
            400: "Podróż poza obsługiwanym obszarem",
            404: "Trasa nieodnaleziona",
            406: "Brak czasów w rozkładzie",
            408: "Limit czasu osiągnięty",
            413: "Niewłaściwy parametr",
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
          ['TRANSFERS', 'Mało przesiadek'],
          ['QUICK',     'Najszybsza podróż'],
          ['SAFE',      'Najbezpieczniejsza podróż'],
          ['TRIANGLE',  'Mieszane preferencje...']  // TODO localize
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
            ['10000',  '10 km']
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
