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
package fr.paris.lutece.plugins.calendar.business.rss;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import fr.paris.lutece.portal.business.rss.IResourceRssType;
import fr.paris.lutece.portal.business.rss.ResourceRssType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

/**
 * Declares the calendar as a resource of the RSS feeds of the site, as the Spring context of the plugin did before
 * Lutece 8
 */
@ApplicationScoped
public class CalendarResourceRssTypeProducer
{
    /**
     * Produces the RSS resource type of the calendar
     *
     * @param strKey
     *            the key of the type
     * @param strTitleI18nKey
     *            the i18n key of its title
     * @param strClassName
     *            the class of the resource
     * @return the RSS resource type
     */
    @Produces
    @ApplicationScoped
    @Named( "calendar.resourceRssType" )
    public IResourceRssType produceResourceRssType( @ConfigProperty( name = "calendar.resourceRssType.key" ) String strKey,
            @ConfigProperty( name = "calendar.resourceRssType.titleI18nKey" ) String strTitleI18nKey,
            @ConfigProperty( name = "calendar.resourceRssType.className" ) String strClassName )
    {
        IResourceRssType resourceRssType = new ResourceRssType( );
        resourceRssType.setKey( strKey );
        resourceRssType.setTitleI18nKey( strTitleI18nKey );
        resourceRssType.setClassName( strClassName );

        return resourceRssType;
    }
}
