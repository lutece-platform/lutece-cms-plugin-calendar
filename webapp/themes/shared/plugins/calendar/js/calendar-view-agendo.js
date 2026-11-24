/*
 * Plugin calendar - Agendo views of the front and back offices, used when the advanced parameter "calendar library"
 * is agendo. Same contract as calendar-view.js (EventCalendar): a container <div data-calendar-view> carries the
 * configuration in data-* attributes, and only one of the two scripts is loaded by a page.
 *   data-feed          URL of the JSON feed of the occurrences (parameters start, end, calendar_id, category_id)
 *   data-date          initial day (yyyyMMdd or yyyy-MM-dd), today when empty
 *   data-view          initial view: day, week, month or list (on small screens list when empty)
 *   data-locale        language of the dates
 *   data-theme         Agendo theme: tabler (back office) or bootstrap (site)
 *   data-label-*       texts of the toolbar: today, day, week, month, list, previous, next
 *   data-sync-url      "true" to keep the date and the view in the address of the page
 *   data-day-max-events  events shown in a day of the month view before a "+n" link, 4 when empty
 * Back office only:
 *   [data-calendar-scope="<container id>"] a Bootstrap modal whose buttons [data-scope] answer occurrence, series or cancel
 *   [data-create-calendar="<container id>"] a select giving the calendar of an event created in the view
 *   data-move-url      URL of the action moving an occurrence (POST), data-move-token its first token
 *   data-create-url    URL of the event creation, with event_date and, for a range of hours, event_time_start and
 *                      event_time_end
 * Filters: checkboxes [data-calendar-filter="<container id>"] whose value is a calendar id, and a select
 * [data-category-filter="<container id>"] whose value is a category id.
 * A [data-calendar-fallback="<container id>"] block (the text list rendered by the server) is hidden once the view runs.
 *
 * Agendo takes a list of events, not a feed: the view loads the occurrences of the month shown (of the year in the
 * year view), with a week on each side, and loads again when the dates shown leave the loaded range.
 */
( () => {
    'use strict';

    const VIEWS = { day: 1, week: 7, month: 'month', list: 'list-month' };
    const SMALL_SCREEN = window.matchMedia( '(max-width: 767.98px)' );
    const DAY_MAX_EVENTS = 4;
    const DAY = 86400000;

    const pad = n => String( n ).padStart( 2, '0' );
    const isoDate = d => `${d.getFullYear( )}-${pad( d.getMonth( ) + 1 )}-${pad( d.getDate( ) )}`;
    const isoDateTime = d => `${isoDate( d )}T${pad( d.getHours( ) )}:${pad( d.getMinutes( ) )}:00`;
    const compactDate = d => isoDate( d ).replaceAll( '-', '' );
    const hourMinute = d => `${pad( d.getHours( ) )}:${pad( d.getMinutes( ) )}`;
    const startOfDay = d => new Date( d.getFullYear( ), d.getMonth( ), d.getDate( ) );

    const parseDay = value => {
        const digits = ( value || '' ).replaceAll( '-', '' );
        if ( !/^\d{8}$/.test( digits ) ) {
            return new Date( );
        }
        return new Date( Number( digits.slice( 0, 4 ) ), Number( digits.slice( 4, 6 ) ) - 1, Number( digits.slice( 6, 8 ) ) );
    };

    const viewKey = view => {
        if ( view === 1 ) {
            return 'day';
        }
        if ( view === 7 ) {
            return 'week';
        }
        return String( view ).startsWith( 'list' ) ? 'list' : 'month';
    };

    const uses24Hour = locale => {
        try {
            const cycle = new Intl.DateTimeFormat( locale, { hour: 'numeric' } ).resolvedOptions( ).hourCycle;
            return cycle === 'h23' || cycle === 'h24';
        } catch ( error ) {
            return true;
        }
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

    // An occurrence of the feed (EventCalendar format) as an Agendo event; the feed fields stay in extendedProps
    const toAgendoEvent = item => {
        const props = item.extendedProps || {};
        return {
            id: item.id,
            title: item.title,
            start: item.start,
            end: item.end || null,
            allDay: Boolean( item.allDay ),
            color: props.color || undefined,
            calendarId: props.calendarId,
            location: [ props.location, props.town ].filter( Boolean ).join( ', ' ),
            description: props.calendarName || '',
            extendedProps: props
        };
    };

    // Agendo with the creation and the edition sent to the pages of the plugin, and the occurrences loaded from the feed
    const createCalendarClass = ( ) => class CalendarAgendo extends Agendo {

        constructor( container, options, hooks ) {
            super( container, options );
            this.hooks = hooks;
            this.loaded = null;
            this.loading = null;
            this.hooks.relabel( this );
            this.render( );
        }

        openCreateModal( start, end, resourceId, allDay = false ) {
            if ( this.hooks ) {
                this.hooks.create( start, end, allDay );
            }
        }

        openEditModal( event ) {
            if ( this.hooks ) {
                this.hooks.open( event );
            }
        }

        render( ) {
            super.render( );
            if ( this.hooks ) {
                this.hooks.rendered( this );
                this.ensureLoaded( );
            }
        }

        rangeShown( ) {
            if ( this.o.daysToShow === 'year' ) {
                return [ new Date( this.date.getFullYear( ), 0, 1 ), new Date( this.date.getFullYear( ) + 1, 0, 1 ) ];
            }
            const first = new Date( this.date.getFullYear( ), this.date.getMonth( ), 1 );
            const next = new Date( this.date.getFullYear( ), this.date.getMonth( ) + 1, 1 );
            return [ new Date( first.getTime( ) - 7 * DAY ), new Date( next.getTime( ) + 7 * DAY ) ];
        }

        reload( ) {
            this.loaded = null;
            this.ensureLoaded( );
        }

        ensureLoaded( ) {
            const [ start, end ] = this.rangeShown( );
            if ( this.loaded && this.loaded[ 0 ] <= start && this.loaded[ 1 ] >= end ) {
                return;
            }
            const key = `${start.getTime( )}-${end.getTime( )}`;
            if ( this.loading === key ) {
                return;
            }
            this.loading = key;
            this.hooks.fetch( start, end ).then( items => {
                if ( this.loading !== key ) {
                    return;
                }
                this.loading = null;
                this.loaded = [ start, end ];
                this.setEvents( items.map( toAgendoEvent ) );
            } ).catch( error => {
                this.loading = null;
                announce( this.el, String( error ) );
            } );
        }
    };

    const init = ( CalendarAgendo, container ) => {
        const ds = container.dataset;
        const editable = Boolean( ds.moveUrl );
        let moveToken = ds.moveToken || '';
        let calendar = null;

        const calendarFilters = ( ) => [ ...document.querySelectorAll( `[data-calendar-filter="${container.id}"]` ) ];
        const categoryFilter = ( ) => document.querySelector( `[data-category-filter="${container.id}"]` );

        const fetchOccurrences = ( start, end ) => {
            const params = new URLSearchParams( { start: isoDateTime( start ), end: isoDateTime( end ) } );
            const filters = calendarFilters( );
            if ( filters.length ) {
                const checked = filters.filter( box => box.checked );
                if ( !checked.length ) {
                    return Promise.resolve( [ ] );
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

        const openCreation = ( start, end, allDay ) => {
            if ( !ds.createUrl ) {
                return;
            }
            const url = new URL( ds.createUrl, document.baseURI );
            url.searchParams.set( 'event_date', isoDate( start ) );
            if ( !allDay ) {
                const lastMinute = new Date( Math.min( end.getTime( ), startOfDay( start ).getTime( ) + DAY ) - 60000 );
                const timeEnd = isoDate( end ) === isoDate( start ) ? hourMinute( end ) : hourMinute( lastMinute );
                url.searchParams.set( 'event_time_start', hourMinute( start ) );
                url.searchParams.set( 'event_time_end', timeEnd );
            }
            const target = document.querySelector( `[data-create-calendar="${container.id}"]` );
            if ( target && target.value ) {
                url.searchParams.set( 'calendar_id', target.value );
            }
            window.location.href = url.href;
        };

        const openEvent = event => {
            const props = event.extendedProps || {};
            if ( props.url ) {
                window.location.href = props.url;
            }
        };

        const move = async ( event, action ) => {
            if ( action !== 'move' && action !== 'resize' ) {
                return;
            }
            const props = event.extendedProps || {};
            let scope = 'occurrence';
            if ( Number( props.seriesSize ) > 1 ) {
                scope = await askScope( container );
                if ( scope === 'cancel' ) {
                    calendar.reload( );
                    return;
                }
            }
            const body = new URLSearchParams( {
                action: 'moveOccurrence',
                occurrence_id: props.occurrenceId,
                start: event.allDay ? isoDate( event.start ) : isoDateTime( event.start ),
                end: event.end ? isoDateTime( event.end ) : '',
                all_day: String( event.allDay ),
                scope,
                token: moveToken
            } );
            try {
                const response = await fetch( ds.moveUrl, { method: 'POST', credentials: 'same-origin', body, headers: { Accept: 'application/json' } } );
                const result = await response.json( );
                moveToken = result.token || moveToken;
                if ( !result.ok ) {
                    calendar.reload( );
                    announce( container, result.message );
                    return;
                }
                announce( container, ds.labelMoved || '' );
                if ( scope === 'series' ) {
                    calendar.reload( );
                }
            } catch ( error ) {
                calendar.reload( );
                announce( container, ds.labelError || String( error ) );
            }
        };

        const relabel = instance => {
            const set = ( selector, text, attribute ) => {
                const element = instance.el.querySelector( selector );
                if ( element && text ) {
                    if ( attribute ) {
                        element.setAttribute( attribute, text );
                    } else {
                        element.textContent = text;
                    }
                }
            };
            set( '[data-action="today"]', ds.labelToday );
            set( '[data-action="prev"]', ds.labelPrevious, 'title' );
            set( '[data-action="next"]', ds.labelNext, 'title' );
            set( '.ago-views [data-days="1"]', ds.labelDay );
            set( '.ago-views [data-days="7"]', ds.labelWeek );
            set( '.ago-views [data-days="month"]', ds.labelMonth );
            set( '.ago-views summary[data-label="List"]', ds.labelList, 'data-label' );
            set( '.ago-views [data-days="list-week"]', ds.labelWeek );
            set( '.ago-views [data-days="list-month"]', ds.labelMonth );
        };

        const rendered = instance => {
            if ( ds.labelAllDay ) {
                instance.el.querySelectorAll( '.ago-allday-label' ).forEach( label => {
                    label.textContent = ds.labelAllDay;
                } );
            }
            if ( ds.syncUrl !== 'true' ) {
                return;
            }
            const url = new URL( window.location.href );
            url.searchParams.set( 'date', compactDate( instance.date ) );
            url.searchParams.set( 'view', viewKey( instance.o.daysToShow ) );
            window.history.replaceState( null, '', url );
        };

        const locale = ds.locale || document.documentElement.lang || navigator.language;
        const requested = VIEWS[ ds.view ];
        const views = SMALL_SCREEN.matches ? [ 'list-week', 'month' ] : [ 1, 7, 'month', 'list-week', 'list-month' ];
        let initialView = requested || ( SMALL_SCREEN.matches ? 'list-week' : 'month' );
        if ( SMALL_SCREEN.matches && !views.includes( initialView ) ) {
            initialView = 'list-week';
        }

        calendar = new CalendarAgendo( container, {
            date: parseDay( ds.date ),
            daysToShow: initialView,
            views,
            locale,
            use24Hour: uses24Hour( locale ),
            firstDayOfWeek: 1,
            theme: ds.theme || null,
            monthMaxEvents: Number( ds.dayMaxEvents ) || DAY_MAX_EVENTS,
            readonly: false,
            enableAllDay: Boolean( ds.createUrl ),
            enableCreate: Boolean( ds.createUrl ),
            enableMove: editable,
            enableResize: editable,
            enableRecurring: false,
            onEventClick: openEvent,
            onEventChange: move
        }, { fetch: fetchOccurrences, create: openCreation, open: openEvent, relabel, rendered } );

        calendarFilters( ).forEach( box => box.addEventListener( 'change', ( ) => calendar.reload( ) ) );
        const category = categoryFilter( );
        if ( category ) {
            category.addEventListener( 'change', ( ) => calendar.reload( ) );
        }
        document.querySelectorAll( `[data-calendar-fallback="${container.id}"]` ).forEach( block => {
            block.hidden = true;
        } );
        container.calendar = calendar;
    };

    const start = ( ) => {
        if ( typeof Agendo === 'undefined' ) {
            return;
        }
        const CalendarAgendo = createCalendarClass( );
        document.querySelectorAll( '[data-calendar-view]' ).forEach( container => init( CalendarAgendo, container ) );
    };

    if ( document.readyState === 'loading' ) {
        document.addEventListener( 'DOMContentLoaded', start );
    } else {
        start( );
    }
} )( );
