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
otp.locale.Russian = {

    config :
    {
        metricsSystem : "Российская",
        rightClickMsg : "Щелкните правой кнопкой мыши по карте для задания начального и конечного пунктов маршрута.",
        attribution   : {
            title   : "Лицензионная атрибуция",
            content : "Отказ от ответственности здесь"
        }
    },

    contextMenu : 
    {
        fromHere         : "Начать маршрут",
        toHere           : "Завершить маршрут",
        intermediateHere : "Добавить остановочный пункт по маршруту",

        centerHere       : "Центрировать карту",
        zoomInHere       : "Увеличение масштаба",
        zoomOutHere      : "уменьшение масштаба",
        previous         : "Последняя позиция карты",
        next             : "Следующая позиция карты"
    },

    bikeTriangle : 
    {
        safeName : "Велопрогулки",
        safeSym  : "B",

        hillName : "Размеренно",
        hillSym  : "F",

        timeName : "Быстро",
        timeSym  : "Q"
    },

    service : 
    {
        weekdays:  "Выходные дни",
        saturday:  "Суббота",
        sunday:    "Воскресенье",
        schedule:  "Расписание"
    },

    indicators : 
    {
        ok         : "ОК",
        date       : "Дата",
        loading    : "Загрузка",
        searching  : "Идет поиск...",
        qEmptyText : "Адрес, пересечения, ориентир или координаты остановочного пункта..."
    },

    buttons: 
    {
        reverse       : "Обратно",
        reverseTip    : "<b>Обратные направления</b><br/>Планирование обратного маршрута путем автоматизированной замены пунктов отправления и назначения и корректировки времени.",
        reverseMiniTip: "Проложить обратный маршрут",

        edit          : "Добавить",
        editTip       : "<b>Добавить маршрут</b><br/>Вернуться на главную страницу поиска маршрута с подробностями.",

        clear         : "Очистить маршрут",
        clearTip      : "<b>Очистить</b><br/>Очистить карту и все активные компоненты",

        fullScreen    : "Развернуть",
        fullScreenTip : "<b>Развернуть</b><br/>Панель инструментов с кнопками свернуть/развернуть",

        print         : "Печать",
        printTip      : "<b>Печать</b><br/>Версия для печати маршрута поезки (без карты).",

        link          : "Ссылка",
        linkTip      : "<b>Ссылка</b><br/>Показать ссылку URL для маршрута",

        feedback      : "Обратная связь",
        feedbackTip   : "<b>Обратная связь</b><br/>Сообщить о своем мнении и об опыте работы с картой",

        submit       : "Отправить",
        clearButton  : "Очистить",
        ok           : "Далее",
        cancel       : "Отменить",
        yes          : "Да",
        no           : "Нет",
        Показать подробности  : "&darr; Показать подробности &darr;",
       Скрыть подробности  : "&uarr; Скрыть подробности &uarr;"
    },

    // note: keep these lower case (and uppercase via template / code if needed)
    directions : 
    {
        southeast:      "юго-восток",
        southwest:      "юго-запад",
        northeast:      "северо-восток",
        northwest:      "северо-запад",
        north:          "север",
        west:           "запад",
        south:          "юг",
        east:           "восток",
        bound:          "ограничение",
        left:           "налево",
        right:          "направо",
        slightly_left:  "плавный поворот налево",
        slightly_right: "плавные поворот направо",
        hard_left:      "резкий поворот налево",
        hard_right:     "резкий поворот направо",
        'continue':     "продолжить",
        to_continue:    "продолжить",
        becomes:        "обращение",
        at:             "в",
        on:             "на",
        to:             "к",
        via:            "через",
        circle_counterclockwise: "принять кольцевой маршрут против часовой стрелки",
        circle_clockwise:        "принять кольцевой маршрут по часовой стрелке",
        // rather than just being a direction, this should be
        // full-fledged to take just the exit name at the end
        elevator: "указать пересадку"
    },

    // see otp.planner.Templates for use
    instructions :
    {
        walk         : "пешком",
        walk_toward  : "пешком",
        walk_verb    : "пешком",
        bike         : "Велосипед",
        bike_toward  : "Велосипед",
        bike_verb    : "Велосипед",
        drive        : "Автомобиль",
        drive_toward : "Автомобиль",
        drive_verb   : "Автомобиль",
        move         : "Продолжить",
        move_toward  : "Продолжить",

        transfer     : "трансфер",
        transfers    : "трансфер",

        continue_as  : "Продолжить от",
        stay_aboard  : "борт транспортного средства",

        depart       : "Отправление",
        arrive       : "Прибытие",

        start_at     : "Откуда",
        end_at       : "Куда"
    },

    // see otp.planner.Templates for use
    labels : 
    {
        agency_msg   : "Предоставление услуг",
        agency_msg_tt: "Открыть сайт агенства в отдельном окне...",
        about        : "О компании",
        stop_id      : "Координаты остановочного пункта",
        trip_details : "Детали маршрута",
        travel       : "Путешествие",
        valid        : "Доступно",
        trip_length  : "Время",
        with_a_walk  : "С прогулкой",
        alert_for_rt : "Сигнал для маршрута",
        fare         : "Тариф",
        regular_fare : "Обычный",
        student_fare : "Студенческий",
        senior_fare  : "Льготный",
        fare_symbol  : "$"
    },

    // see otp.planner.Templates for use -- one output are the itinerary leg headers
    modes :
    {
        WALK:           "Пешком",
        BICYCLE:        "велосипед",
        CAR:            "Автомобиль",
        TRAM:           "Трамвай",
        SUBWAY:         "Метро",
        RAIL:           "Поезд",
        BUS:            "Автобус",
        FERRY:          "Паром",
        CABLE_CAR:      "Канатная дорога",
        GONDOLA:        "Гондола",
        FUNICULAR:      "Фуникулер"
    },

    ordinal_exit:
    {
        1:  "к первому выходу",
        2:  "ко второму выходу",
        3:  "к третьему выходу",
        4:  "к четвертому выходу",
        5:  "к пятому выходу",
        6:  "к шестому выходу",
        7:  "к седьмому выходу",
        8:  "к восьмому выходу",
        9:  "к девятому выходу",
        10: "к десятому выходу"
    },

    time:
    {
        hour_abbrev    : "ч",
        hours_abbrev   : "ч",
        hour           : "час",
        hours          : "часов",

        minute         : "минута",
        minutes        : "минут",
        minute_abbrev  : "мин",
        minutes_abbrev : "мин",
        second_abbrev  : "сек",
        seconds_abbrev : "сек",
        format         : "F jS, Y @ g:ia",
        date_format    : "j/n/Y",
        time_format    : "g:ia",
        months         : ['Янв', 'Фев', 'Мар', 'Апр', 'Май', 'Июн', 'Июл', 'Авг', 'Сен', 'Окт', 'Ноя', 'Дек']
    },

    systemmap :
    {
        labels :
        {
            panelTitle : "Системная карта"
        }
    },

    tripPlanner :
    {
        // see otp/planner/*.js for where these values are used
        labels : 
        {
            panelTitle    : "Планировшик маршрутов",
            tabTitle      : "Составить маршрут",
            inputTitle    : "Подробности маршрута",
            optTitle      : "Предпочтения маршрута (опционально)",
            submitMsg     : "Идет поиск Вашего маршрута...",
            optionalTitle : "Опции",
            date          : "Дата",
            time          : "Время",
            when          : "Когда",
            from          : "Откуда",
            fromHere      : "Отсюда",
            to            : "Куда",
            toHere        : "Сюда",
            intermediate  : "Остановочные пункты на маршруте",
            minimize      : "Показать",
            maxWalkDistance: "Предпочтительнее пешком",
            walkSpeed     : "Скорость пешехода",
            maxBikeDistance: "Предпочтительнее на велосипеде",
            bikeSpeed     : "Скорость велосипеда",
            arriveDepart  : "Отправление/Прибытие",
            mode          : "Транспортное средство",
            wheelchair    : "Маршруты для людей с ограниченными способностями", 
            go            : "Далее",
            planTrip      : "Проложить маршрут",
            newTrip       : "Новый маршрут"
        },

        // see otp/config.js for where these values are used
        link : 
        {
            text           : "Ссылка на этот маршрут",
            trip_separator : "Данный маршрут на других транзитных планировщиках маршрутов",
            bike_separator : "На других планировщиках велопрогулок",
            walk_separator : "на других планировщиках пешеходных прогулок",
            google_transit : "Google Транзит",
            google_bikes   : "Велопрогулки на Google",
            google_walk    : "Пешеходные прогулки на Google",
            google_domain  : "http://www.google.com"
        },

        // see otp.planner.Forms for use
        geocoder:
        {
            working      : "Проверка доступности адреса ....",
            error        : "Поиск не дал результатов",
            msg_title    : "Проверить план маршрута",
            msg_content  : "Пожалуйста, исправьте ошибки до проложения маршрута",
            select_result_title : "Пожалуйста, выберите результат",
            address_header : "Адрес"
        },

        error:
        {
            title        : 'Ошибка планировщика маршрутов',
            deadMsg      : "В настоящее время карта планировщика маршрутов недоступна. Пожалуйста, попробуйте еще раз через несколько минут или воспользуйтесь планировщиком маршутов в текстовом виде(см ссылку ниже).",
            geoFromMsg   : "Пожалуйста, заполните поле "Откуда" : ",
            geoToMsg     : "Пожалуйста заполните поле "Куда": "
        },
        
        // default messages from server if a message was not returned ... 'Place' error messages also used when trying to submit without From & To coords.
        msgcodes:
        {
            200: "Проложить маршрут",
            500: "Ошибка на сервере",
            400: "Недоступный маршрут",
            404: "Путь не найден",
            406: "Недействительное значение времени",
            408: "Истекло время запроса",
            413: "Недействительный параметр",
            440: "Остановочный пункт 'Откуда' не найден ... Пожалуйста, укажите его снова.",
            450: "Остановочный пункт 'Куда' не найден ... Пожалуйста, укажите его снова.",
            460: "Остановочные пункты 'Откуда' и 'Куда'не найдены ... Пожалуйста,  укажите их снова.",
            470: "Остановочные пункты 'Откуда' или 'Куда' не доступны для людей с повышенными потребностями",
            409: "Слишком близко",
            340: "Запрос 'Откуда' слишком сложный",
            350: "Запрос 'Куда' слишком сложный",
            360: "Запросы 'Откуда' и 'Куда' слишком сложные"
        },

        options: 
        [
          ['TRANSFERS', 'Fewest transfers'],
          ['QUICK',     'Quick trip'],
          ['SAFE',      'Bike friendly trip'],
          ['TRIANGLE',  'Custom trip...']
        ],
    
        arriveDepart: 
        [
          ['false', 'Depart'], 
          ['true',  'Arrive']
        ],
    
        maxWalkDistance : 
        [
            ['160',   '1/10 mile'],
            ['420',   '1/4 mile'],
            ['840',   '1/2 mile'],
            ['1260',  '3/4 mile'],
            ['1609',  '1 mile'],
            ['3219',  '2 miles'],
            ['4828',  '3 miles'],
            ['6437',  '4 miles'],
            ['8047',  '5 miles'],
            ['16093',  '10 miles'],
            ['24140',  '15 miles'],
            ['32187',  '20 miles'],
            ['48280',  '30 miles'],
            ['64374',  '40 miles'],
            ['80467',  '50 miles'],
            ['160934',  '100 miles']
        ],

  walkSpeed :
	[
		['0.447',  '1 mph'],
		['0.894',  '2 mph'],
		['1.341',  '3 mph'],
		['1.788',  '4 mph'],
		['2.235',  '5 mph']
	],

        mode : 
        [
            ['TRANSIT,WALK', 'Transit'],
            ['BUSISH,WALK', 'Bus only'],
            ['TRAINISH,WALK', 'Train only'],
            ['WALK', 'Walk only'],
            ['BICYCLE', 'Bicycle only'],
            ['TRANSIT,BICYCLE', 'Transit & Bicycle']
        ],

        // TODO: remove this hack, and provide code that allows the mode array to be configured with different transit modes.
        //       (note that we've been broken for awhile here, since many agencies don't have a 'Train' mode either...this needs attention)
        // IDEA: maybe we start with a big array (like below), and the pull out modes from this array when turning off various modes...
        with_bikeshare_mode : 
        [
            ['TRANSIT,WALK', 'Transit'],
            ['BUSISH,WALK', 'Bus only'],
            ['TRAINISH,WALK', 'Train only'],
            ['WALK', 'Walk only'],
            ['BICYCLE', 'Bicycle only'],
            ['WALK,BICYCLE', 'Rented Bicycle'],
            ['TRANSIT,BICYCLE', 'Transit & Bicycle'],
            ['TRANSIT,WALK,BICYCLE', 'Transit & Rented Bicycle']
        ],

        wheelchair :
        [
            ['false', 'Not required'],
            ['true', 'Required']
        ]
    },

    CLASS_NAME : "otp.locale.Russian"
};

