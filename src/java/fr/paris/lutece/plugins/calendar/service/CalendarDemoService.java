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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.plugins.calendar.business.CalendarHome;
import fr.paris.lutece.plugins.calendar.business.OccurrenceEvent;
import fr.paris.lutece.plugins.calendar.business.SimpleEvent;
import fr.paris.lutece.plugins.calendar.business.category.Category;
import fr.paris.lutece.plugins.calendar.business.category.CategoryHome;
import fr.paris.lutece.plugins.calendar.business.details.EventDetails;
import fr.paris.lutece.plugins.calendar.business.details.EventDetailsHome;
import fr.paris.lutece.plugins.calendar.business.portlet.CalendarPortlet;
import fr.paris.lutece.plugins.calendar.business.portlet.CalendarPortletHome;
import fr.paris.lutece.plugins.calendar.service.search.CalendarIndexer;
import fr.paris.lutece.plugins.calendar.utils.CalendarIndexerUtils;
import fr.paris.lutece.plugins.calendar.web.Constants;
import fr.paris.lutece.portal.business.indexeraction.IndexerAction;
import fr.paris.lutece.portal.business.page.Page;
import fr.paris.lutece.portal.business.page.PageHome;
import fr.paris.lutece.portal.business.portlet.Portlet;
import fr.paris.lutece.portal.business.portlet.PortletHome;
import fr.paris.lutece.portal.business.portlet.PortletTemplate;
import fr.paris.lutece.portal.business.portlet.PortletTemplateHome;
import fr.paris.lutece.portal.business.style.PageTemplate;
import fr.paris.lutece.portal.business.style.PageTemplateHome;
import fr.paris.lutece.portal.service.datastore.DatastoreService;
import fr.paris.lutece.portal.service.image.ImageResource;
import fr.paris.lutece.portal.service.page.IPageService;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.portal.PortalService;
import fr.paris.lutece.portal.service.search.IndexationService;
import fr.paris.lutece.portal.service.util.AppException;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.portal.service.workgroup.AdminWorkgroupService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * The demonstration data of the plugin: a calendar of events relative to the current day, some with a picture and
 * extra details, its categories, and a page holding a calendar portlet in each template. The data is read from
 * WEB-INF/plugins/calendar/demo/calendar_demo.json; what was created is remembered in the datastore so that the demo
 * can be installed again (it replaces the previous one) or removed.
 */
@ApplicationScoped
public class CalendarDemoService
{
    private static final String PATH_DEMO = "/WEB-INF/plugins/calendar/demo/";
    private static final String FILE_DEMO = "calendar_demo.json";
    private static final String PATH_IMAGES = "images/";
    private static final String EXTENSION_SVG = ".svg";
    private static final String MIME_TYPE_SVG = "image/svg+xml";
    private static final String PATH_PORTLET_TEMPLATES = "skin/plugins/calendar/portlet/";
    private static final String KEY_AGENDA = "calendar.demo.agendaId";
    private static final String KEY_PAGE = "calendar.demo.pageId";
    private static final String KEY_CATEGORIES = "calendar.demo.categoryIds";
    private static final String SEPARATOR = ",";
    private static final String REPEAT_DAILY = "daily";
    private static final String REPEAT_WEEKLY = "weekly";
    private static final String REPEAT_MONTHLY = "monthly";
    private static final int DEFAULT_DAYS = 120;
    private static final int DEVICE_ALL = Portlet.FLAG_DISPLAY_ON_SMALL_DEVICE | Portlet.FLAG_DISPLAY_ON_NORMAL_DEVICE
            | Portlet.FLAG_DISPLAY_ON_LARGE_DEVICE | Portlet.FLAG_DISPLAY_ON_XLARGE_DEVICE;

    @Inject
    private CalendarService _calendarService;
    @Inject
    private EventListService _eventListService;
    @Inject
    private CalendarPortletHome _portletHome;
    @Inject
    private IPageService _pageService;

    /**
     * Tells whether the demonstration data is installed
     * @return true when the demonstration calendar exists
     */
    public boolean isInstalled( )
    {
        return getStoredId( KEY_AGENDA ) > 0;
    }

    /**
     * Returns the identifier of the demonstration page
     * @return the identifier of the page, or 0 when the demonstration is not installed
     */
    public int getPageId( )
    {
        return getStoredId( KEY_PAGE );
    }

    /**
     * Installs the demonstration data, replacing a previous installation
     * @param plugin the plugin
     * @return the identifier of the demonstration page
     */
    public int install( Plugin plugin )
    {
        remove( plugin );

        JsonNode root = readDemo( );
        AgendaResource agenda = createAgenda( root.path( "calendar" ), plugin );
        Map<String, Category> mapCategories = createCategories( root.path( "categories" ), plugin );
        Date today = getToday( );

        for ( JsonNode node : root.path( "events" ) )
        {
            createEvent( node, Integer.parseInt( agenda.getId( ) ), mapCategories, today, plugin );
        }

        IndexationService.processIndexing( false );

        int nPageId = createPage( root.path( "page" ), root.path( "portlets" ), agenda.getId( ) );
        DatastoreService.setDataValue( KEY_AGENDA, agenda.getId( ) );
        DatastoreService.setDataValue( KEY_PAGE, String.valueOf( nPageId ) );
        DatastoreService.setDataValue( KEY_CATEGORIES, StringUtils.join( mapCategories.values( ).stream( ).map( c -> String.valueOf( c.getId( ) ) ).toList( ), SEPARATOR ) );

        return nPageId;
    }

    /**
     * Removes the demonstration data: its page and portlets, its events, its calendar and its categories
     * @param plugin the plugin
     */
    public void remove( Plugin plugin )
    {
        int nPageId = getStoredId( KEY_PAGE );

        if ( ( nPageId > 0 ) && ( PageHome.findByPrimaryKey( nPageId ) != null ) )
        {
            _pageService.removePage( nPageId );
        }

        int nAgendaId = getStoredId( KEY_AGENDA );

        if ( ( nAgendaId > 0 ) && ( CalendarHome.findAgendaResource( nAgendaId, plugin ) != null ) )
        {
            for ( SimpleEvent event : _eventListService.getSimpleEvents( nAgendaId, Constants.SORT_ASC ) )
            {
                index( nAgendaId, event.getId( ), IndexerAction.TASK_DELETE, plugin );
                _eventListService.doRemoveEvent( nAgendaId, event.getId( ), null, plugin );
            }

            _calendarService.doRemoveAgenda( nAgendaId, plugin );
            IndexationService.processIndexing( false );
        }

        for ( String strId : StringUtils.split( DatastoreService.getDataValue( KEY_CATEGORIES, StringUtils.EMPTY ), SEPARATOR ) )
        {
            int nCategoryId = parseId( strId );

            if ( ( nCategoryId > 0 ) && ( CategoryHome.find( nCategoryId, plugin ) != null ) && ( CategoryHome.findCountIdEvents( nCategoryId, plugin ) == 0 ) )
            {
                CategoryHome.remove( nCategoryId, plugin );
            }
        }

        DatastoreService.removeData( KEY_AGENDA );
        DatastoreService.removeData( KEY_PAGE );
        DatastoreService.removeData( KEY_CATEGORIES );
    }

    /**
     * Reads the description of the demonstration
     * @return the root node of the description
     */
    private JsonNode readDemo( )
    {
        try
        {
            return new ObjectMapper( ).readTree( readFile( FILE_DEMO ) );
        }
        catch( IOException e )
        {
            throw new AppException( "Unable to read the calendar demonstration data", e );
        }
    }

    /**
     * Reads a file of the demonstration folder
     * @param strRelativePath the path of the file in the demonstration folder
     * @return its content
     * @throws IOException when the file cannot be read
     */
    private byte [ ] readFile( String strRelativePath ) throws IOException
    {
        return Files.readAllBytes( Path.of( AppPathService.getAbsolutePathFromRelativePath( PATH_DEMO + strRelativePath ) ) );
    }

    /**
     * Creates the demonstration calendar
     * @param node the description of the calendar
     * @param plugin the plugin
     * @return the calendar
     */
    private AgendaResource createAgenda( JsonNode node, Plugin plugin )
    {
        AgendaResource agenda = new AgendaResource( );
        agenda.setName( node.path( "name" ).asText( ) );
        agenda.setEventImage( node.path( "color" ).asText( ) );
        agenda.setEventPrefix( node.path( "prefix" ).asText( ) );
        agenda.setRole( Constants.PROPERTY_ROLE_NONE );
        agenda.setRoleManager( Constants.PROPERTY_ROLE_NONE );
        agenda.setWorkgroup( AdminWorkgroupService.ALL_GROUPS );
        agenda.setNotify( false );
        agenda.setPeriodValidity( -1 );
        _calendarService.doCreateAgenda( agenda, plugin );

        return agenda;
    }

    /**
     * Creates the demonstration categories
     * @param nodes the descriptions of the categories
     * @param plugin the plugin
     * @return the categories by their key in the description
     */
    private Map<String, Category> createCategories( JsonNode nodes, Plugin plugin )
    {
        Map<String, Category> mapCategories = new HashMap<>( );

        for ( JsonNode node : nodes )
        {
            Category category = new Category( );
            category.setName( node.path( "name" ).asText( ) );
            category.setDescription( node.path( "description" ).asText( ) );
            category.setWorkgroup( AdminWorkgroupService.ALL_GROUPS );
            CategoryHome.create( category, plugin );
            mapCategories.put( node.path( "key" ).asText( ), category );
        }

        return mapCategories;
    }

    /**
     * Creates an event, its occurrences, its details, and asks for their indexing
     * @param node the description of the event
     * @param nAgendaId the identifier of the calendar
     * @param mapCategories the categories by their key
     * @param today the current day
     * @param plugin the plugin
     */
    private void createEvent( JsonNode node, int nAgendaId, Map<String, Category> mapCategories, Date today, Plugin plugin )
    {
        Date dateStart = Utils.addDays( today, node.path( "offset" ).asLong( ) );
        SimpleEvent event = new SimpleEvent( );
        event.setIdCalendar( nAgendaId );
        event.setTitle( node.path( "title" ).asText( ) );
        event.setDescription( node.path( "description" ).asText( ) );
        event.setDate( dateStart );
        event.setDateTimeStart( node.path( "timeStart" ).asText( ) );
        event.setDateTimeEnd( node.path( "timeEnd" ).asText( ) );
        event.setLocation( node.path( "location" ).asText( ) );
        event.setLocationAddress( node.path( "address" ).asText( ) );
        event.setLocationZip( node.path( "zip" ).asText( ) );
        event.setLocationTown( node.path( "town" ).asText( ) );
        event.setLinkUrl( StringUtils.EMPTY );
        event.setPageUrl( StringUtils.EMPTY );
        event.setMapUrl( StringUtils.EMPTY );
        event.setDocumentId( -1 );
        event.setTopEvent( node.path( "top" ).asInt( 0 ) );
        event.setStatus( node.path( "status" ).asText( AppPropertiesService.getProperty( Constants.PROPERTY_EVENT_DEFAULT_STATUS ) ) );
        event.setTags( StringUtils.split( node.path( "tags" ).asText( ) ) );
        setRepetition( event, node, dateStart );

        List<Category> listCategories = new ArrayList<>( );

        for ( JsonNode key : node.path( "categories" ) )
        {
            Category category = mapCategories.get( key.asText( ) );

            if ( category != null )
            {
                listCategories.add( category );
            }
        }

        event.setListCategories( listCategories );
        event.setImageResource( getImage( node.path( "image" ).asText( ) ) );

        _eventListService.doAddEvent( event, null, plugin );
        EventDetailsHome.save( getDetails( node.path( "details" ), event.getId( ) ), plugin );
        index( nAgendaId, event.getId( ), IndexerAction.TASK_CREATE, plugin );
    }

    /**
     * Sets the repetition of an event: a single day, several days in a row, or a daily, weekly or monthly series
     * @param event the event
     * @param node the description of the event
     * @param dateStart the first day of the event
     */
    private void setRepetition( SimpleEvent event, JsonNode node, Date dateStart )
    {
        String strRepeat = node.path( "repeat" ).asText( StringUtils.EMPTY );
        int nCount = Math.max( 1, node.path( "count" ).asInt( 1 ) );
        int nPeriodicity;

        switch( strRepeat )
        {
            case REPEAT_DAILY:
                nPeriodicity = Constants.PARAM_DAY;
                break;
            case REPEAT_WEEKLY:
                nPeriodicity = Constants.PARAM_WEEK;
                break;
            case REPEAT_MONTHLY:
                nPeriodicity = Constants.PARAM_MONTH;
                break;
            default:
                nPeriodicity = 0;
                nCount = Math.max( 1, node.path( "days" ).asInt( 1 ) );
                break;
        }

        event.setPeriodicity( nPeriodicity );
        event.setOccurrence( nCount );
        event.setDateEnd( ( nPeriodicity == 0 ) ? Utils.addDays( dateStart, nCount - 1L ) : Utils.getDateForward( dateStart, nPeriodicity, nCount, null ) );
    }

    /**
     * Returns the picture of an event
     * @param strName the name of the picture in the demonstration folder, or an empty string
     * @return the picture, or null when the event has none
     */
    private ImageResource getImage( String strName )
    {
        if ( StringUtils.isBlank( strName ) )
        {
            return null;
        }

        try
        {
            ImageResource image = new ImageResource( );
            image.setImage( readFile( PATH_IMAGES + strName + EXTENSION_SVG ) );
            image.setMimeType( MIME_TYPE_SVG );

            return image;
        }
        catch( IOException e )
        {
            AppLogService.getLogger( ).error( "Calendar demonstration: picture {} not found", strName, e );

            return null;
        }
    }

    /**
     * Builds the details of an event
     * @param node the description of the details
     * @param nEventId the identifier of the event
     * @return the details
     */
    private EventDetails getDetails( JsonNode node, int nEventId )
    {
        EventDetails details = new EventDetails( );
        details.setIdEvent( nEventId );
        details.setPriceType( node.path( "priceType" ).asText( EventDetails.PRICE_FREE ) );
        details.setPrice( text( node, "price" ) );
        details.setAttendanceMode( node.path( "attendance" ).asText( EventDetails.ATTENDANCE_OFFLINE ) );
        details.setOnlineUrl( text( node, "onlineUrl" ) );
        details.setOrganizerName( text( node, "organizer" ) );
        details.setOrganizerEmail( text( node, "organizerEmail" ) );
        details.setOrganizerPhone( text( node, "organizerPhone" ) );
        details.setOrganizerUrl( text( node, "organizerUrl" ) );
        details.setRegistrationUrl( text( node, "registrationUrl" ) );
        details.setAccessibility( text( node, "accessibility" ) );
        details.setLatitude( node.has( "lat" ) ? node.path( "lat" ).asDouble( ) : null );
        details.setLongitude( node.has( "lon" ) ? node.path( "lon" ).asDouble( ) : null );

        return details;
    }

    /**
     * Returns a text field of a node
     * @param node the node
     * @param strField the name of the field
     * @return its value, or null when it is absent or blank
     */
    private static String text( JsonNode node, String strField )
    {
        return StringUtils.defaultIfBlank( node.path( strField ).asText( null ), null );
    }

    /**
     * Asks for the indexing of the occurrences of an event
     * @param nAgendaId the identifier of the calendar
     * @param nEventId the identifier of the event
     * @param nTask the indexing task
     * @param plugin the plugin
     */
    private void index( int nAgendaId, int nEventId, int nTask, Plugin plugin )
    {
        for ( OccurrenceEvent occurrence : _eventListService.getOccurrenceEvents( nAgendaId, nEventId, Constants.SORT_ASC, plugin ) )
        {
            IndexationService.addIndexerAction( Integer.toString( occurrence.getId( ) ), AppPropertiesService.getProperty( CalendarIndexer.PROPERTY_INDEXER_NAME ), nTask );
            CalendarIndexerUtils.addIndexerAction( occurrence.getId( ), nTask );
        }
    }

    /**
     * Creates the demonstration page under the root page, with a calendar portlet in each template
     * @param nodePage the description of the page
     * @param nodePortlets the descriptions of the portlets
     * @param strAgendaId the identifier of the demonstration calendar
     * @return the identifier of the page
     */
    private int createPage( JsonNode nodePage, JsonNode nodePortlets, String strAgendaId )
    {
        int nRootId = PortalService.getRootPageId( );
        Page root = PageHome.findByPrimaryKey( nRootId );
        Page page = new Page( );
        page.setParentPageId( nRootId );
        page.setName( nodePage.path( "name" ).asText( ) );
        page.setDescription( nodePage.path( "description" ).asText( ) );
        page.setPageTemplateId( getPageTemplateId( nodePage.path( "templateFile" ).asText( ) ) );
        page.setOrder( PageHome.getChildPagesMinimalData( nRootId ).size( ) + 1 );
        page.setRole( Constants.PROPERTY_ROLE_NONE );
        page.setCodeTheme( root.getCodeTheme( ) );
        page.setNodeStatus( 1 );
        page.setIdAuthorizationNode( root.getIdAuthorizationNode( ) );
        _pageService.createPage( page );

        int nDays = nodePage.path( "days" ).asInt( DEFAULT_DAYS );
        Map<String, Integer> mapTemplates = getPortletTemplateIds( );
        Map<Integer, Integer> mapOrders = new HashMap<>( );

        for ( JsonNode node : nodePortlets )
        {
            Integer nTemplateId = mapTemplates.get( node.path( "template" ).asText( ) );

            if ( nTemplateId != null )
            {
                int nColumn = node.path( "column" ).asInt( 1 );
                int nOrder = mapOrders.merge( nColumn, 1, Integer::sum );
                CalendarPortlet portlet = new CalendarPortlet( );
                portlet.setPageId( page.getId( ) );
                portlet.setPortletTypeId( _portletHome.getPortletTypeId( ) );
                portlet.setName( node.path( "name" ).asText( ) );
                portlet.setColumn( nColumn );
                portlet.setOrder( nOrder );
                portlet.setIdTemplate( nTemplateId );
                portlet.setStyleId( getStyleId( ) );
                portlet.setRole( Constants.PROPERTY_ROLE_NONE );
                portlet.setDisplayPortletTitle( 0 );
                portlet.setAcceptAlias( 0 );
                portlet.setDeviceDisplayFlags( DEVICE_ALL );
                _portletHome.create( portlet );
                CalendarPortletHome.insertCalendar( portlet.getId( ), strAgendaId, nDays );
            }
        }

        _pageService.invalidateContent( page.getId( ) );

        return page.getId( );
    }

    /**
     * Returns the identifier of a page template by its file, or the first template when none matches
     * @param strFile the file of the template
     * @return the identifier of the template
     */
    private static int getPageTemplateId( String strFile )
    {
        List<PageTemplate> listTemplates = PageTemplateHome.getPageTemplatesList( );

        return listTemplates.stream( ).filter( t -> strFile.equals( t.getFile( ) ) ).findFirst( ).or( ( ) -> listTemplates.stream( ).findFirst( ) )
                .map( PageTemplate::getId ).orElse( 1 );
    }

    /**
     * Returns the templates of the calendar portlet by their file name
     * @return the identifiers of the templates by their file name
     */
    private Map<String, Integer> getPortletTemplateIds( )
    {
        Map<String, Integer> mapTemplates = new HashMap<>( );

        for ( PortletTemplate template : PortletTemplateHome.findByPortletType( _portletHome.getPortletTypeId( ) ) )
        {
            mapTemplates.put( Strings.CS.removeStart( template.getTemplatePath( ), PATH_PORTLET_TEMPLATES ), template.getId( ) );
        }

        return mapTemplates;
    }

    /**
     * Returns the style of the calendar portlets: the first style of the type, or 0 when it has none
     * @return the identifier of the style
     */
    private int getStyleId( )
    {
        return PortletHome.getStylesList( _portletHome.getPortletTypeId( ) ).stream( ).findFirst( ).map( item -> parseId( item.getCode( ) ) ).orElse( 0 );
    }

    /**
     * Returns the current day at midnight
     * @return the current day
     */
    private static Date getToday( )
    {
        Calendar calendar = new GregorianCalendar( );
        calendar.set( Calendar.HOUR_OF_DAY, 0 );
        calendar.set( Calendar.MINUTE, 0 );
        calendar.set( Calendar.SECOND, 0 );
        calendar.set( Calendar.MILLISECOND, 0 );

        return calendar.getTime( );
    }

    /**
     * Returns an identifier kept in the datastore
     * @param strKey the key of the datastore
     * @return the identifier, or 0 when there is none
     */
    private static int getStoredId( String strKey )
    {
        return parseId( DatastoreService.getDataValue( strKey, StringUtils.EMPTY ) );
    }

    /**
     * Parses an identifier
     * @param strId the identifier as text
     * @return the identifier, or 0 when the text is not a number
     */
    private static int parseId( String strId )
    {
        return StringUtils.isNumeric( strId ) ? Integer.parseInt( strId ) : 0;
    }
}
