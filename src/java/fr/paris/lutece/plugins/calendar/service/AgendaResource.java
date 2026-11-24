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
package fr.paris.lutece.plugins.calendar.service;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.calendar.business.Agenda;
import fr.paris.lutece.portal.service.resource.Resource;
import fr.paris.lutece.portal.service.workgroup.AdminWorkgroupResource;
import fr.paris.lutece.portal.service.workgroup.AdminWorkgroupService;


/**
 *  This class describes an agenda resource
 */
public class AgendaResource implements Serializable, Resource, AdminWorkgroupResource
{
	private static final long serialVersionUID = 5430301763071821176L;
    /** The colour of a calendar that has none */
    public static final String DEFAULT_COLOR_CODE = "#868e96";
    private static final Pattern PATTERN_COLOR_CODE = Pattern.compile( "^#[0-9a-f]{6}$" );
    private static final Map<String, String> MAP_LEGACY_COLORS = Map.of( "blue", "#206bc4", "green", "#2fb344", "orange", "#f76707", "purple",
            "#ae3ec9", "red", "#d63939" );

    // Variables declarations
    private String _strId;
    private String _strName;
    private String _strEventImage;
    private String _strEventPrefix;
    private String _strLoaderClassName;
    private String _strLoaderParameter;
    private String _strRole;
    private String _strRoleManager;
    private Agenda _agenda;
    private String _strAdminWorkgroup;
    private String _strResourceType;
    private boolean _bNotify;
    private int _nPeriodValidity;
    private boolean _bFrontProposals;
    private String _strDefaultView;

    /**
     * Returns the Id
     *
     * @return The Id
     */
    public String getId(  )
    {
        return _strId;
    }

    /**
     * Sets the Id
     *
     * @param strId The Id
     */
    public void setId( String strId )
    {
        _strId = strId;
    }

    /**
     * Returns the Name
     *
     * @return The Name
     */
    public String getName(  )
    {
        return _strName;
    }

    /**
     * Sets the Name
     *
     * @param strName The Name
     */
    public void setName( String strName )
    {
        _strName = strName;
    }

    /**
     * Returns the Resource Type
     *
     * @return The Resource Type
     */
    public String getResourceType(  )
    {
        return _strResourceType;
    }

    /**
     * Sets the Resource Type
     *
     * @param strResourceType The Resource Type
     */
    public void setResourceType( String strResourceType )
    {
        _strResourceType = strResourceType;
    }

    /**
     * Returns the EventImage
     *
     * @return The EventImage
     */
    public String getEventImage(  )
    {
        return _strEventImage;
    }

    /**
     * Returns the colour of the calendar as a CSS hexadecimal colour (#rrggbb), set as the custom property --calendar-dot-color
     * of its dots and events. A colour name or a legacy image path stored before the colour picker gives the colour it
     * stood for; anything else gives the default grey
     *
     * @return The colour, #rrggbb in lower case
     */
    public String getEventColorCode( )
    {
        String strColor = getValidColorCode( _strEventImage );

        return ( strColor != null ) ? strColor : MAP_LEGACY_COLORS.getOrDefault( getEventColor( ), DEFAULT_COLOR_CODE );
    }

    /**
     * Returns the hexadecimal colour of a colour name of the former dots (blue, green, orange, purple, red) or of a hexadecimal colour
     *
     * @param strColor
     *            the colour name or the hexadecimal colour
     * @return the colour as #rrggbb in lower case, or null when it is neither
     */
    public static String getColorCode( String strColor )
    {
        String strCode = getValidColorCode( strColor );

        return ( strCode != null || strColor == null ) ? strCode : MAP_LEGACY_COLORS.get( strColor.trim( ).toLowerCase( Locale.ROOT ) );
    }

    /**
     * Checks a colour posted by the colour picker
     *
     * @param strColor
     *            the colour
     * @return the colour as #rrggbb in lower case, or null when it is not a hexadecimal colour
     */
    public static String getValidColorCode( String strColor )
    {
        if ( strColor == null )
        {
            return null;
        }

        String strCode = strColor.trim( ).toLowerCase( Locale.ROOT );

        return PATTERN_COLOR_CODE.matcher( strCode ).matches( ) ? strCode : null;
    }

    /**
     * Returns the colour of the calendar dot, used as the suffix of the CSS class calendar-dot-&lt;colour&gt;. The colour is
     * stored in the event image field; a legacy image path (images/.../dots/blue.png) gives its file name (blue)
     *
     * @return The colour, made of lower case letters, digits and dashes only
     */
    public String getEventColor( )
    {
        if ( _strEventImage == null )
        {
            return "";
        }

        String strColor = _strEventImage.substring( _strEventImage.lastIndexOf( '/' ) + 1 );
        int nDot = strColor.indexOf( '.' );

        if ( nDot >= 0 )
        {
            strColor = strColor.substring( 0, nDot );
        }

        return strColor.toLowerCase( java.util.Locale.ROOT ).replaceAll( "[^a-z0-9-]", "" );
    }

    /**
     * Sets the EventImage
     *
     * @param strEventImage The EventImage
     */
    public void setEventImage( String strEventImage )
    {
        _strEventImage = strEventImage;
    }

    /**
     * Returns the EventPrefix
     *
     * @return The EventPrefix
     */
    public String getEventPrefix(  )
    {
        return _strEventPrefix;
    }

    /**
     * Sets the EventPrefix
     *
     * @param strEventPrefix The EventPrefix
     */
    public void setEventPrefix( String strEventPrefix )
    {
        _strEventPrefix = strEventPrefix;
    }

    /**
     * Returns the LoaderClassName
     *
     * @return The LoaderClassName
     */
    public String getLoaderClassName(  )
    {
        return _strLoaderClassName;
    }

    /**
     * Sets the LoaderClassName
     *
     * @param strLoaderClassName The LoaderClassName
     */
    public void setLoaderClassName( String strLoaderClassName )
    {
        _strLoaderClassName = strLoaderClassName;
    }

    /**
     * Returns the LoaderParameter
     *
     * @return The LoaderParameter
     */
    public String getLoaderParameter(  )
    {
        return _strLoaderParameter;
    }

    /**
     * Sets the LoaderParameter
     *
     * @param strLoaderParameter The LoaderParameter
     */
    public void setLoaderParameter( String strLoaderParameter )
    {
        _strLoaderParameter = strLoaderParameter;
    }

    /**
     * Returns the Role
     *
     * @return The Role
     */
    public String getRole(  )
    {
        return _strRole;
    }

    /**
     * Sets the Role
     *
     * @param strRole The Role
     */
    public void setRole( String strRole )
    {
        _strRole = strRole;
    }

    /**
    * Returns the  Manager Role
    *
    * @return The ManagerRole
    */
    public String getRoleManager(  )
    {
        return _strRoleManager;
    }

    /**
     * Sets the Manager Role
     * @param strRoleManager The role of the manager of the agenda
     */
    public void setRoleManager( String strRoleManager )
    {
        _strRoleManager = strRoleManager;
    }

    /**
     * Returns the Agenda
     *
     * @return The Agenda
     */
    public Agenda getAgenda(  )
    {
        return _agenda;
    }

    /**
     * Sets the Agenda
     * @param agenda The Agenda
     */
    public void setAgenda( Agenda agenda )
    {
        _agenda = agenda;
    }

    /**
     * Returns the workgroup
     * @return The workgroup
     */
    public String getWorkgroup(  )
    {
        return _strAdminWorkgroup;
    }

    /**
     * Sets the workgroup
     * @param strAdminWorkgroup The string representation of the admin workgroup
     */
    public void setWorkgroup( String strAdminWorkgroup )
    {
        _strAdminWorkgroup = AdminWorkgroupService.normalizeWorkgroupKey( strAdminWorkgroup );
    }
    
    /**
     * Returns the _bNotify
     * @return The _bNotify
     */
    public boolean isNotify(  )
    {
        return _bNotify;
    }

    /**
     * Sets the _bNotify
     * @param notify the _bNotification to set
     */
    public void setNotify( Boolean notify )
    {
    	_bNotify = notify;
    }
    
    /**
     * Returns the period validity of mail notification
     * @return The period validity of mail notification
     */
    public int getPeriodValidity(  )
    {
        return _nPeriodValidity;
    }

    /**
     * Sets the the period validity of mail notification
     * @param nPeriodValidity The int period validity of mail notification
     */
    public void setPeriodValidity( int nPeriodValidity )
    {
    	_nPeriodValidity = nPeriodValidity;
    }

    /**
     * Tells whether the calendar accepts events proposed from the site by logged-in users: they get the status proposed and
     * stay hidden from the public views until an administrator confirms them
     * @return true when the calendar accepts proposals
     */
    public boolean isFrontProposals( )
    {
        return _bFrontProposals;
    }

    /**
     * Sets whether the calendar accepts events proposed from the site by logged-in users
     * @param bFrontProposals true to accept proposals
     */
    public void setFrontProposals( boolean bFrontProposals )
    {
        _bFrontProposals = bFrontProposals;
    }

    /**
     * Returns the view the calendar page opens on when it shows this calendar alone
     * @return month, week, day or list, or an empty string for the choice by the size of the screen
     */
    public String getDefaultView( )
    {
        return StringUtils.defaultString( _strDefaultView );
    }

    /**
     * Sets the view the calendar page opens on when it shows this calendar alone
     * @param strDefaultView month, week, day or list, or an empty string for the choice by the size of the screen
     */
    public void setDefaultView( String strDefaultView )
    {
        _strDefaultView = strDefaultView;
    }
}
