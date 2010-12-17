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
otp.locale.Hungarian = {

    contextMenu : 
    {
        fromHere         : "Útvonal kezdete itt",
        toHere           : "Útvonal vége itt",

        centerHere       : "Térkép középre helyezése ide",
        zoomInHere       : "Közelítés ide",
        zoomOutHere      : "Távolítás innen",
        previous         : "Legutóbbi térképpozíció",
        next             : "Következő térképpozíció"
    },

    service : 
    {
        weekdays:  "Hétköznap",
        saturday:  "Szombat",
        sunday:    "Vasárnap",
        schedule:  "Menetrend"
    },

    indicators : 
    {
        ok         : "OK",
        date       : "Dátum",
        loading    : "Betöltés",
        searching  : "Keresés...",
        qEmptyText : "Cím, útkereszteződés, jellegzetes pont vagy megálló azonosító..."
    },

    buttons: 
    {
        reverse       : "Megfordítás",
        reverseTip    : "<b>Irány megfordítása</b><br/>Visszaút tervezése a kezdő- és végpont megfordításával, és az idő későbbre állításával.",
        reverseMiniTip: "Irány megfordítása",

        edit          : "Szerkesztés",
        editTip       : "<b>Útvonal szerkesztése</b><br/>Vissza a fő útvonaltervező beviteli űrlaphoz ennek az útvonalnak a részleteivel.",

        clear         : "Törlés",
        clearTip      : "<b>Törlés</b><br/>A térkép és az összes aktív eszköz törlése.",

        fullScreen    : "Teljes képernyő",
        fullScreenTip : "<b>Teljes képernyő</b><br/>Eszközpanel megjelenítése -vagy- elrejtése",

        print         : "Nyomtatás",
        printTip      : "<b>Nyomtatás</b><br/>Az útvonalterv nyomtatóbarát változata (térkép nélkül).",

        link          : "Hivatkozás",
        linkTip      : "<b>Hivatkozás</b><br/>Hivatkozó URL megjelenítése ehhez az útvonaltervhez.",

        feedback      : "Visszajelzés",
        feedbackTip   : "<b>Visszajelzés</b><br/>Küldje el gondolatait vagy tapasztalatait a térképpel kapcsolatban",

        submit       : "Küldés",
        clearButton  : "Törlés",
        ok           : "OK",
        cancel       : "Mégse",
        yes          : "Igen",
        no           : "Nem"
    },

    // note: keep these lower case (and uppercase via template / code if needed)
    directions : 
    {
        southEast:      "délkelet",
        southWest:      "délnyugat",
        northEast:      "északkelet",
        northWest:      "északnyugat",
        north:          "észak",
        west:           "nyugat",
        south:          "dél",
        east:           "kelet",
        bound:          "határ",
        left:           "balra",
        right:          "jobbra",
        slightly_left:  "enyhén balra",
        slightly_right: "enyhén jobbra",
        hard_left:      "élesen balra",
        hard_right:     "élesen jobbra",
        'continue':     "haladjon ezen:",
        to_continue:    "haladjon ezen:",
        becomes:        "evvé válik:",
        at:             "ide:"
    },

    time:
    {
        minute_abbrev:  "perc",
        minutes_abbrev: "perc",
        second_abbrev: "másodperc",
        seconds_abbrev: "másodperc",
        months:         ['jan', 'feb', 'már', 'ápr', 'máj', 'jún', 'júl', 'aug', 'szep', 'okt', 'nov', 'dec']
    },
    
    systemmap :
    {
        labels :
        {
            panelTitle : "Rendszertérkép"
        }
    },

    tripPlanner :
    {
        labels : 
        {
            panelTitle    : "Útvonaltervező",
            tabTitle      : "Útvonal tervezése",
            inputTitle    : "Útvonal részletei",
            optTitle      : "Útvonal beállításai (opcionális)",
            submitMsg     : "Útvonal tervezése...",
            optionalTitle : "",
            date          : "Dátum",
            time          : "Idő",
            when          : "Mikor",
            from          : "Honnan",
            fromHere      : "Innen",
            to            : "Hová",
            toHere        : "Ide",
            minimize      : "A következő megjelenítése",
            maxWalkDistance: "Maximális gyaloglás",
            arriveDepart  : "Érkezés/indulás",
            mode          : "Utazás ezzel",
            wheelchair    : "Kerekesszékkel megtehető útvonal", 
            go            : "Menj",
            planTrip      : "Útvonal tervezése",
            newTrip       : "Új útvonal"
        },

        error:
        {
            title        : 'Útvonaltervező hiba',
            deadMsg      : "A térképes útvonaltervező jelenleg nem válaszol. Kérem, várjon néhány percet, és próbálja újra, vagy próbálja a szöveges útvonaltervezővel (lásd a hivatkozást alább).",
            geoFromMsg   : "Kérem, válasszon kezdőpontot az útvonalhoz: ",
            geoToMsg     : "Kérem, válasszon végpontot az útvonalhoz: "
        },
        
        // default messages from server if a message was not returned
        msgcodes:
        {
            200: "Tervezés OK",
            500: "Szerverhiba",
            400: "Az útvonal határon kívül",
            404: "Nem található útvonal",
            406: "Nincs közlekedési idő",
            408: "A kérés túllépte az időt",
            413: "Érvénytelen paraméter",
            440: "A kezdőpont geokódja nem található",
            450: "A végpont geokódja nem található",
            460: "A kezdő- és végpont geokódja nem található",
            409: "Túl közel",
            340: "A kezdőpont geokódja nem egyértelmű",
            350: "A végpont geokódja nem egyértelmű",
            360: "A kezdő- és végpont geokódja nem egyértelmű"
        },

        options: 
        [
          ['TRANSFERS', 'Legkevesebb átszállással'],
          ['QUICK',     'Leggyorsabb útvonal'],
          ['SAFE',      'Legbiztonságosabb útvonal']
        ],
    
        arriveDepart: 
        [
          ['false', 'Indulás'], 
          ['true',  'Érkezés']
        ],
    
        maxWalkDistance : 
        [
            ['100',   '100 m'],
            ['500',   '500 m'],
            ['1000',  '1 km'],
            ['5000',  '5 km'],
            ['10000', '10 km'],
            ['50000', '50 km'],
            ['100000','100 km']
        ],
    
        mode : 
        [
            ['TRANSIT,WALK', 'Tömegközlekedés'],
            ['BUSISH,TRAINISH,WALK', 'Busz és vonat'],
            ['BUSISH,WALK', 'Csak busz'],
            ['TRAINISH,WALK', 'Csak vonat'],
            ['WALK', 'Csak gyalog'],
            ['BICYCLE', 'Kerékpár'],
            ['TRANSIT,BICYCLE', 'Tömegközlekedés és kerékpár']
        ],

        wheelchair :
        [
            ['false', 'Nem szükséges'],
            ['true', 'Szükséges']
        ]
    },

    CLASS_NAME : "otp.locale.Hungarian"
};
