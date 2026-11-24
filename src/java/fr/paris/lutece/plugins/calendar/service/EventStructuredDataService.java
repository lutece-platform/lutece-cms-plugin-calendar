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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.plugins.calendar.business.SimpleEvent;
import fr.paris.lutece.plugins.calendar.business.details.EventDetails;
import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Builds the schema.org Event description (JSON-LD) of the page of an event, read by search engines
 */
@ApplicationScoped
public class EventStructuredDataService
{
    private static final String SCHEMA = "https://schema.org/";
    private static final ObjectMapper MAPPER = new ObjectMapper( );

    /**
     * Builds the JSON-LD of an event, safe to print inside a script element
     *
     * @param event
     *            the event
     * @param details
     *            its details
     * @param strPageUrl
     *            the absolute URL of its page
     * @param strImageUrl
     *            the absolute URL of its image, or null
     * @return the JSON-LD, or an empty string when it cannot be written
     */
    public String buildJsonLd( SimpleEvent event, EventDetails details, String strPageUrl, String strImageUrl )
    {
        Map<String, Object> data = new LinkedHashMap<>( );
        data.put( "@context", "https://schema.org" );
        data.put( "@type", "Event" );
        data.put( "name", StringUtils.defaultString( event.getTitle( ) ) );
        data.put( "url", strPageUrl );
        data.put( "startDate", toIso( event.getDate( ), event.getDateTimeStart( ) ) );
        data.put( "endDate", toIso( ( event.getDateEnd( ) != null ) ? event.getDateEnd( ) : event.getDate( ),
                StringUtils.defaultIfBlank( event.getDateTimeEnd( ), event.getDateTimeStart( ) ) ) );
        data.put( "eventStatus", SCHEMA + getEventStatus( event.getStatus( ) ) );

        String strMode = StringUtils.defaultIfEmpty( details.getAttendanceMode( ), EventDetails.ATTENDANCE_OFFLINE );
        data.put( "eventAttendanceMode", SCHEMA + switch( strMode )
        {
            case EventDetails.ATTENDANCE_ONLINE -> "OnlineEventAttendanceMode";
            case EventDetails.ATTENDANCE_MIXED -> "MixedEventAttendanceMode";
            default -> "OfflineEventAttendanceMode";
        } );

        String strDescription = CalendarIcalFeedService.toPlainText( event.getDescription( ) );

        if ( !strDescription.isEmpty( ) )
        {
            data.put( "description", strDescription );
        }

        if ( StringUtils.isNotBlank( strImageUrl ) )
        {
            data.put( "image", List.of( strImageUrl ) );
        }

        List<Object> listLocations = new ArrayList<>( );

        if ( !EventDetails.ATTENDANCE_ONLINE.equals( strMode ) )
        {
            listLocations.add( getPlace( event, details ) );
        }

        if ( !EventDetails.ATTENDANCE_OFFLINE.equals( strMode ) && StringUtils.isNotBlank( details.getOnlineUrl( ) ) )
        {
            listLocations.add( Map.of( "@type", "VirtualLocation", "url", details.getOnlineUrl( ) ) );
        }

        if ( !listLocations.isEmpty( ) )
        {
            data.put( "location", ( listLocations.size( ) == 1 ) ? listLocations.get( 0 ) : listLocations );
        }

        Map<String, Object> offer = getOffer( details, strPageUrl );

        if ( offer != null )
        {
            data.put( "offers", offer );
        }

        if ( StringUtils.isNotBlank( details.getOrganizerName( ) ) )
        {
            Map<String, Object> organizer = new LinkedHashMap<>( );
            organizer.put( "@type", "Organization" );
            organizer.put( "name", details.getOrganizerName( ) );
            putIfNotBlank( organizer, "url", details.getOrganizerUrl( ) );
            putIfNotBlank( organizer, "email", details.getOrganizerEmail( ) );
            putIfNotBlank( organizer, "telephone", details.getOrganizerPhone( ) );
            data.put( "organizer", organizer );
        }

        try
        {
            return MAPPER.writeValueAsString( data ).replace( "</", "<\\/" );
        }
        catch( JsonProcessingException e )
        {
            AppLogService.error( "Calendar: the structured data of the event {} cannot be written", event.getId( ), e );
            return StringUtils.EMPTY;
        }
    }

    /**
     * Gives the schema.org status of a status of the plugin
     *
     * @param strStatus
     *            the status of the event
     * @return EventCancelled, EventPostponed, EventMovedOnline or EventScheduled
     */
    public static String getEventStatus( String strStatus )
    {
        return switch( StringUtils.defaultString( strStatus ).trim( ) )
        {
            case "cancelled" -> "EventCancelled";
            case "postponed" -> "EventPostponed";
            case "movedOnline" -> "EventMovedOnline";
            default -> "EventScheduled";
        };
    }

    /**
     * Builds the place of an event
     *
     * @param event
     *            the event
     * @param details
     *            its details
     * @return the schema.org Place
     */
    private static Map<String, Object> getPlace( SimpleEvent event, EventDetails details )
    {
        Map<String, Object> address = new LinkedHashMap<>( );
        address.put( "@type", "PostalAddress" );
        putIfNotBlank( address, "streetAddress", event.getLocationAddress( ) );
        putIfNotBlank( address, "postalCode", event.getLocationZip( ) );
        putIfNotBlank( address, "addressLocality", event.getLocationTown( ) );
        address.put( "addressCountry", "FR" );

        Map<String, Object> place = new LinkedHashMap<>( );
        place.put( "@type", "Place" );
        place.put( "name", StringUtils.defaultIfBlank( event.getLocation( ), StringUtils.defaultString( event.getLocationTown( ) ) ) );
        place.put( "address", address );

        if ( details.isLocated( ) )
        {
            place.put( "geo", Map.of( "@type", "GeoCoordinates", "latitude", details.getLatitude( ), "longitude", details.getLongitude( ) ) );
        }

        return place;
    }

    /**
     * Builds the offer of an event: free, or paid with a price when it is a number
     *
     * @param details
     *            the details of the event
     * @param strPageUrl
     *            the URL of the page of the event
     * @return the schema.org Offer, or null when the price is not given
     */
    private static Map<String, Object> getOffer( EventDetails details, String strPageUrl )
    {
        if ( StringUtils.isEmpty( details.getPriceType( ) ) )
        {
            return null;
        }

        Map<String, Object> offer = new LinkedHashMap<>( );
        offer.put( "@type", "Offer" );
        offer.put( "url", StringUtils.defaultIfBlank( details.getRegistrationUrl( ), strPageUrl ) );
        offer.put( "priceCurrency", "EUR" );

        if ( EventDetails.PRICE_FREE.equals( details.getPriceType( ) ) )
        {
            offer.put( "price", "0" );
        }
        else
        {
            String strPrice = StringUtils.defaultString( details.getPrice( ) ).replace( ',', '.' ).replaceAll( "[^0-9.]", "" );

            if ( strPrice.matches( "\\d+(\\.\\d+)?" ) )
            {
                offer.put( "price", strPrice );
            }
        }

        return offer;
    }

    /**
     * Gives the ISO 8601 date, or date-time with the offset of the server, of a day and an optional time
     *
     * @param date
     *            the day
     * @param strTime
     *            the time ( HH:mm ), or null for the whole day
     * @return the ISO value, or null when there is no day
     */
    private static String toIso( Date date, String strTime )
    {
        if ( date == null )
        {
            return null;
        }

        LocalDate day = date.toInstant( ).atZone( ZoneId.systemDefault( ) ).toLocalDate( );
        LocalTime time = CalendarEventFeedService.parseTime( strTime );

        return ( time == null ) ? day.toString( ) : day.atTime( time ).atZone( ZoneId.systemDefault( ) ).toOffsetDateTime( ).toString( );
    }

    /**
     * Puts a value in a map when it is not blank
     *
     * @param map
     *            the map
     * @param strKey
     *            the key
     * @param strValue
     *            the value
     */
    private static void putIfNotBlank( Map<String, Object> map, String strKey, String strValue )
    {
        if ( StringUtils.isNotBlank( strValue ) )
        {
            map.put( strKey, strValue );
        }
    }
}
