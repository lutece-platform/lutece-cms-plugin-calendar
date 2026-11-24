/*
 * Copyright (c) 2002-2014, Mairie de Paris
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
package fr.paris.lutece.plugins.calendar.web;

import fr.paris.lutece.api.user.User;
import fr.paris.lutece.plugins.calendar.business.Agenda;
import fr.paris.lutece.portal.service.admin.AccessDeniedException;
import fr.paris.lutece.plugins.calendar.business.CalendarHome;
import fr.paris.lutece.plugins.calendar.business.SimpleEvent;
import fr.paris.lutece.plugins.calendar.service.AgendaResource;
import fr.paris.lutece.plugins.calendar.service.Utils;
import fr.paris.lutece.portal.business.user.AdminUser;
import fr.paris.lutece.portal.service.admin.AdminUserService;
import fr.paris.lutece.portal.service.message.AdminMessage;
import fr.paris.lutece.portal.service.message.AdminMessageService;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.portal.service.template.AppTemplateService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.portal.service.workgroup.AdminWorkgroupService;
import fr.paris.lutece.portal.util.mvc.admin.MVCAdminJspBean;
import fr.paris.lutece.portal.util.mvc.admin.annotations.Controller;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.portal.web.constants.Messages;
import fr.paris.lutece.portal.web.insert.InsertServiceSelectionBean;
import fr.paris.lutece.portal.web.insert.InsertServiceSelectorJspBean;
import fr.paris.lutece.util.ReferenceList;
import fr.paris.lutece.util.html.HtmlTemplate;
import fr.paris.lutece.util.url.UrlItem;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;


/**
 * The insert service of the editors: inserts a link toward a calendar event
 */
@RequestScoped
@Named
@Controller( controllerJsp = CalendarServiceJspBean.CONTROLLER_JSP, controllerPath = CalendarServiceJspBean.CONTROLLER_PATH, right = CalendarServiceJspBean.RIGHT_MANAGE_CALENDAR, securityTokenEnabled = true )
public class CalendarServiceJspBean extends MVCAdminJspBean implements InsertServiceSelectionBean
{
    public static final String CONTROLLER_JSP = "SelectCalendar.jsp";
    public static final String CONTROLLER_PATH = "jsp/admin/plugins/calendar/";
    public static final String RIGHT_MANAGE_CALENDAR = "CALENDAR_MANAGEMENT";
    private static final long serialVersionUID = 2694692453596836769L;

    // Constants
    private static final String MESSAGE_ACCESS_DENIED = "Access denied";
    private static final String REGEX_ID = "^[\\d]+$";

    // Views
    private static final String VIEW_SELECT_CALENDAR = "selectCalendar";
    private static final String VIEW_SELECT_EVENTS = "selectEvents";
    private static final String VIEW_INSERT_EVENT_LINK = "insertEventLink";

    // Templates
    private static final String TEMPLATE_SELECTOR_CALENDAR = "admin/plugins/calendar/calendar_selector.html";
    private static final String TEMPLATE_SELECTOR_EVENT = "admin/plugins/calendar/event_selector.html";
    private static final String TEMPLATE_LINK = "admin/plugins/calendar/calendar_link.html";

    // JSP
    private static final String JSP_DO_INSERT = "jsp/admin/insert/DoInsertIntoElement.jsp";

    // Parameters
    private static final String PARAMETER_ALT = "alt";
    private static final String PARAMETER_TARGET = "target";
    private static final String PARAMETER_NAME = "name";
    private static final String PARAMETER_INPUT = "input";
    private static final String PARAMETER_MODE = "mode";

    // Markers
    private static final String MARK_ALT = "alt";
    private static final String MARK_NAME = "name";
    private static final String MARK_URL = "url";
    private static final String MARK_TARGET = "target";
    private static final String MARK_INPUT = "input_name";

    /**
     * {@inheritDoc}
     * The core instantiates the bean by reflection to show the first screen
     */
    @Override
    public String getInsertServiceSelectorUI( HttpServletRequest request )
    {
        return renderCalendarSelector( request );
    }

    /**
     * The insert services list of the core reads the sub categories of each service
     * @return an empty list: the service has none
     */
    public ReferenceList getSubCategories( )
    {
        return new ReferenceList( );
    }

    /**
     * Initializes the bean for a request, then checks that the calendar the request names (its calendar_id, or the calendar
     * of its event_id) belongs to the workgroups of the user: the selector only lists those calendars, and this check
     * refuses the others when their identifier is typed in an address
     * 
     * @param request
     *            the request
     * @param strRight
     *            the right of the feature
     * @throws AccessDeniedException
     *             if the user may not use the feature or the calendar the request names
     */
    @Override
    public void init( HttpServletRequest request, String strRight ) throws AccessDeniedException
    {
        super.init( request, strRight );

        Plugin pluginCalendar = PluginService.getPlugin( Constants.PLUGIN_NAME );
        String strCalendarId = request.getParameter( Constants.PARAMETER_CALENDAR_ID );
        String strEventId = request.getParameter( Constants.PARAMETER_EVENT_ID );
        java.util.List<Integer> listCalendarIds = new java.util.ArrayList<>( );

        if ( ( strCalendarId != null ) && strCalendarId.matches( REGEX_ID ) )
        {
            listCalendarIds.add( Integer.parseInt( strCalendarId ) );
        }

        if ( ( strEventId != null ) && strEventId.matches( REGEX_ID ) )
        {
            SimpleEvent event = CalendarHome.findEvent( Integer.parseInt( strEventId ), pluginCalendar );

            if ( event != null )
            {
                listCalendarIds.add( event.getIdCalendar( ) );
            }
        }

        for ( int nCalendarId : listCalendarIds )
        {
            AgendaResource agenda = CalendarHome.findAgendaResource( nCalendarId, pluginCalendar );

            if ( ( agenda != null ) && !AdminWorkgroupService.isAuthorized( agenda, (User) AdminUserService.getAdminUser( request ) ) )
            {
                throw new AccessDeniedException( MESSAGE_ACCESS_DENIED );
            }
        }
    }

    /**
     * Returns the calendar selection screen
     * @param request The Http request
     * @return The html code of the screen
     */
    @View( value = VIEW_SELECT_CALENDAR, defaultView = true )
    public String getSelectCalendar( HttpServletRequest request )
    {
        return renderCalendarSelector( request );
    }

    /**
     * Returns the event selection screen of a calendar
     * @param request The Http request
     * @return The html code of the screen
     */
    @View( VIEW_SELECT_EVENTS )
    public String getSelectEvents( HttpServletRequest request )
    {
        String strCalendarId = request.getParameter( Constants.PARAMETER_CALENDAR_ID );

        if ( ( strCalendarId == null ) || !strCalendarId.matches( REGEX_ID ) )
        {
            return redirect( request, AdminMessageService.getMessageUrl( request, Messages.MANDATORY_FIELDS, AdminMessage.TYPE_STOP ) );
        }

        Agenda agenda = Utils.getAgendaWithOccurrences( strCalendarId, request );

        if ( agenda == null )
        {
            return redirect( request, AdminMessageService.getMessageUrl( request, Messages.MANDATORY_FIELDS, AdminMessage.TYPE_STOP ) );
        }

        Map<String, Object> model = getDefaultModel( request );
        model.put( Constants.MARK_OCCURRENCES_LIST, agenda.getEvents( ) );
        model.put( Constants.MARK_CALENDAR_ID, strCalendarId );

        HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_SELECTOR_EVENT, getLocale( ), model );

        return template.getHtml( );
    }

    /**
     * Inserts the link toward the selected event in the editor
     * @param request The Http request
     * @return The redirection to the insertion of the core
     */
    @View( VIEW_INSERT_EVENT_LINK )
    public String getInsertEventLink( HttpServletRequest request )
    {
        String strEventId = request.getParameter( Constants.PARAMETER_EVENT_ID );
        String strTarget = request.getParameter( PARAMETER_TARGET );
        String strCalendarId = request.getParameter( Constants.PARAMETER_CALENDAR_ID );
        String strAlt = request.getParameter( PARAMETER_ALT );
        String strName = request.getParameter( PARAMETER_NAME );

        if ( ( strEventId == null ) || !strEventId.matches( REGEX_ID ) )
        {
            return redirect( request, AdminMessageService.getMessageUrl( request, Messages.MANDATORY_FIELDS, AdminMessage.TYPE_STOP ) );
        }

        Plugin plugin = PluginService.getPlugin( Constants.PLUGIN_NAME );
        SimpleEvent event = CalendarHome.findEvent( Integer.parseInt( strEventId ), plugin );

        if ( event == null )
        {
            return redirect( request, AdminMessageService.getMessageUrl( request, Messages.MANDATORY_FIELDS, AdminMessage.TYPE_STOP ) );
        }

        UrlItem url = new UrlItem( AppPathService.getPortalUrl( ) );
        url.addParameter( Constants.PARAMETER_PAGE, Constants.PLUGIN_NAME );
        url.addParameter( Constants.PARAMETER_ACTION, Constants.ACTION_SHOW_RESULT );
        url.addParameter( Constants.PARAMETER_EVENT_ID, event.getId( ) );
        url.addParameter( Constants.PARAMETER_DOCUMENT_ID, event.getDocumentId( ) );
        url.addParameter( Constants.PARAM_AGENDA, strCalendarId );

        Map<String, Object> model = new HashMap<>( );
        model.put( MARK_URL, url.getUrl( ) );
        model.put( MARK_TARGET, strTarget );
        model.put( MARK_ALT, strAlt );
        model.put( MARK_NAME, StringUtils.isEmpty( strName ) ? event.getTitle( ) : strName );

        HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_LINK, null, model );

        return redirect( request, insertUrl( request, request.getParameter( PARAMETER_INPUT ),
                StringEscapeUtils.escapeEcmaScript( template.getHtml( ) ) ) );
    }

    /**
     * Renders the calendar selection screen
     * @param request The Http request
     * @return The html code of the screen
     */
    private String renderCalendarSelector( HttpServletRequest request )
    {
        AdminUser user = AdminUserService.getAdminUser( request );
        Collection<AgendaResource> listAgendas = CalendarHome.findAgendaResourcesList( PluginService.getPlugin( Constants.PLUGIN_NAME ) );
        listAgendas = AdminWorkgroupService.getAuthorizedCollection( listAgendas, (User) user );

        Map<String, Object> model = getDefaultModel( request );
        model.put( Constants.MARK_CALENDARS_LIST, listAgendas );

        HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_SELECTOR_CALENDAR, user.getLocale( ), model );

        return template.getHtml( );
    }

    /**
     * Builds the url that inserts the code into the editor, as the core insert services do
     * @param request The Http request
     * @param strInput The name of the input
     * @param strInsert The code to insert
     * @return The url
     */
    private static String insertUrl( HttpServletRequest request, String strInput, String strInsert )
    {
        // No CR is allowed in the insert string
        String strCleanInsert = strInsert.replace( "\n", "" ).replace( "\r", "" );

        UrlItem urlDoInsert = new UrlItem( AppPathService.getBaseUrl( request ) + JSP_DO_INSERT );
        urlDoInsert.addParameter( PARAMETER_INPUT, strInput );
        request.getSession( ).setAttribute( InsertServiceSelectorJspBean.SESSION_INSERT, strCleanInsert );
        urlDoInsert.addParameter( PARAMETER_MODE, 1 );

        return urlDoInsert.getUrl( );
    }

    /**
     * Returns the model shared by the screens
     * @param request The Http request
     * @return the model
     */
    private static Map<String, Object> getDefaultModel( HttpServletRequest request )
    {
        Map<String, Object> model = new HashMap<>( );
        model.put( MARK_INPUT, request.getParameter( PARAMETER_INPUT ) );

        return model;
    }
}
