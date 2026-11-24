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
package fr.paris.lutece.plugins.calendar.web.portlet;

import fr.paris.lutece.plugins.calendar.business.portlet.MiniCalendarPortlet;
import fr.paris.lutece.plugins.calendar.business.portlet.MiniCalendarPortletHome;
import fr.paris.lutece.plugins.calendar.web.Constants;
import fr.paris.lutece.portal.business.portlet.Portlet;
import fr.paris.lutece.portal.business.portlet.PortletHome;
import fr.paris.lutece.portal.service.security.SecurityTokenService;
import fr.paris.lutece.portal.service.template.AppTemplateService;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.message.AdminMessage;
import fr.paris.lutece.portal.service.message.AdminMessageService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.portal.web.constants.Messages;
import fr.paris.lutece.portal.web.portlet.PortletJspBean;
import fr.paris.lutece.util.ReferenceList;
import fr.paris.lutece.util.html.HtmlTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;


/**
 * This class provides the user interface to manage calendar interval portlets.
 */
@RequestScoped
@Named
public class MiniCalendarPortletJspBean extends PortletJspBean
{
    @Inject
    private MiniCalendarPortletHome _portletHome;

    // Prefix of the properties related to this checkbox
    public static final String PROPERTY_TIME_INTERVAL_LIST = "calendar.interval.time";

    private static final long serialVersionUID = 8594742404929705042L;

    // Security tokens
    private static final String ACTION_CREATE_PORTLET = "calendar.createMiniCalendarPortlet";
    private static final String ACTION_MODIFY_PORTLET = "calendar.modifyMiniCalendarPortlet";
    private static final String MESSAGE_INVALID_TOKEN = "calendar.message.invalidToken";
    private static final String MESSAGE_PORTLET_NOT_FOUND = "calendar.message.portletNotFound";
    private static final String TEMPLATE_PORTLET_NOT_FOUND = "admin/plugins/calendar/portlet_not_found.html";

    // Bookmarks
    private static final String BOOKMARK_PAGE_ID = "@page_id@";
    private static final String BOOKMARK_PORTLET_ID = "@portlet_id@";

    // Parameters
    private static final String PARAMETER_PAGE_ID = "page_id";
    private static final String PARAMETER_PORTLET_ID = "portlet_id";
    private static final String PARAMETER_PORTLET_TYPE_ID = "portlet_type_id";
    private static final String PARAMETER_CBX_TOP_EVENT = "cbx_top_event";

    // Templates
    private static final String MARK_TOP_EVENT = "top_event";
    private static final String MARK_BASE_URL = "base_url";
    private static final String MARK_MESSAGE = "message";

    /**
     * Returns the creation form for the portlet
     * 
     * @param request the HTML request
     * @return the HTML code for the page
     */
    public String getCreate( HttpServletRequest request )
    {
        String strPageId = request.getParameter( PARAMETER_PAGE_ID );
        String strPortletTypeId = request.getParameter( PARAMETER_PORTLET_TYPE_ID );

        Map<String, Object> model = new HashMap<>( );
        model.put( SecurityTokenService.MARK_TOKEN, getSecurityTokenService( ).getToken( request, ACTION_CREATE_PORTLET ) );

        HtmlTemplate template = getCreateTemplate( strPageId, strPortletTypeId, model );

        return template.getHtml( );
    }

    /**
     * Processes the creation of the portlet
     * 
     * @param request the HTML request
     * @return the URL to redirect to
     */
    public String doCreate( HttpServletRequest request )
    {
        if ( !getSecurityTokenService( ).validate( request, ACTION_CREATE_PORTLET ) )
        {
            return AdminMessageService.getMessageUrl( request, MESSAGE_INVALID_TOKEN, AdminMessage.TYPE_STOP );
        }

        MiniCalendarPortlet portlet = new MiniCalendarPortlet( );

        // Standard controls on the creation form
        String strIdPage = request.getParameter( PARAMETER_PAGE_ID );
        int nIdPage = Integer.parseInt( strIdPage );


        String strError = setPortletCommonData( request, portlet );

        if ( strError != null )
        {
            return strError;
        }

        // mandatory field
        String strName = portlet.getName( );

        if ( strName.trim( ).equals( "" ) )
        {
            return AdminMessageService.getMessageUrl( request, Messages.MANDATORY_FIELDS, AdminMessage.TYPE_STOP );
        }

        portlet.setPageId( nIdPage );

        // Creating portlet
        _portletHome.create( portlet );

        // Returns page with new created portlet
        return getPageUrl( portlet.getPageId( ) );
    }

    /**
     * Returns the modification form for the portlet
     * 
     * @param request the HTML request
     * @return the HTML code for the page
     */
    public String getModify( HttpServletRequest request )
    {
        String strPortletId = request.getParameter( PARAMETER_PORTLET_ID );
        MiniCalendarPortlet portlet = findPortlet( strPortletId );

        if ( portlet == null )
        {
            Map<String, Object> modelError = new HashMap<>( );
            modelError.put( MARK_MESSAGE, I18nService.getLocalizedString( MESSAGE_PORTLET_NOT_FOUND, getLocale( ) ) );

            return AppTemplateService.getTemplate( TEMPLATE_PORTLET_NOT_FOUND, getLocale( ), modelError ).getHtml( );
        }

        int nPortletId = portlet.getId( );

        String strIdPage = request.getParameter( PARAMETER_PAGE_ID );
        String strBaseUrl = AppPathService.getBaseUrl( request );

        HashMap<String, Object> model = new HashMap<String, Object>( );
        model.put( BOOKMARK_PORTLET_ID, strPortletId );
        model.put( BOOKMARK_PAGE_ID, strIdPage );
        model.put( SecurityTokenService.MARK_TOKEN, getSecurityTokenService( ).getToken( request, ACTION_MODIFY_PORTLET ) );

        boolean top_event = MiniCalendarPortletHome.showTopEvent( );

        model.put( MARK_TOP_EVENT, top_event );
        model.put( MARK_BASE_URL, strBaseUrl );
        model.put( Constants.MARK_LOCALE, getLocale( ).getLanguage( ) );

        // Fill the specific part of the modify form
        HtmlTemplate template = getModifyTemplate( portlet, model );

        return template.getHtml( );
    }

    /**
     * Processes the modification of the portlet
     * 
     * @param request the HTTP request
     * @return the URL to redirect to
     */
    public String doModify( HttpServletRequest request )
    {
        if ( !getSecurityTokenService( ).validate( request, ACTION_MODIFY_PORTLET ) )
        {
            return AdminMessageService.getMessageUrl( request, MESSAGE_INVALID_TOKEN, AdminMessage.TYPE_STOP );
        }

        // Use the id in the request to load the portlet
        String strPortletId = request.getParameter( PARAMETER_PORTLET_ID );
        MiniCalendarPortlet portlet = findPortlet( strPortletId );

        if ( portlet == null )
        {
            return AdminMessageService.getMessageUrl( request, MESSAGE_PORTLET_NOT_FOUND, AdminMessage.TYPE_STOP );
        }

        int nPortletId = portlet.getId( );

        // Standard controls on the creation form

        String strError = setPortletCommonData( request, portlet );

        if ( strError != null )
        {
            return strError;
        }

        // mandatory field
        String strName = portlet.getName( );

        if ( strName.trim( ).equals( "" ) )
        {
            return AdminMessageService.getMessageUrl( request, Messages.MANDATORY_FIELDS, AdminMessage.TYPE_STOP );
        }

        portlet.update( );

        String strTopEvent = request.getParameter( PARAMETER_CBX_TOP_EVENT );
        boolean top_event = Boolean.FALSE;

        if ( strTopEvent != null )
        {
            top_event = Boolean.TRUE;
        }

        MiniCalendarPortletHome.updateTopEvent( top_event );

        return getPageUrl( portlet.getPageId( ) );
    }

    /**
     * Return the list of time intervals declared in properties file
     * @return A ReferenceList of time interval
     * @param request The HttpRequest
     */
    public static ReferenceList getIntervalList( HttpServletRequest request )
    {
        StringTokenizer st = new StringTokenizer( AppPropertiesService.getProperty( PROPERTY_TIME_INTERVAL_LIST ), "," );
        ReferenceList timeIntervalList = new ReferenceList( );

        while ( st.hasMoreElements( ) )
        {
            String strIntervalName = st.nextToken( ).trim( );
            String strDescription = I18nService.getLocalizedString( "calendar.interval." + strIntervalName
                    + ".description", request.getLocale( ) );
            int nDays = AppPropertiesService.getPropertyInt( "calendar.interval." + strIntervalName + ".value", 7 );
            timeIntervalList.addItem( nDays, strDescription );
        }

        return timeIntervalList;
    }

    /**
     * Loads the portlet named by the request, after checking it exists: the core dereferences the row it loads
     * @param strPortletId the portlet id
     * @return the portlet, or null when the id is not numeric, unknown, or of another type
     */
    private MiniCalendarPortlet findPortlet( String strPortletId )
    {
        if ( StringUtils.isEmpty( strPortletId ) || !StringUtils.isNumeric( strPortletId ) )
        {
            return null;
        }

        int nPortletId = Integer.parseInt( strPortletId );

        if ( _portletHome.getDAO( ).load( nPortletId ).getId( ) != nPortletId )
        {
            return null;
        }

        Portlet portlet = PortletHome.findByPrimaryKey( nPortletId );

        return ( portlet instanceof MiniCalendarPortlet ) ? ( MiniCalendarPortlet ) portlet : null;
    }
}
