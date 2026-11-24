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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.calendar.business.CalendarHome;
import fr.paris.lutece.plugins.calendar.business.OccurrenceEvent;
import fr.paris.lutece.plugins.calendar.business.SimpleEvent;
import fr.paris.lutece.plugins.calendar.web.Constants;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes the iCalendar (RFC 5545) subscription feed of a calendar open to everybody, read by calendar applications
 * (Outlook, Google, Apple...): one VEVENT per occurrence, from some days before today to some days after
 */
@ApplicationScoped
public class CalendarIcalFeedService
{
    /** Request parameter: the calendar */
    public static final String PARAMETER_CALENDAR_ID = "calendar_id";
    /** Request parameter: a single occurrence */
    public static final String PARAMETER_OCCURRENCE_ID = "occurrence_id";

    private static final String PROPERTY_DAYS_BEFORE = "calendar.ical.feed.daysBefore";
    private static final String PROPERTY_DAYS_AFTER = "calendar.ical.feed.daysAfter";
    private static final int DEFAULT_DAYS_BEFORE = 90;
    private static final int DEFAULT_DAYS_AFTER = 365;
    private static final String CRLF = "\r\n";
    private static final int MAX_LINE_OCTETS = 75;
    private static final String CONTENT_TYPE = "text/calendar";
    private static final String URL_SHOW_OCCURRENCE = "jsp/site/Portal.jsp?page=calendar&action=show_result&event_id=%d&agenda=%d";
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern( "yyyyMMdd", Locale.ROOT );
    private static final DateTimeFormatter FORMAT_UTC = DateTimeFormatter.ofPattern( "yyyyMMdd'T'HHmmss'Z'", Locale.ROOT );

    @Inject
    private CalendarService _calendarService;

    /**
     * Writes the feed of the calendar given by the parameter calendar_id, or the file of the single occurrence given by
     * the parameter occurrence_id. Answers 404 when the calendar does not exist or the visitor may not see it: a calendar
     * restricted to a role is served only to a visitor logged in with that role
     *
     * @param request
     *            the request
     * @param response
     *            the response
     * @throws IOException
     *             if the response cannot be written
     */
    public void writeFeed( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        String strOccurrenceId = request.getParameter( PARAMETER_OCCURRENCE_ID );

        if ( strOccurrenceId != null )
        {
            writeOccurrence( request, response, strOccurrenceId );
            return;
        }

        String strCalendarId = request.getParameter( PARAMETER_CALENDAR_ID );
        AgendaResource agenda = getVisibleAgenda( request, ( strCalendarId != null ) ? strCalendarId : request.getParameter( Constants.PARAM_AGENDA ) );

        if ( agenda == null )
        {
            response.sendError( HttpServletResponse.SC_NOT_FOUND );
            return;
        }

        LocalDate today = LocalDate.now( );
        List<OccurrenceEvent> listOccurrences = EventProposals.publicOnly( CalendarHome.findOccurrencesByDateRange( List.of( Integer.parseInt( agenda.getId( ) ) ),
                today.minusDays( AppPropertiesService.getPropertyInt( PROPERTY_DAYS_BEFORE, DEFAULT_DAYS_BEFORE ) ),
                today.plusDays( AppPropertiesService.getPropertyInt( PROPERTY_DAYS_AFTER, DEFAULT_DAYS_AFTER ) + 1L ), List.of( ),
                PluginService.getPlugin( Constants.PLUGIN_NAME ) ) );

        writeCalendar( request, response, agenda, listOccurrences, "calendar-" + agenda.getId( ) );
    }

    /**
     * Writes the iCalendar file of a single occurrence, for the "add to calendar" button
     *
     * @param request
     *            the request
     * @param response
     *            the response
     * @param strOccurrenceId
     *            the occurrence
     * @throws IOException
     *             if the response cannot be written
     */
    private void writeOccurrence( HttpServletRequest request, HttpServletResponse response, String strOccurrenceId ) throws IOException
    {
        Plugin plugin = PluginService.getPlugin( Constants.PLUGIN_NAME );
        OccurrenceEvent occurrence = strOccurrenceId.matches( "\\d{1,9}" ) ? CalendarHome.findOccurrence( Integer.parseInt( strOccurrenceId ), plugin )
                : null;
        occurrence = ( ( occurrence != null ) && EventProposals.isProposed( occurrence.getStatus( ) ) ) ? null : occurrence;
        SimpleEvent event = ( occurrence != null ) ? CalendarHome.findEvent( occurrence.getEventId( ), plugin ) : null;
        AgendaResource agenda = ( event != null ) ? getVisibleAgenda( request, String.valueOf( event.getIdCalendar( ) ) ) : null;

        if ( agenda == null )
        {
            response.sendError( HttpServletResponse.SC_NOT_FOUND );
            return;
        }

        occurrence.setId( Integer.parseInt( strOccurrenceId ) );
        occurrence.setIdCalendar( event.getIdCalendar( ) );
        occurrence.setLocation( event.getLocation( ) );
        occurrence.setLocationTown( event.getLocationTown( ) );
        occurrence.setDescription( event.getDescription( ) );
        writeCalendar( request, response, agenda, List.of( occurrence ), "event-" + strOccurrenceId );
    }

    /**
     * Writes an iCalendar response
     *
     * @param request
     *            the request
     * @param response
     *            the response
     * @param agenda
     *            the calendar
     * @param listOccurrences
     *            the occurrences
     * @param strFileName
     *            the name of the file, without extension
     * @throws IOException
     *             if the response cannot be written
     */
    private void writeCalendar( HttpServletRequest request, HttpServletResponse response, AgendaResource agenda, List<OccurrenceEvent> listOccurrences,
            String strFileName ) throws IOException
    {
        String strBaseUrl = AppPathService.getBaseUrl( request );
        String strBody = buildCalendar( agenda, listOccurrences, strBaseUrl, getHost( strBaseUrl ), LocalDateTime.now( ZoneOffset.UTC ) );

        response.setContentType( CONTENT_TYPE );
        response.setCharacterEncoding( StandardCharsets.UTF_8.name( ) );
        response.setHeader( "Content-Disposition", "inline; filename=\"" + strFileName + ".ics\"" );
        response.getWriter( ).write( strBody );
        response.getWriter( ).flush( );
    }

    /**
     * Gives a calendar the visitor may see
     *
     * @param request
     *            the request, whose session gives the roles of the visitor
     * @param strId
     *            the calendar
     * @return the calendar, or null when it does not exist or the visitor may not see it
     */
    public AgendaResource getVisibleAgenda( HttpServletRequest request, String strId )
    {
        if ( strId == null || !strId.matches( "\\d{1,9}" ) )
        {
            return null;
        }

        return _calendarService.getAgendaResources( request ).stream( ).filter( agenda -> strId.equals( agenda.getId( ) ) ).findFirst( ).orElse( null );
    }

    /**
     * Tells whether a calendar may be read without logging in
     *
     * @param agenda
     *            the calendar
     * @return true when it is restricted to no role
     */
    public static boolean isPublic( AgendaResource agenda )
    {
        String strRole = agenda.getRole( );

        return StringUtils.isBlank( strRole ) || Constants.PROPERTY_ROLE_NONE.equals( strRole );
    }

    /**
     * Builds the iCalendar text of a calendar
     *
     * @param agenda
     *            the calendar
     * @param listOccurrences
     *            its occurrences
     * @param strBaseUrl
     *            the base URL of the site, for the links of the occurrences
     * @param strHost
     *            the host of the site, for the unique identifiers of the occurrences
     * @param stamp
     *            the time of the export, in UTC
     * @return the text, with CRLF line ends and folded lines
     */
    public String buildCalendar( AgendaResource agenda, List<OccurrenceEvent> listOccurrences, String strBaseUrl, String strHost,
            LocalDateTime stamp )
    {
        StringBuilder sb = new StringBuilder( );
        appendLine( sb, "BEGIN:VCALENDAR" );
        appendLine( sb, "VERSION:2.0" );
        appendLine( sb, "PRODID:-//Lutece//plugin-calendar//FR" );
        appendLine( sb, "CALSCALE:GREGORIAN" );
        appendLine( sb, "METHOD:PUBLISH" );
        appendLine( sb, "X-WR-CALNAME:" + escapeText( agenda.getName( ) ) );

        for ( OccurrenceEvent occurrence : listOccurrences )
        {
            appendEvent( sb, occurrence, strBaseUrl, strHost, stamp );
        }

        appendLine( sb, "END:VCALENDAR" );

        return sb.toString( );
    }

    /**
     * Appends the VEVENT of an occurrence
     *
     * @param sb
     *            the text
     * @param occurrence
     *            the occurrence
     * @param strBaseUrl
     *            the base URL of the site
     * @param strHost
     *            the host of the site
     * @param stamp
     *            the time of the export, in UTC
     */
    private void appendEvent( StringBuilder sb, OccurrenceEvent occurrence, String strBaseUrl, String strHost, LocalDateTime stamp )
    {
        LocalDateTime start = CalendarEventFeedService.getStart( occurrence );
        LocalTime timeStart = CalendarEventFeedService.parseTime( occurrence.getDateTimeStart( ) );
        LocalTime timeEnd = CalendarEventFeedService.parseTime( occurrence.getDateTimeEnd( ) );

        appendLine( sb, "BEGIN:VEVENT" );
        appendLine( sb, "UID:calendar-occurrence-" + occurrence.getId( ) + "@" + strHost );
        appendLine( sb, "DTSTAMP:" + FORMAT_UTC.format( stamp ) );

        if ( timeStart == null )
        {
            appendLine( sb, "DTSTART;VALUE=DATE:" + FORMAT_DATE.format( start.toLocalDate( ) ) );
            appendLine( sb, "DTEND;VALUE=DATE:" + FORMAT_DATE.format( start.toLocalDate( ).plusDays( 1 ) ) );
        }
        else
        {
            appendLine( sb, "DTSTART:" + toUtc( start ) );

            if ( timeEnd != null && timeEnd.isAfter( timeStart ) )
            {
                appendLine( sb, "DTEND:" + toUtc( start.toLocalDate( ).atTime( timeEnd ) ) );
            }
        }

        appendLine( sb, "SUMMARY:" + escapeText( occurrence.getTitle( ) ) );

        String strLocation = String.join( ", ",
                List.of( StringUtils.defaultString( occurrence.getLocation( ) ), StringUtils.defaultString( occurrence.getLocationTown( ) ) ).stream( )
                        .filter( StringUtils::isNotBlank ).toList( ) );

        if ( !strLocation.isEmpty( ) )
        {
            appendLine( sb, "LOCATION:" + escapeText( strLocation ) );
        }

        String strStatus = StringUtils.defaultString( occurrence.getStatus( ) ).trim( ).toUpperCase( Locale.ROOT );

        if ( List.of( "CONFIRMED", "TENTATIVE", "CANCELLED" ).contains( strStatus ) )
        {
            appendLine( sb, "STATUS:" + strStatus );
        }

        String strDescription = toPlainText( occurrence.getDescription( ) );

        if ( !strDescription.isEmpty( ) )
        {
            appendLine( sb, "DESCRIPTION:" + escapeText( strDescription ) );
        }

        appendLine( sb, "URL:" + strBaseUrl + String.format( URL_SHOW_OCCURRENCE, occurrence.getEventId( ), occurrence.getIdCalendar( ) ) );
        appendLine( sb, "END:VEVENT" );
    }

    /**
     * Converts a local time of the server into an iCalendar UTC time
     *
     * @param dateTime
     *            the local time
     * @return the UTC time ( 20261112T110000Z )
     */
    private static String toUtc( LocalDateTime dateTime )
    {
        return FORMAT_UTC.format( dateTime.atZone( ZoneId.systemDefault( ) ).withZoneSameInstant( ZoneOffset.UTC ) );
    }

    /**
     * Escapes a text value: backslash, semicolon, comma and line ends
     *
     * @param strText
     *            the text, or null
     * @return the escaped text
     */
    public static String escapeText( String strText )
    {
        if ( strText == null )
        {
            return "";
        }

        return strText.replace( "\\", "\\\\" ).replace( ";", "\\;" ).replace( ",", "\\," ).replace( "\r\n", "\\n" ).replace( "\n", "\\n" )
                .replace( "\r", "\\n" );
    }

    /**
     * Appends a content line, folded every 75 octets as RFC 5545 asks, without cutting a UTF-8 character
     *
     * @param sb
     *            the text
     * @param strLine
     *            the line
     */
    public static void appendLine( StringBuilder sb, String strLine )
    {
        int nOctets = 0;

        for ( int i = 0; i < strLine.length( ); )
        {
            int nCodePoint = strLine.codePointAt( i );
            String strChar = new String( Character.toChars( nCodePoint ) );
            int nCharOctets = strChar.getBytes( StandardCharsets.UTF_8 ).length;

            if ( nOctets + nCharOctets > MAX_LINE_OCTETS )
            {
                sb.append( CRLF ).append( ' ' );
                nOctets = 1;
            }

            sb.append( strChar );
            nOctets += nCharOctets;
            i += Character.charCount( nCodePoint );
        }

        sb.append( CRLF );
    }

    /**
     * Gives the host of the site, for the unique identifiers of the occurrences
     *
     * @param strBaseUrl
     *            the base URL of the site
     * @return its host, or "lutece" when it cannot be read
     */
    private static String getHost( String strBaseUrl )
    {
        try
        {
            String strHost = URI.create( strBaseUrl ).getHost( );

            return StringUtils.isNotBlank( strHost ) ? strHost : "lutece";
        }
        catch( IllegalArgumentException e )
        {
            return "lutece";
        }
    }

    /**
     * Turns a rich text into the plain text of a calendar description: tags removed, entities decoded, a line break for
     * each paragraph, line break and list item, spaces collapsed
     *
     * @param strHtml
     *            the rich text, may be null
     * @return the plain text, empty when there is none
     */
    public static String toPlainText( String strHtml )
    {
        if ( StringUtils.isBlank( strHtml ) )
        {
            return StringUtils.EMPTY;
        }

        org.jsoup.nodes.Document document = org.jsoup.Jsoup.parse( strHtml );
        document.select( "li" ).prepend( "- " );
        document.select( "p, div, li, tr, h1, h2, h3, h4, h5, h6, ul, ol, br" ).after( "\n" );
        String strText = document.body( ).wholeText( );
        String strLines = strText.lines( ).map( strLine -> strLine.replaceAll( "[\\s\\u00A0]+", " " ).trim( ) )
                .collect( java.util.stream.Collectors.joining( "\n" ) );

        return strLines.replaceAll( "\n{3,}", "\n\n" ).trim( );
    }
}
