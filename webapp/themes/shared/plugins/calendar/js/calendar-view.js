/*
 * Plugin calendar - EventCalendar views of the front and back offices.
 *
 * A container <div data-calendar-view> carries the configuration in data-* attributes:
 *   data-feed          URL of the JSON feed of the occurrences (parameters start, end, calendar_id, category_id)
 *   data-date          initial day (yyyyMMdd or yyyy-MM-dd), today when empty
 *   data-view          initial view: day, week, month or list (on small screens list when empty)
 *   data-locale        language of the dates
 *   data-label-*       texts of the toolbar: today, day, week, month, list, previous, next, no-events
 *   data-sync-url      "true" to keep the date and the view in the address of the page
 *   data-day-max-events  events shown in a day of the month view before a "+n" link, 4 when empty. A number, not true:
 *                      true makes every week row as tall as the fullest one, a number lets each row fit its own events
 * Back office only:
 *   [data-calendar-scope="<container id>"] a Bootstrap modal whose buttons [data-scope] answer occurrence, series or cancel
 *   [data-create-calendar="<container id>"] a select giving the calendar of an event created by a click on a day
 *   data-move-url      URL of the action moving an occurrence (POST), data-move-token its first token
 *   data-create-url    URL of the event creation, the clicked day is added as event_date=yyyy-MM-dd; in the week and the
 *                      day views, the clicked hour (for one hour) or the dragged range of hours adds event_time_start and
 *                      event_time_end (HH:mm)
 * Filters: checkboxes [data-calendar-filter="<container id>"] whose value is a calendar id, and a select
 * [data-category-filter="<container id>"] whose value is a category id.
 * A [data-calendar-fallback="<container id>"] block (the text list rendered by the server) is hidden once the view runs.
 */
( () => {
    'use strict';

    const VIEWS = { day: 'timeGridDay', week: 'timeGridWeek', month: 'dayGridMonth', list: 'listMonth' };
    const VIEW_KEYS = { timeGridDay: 'day', timeGridWeek: 'week', dayGridMonth: 'month', listMonth: 'list', listWeek: 'list', listDay: 'list' };
    const SMALL_SCREEN = window.matchMedia( '(max-width: 767.98px)' );
    const DAY_MAX_EVENTS = 4;

    const pad = n => String( n ).padStart( 2, '0' );
    const isoDate = d => `${d.getFullYear( )}-${pad( d.getMonth( ) + 1 )}-${pad( d.getDate( ) )}`;
    const isoDateTime = d => `${isoDate( d )}T${pad( d.getHours( ) )}:${pad( d.getMinutes( ) )}:00`;
    const compactDate = d => isoDate( d ).replaceAll( '-', '' );
    const hourMinute = d => `${pad( d.getHours( ) )}:${pad( d.getMinutes( ) )}`;
    // The last minute of a range: a range ending at midnight ends on the day it started
    const lastMinute = d => new Date( d.getTime( ) - 60000 );

    const parseDay = value => {
        const digits = ( value || '' ).replaceAll( '-', '' );
        if ( !/^\d{8}$/.test( digits ) ) {
            return new Date( );
        }
        return new Date( Number( digits.slice( 0, 4 ) ), Number( digits.slice( 4, 6 ) ) - 1, Number( digits.slice( 6, 8 ) ) );
    };

    const announce = ( container, message ) => {
        const region = document.querySelector( `[data-calendar-status="${container.id}"]` );
        if ( region ) {
            region.textContent = message || '';
        }
    };

    const askScope = container => new Promise( resolve => {
        const element = document.querySelector( `[data-calendar-scope="${container.id}"]` );
        if ( !element || !window.bootstrap || !window.bootstrap.Modal ) {
            resolve( 'occurrence' );
            return;
        }
        const modal = window.bootstrap.Modal.getOrCreateInstance( element );
        let choice = 'cancel';
        const onClick = event => {
            const button = event.target.closest( '[data-scope]' );
            if ( button ) {
                choice = button.dataset.scope;
                modal.hide( );
            }
        };
        element.addEventListener( 'click', onClick );
        element.addEventListener( 'hidden.bs.modal', ( ) => {
            element.removeEventListener( 'click', onClick );
            resolve( choice );
        }, { once: true } );
        modal.show( );
    } );

    const buildContent = ( info, editable ) => {
        const props = info.event.extendedProps || {};
        const link = document.createElement( 'a' );
        link.className = 'calendar-event-link';
        link.href = props.url || '#';
        if ( editable ) {
            link.draggable = false;
        }
        const label = [ info.timeText, info.event.title, props.calendarName, [ props.location, props.town ].filter( Boolean ).join( ', ' ) ]
            .filter( Boolean ).join( ' - ' );
        link.title = label;
        if ( info.view.type.startsWith( 'list' ) && props.color ) {
            const dot = document.createElement( 'span' );
            dot.className = 'calendar-dot me-1';
            dot.style.setProperty( '--calendar-dot-color', props.color );
            dot.setAttribute( 'aria-hidden', 'true' );
            link.append( dot );
        }
        if ( info.timeText && !info.view.type.startsWith( 'list' ) ) {
            const time = document.createElement( 'span' );
            time.className = 'calendar-event-time';
            time.textContent = info.timeText;
            link.append( time, ' ' );
        }
        const title = document.createElement( 'span' );
        title.className = 'calendar-event-title';
        title.textContent = info.event.title;
        link.append( title );
        return { domNodes: [ link ] };
    };

    const init = container => {
        const ds = container.dataset;
        const editable = Boolean( ds.moveUrl );
        let moveToken = ds.moveToken || '';
        let calendar = null;

        const calendarFilters = ( ) => [ ...document.querySelectorAll( `[data-calendar-filter="${container.id}"]` ) ];
        const categoryFilter = ( ) => document.querySelector( `[data-category-filter="${container.id}"]` );

        const fetchEvents = fetchInfo => {
            const params = new URLSearchParams( { start: isoDateTime( fetchInfo.start ), end: isoDateTime( fetchInfo.end ) } );
            const filters = calendarFilters( );
            if ( filters.length ) {
                const checked = filters.filter( box => box.checked );
                if ( !checked.length ) {
                    return [ ];
                }
                checked.forEach( box => params.append( 'calendar_id', box.value ) );
            }
            const category = categoryFilter( );
            if ( category && category.value ) {
                params.append( 'category_id', category.value );
            }
            const separator = ds.feed.includes( '?' ) ? '&' : '?';
            return fetch( `${ds.feed}${separator}${params}`, { credentials: 'same-origin', headers: { Accept: 'application/json' } } )
                .then( response => {
                    if ( !response.ok ) {
                        throw new Error( response.statusText );
                    }
                    return response.json( );
                } );
        };

        const move = async info => {
            const props = info.event.extendedProps || {};
            let scope = 'occurrence';
            if ( Number( props.seriesSize ) > 1 ) {
                scope = await askScope( container );
                if ( scope === 'cancel' ) {
                    info.revert( );
                    return;
                }
            }
            const body = new URLSearchParams( {
                action: 'moveOccurrence',
                occurrence_id: props.occurrenceId,
                start: info.event.allDay ? isoDate( info.event.start ) : isoDateTime( info.event.start ),
                end: info.event.end ? isoDateTime( info.event.end ) : '',
                all_day: String( info.event.allDay ),
                scope,
                token: moveToken
            } );
            try {
                const response = await fetch( ds.moveUrl, { method: 'POST', credentials: 'same-origin', body, headers: { Accept: 'application/json' } } );
                const result = await response.json( );
                moveToken = result.token || moveToken;
                if ( !result.ok ) {
                    info.revert( );
                    announce( container, result.message );
                    return;
                }
                announce( container, ds.labelMoved || '' );
                if ( scope === 'series' ) {
                    calendar.refetchEvents( );
                }
            } catch ( error ) {
                info.revert( );
                announce( container, ds.labelError || String( error ) );
            }
        };

        const requested = VIEWS[ ds.view ];
        const initialView = requested || ( SMALL_SCREEN.matches ? 'listWeek' : 'dayGridMonth' );
        const labels = {
            today: ds.labelToday, dayGridMonth: ds.labelMonth, timeGridWeek: ds.labelWeek, timeGridDay: ds.labelDay,
            listMonth: ds.labelList, listWeek: ds.labelList, listDay: ds.labelList
        };
        const toolbarEnd = SMALL_SCREEN.matches ? 'listWeek,dayGridMonth' : 'dayGridMonth,timeGridWeek,timeGridDay,listMonth';

        const options = {
            view: initialView,
            date: parseDay( ds.date ),
            locale: ds.locale || document.documentElement.lang || undefined,
            firstDay: 1,
            height: 'auto',
            dayMaxEvents: Number( ds.dayMaxEvents ) || DAY_MAX_EVENTS,
            nowIndicator: true,
            scrollTime: '08:00:00',
            headerToolbar: { start: 'prev,next today', center: 'title', end: toolbarEnd },
            buttonText: text => {
                const merged = { ...text };
                Object.entries( labels ).forEach( ( [ key, value ] ) => {
                    if ( value ) {
                        merged[ key ] = value;
                    }
                } );
                if ( ds.labelPrevious ) {
                    merged.prev = ds.labelPrevious;
                }
                if ( ds.labelNext ) {
                    merged.next = ds.labelNext;
                }
                return merged;
            },
            allDayContent: ds.labelAllDay || undefined,
            noEventsContent: ds.labelNoEvents || undefined,
            eventSources: [ { events: fetchEvents } ],
            eventContent: info => buildContent( info, editable ),
            editable,
            eventStartEditable: editable,
            eventDurationEditable: editable,
            datesSet: info => {
                if ( ds.syncUrl !== 'true' ) {
                    return;
                }
                const url = new URL( window.location.href );
                url.searchParams.set( 'date', compactDate( info.view.currentStart ) );
                url.searchParams.set( 'view', VIEW_KEYS[ info.view.type ] || 'month' );
                window.history.replaceState( null, '', url );
            }
        };

        if ( editable ) {
            options.eventDrop = move;
            options.eventResize = move;
        }

        if ( ds.createUrl ) {
            const openCreation = ( date, timeStart, timeEnd ) => {
                const url = new URL( ds.createUrl, document.baseURI );
                url.searchParams.set( 'event_date', isoDate( date ) );
                if ( timeStart ) {
                    url.searchParams.set( 'event_time_start', timeStart );
                    url.searchParams.set( 'event_time_end', timeEnd );
                }
                const target = document.querySelector( `[data-create-calendar="${container.id}"]` );
                if ( target && target.value ) {
                    url.searchParams.set( 'calendar_id', target.value );
                }
                window.location.href = url.href;
            };
            // Week and day views: a click on an hour opens the creation from this hour, for one hour within the day
            options.dateClick = info => {
                if ( info.allDay ) {
                    openCreation( info.date );
                    return;
                }
                const end = new Date( Math.min( info.date.getTime( ) + 3600000, new Date( info.date ).setHours( 23, 59, 0, 0 ) ) );
                openCreation( info.date, hourMinute( info.date ), hourMinute( end ) );
            };
            // Week and day views: a range of hours dragged in one day opens the creation with these hours
            options.selectable = true;
            options.selectConstraint = info => info.allDay || isoDate( info.start ) === isoDate( lastMinute( info.end ) );
            options.select = info => {
                if ( info.allDay ) {
                    openCreation( info.start );
                    return;
                }
                const end = isoDate( info.end ) === isoDate( info.start ) ? hourMinute( info.end ) : hourMinute( lastMinute( info.end ) );
                openCreation( info.start, hourMinute( info.start ), end );
            };
        }

        calendar = EventCalendar.create( container, options );

        calendarFilters( ).forEach( box => box.addEventListener( 'change', ( ) => calendar.refetchEvents( ) ) );
        const category = categoryFilter( );
        if ( category ) {
            category.addEventListener( 'change', ( ) => calendar.refetchEvents( ) );
        }
        document.querySelectorAll( `[data-calendar-fallback="${container.id}"]` ).forEach( block => {
            block.hidden = true;
        } );
        container.calendar = calendar;
    };

    const start = ( ) => {
        if ( typeof EventCalendar === 'undefined' ) {
            return;
        }
        document.querySelectorAll( '[data-calendar-view]' ).forEach( init );
    };

    if ( document.readyState === 'loading' ) {
        document.addEventListener( 'DOMContentLoaded', start );
    } else {
        start( );
    }
} )( );
