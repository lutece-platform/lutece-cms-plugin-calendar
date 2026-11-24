/*
 * Plugin calendar - behaviour of the templates of the calendar portlet:
 *   [data-calendar-pf-track]      a horizontal track, moved one item at a time by [data-calendar-pf-prev] and
 *                                 [data-calendar-pf-next] buttons of the same [data-calendar-pf] container
 *   [data-calendar-pf-countdown]  a countdown to the ISO local date-time of its data-start attribute
 *   [data-calendar-pf-widget]     a month whose day buttons [data-day] show the events [data-day-key] of that day
 *   img.calendar-pf-img           an event picture: a missing one is replaced by a placeholder
 * The file can be included by several portlets of the same page: it runs once.
 */
( () => {
    'use strict';

    if ( window.calendarPortletsLoaded ) {
        return;
    }
    window.calendarPortletsLoaded = true;

    const initTrack = container => {
        const track = container.querySelector( '[data-calendar-pf-track]' );
        if ( !track ) {
            return;
        }
        const step = direction => {
            const item = track.firstElementChild;
            const gap = parseFloat( getComputedStyle( track ).columnGap ) || 0;
            const width = item ? item.getBoundingClientRect( ).width + gap : track.clientWidth;
            track.scrollBy( { left: direction * width } );
        };
        container.querySelectorAll( '[data-calendar-pf-prev]' ).forEach( button => button.addEventListener( 'click', ( ) => step( -1 ) ) );
        container.querySelectorAll( '[data-calendar-pf-next]' ).forEach( button => button.addEventListener( 'click', ( ) => step( 1 ) ) );
    };

    const initCountdown = element => {
        const start = new Date( element.dataset.start );
        if ( Number.isNaN( start.getTime( ) ) ) {
            return;
        }
        const parts = {
            days: element.querySelector( '[data-unit="days"]' ),
            hours: element.querySelector( '[data-unit="hours"]' ),
            minutes: element.querySelector( '[data-unit="minutes"]' ),
            seconds: element.querySelector( '[data-unit="seconds"]' )
        };
        const pad = n => String( n ).padStart( 2, '0' );
        const tick = ( ) => {
            const remaining = Math.max( 0, start.getTime( ) - Date.now( ) );
            const seconds = Math.floor( remaining / 1000 );
            parts.days.textContent = Math.floor( seconds / 86400 );
            parts.hours.textContent = pad( Math.floor( seconds % 86400 / 3600 ) );
            parts.minutes.textContent = pad( Math.floor( seconds % 3600 / 60 ) );
            parts.seconds.textContent = pad( seconds % 60 );
            return remaining > 0;
        };
        if ( tick( ) ) {
            const timer = window.setInterval( ( ) => {
                if ( !tick( ) ) {
                    window.clearInterval( timer );
                }
            }, 1000 );
        }
    };

    const initWidget = widget => {
        const buttons = [ ...widget.querySelectorAll( 'button[data-day]' ) ];
        const items = [ ...widget.querySelectorAll( '[data-day-key]' ) ];
        const title = widget.querySelector( '[data-calendar-pf-day-title]' );
        const empty = widget.querySelector( '[data-calendar-pf-day-empty]' );
        const show = button => {
            buttons.forEach( b => b.setAttribute( 'aria-pressed', String( b === button ) ) );
            let visible = 0;
            items.forEach( item => {
                const match = item.dataset.dayKey === button.dataset.day;
                item.hidden = !match;
                visible += match ? 1 : 0;
            } );
            if ( title ) {
                title.textContent = button.dataset.label;
            }
            if ( empty ) {
                empty.hidden = visible > 0;
            }
        };
        buttons.forEach( button => button.addEventListener( 'click', ( ) => show( button ) ) );
        const first = buttons.find( b => !b.disabled && b.classList.contains( 'calendar-pf-today' ) ) || buttons.find( b => !b.disabled );
        if ( first ) {
            show( first );
        }
    };

    const initImage = image => {
        const fail = ( ) => {
            const media = image.closest( '.calendar-pf-media' );
            if ( media ) {
                media.classList.add( 'calendar-pf-noimage' );
            }
            image.remove( );
        };
        if ( image.complete && image.naturalWidth === 0 ) {
            fail( );
        } else {
            image.addEventListener( 'error', fail, { once: true } );
        }
    };

    const start = ( ) => {
        document.querySelectorAll( '[data-calendar-pf]' ).forEach( initTrack );
        document.querySelectorAll( '[data-calendar-pf-countdown]' ).forEach( initCountdown );
        document.querySelectorAll( '[data-calendar-pf-widget]' ).forEach( initWidget );
        document.querySelectorAll( 'img.calendar-pf-img' ).forEach( initImage );
    };

    if ( document.readyState === 'loading' ) {
        document.addEventListener( 'DOMContentLoaded', start );
    } else {
        start( );
    }
} )( );
