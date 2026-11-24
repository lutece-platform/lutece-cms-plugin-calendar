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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.plugins.calendar.business.CalendarHome;
import fr.paris.lutece.plugins.calendar.business.OccurrenceEvent;
import fr.paris.lutece.plugins.calendar.web.Constants;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Builds the JSON feed of the calendar occurrences read by the EventCalendar views of the front and back offices: one
 * item per occurrence, in the format of an EventCalendar event object
 */
@ApplicationScoped
public class CalendarEventFeedService
{
    /** Request parameter: first day of the range (ISO date or date-time, as sent by EventCalendar) */
    public static final String PARAMETER_START = "start";
    /** Request parameter: day after the range */
    public static final String PARAMETER_END = "end";
    /** Request parameter: a calendar to keep, repeatable */
    public static final String PARAMETER_CALENDAR_ID = "calendar_id";
    /** Request parameter: a category to keep, repeatable */
    public static final String PARAMETER_CATEGORY_ID = "category_id";

    /** Longest range served, in days: a year view plus its padding */
    public static final int MAX_RANGE_DAYS = 400;

    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CLASS_EVENT = "calendar-event";
    private static final String STYLE_EVENT_COLOR = "--calendar-dot-color: ";
    private static final String CLASS_STATUS_PREFIX = "calendar-status-";
    private static final String PATTERN_TIME = "\\d{1,2}:\\d{2}";
    private static final ObjectMapper MAPPER = new ObjectMapper( );

    /**
     * Builds the items of a feed
     *
     * @param listAgendas
     *            the calendars the user may see
     * @param dateStart
     *            the first day of the range
     * @param dateEnd
     *            the day after the range
     * @param listCategoryIds
     *            the categories to keep, empty for all
     * @param urlBuilder
     *            gives the link of an occurrence
     * @param bEditable
     *            true when the user may move the occurrences (back office); the proposed events are only given then
     * @return the items, in the order of the occurrences
     */
    public List<Map<String, Object>> getItems( Collection<AgendaResource> listAgendas, LocalDate dateStart, LocalDate dateEnd,
            Collection<Integer> listCategoryIds, Function<OccurrenceEvent, String> urlBuilder, boolean bEditable )
    {
        Map<Integer, AgendaResource> mapAgendas = listAgendas.stream( ).filter( a -> a.getId( ) != null && a.getId( ).matches( "\\d{1,9}" ) )
                .collect( Collectors.toMap( a -> Integer.parseInt( a.getId( ) ), Function.identity( ), ( a, b ) -> a, LinkedHashMap::new ) );
        Plugin plugin = PluginService.getPlugin( Constants.PLUGIN_NAME );
        List<Map<String, Object>> listItems = new ArrayList<>( );

        List<OccurrenceEvent> listOccurrences = CalendarHome.findOccurrencesByDateRange( mapAgendas.keySet( ), dateStart, dateEnd, listCategoryIds,
                plugin );

        for ( OccurrenceEvent occurrence : bEditable ? listOccurrences : EventProposals.publicOnly( listOccurrences ) )
        {
            listItems.add( toItem( occurrence, mapAgendas.get( occurrence.getIdCalendar( ) ), urlBuilder, bEditable ) );
        }

        return listItems;
    }

    /**
     * Builds the items of a feed from a request carrying the parameters start, end, calendar_id and category_id
     *
     * @param request
     *            the request
     * @param listAgendas
     *            the calendars the user may see
     * @param urlBuilder
     *            gives the link of an occurrence
     * @param bEditable
     *            true when the user may move the occurrences
     * @return the items, empty when the range is missing, inverted or longer than {@link #MAX_RANGE_DAYS} days
     */
    public List<Map<String, Object>> getItems( HttpServletRequest request, Collection<AgendaResource> listAgendas,
            Function<OccurrenceEvent, String> urlBuilder, boolean bEditable )
    {
        LocalDate dateStart = parseDate( request.getParameter( PARAMETER_START ) );
        LocalDate dateEnd = parseDate( request.getParameter( PARAMETER_END ) );

        if ( dateStart == null || dateEnd == null || !dateEnd.isAfter( dateStart ) || ChronoUnit.DAYS.between( dateStart, dateEnd ) > MAX_RANGE_DAYS )
        {
            return new ArrayList<>( );
        }

        Set<Integer> setCalendarIds = getIds( request.getParameterValues( PARAMETER_CALENDAR_ID ) );
        Collection<AgendaResource> listKept = setCalendarIds.isEmpty( ) ? listAgendas
                : listAgendas.stream( ).filter( a -> setCalendarIds.contains( NumberUtils.toInt( a.getId( ), -1 ) ) ).toList( );

        return getItems( listKept, dateStart, dateEnd, getIds( request.getParameterValues( PARAMETER_CATEGORY_ID ) ), urlBuilder, bEditable );
    }

    /**
     * Writes a feed as the JSON response of a request carrying the parameters start, end, calendar_id and category_id
     *
     * @param request
     *            the request
     * @param response
     *            the response
     * @param listAgendas
     *            the calendars the user may see
     * @param urlBuilder
     *            gives the link of an occurrence
     * @param bEditable
     *            true when the user may move the occurrences
     * @throws IOException
     *             if the response cannot be written
     */
    public void writeFeed( HttpServletRequest request, HttpServletResponse response, Collection<AgendaResource> listAgendas,
            Function<OccurrenceEvent, String> urlBuilder, boolean bEditable ) throws IOException
    {
        writeJson( response, getItems( request, listAgendas, urlBuilder, bEditable ) );
    }

    /**
     * Writes an object as the JSON body of a response
     *
     * @param response
     *            the response
     * @param object
     *            the object
     * @throws IOException
     *             if the response cannot be written
     */
    public void writeJson( HttpServletResponse response, Object object ) throws IOException
    {
        String strJson;

        try
        {
            strJson = MAPPER.writeValueAsString( object );
        }
        catch( JsonProcessingException e )
        {
            AppLogService.error( "Calendar: the JSON feed cannot be written", e );
            response.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
            return;
        }

        response.setContentType( CONTENT_TYPE_JSON );
        response.setCharacterEncoding( StandardCharsets.UTF_8.name( ) );
        response.setHeader( "Cache-Control", "no-store" );
        response.getWriter( ).write( strJson );
        response.getWriter( ).flush( );
    }

    /**
     * Reads the date of a feed parameter: an ISO date, or the date of an ISO date-time ( 2026-11-12T00:00:00 )
     *
     * @param strValue
     *            the parameter
     * @return the date, or null when it is missing or invalid
     */
    public static LocalDate parseDate( String strValue )
    {
        if ( StringUtils.length( strValue ) < 10 )
        {
            return null;
        }

        try
        {
            return LocalDate.parse( strValue.substring( 0, 10 ) );
        }
        catch( DateTimeParseException e )
        {
            return null;
        }
    }

    /**
     * Reads a time of the plugin ( HH:mm ), as stored for the start and end of an occurrence
     *
     * @param strTime
     *            the time
     * @return the time, or null when there is none or it is invalid
     */
    public static LocalTime parseTime( String strTime )
    {
        if ( strTime == null || !strTime.trim( ).matches( PATTERN_TIME ) )
        {
            return null;
        }

        String [ ] parts = strTime.trim( ).split( ":" );
        int nHour = Integer.parseInt( parts [0] );
        int nMinute = Integer.parseInt( parts [1] );

        return ( nHour < 24 && nMinute < 60 ) ? LocalTime.of( nHour, nMinute ) : null;
    }

    /**
     * Gives the start of an occurrence
     *
     * @param occurrence
     *            the occurrence
     * @return its day at its start time, or at midnight when it lasts all day
     */
    public static LocalDateTime getStart( OccurrenceEvent occurrence )
    {
        LocalDate date = occurrence.getDate( ).toInstant( ).atZone( ZoneId.systemDefault( ) ).toLocalDate( );
        LocalTime time = parseTime( occurrence.getDateTimeStart( ) );

        return ( time == null ) ? date.atStartOfDay( ) : date.atTime( time );
    }

    /**
     * Converts an occurrence into an EventCalendar event object
     *
     * @param occurrence
     *            the occurrence
     * @param agenda
     *            its calendar, or null
     * @param urlBuilder
     *            gives the link of the occurrence
     * @param bEditable
     *            true when the user may move the occurrence
     * @return the event object
     */
    private Map<String, Object> toItem( OccurrenceEvent occurrence, AgendaResource agenda, Function<OccurrenceEvent, String> urlBuilder,
            boolean bEditable )
    {
        LocalDateTime start = getStart( occurrence );
        LocalTime timeStart = parseTime( occurrence.getDateTimeStart( ) );
        LocalTime timeEnd = parseTime( occurrence.getDateTimeEnd( ) );
        boolean bAllDay = timeStart == null;
        String strColor = ( agenda != null ) ? agenda.getEventColorCode( ) : StringUtils.EMPTY;
        String strStatus = StringUtils.defaultString( occurrence.getStatus( ) ).trim( );

        Map<String, Object> item = new LinkedHashMap<>( );
        item.put( "id", "o" + occurrence.getId( ) );
        item.put( "title", StringUtils.defaultString( occurrence.getTitle( ) ) );
        item.put( "allDay", bAllDay );

        if ( bAllDay )
        {
            item.put( "start", start.toLocalDate( ).toString( ) );
        }
        else
        {
            item.put( "start", start.toString( ) );

            if ( timeEnd != null && timeEnd.isAfter( timeStart ) )
            {
                item.put( "end", start.toLocalDate( ).atTime( timeEnd ).toString( ) );
            }
        }

        List<String> listClasses = new ArrayList<>( );
        listClasses.add( CLASS_EVENT );

        if ( strStatus.matches( "[a-zA-Z]+" ) )
        {
            listClasses.add( CLASS_STATUS_PREFIX + strStatus.toLowerCase( ) );
        }

        item.put( "classNames", listClasses );

        if ( StringUtils.isNotEmpty( strColor ) )
        {
            item.put( "styles", List.of( STYLE_EVENT_COLOR + strColor ) );
        }

        item.put( "editable", bEditable );

        Map<String, Object> props = new LinkedHashMap<>( );
        props.put( "occurrenceId", occurrence.getId( ) );
        props.put( "eventId", occurrence.getEventId( ) );
        props.put( "calendarId", occurrence.getIdCalendar( ) );
        props.put( "calendarName", ( agenda != null ) ? StringUtils.defaultString( agenda.getName( ) ) : StringUtils.EMPTY );
        props.put( "color", strColor );
        props.put( "status", strStatus );
        props.put( "location", StringUtils.defaultString( occurrence.getLocation( ) ) );
        props.put( "town", StringUtils.defaultString( occurrence.getLocationTown( ) ) );
        props.put( "seriesSize", occurrence.getOccurrence( ) );
        props.put( "url", urlBuilder.apply( occurrence ) );
        item.put( "extendedProps", props );

        return item;
    }

    /**
     * Reads the numeric identifiers of a repeatable parameter, the others being ignored
     *
     * @param values
     *            the parameter values, or null
     * @return the identifiers, in their order
     */
    private static Set<Integer> getIds( String [ ] values )
    {
        if ( values == null )
        {
            return Set.of( );
        }

        return java.util.Arrays.stream( values ).filter( v -> v != null && v.matches( "\\d{1,9}" ) ).map( Integer::valueOf )
                .collect( Collectors.toCollection( java.util.LinkedHashSet::new ) );
    }
}
