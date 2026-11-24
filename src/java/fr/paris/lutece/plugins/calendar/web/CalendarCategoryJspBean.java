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
import fr.paris.lutece.plugins.calendar.business.category.Category;
import fr.paris.lutece.plugins.calendar.business.category.CategoryHome;
import fr.paris.lutece.plugins.calendar.service.CalendarResourceIdService;
import fr.paris.lutece.plugins.calendar.service.CategoryService;
import fr.paris.lutece.portal.business.rbac.RBAC;
import fr.paris.lutece.portal.business.user.AdminUser;
import fr.paris.lutece.portal.service.admin.AccessDeniedException;
import fr.paris.lutece.portal.service.message.AdminMessage;
import fr.paris.lutece.portal.service.message.AdminMessageService;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.portal.service.rbac.RBACService;
import fr.paris.lutece.portal.service.upload.MultipartItem;
import fr.paris.lutece.portal.service.workgroup.AdminWorkgroupService;
import fr.paris.lutece.portal.util.mvc.admin.MVCAdminJspBean;
import fr.paris.lutece.portal.util.mvc.admin.annotations.Controller;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.portal.web.cdi.mvc.Models;
import fr.paris.lutece.portal.web.constants.Messages;
import fr.paris.lutece.portal.web.upload.MultipartHttpServletRequest;
import fr.paris.lutece.util.ReferenceList;
import fr.paris.lutece.util.url.UrlItem;

import java.util.Collection;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;


/**
 * This class provides the user interface to manage calendar categories
 */
@RequestScoped
@Named
@Controller( controllerJsp = "ManageCalendarCategory.jsp", controllerPath = "jsp/admin/plugins/calendar/", right = CalendarCategoryJspBean.RIGHT_CATEGORY_MANAGEMENT, securityTokenEnabled = true )
public class CalendarCategoryJspBean extends MVCAdminJspBean
{
    // Right
    public static final String RIGHT_CATEGORY_MANAGEMENT = "CALENDAR_MANAGEMENT";
    private static final long serialVersionUID = 6326580154716939427L;

    //Constants
    private static final String REGEX_ID = "^[\\d]+$";
    private static final int ERROR_ID_CATEGORY = -1;
    private static final String MESSAGE_ACCESS_DENIED = "Access denied";

    // Views
    private static final String VIEW_MANAGE_CATEGORY = "manageCategory";
    private static final String VIEW_CREATE_CATEGORY = "createCategory";
    private static final String VIEW_MODIFY_CATEGORY = "modifyCategory";
    private static final String VIEW_CONFIRM_REMOVE_CATEGORY = "confirmRemoveCategory";

    // Actions
    private static final String ACTION_CREATE_CATEGORY = "createCategory";
    private static final String ACTION_MODIFY_CATEGORY = "modifyCategory";
    private static final String ACTION_REMOVE_CATEGORY = "removeCategory";

    //Markers
    private static final String MARK_CATEGORY_LIST = "category_list";
    private static final String MARK_CATEGORY_DISPLAY = "categoryDisplay";
    private static final String MARK_USER_WORKGROUP_LIST = "user_workgroup_list";
    private static final String MARK_WORKGROUP_SELECTED = "selected_workgroup";

    // Parameters
    private static final String PARAMETER_CATEGORY_ID = "category_id";
    private static final String PARAMETER_CATEGORY_NAME = "category_name";
    private static final String PARAMETER_CATEGORY_DESCRIPTION = "category_description";
    private static final String PARAMETER_CATEGORY_UPDATE_ICON = "update_icon";
    private static final String PARAMETER_IMAGE_CONTENT = "category_icon";
    private static final String PARAMETER_WORKGROUP_KEY = "workgroup_key";

    // Templates
    private static final String TEMPLATE_MANAGE_CATEGORY = "admin/plugins/calendar/category/manage_category.html";
    private static final String TEMPLATE_CREATE_CATEGORY = "admin/plugins/calendar/category/create_category.html";
    private static final String TEMPLATE_MODIFY_CATEGORY = "admin/plugins/calendar/category/modify_category.html";

    // Properties
    private static final String PROPERTY_PAGE_TITLE_MANAGE_CATEGORY = "calendar.manage_category.pageTitle";
    private static final String PROPERTY_PAGE_TITLE_CREATE_CATEGORY = "calendar.create_category.pageTitle";
    private static final String PROPERTY_PAGE_TITLE_MODIFY_CATEGORY = "calendar.modify_category.pageTitle";

    // Message
    private static final String MESSAGE_CATEGORY_EXIST = "calendar.message.categoryExist";
    private static final String MESSAGE_CATEGORY_IS_LINKED = "calendar.message.categoryIsLinked";
    private static final String MESSAGE_CATEGORY_ERROR = "calendar.message.categoryError";
    private static final String MESSAGE_CONFIRM_REMOVE_CATEGORY = "calendar.message.confirmRemoveCategory";

    /**
     * Returns the category list management page
     * @param model the model
     * @param request The Http request
     * @return Html page
     * @throws AccessDeniedException If the user is not allowed to manage the calendars
     */
    @View( value = VIEW_MANAGE_CATEGORY, defaultView = true )
    public String getManageCategory( Models model, HttpServletRequest request ) throws AccessDeniedException
    {
        checkPermission( );
        model.put( MARK_CATEGORY_LIST, CategoryService.getAllCategoriesDisplay( getUser( ) ) );

        return getPage( PROPERTY_PAGE_TITLE_MANAGE_CATEGORY, TEMPLATE_MANAGE_CATEGORY );
    }

    /**
     * Returns the category creation page
     * @param model the model
     * @param request The Http request
     * @return Html page
     * @throws AccessDeniedException If the user is not allowed to manage the calendars
     */
    @View( VIEW_CREATE_CATEGORY )
    public String getCreateCategory( Models model, HttpServletRequest request ) throws AccessDeniedException
    {
        checkPermission( );

        AdminUser user = getUser( );
        ReferenceList refListWorkGroups = AdminWorkgroupService.getUserWorkgroups( user, getLocale( ) );
        model.put( MARK_USER_WORKGROUP_LIST, refListWorkGroups );

        //LUTECE-890 : the first workgroup will be selected by default
        if ( !refListWorkGroups.isEmpty( ) )
        {
            model.put( MARK_WORKGROUP_SELECTED, refListWorkGroups.get( 0 ).getCode( ) );
        }

        return getPage( PROPERTY_PAGE_TITLE_CREATE_CATEGORY, TEMPLATE_CREATE_CATEGORY );
    }

    /**
     * Creates a category
     * @param request The Http request
     * @return The redirection
     * @throws AccessDeniedException If the user is not allowed to manage the calendars
     */
    @Action( ACTION_CREATE_CATEGORY )
    public String doCreateCategory( HttpServletRequest request ) throws AccessDeniedException
    {
        checkPermission( );

        Category category = new Category( );
        String strCategoryName = request.getParameter( PARAMETER_CATEGORY_NAME );
        String strCategoryDescription = request.getParameter( PARAMETER_CATEGORY_DESCRIPTION );
        String strWorkgroup = request.getParameter( PARAMETER_WORKGROUP_KEY );

        MultipartHttpServletRequest mRequest = (MultipartHttpServletRequest) request;
        MultipartItem item = mRequest.getFile( PARAMETER_IMAGE_CONTENT );

        // Mandatory field
        if ( ( strCategoryName == null ) || ( strCategoryName.length( ) == 0 ) )
        {
            return redirect( request,
                    AdminMessageService.getMessageUrl( request, Messages.MANDATORY_FIELDS, AdminMessage.TYPE_STOP ) );
        }

        Plugin plugin = PluginService.getPlugin( Constants.PLUGIN_NAME );

        // check if category exist
        if ( !CategoryHome.findByName( strCategoryName, plugin ).isEmpty( ) )
        {
            return redirect( request,
                    AdminMessageService.getMessageUrl( request, MESSAGE_CATEGORY_EXIST, AdminMessage.TYPE_STOP ) );
        }

        category.setName( strCategoryName );
        category.setDescription( strCategoryDescription );

        if ( item != null )
        {
            category.setIconContent( item.get( ) );
            category.setIconMimeType( item.getContentType( ) );
        }

        category.setWorkgroup( strWorkgroup );
        CategoryHome.create( category, plugin );

        return redirectView( request, VIEW_MANAGE_CATEGORY );
    }

    /**
     * Returns the category modification page
     * @param model the model
     * @param request The Http request
     * @return Html page
     * @throws AccessDeniedException If the user is not allowed to manage the calendars
     */
    @View( VIEW_MODIFY_CATEGORY )
    public String getModifyCategory( Models model, HttpServletRequest request ) throws AccessDeniedException
    {
        checkPermission( );

        int nIdCategory = checkCategoryId( request );

        if ( nIdCategory == ERROR_ID_CATEGORY )
        {
            return redirect( request,
                    AdminMessageService.getMessageUrl( request, MESSAGE_CATEGORY_ERROR, AdminMessage.TYPE_ERROR ) );
        }

        checkWorkgroup( nIdCategory );

        AdminUser user = getUser( );
        ReferenceList refListWorkGroups = AdminWorkgroupService.getUserWorkgroups( user, getLocale( ) );
        model.put( MARK_CATEGORY_DISPLAY, CategoryService.getCategoryDisplay( nIdCategory ) );
        model.put( MARK_USER_WORKGROUP_LIST, refListWorkGroups );

        return getPage( PROPERTY_PAGE_TITLE_MODIFY_CATEGORY, TEMPLATE_MODIFY_CATEGORY );
    }

    /**
     * Modifies a category
     * @param request The Http request
     * @return The redirection
     * @throws AccessDeniedException If the user is not allowed to manage the calendars
     */
    @Action( ACTION_MODIFY_CATEGORY )
    public String doModifyCategory( HttpServletRequest request ) throws AccessDeniedException
    {
        checkPermission( );

        String strCategoryName = request.getParameter( PARAMETER_CATEGORY_NAME );
        String strCategoryDescription = request.getParameter( PARAMETER_CATEGORY_DESCRIPTION );
        String strCategoryUpdateIcon = request.getParameter( PARAMETER_CATEGORY_UPDATE_ICON );
        String strWorkgroup = request.getParameter( PARAMETER_WORKGROUP_KEY );
        int nIdCategory = checkCategoryId( request );

        if ( nIdCategory == ERROR_ID_CATEGORY )
        {
            return redirect( request,
                    AdminMessageService.getMessageUrl( request, MESSAGE_CATEGORY_ERROR, AdminMessage.TYPE_ERROR ) );
        }

        checkWorkgroup( nIdCategory );

        // Mandatory field
        if ( ( strCategoryName == null ) || ( strCategoryName.length( ) == 0 ) )
        {
            return redirect( request,
                    AdminMessageService.getMessageUrl( request, Messages.MANDATORY_FIELDS, AdminMessage.TYPE_STOP ) );
        }

        Plugin plugin = PluginService.getPlugin( Constants.PLUGIN_NAME );

        // check if category exist
        Collection<Category> categoriesList = CategoryHome.findByName( strCategoryName, plugin );

        if ( !categoriesList.isEmpty( ) && ( categoriesList.iterator( ).next( ).getId( ) != nIdCategory ) )
        {
            return redirect( request,
                    AdminMessageService.getMessageUrl( request, MESSAGE_CATEGORY_EXIST, AdminMessage.TYPE_STOP ) );
        }

        Category category = CategoryHome.find( nIdCategory, plugin );
        category.setName( strCategoryName );
        category.setDescription( strCategoryDescription );

        if ( strCategoryUpdateIcon != null )
        {
            MultipartHttpServletRequest mRequest = (MultipartHttpServletRequest) request;
            MultipartItem item = mRequest.getFile( PARAMETER_IMAGE_CONTENT );

            if ( item != null )
            {
                category.setIconContent( item.get( ) );
                category.setIconMimeType( item.getContentType( ) );
            }
        }

        category.setWorkgroup( strWorkgroup );
        CategoryHome.update( category, plugin );

        return redirectView( request, VIEW_MANAGE_CATEGORY );
    }

    /**
     * Returns the confirmation to remove a category
     * @param request The Http request
     * @return The confirmation message
     * @throws AccessDeniedException If the user is not allowed to manage the calendars
     */
    @View( value = VIEW_CONFIRM_REMOVE_CATEGORY, securityTokenAction = ACTION_REMOVE_CATEGORY )
    public String getConfirmRemoveCategory( HttpServletRequest request ) throws AccessDeniedException
    {
        checkPermission( );

        int nIdCategory = checkCategoryId( request );

        if ( nIdCategory == ERROR_ID_CATEGORY )
        {
            return redirect( request,
                    AdminMessageService.getMessageUrl( request, MESSAGE_CATEGORY_ERROR, AdminMessage.TYPE_ERROR ) );
        }

        checkWorkgroup( nIdCategory );

        Plugin plugin = PluginService.getPlugin( Constants.PLUGIN_NAME );

        // Test if the category is assigned
        if ( CategoryHome.findCountIdEvents( nIdCategory, plugin ) > 0 )
        {
            return redirect( request,
                    AdminMessageService.getMessageUrl( request, MESSAGE_CATEGORY_IS_LINKED, AdminMessage.TYPE_STOP ) );
        }

        UrlItem url = new UrlItem( getActionUrl( ACTION_REMOVE_CATEGORY ) );
        url.addParameter( PARAMETER_CATEGORY_ID, Integer.toString( nIdCategory ) );

        return redirect( request, AdminMessageService.getMessageUrl( request, MESSAGE_CONFIRM_REMOVE_CATEGORY,
                url.getUrl( ), AdminMessage.TYPE_CONFIRMATION ) );
    }

    /**
     * Removes a category
     * @param request The Http request
     * @return The redirection
     * @throws AccessDeniedException If the user is not allowed to manage the calendars
     */
    @Action( ACTION_REMOVE_CATEGORY )
    public String doRemoveCategory( HttpServletRequest request ) throws AccessDeniedException
    {
        checkPermission( );

        int nIdCategory = checkCategoryId( request );

        if ( nIdCategory == ERROR_ID_CATEGORY )
        {
            return redirect( request,
                    AdminMessageService.getMessageUrl( request, MESSAGE_CATEGORY_ERROR, AdminMessage.TYPE_ERROR ) );
        }

        checkWorkgroup( nIdCategory );

        Plugin plugin = PluginService.getPlugin( Constants.PLUGIN_NAME );

        // Test if the category is assigned
        if ( CategoryHome.findCountIdEvents( nIdCategory, plugin ) > 0 )
        {
            return redirect( request,
                    AdminMessageService.getMessageUrl( request, MESSAGE_CATEGORY_IS_LINKED, AdminMessage.TYPE_STOP ) );
        }

        CategoryHome.remove( nIdCategory, plugin );

        return redirectView( request, VIEW_MANAGE_CATEGORY );
    }

    /**
     * Checks the RBAC permission to manage the calendars
     * @throws AccessDeniedException If the user is not allowed to manage the calendars
     */
    private void checkPermission( ) throws AccessDeniedException
    {
        if ( !RBACService.isAuthorized( CalendarResourceIdService.RESOURCE_TYPE, RBAC.WILDCARD_RESOURCES_ID,
                CalendarResourceIdService.PERMISSION_MANAGE, (User) getUser( ) ) )
        {
            throw new AccessDeniedException( MESSAGE_ACCESS_DENIED );
        }
    }

    /**
     * Checks the workgroup of a category against the user
     * @param nIdCategory the category id
     * @throws AccessDeniedException If the category belongs to a workgroup of another user
     */
    private void checkWorkgroup( int nIdCategory ) throws AccessDeniedException
    {
        Category category = CategoryHome.find( nIdCategory, PluginService.getPlugin( Constants.PLUGIN_NAME ) );

        if ( ( category != null ) && !AdminWorkgroupService.isAuthorized( category, (User) getUser( ) ) )
        {
            throw new AccessDeniedException( MESSAGE_ACCESS_DENIED );
        }
    }

    /**
     * Check if the id of the category is valid
     * @param request The HTTP request
     * @return the id of the category, or -1 when it is not valid
     */
    private int checkCategoryId( HttpServletRequest request )
    {
        String strCategoryId = request.getParameter( PARAMETER_CATEGORY_ID );

        if ( ( strCategoryId == null ) || !strCategoryId.matches( REGEX_ID ) )
        {
            return ERROR_ID_CATEGORY;
        }

        int nIdCategory = Integer.parseInt( strCategoryId );

        if ( CategoryHome.find( nIdCategory, PluginService.getPlugin( Constants.PLUGIN_NAME ) ) == null )
        {
            return ERROR_ID_CATEGORY;
        }

        return nIdCategory;
    }
}
