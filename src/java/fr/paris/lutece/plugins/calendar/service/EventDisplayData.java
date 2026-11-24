/*
 * Copyright (c) 2002-2026, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.calendar.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.calendar.business.CalendarHome;
import fr.paris.lutece.plugins.calendar.business.Event;
import fr.paris.lutece.plugins.calendar.business.details.EventDetails;
import fr.paris.lutece.plugins.calendar.business.details.EventDetailsHome;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.util.AppPathService;

/**
 * The display data of an event, shared by the templates of the calendar portlet and the search results: its day split
 * for a date badge, its hours, its place, its colour, a plain text excerpt, its status and its details
 */
public final class EventDisplayData
{
    /** The key of the start of the event, ISO local date-time, used to sort */
    public static final String KEY_START = "start";
    private static final String URL_SHOW_EVENT = "?page=calendar&action=show_result&event_id=%d&agenda=%s";
    private static final String URL_OSM = "https://www.openstreetmap.org/?mlat=%.6f&mlon=%.6f#map=17/%.6f/%.6f";
    private static final int EXCERPT_LENGTH = 220;
    private static final String SEPARATOR_PLACE = ", ";

    /**
     * Private constructor
     */
    private EventDisplayData( )
    {
    }

    /**
     * Builds the display data of an event
     * @param event the event
     * @param agenda its calendar
     * @param dateShown the day the badge shows: the first day of the event, or its next date in a period
     * @param locale the locale of the names of the days and months
     * @param plugin the plugin
     * @return the display data: day, weekday, monthName, monthShort, monthYear, year, dayKey, fullDate, start, timeStart,
     *         timeEnd, location, url, color, agendaName, excerpt, status, registrationUrl, priceType, price, attendanceMode,
     *         placeName, address, phone, mapUrl, firstDate, lastDate, multiDay
     */
    public static Map<String, Object> build( Event event, AgendaResource agenda, Date dateShown, Locale locale, Plugin plugin )
    {
        Map<String, Object> map = new HashMap<>( );
        LocalDate day = toLocalDate( ( dateShown != null ) ? dateShown : event.getDate( ) );
        LocalDate first = toLocalDate( event.getDate( ) );
        LocalDate last = ( event.getDateEnd( ) != null ) ? toLocalDate( event.getDateEnd( ) ) : first;
        LocalTime timeStart = CalendarEventFeedService.parseTime( event.getDateTimeStart( ) );
        LocalTime timeEnd = CalendarEventFeedService.parseTime( event.getDateTimeEnd( ) );
        EventDetails details = EventDetailsHome.findByEvent( event.getId( ), plugin );
        DateTimeFormatter formatterLong = DateTimeFormatter.ofLocalizedDate( FormatStyle.LONG ).withLocale( locale );

        map.put( "day", String.format( "%02d", day.getDayOfMonth( ) ) );
        map.put( "weekday", day.getDayOfWeek( ).getDisplayName( TextStyle.FULL, locale ) );
        map.put( "monthName", day.getMonth( ).getDisplayName( TextStyle.FULL, locale ) );
        map.put( "monthShort", day.getMonth( ).getDisplayName( TextStyle.SHORT, locale ) );
        map.put( "monthYear", DateTimeFormatter.ofPattern( "MMMM yyyy", locale ).format( day ) );
        map.put( "year", String.valueOf( day.getYear( ) ) );
        map.put( "dayKey", day.toString( ) );
        map.put( "fullDate", DateTimeFormatter.ofLocalizedDate( FormatStyle.FULL ).withLocale( locale ).format( day ) );
        map.put( "firstDate", formatterLong.format( first ) );
        map.put( "lastDate", formatterLong.format( last ) );
        map.put( "multiDay", !first.equals( last ) );
        map.put( KEY_START, ( timeStart != null ) ? day.atTime( timeStart ).toString( ) : day.atStartOfDay( ).toString( ) );
        map.put( "timeStart", ( timeStart != null ) ? event.getDateTimeStart( ).trim( ) : StringUtils.EMPTY );
        map.put( "timeEnd", ( timeEnd != null ) ? event.getDateTimeEnd( ).trim( ) : StringUtils.EMPTY );
        map.put( "location", Stream.of( event.getLocation( ), event.getLocationTown( ) ).filter( StringUtils::isNotBlank ).collect( Collectors.joining( SEPARATOR_PLACE ) ) );
        map.put( "url", AppPathService.getPortalUrl( ) + String.format( URL_SHOW_EVENT, event.getId( ), agenda.getId( ) ) );
        map.put( "color", agenda.getEventColorCode( ) );
        map.put( "agendaName", StringUtils.defaultString( agenda.getName( ) ) );
        map.put( "excerpt", StringUtils.abbreviate( CalendarIcalFeedService.toPlainText( event.getDescription( ) ).replace( '\n', ' ' ), EXCERPT_LENGTH ) );
        map.put( "status", StringUtils.defaultString( event.getStatus( ) ).trim( ) );
        map.put( "registrationUrl", StringUtils.defaultString( details.getRegistrationUrl( ) ) );
        map.put( "priceType", StringUtils.defaultString( details.getPriceType( ) ) );
        map.put( "price", StringUtils.defaultString( details.getPrice( ) ) );
        map.put( "attendanceMode", StringUtils.defaultString( details.getAttendanceMode( ) ) );
        map.put( "placeName", StringUtils.defaultString( event.getLocation( ) ) );
        map.put( "address", Stream.of( event.getLocationAddress( ),
                Stream.of( event.getLocationZip( ), event.getLocationTown( ) ).filter( StringUtils::isNotBlank ).collect( Collectors.joining( " " ) ) )
                .filter( StringUtils::isNotBlank ).collect( Collectors.joining( SEPARATOR_PLACE ) ) );
        map.put( "phone", StringUtils.defaultString( details.getOrganizerPhone( ) ) );
        map.put( "mapUrl", details.isLocated( )
                ? String.format( Locale.ROOT, URL_OSM, details.getLatitude( ), details.getLongitude( ), details.getLatitude( ), details.getLongitude( ) )
                : StringUtils.defaultString( event.getMapUrl( ) ) );

        return map;
    }

    /**
     * Tells whether an event has a picture
     * @param nEventId the identifier of the event
     * @param plugin the plugin
     * @return true when a picture is stored for the event; the picture itself is not loaded
     */
    public static boolean hasImage( int nEventId, Plugin plugin )
    {
        return CalendarHome.hasImage( nEventId, plugin );
    }

    /**
     * Converts a date to a local day
     * @param date the date
     * @return the day in the time zone of the server
     */
    private static LocalDate toLocalDate( Date date )
    {
        return ( date instanceof java.sql.Date ) ? ( (java.sql.Date) date ).toLocalDate( ) : date.toInstant( ).atZone( ZoneId.systemDefault( ) ).toLocalDate( );
    }
}
