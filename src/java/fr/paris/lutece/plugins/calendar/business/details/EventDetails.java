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
package fr.paris.lutece.plugins.calendar.business.details;

import java.io.Serializable;

/**
 * The details of an event that are not in its core record: price, attendance mode, organizer, registration,
 * accessibility and coordinates of its place
 */
public class EventDetails implements Serializable
{
    /** Price type: free */
    public static final String PRICE_FREE = "free";
    /** Price type: paid */
    public static final String PRICE_PAID = "paid";
    /** Attendance mode: in person */
    public static final String ATTENDANCE_OFFLINE = "offline";
    /** Attendance mode: online */
    public static final String ATTENDANCE_ONLINE = "online";
    /** Attendance mode: in person and online */
    public static final String ATTENDANCE_MIXED = "mixed";

    private static final long serialVersionUID = 1L;

    private int _nIdEvent;
    private String _strPriceType;
    private String _strPrice;
    private String _strAttendanceMode;
    private String _strOnlineUrl;
    private String _strOrganizerName;
    private String _strOrganizerEmail;
    private String _strOrganizerPhone;
    private String _strOrganizerUrl;
    private String _strRegistrationUrl;
    private String _strAccessibility;
    private Double _latitude;
    private Double _longitude;

    /**
     * Returns the event
     *
     * @return the id of the event
     */
    public int getIdEvent( )
    {
        return _nIdEvent;
    }

    /**
     * Sets the event
     *
     * @param nIdEvent
     *            the id of the event
     */
    public void setIdEvent( int nIdEvent )
    {
        _nIdEvent = nIdEvent;
    }

    /**
     * Returns the price type: empty when not given, free or paid
     *
     * @return the price type: empty when not given, free or paid
     */
    public String getPriceType( )
    {
        return _strPriceType;
    }

    /**
     * Sets the price type: empty when not given, free or paid
     *
     * @param strPriceType
     *            the price type: empty when not given, free or paid
     */
    public void setPriceType( String strPriceType )
    {
        _strPriceType = strPriceType;
    }

    /**
     * Returns the price, as written by the webmaster
     *
     * @return the price, as written by the webmaster
     */
    public String getPrice( )
    {
        return _strPrice;
    }

    /**
     * Sets the price, as written by the webmaster
     *
     * @param strPrice
     *            the price, as written by the webmaster
     */
    public void setPrice( String strPrice )
    {
        _strPrice = strPrice;
    }

    /**
     * Returns the attendance mode: offline, online or mixed
     *
     * @return the attendance mode: offline, online or mixed
     */
    public String getAttendanceMode( )
    {
        return _strAttendanceMode;
    }

    /**
     * Sets the attendance mode: offline, online or mixed
     *
     * @param strAttendanceMode
     *            the attendance mode: offline, online or mixed
     */
    public void setAttendanceMode( String strAttendanceMode )
    {
        _strAttendanceMode = strAttendanceMode;
    }

    /**
     * Returns the URL of the online event
     *
     * @return the URL of the online event
     */
    public String getOnlineUrl( )
    {
        return _strOnlineUrl;
    }

    /**
     * Sets the URL of the online event
     *
     * @param strOnlineUrl
     *            the URL of the online event
     */
    public void setOnlineUrl( String strOnlineUrl )
    {
        _strOnlineUrl = strOnlineUrl;
    }

    /**
     * Returns the name of the organizer
     *
     * @return the name of the organizer
     */
    public String getOrganizerName( )
    {
        return _strOrganizerName;
    }

    /**
     * Sets the name of the organizer
     *
     * @param strOrganizerName
     *            the name of the organizer
     */
    public void setOrganizerName( String strOrganizerName )
    {
        _strOrganizerName = strOrganizerName;
    }

    /**
     * Returns the e-mail of the organizer
     *
     * @return the e-mail of the organizer
     */
    public String getOrganizerEmail( )
    {
        return _strOrganizerEmail;
    }

    /**
     * Sets the e-mail of the organizer
     *
     * @param strOrganizerEmail
     *            the e-mail of the organizer
     */
    public void setOrganizerEmail( String strOrganizerEmail )
    {
        _strOrganizerEmail = strOrganizerEmail;
    }

    /**
     * Returns the phone of the organizer
     *
     * @return the phone of the organizer
     */
    public String getOrganizerPhone( )
    {
        return _strOrganizerPhone;
    }

    /**
     * Sets the phone of the organizer
     *
     * @param strOrganizerPhone
     *            the phone of the organizer
     */
    public void setOrganizerPhone( String strOrganizerPhone )
    {
        _strOrganizerPhone = strOrganizerPhone;
    }

    /**
     * Returns the website of the organizer
     *
     * @return the website of the organizer
     */
    public String getOrganizerUrl( )
    {
        return _strOrganizerUrl;
    }

    /**
     * Sets the website of the organizer
     *
     * @param strOrganizerUrl
     *            the website of the organizer
     */
    public void setOrganizerUrl( String strOrganizerUrl )
    {
        _strOrganizerUrl = strOrganizerUrl;
    }

    /**
     * Returns the URL of the registration (a form of plugin-forms, or another site)
     *
     * @return the URL of the registration (a form of plugin-forms, or another site)
     */
    public String getRegistrationUrl( )
    {
        return _strRegistrationUrl;
    }

    /**
     * Sets the URL of the registration (a form of plugin-forms, or another site)
     *
     * @param strRegistrationUrl
     *            the URL of the registration (a form of plugin-forms, or another site)
     */
    public void setRegistrationUrl( String strRegistrationUrl )
    {
        _strRegistrationUrl = strRegistrationUrl;
    }

    /**
     * Returns the accessibility information of the place
     *
     * @return the accessibility information of the place
     */
    public String getAccessibility( )
    {
        return _strAccessibility;
    }

    /**
     * Sets the accessibility information of the place
     *
     * @param strAccessibility
     *            the accessibility information of the place
     */
    public void setAccessibility( String strAccessibility )
    {
        _strAccessibility = strAccessibility;
    }

    /**
     * Returns the latitude of the place, or null
     *
     * @return the latitude of the place, or null
     */
    public Double getLatitude( )
    {
        return _latitude;
    }

    /**
     * Sets the latitude of the place, or null
     *
     * @param dLatitude
     *            the latitude of the place, or null
     */
    public void setLatitude( Double dLatitude )
    {
        _latitude = dLatitude;
    }

    /**
     * Returns the longitude of the place, or null
     *
     * @return the longitude of the place, or null
     */
    public Double getLongitude( )
    {
        return _longitude;
    }

    /**
     * Sets the longitude of the place, or null
     *
     * @param dLongitude
     *            the longitude of the place, or null
     */
    public void setLongitude( Double dLongitude )
    {
        _longitude = dLongitude;
    }

    /**
     * Tells whether the place has coordinates
     *
     * @return true when the latitude and the longitude are both set
     */
    public boolean isLocated( )
    {
        return _latitude != null && _longitude != null;
    }
}
