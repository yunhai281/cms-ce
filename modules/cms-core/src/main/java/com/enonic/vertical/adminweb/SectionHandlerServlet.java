/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.adminweb;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.mail.MessagingException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

import com.enonic.esl.containers.ExtendedMap;
import com.enonic.esl.containers.MultiValueMap;
import com.enonic.esl.net.URL;
import com.enonic.esl.servlet.http.CookieUtil;
import com.enonic.esl.servlet.http.HttpServletRequestWrapper;
import com.enonic.esl.xml.XMLTool;
import com.enonic.vertical.adminweb.handlers.ListCountResolver;
import com.enonic.vertical.adminweb.wizard.Wizard;
import com.enonic.vertical.engine.SectionCriteria;
import com.enonic.vertical.engine.Types;
import com.enonic.vertical.engine.VerticalEngineException;
import com.enonic.vertical.engine.VerticalRemoveException;
import com.enonic.vertical.engine.VerticalSecurityException;
import com.enonic.vertical.engine.criteria.CategoryCriteria;

import com.enonic.cms.core.DeploymentPathResolver;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.portal.cache.PageCache;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.service.AdminService;
import com.enonic.cms.core.servlet.ServletRequestAccessor;
import com.enonic.cms.core.structure.SiteEntity;
import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.core.structure.menuitem.AddContentToSectionCommand;
import com.enonic.cms.core.structure.menuitem.ApproveContentInSectionCommand;
import com.enonic.cms.core.structure.menuitem.ApproveContentsInSectionCommand;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.core.structure.menuitem.MenuItemKey;
import com.enonic.cms.core.structure.menuitem.MenuItemServiceCommand;
import com.enonic.cms.core.structure.menuitem.OrderContentsInSectionCommand;
import com.enonic.cms.core.structure.menuitem.RemoveContentsFromSectionCommand;
import com.enonic.cms.core.structure.menuitem.UnapproveContentsInSectionCommand;
import com.enonic.cms.core.structure.page.template.PageTemplateType;


public class SectionHandlerServlet
    extends AdminHandlerBaseServlet
{
    private static final String WIZARD_CONFIG_PUBLISH = "wizardconfig_publish_to_section.xml";

    public static final int COOKIE_TIMEOUT = 60 * 60 * 24 * 365 * 50;

    public void handlerCustom( HttpServletRequest request, HttpServletResponse response, HttpSession session, AdminService admin,
                               ExtendedMap formItems, String operation )
        throws VerticalAdminException, VerticalEngineException, MessagingException, IOException
    {

        if ( "moveup".equals( operation ) )
        {
            moveContentUp( request, response, session, formItems );
        }
        else if ( "movedown".equals( operation ) )
        {
            moveContentDown( request, response, session, formItems );
        }
        else if ( "approve".equals( operation ) )
        {
            approveContent( request, response, session, admin, formItems );
        }
        else if ( "unapprove".equals( operation ) )
        {
            unapproveContent( request, response, session, admin, formItems );
        }
        else if ( "add".equals( operation ) )
        {
            addContent( request, response, admin, formItems );
        }
        else if ( "save".equals( operation ) )
        {
            saveContents( request, response, session, formItems );
        }
        else if ( "removecontent".equals( operation ) )
        {
            removeContent( request, response, session, formItems );
        }
        else if ( "batchremove".equals( operation ) )
        {
            batchRemove( request, response, session, formItems );
        }
        else if ( "batchactivate".equals( operation ) )
        {
            batchActivate( request, response, session, formItems, true );
        }
        else if ( "batchdeactivate".equals( operation ) )
        {
            batchActivate( request, response, session, formItems, false );
        }
        else
        {
            super.handlerCustom( request, response, session, admin, formItems, operation );
        }
    }


    public void batchRemove( HttpServletRequest request, HttpServletResponse response, HttpSession session, ExtendedMap formItems )
        throws VerticalAdminException, VerticalEngineException
    {

        User user = securityService.getLoggedInAdminConsoleUser();
        MenuItemKey sectionKey = new MenuItemKey( formItems.getString( "menuitemkey" ) );
        MenuItemEntity section = menuItemDao.findByKey( sectionKey );
        assert section.isSection();

        String[] contentKeyStrings = formItems.getStringArray( "batch_operation" );
        Set<ContentKey> contentKeys = new HashSet<ContentKey>();
        if ( contentKeyStrings != null && contentKeyStrings.length > 0 )
        {
            for ( String contentKeyString : contentKeyStrings )
            {
                contentKeys.add( new ContentKey( contentKeyString ) );
            }
        }

        boolean topLevel = "true".equals( formItems.getString( "toplevel", "" ) );
        boolean reordered = "true".equals( formItems.getString( "reordered", "" ) );
        boolean ordered = false;

        RemoveContentsFromSectionCommand command = new RemoveContentsFromSectionCommand();
        command.setRemover( user.getKey() );
        command.setSection( sectionKey );
        for ( ContentKey contentKey : contentKeys )
        {
            command.addContentToRemove( contentKey );
        }

        if ( topLevel )
        {
            menuItemService.execute( command );
        }
        else
        {
            Document doc = XMLTool.domparse( (String) session.getAttribute( "sectionxml" ) );
            Element sectionElem = XMLTool.getElement( doc.getDocumentElement(), "section" );
            ordered = Boolean.valueOf( sectionElem.getAttribute( "ordered" ) );

            // Set @removed=false
            Element[] contentTitleElems = XMLTool.getElements( doc.getDocumentElement(), "contenttitle" );
            for ( Element contentTitleElem : contentTitleElems )
            {
                ContentKey key = new ContentKey( Integer.parseInt( contentTitleElem.getAttribute( "key" ) ) );
                if ( contentKeys.contains( key ) )
                {
                    contentTitleElem.setAttribute( "removed", "true" );
                }
            }

            if ( !( ordered && reordered ) )
            {
                menuItemService.execute( command );
            }
            session.setAttribute( "sectionxml", XMLTool.documentToString( doc ) );
        }
        int siteKey = formItems.getInt( "menukey", -1 );
        if ( siteKey == -1 )
        {
            siteKey = formItems.getInt( "sitekey", -1 );
            if ( siteKey == -1 )
            {
                siteKey = section.getSite().getKey().toInt();
            }
        }

        invalidatePageCache( sectionKey );

        MultiValueMap queryParams = new MultiValueMap();
        queryParams.put( "page", formItems.get( "page" ) );
        queryParams.put( "op", "browse" );
        queryParams.put( "menukey", siteKey );
        queryParams.put( "sitekey", siteKey );
        queryParams.put( "menuitemkey", sectionKey.toInt() );
        if ( !topLevel )
        {
            queryParams.put( "sec", sectionKey );
        }
        if ( ordered && reordered )
        {
            queryParams.put( "keepxml", "yes" );
            queryParams.put( "reordered", "true" );
        }
        redirectClientToAdminPath( "adminpage", queryParams, request, response );
    }

    public void batchActivate( HttpServletRequest request, HttpServletResponse response, HttpSession session, ExtendedMap formItems,
                               boolean approved )
        throws VerticalAdminException, VerticalEngineException
    {

        final MenuItemKey menuItemKey = new MenuItemKey( formItems.getString( "menuitemkey" ) );
        final String[] contentKeyStrings = formItems.getStringArray( "batch_operation" );
        final Set<ContentKey> contentKeySet = new LinkedHashSet<ContentKey>();
        for ( String contentKeyString : contentKeyStrings )
        {
            contentKeySet.add( new ContentKey( contentKeyString ) );
        }

        User user = securityService.getLoggedInAdminConsoleUser();
        Document doc = XMLTool.domparse( (String) session.getAttribute( "sectionxml" ) );
        Element sectionElem = XMLTool.getElement( doc.getDocumentElement(), "section" );
        boolean ordered = Boolean.valueOf( sectionElem.getAttribute( "ordered" ) );

        // Set approved attribute
        Element[] contentTitleElems = XMLTool.getElements( doc.getDocumentElement(), "contenttitle" );
        for ( int i = 0; i < contentTitleElems.length; i++ )
        {
            ContentKey key = new ContentKey( contentTitleElems[i].getAttribute( "key" ) );
            if ( contentKeySet.contains( key ) )
            {
                contentTitleElems[i].setAttribute( "approved", String.valueOf( approved ) );

                if ( ordered )
                {
                    if ( approved )
                    {
                        // Move the element to the top in the xml
                        Element parent = (Element) contentTitleElems[i].getParentNode();
                        contentTitleElems[i] = (Element) parent.removeChild( contentTitleElems[i] );
                        doc.importNode( contentTitleElems[i], true );
                        parent.insertBefore( contentTitleElems[i], parent.getFirstChild() );
                    }
                    else
                    {
                        NodeList unapprovedContents =
                            XMLTool.selectNodes( doc, "/contenttitles/contenttitle[@approved = 'false' and not(@removed = 'true')]" );

                        Element parent = doc.getDocumentElement();
                        String title = XMLTool.getElementText( contentTitleElems[i] );

                        Element next = null;
                        for ( int j = 0; j < unapprovedContents.getLength(); j++ )
                        {
                            Element current = (Element) unapprovedContents.item( j );
                            if ( XMLTool.getElementText( current ).compareTo( title ) > 0 )
                            {
                                next = current;
                                break;
                            }
                        }

                        if ( next != null )
                        {
                            parent.insertBefore( contentTitleElems[i], next );
                        }
                        else
                        {
                            parent.insertBefore( contentTitleElems[i],
                                                 unapprovedContents.item( unapprovedContents.getLength() - 1 ).getNextSibling() );
                        }
                    }
                }
            }
        }

        if ( !ordered )
        {
            if ( approved )
            {
                final ApproveContentsInSectionCommand command = new ApproveContentsInSectionCommand();
                command.setSection( menuItemKey );
                command.setApprover( user.getKey() );
                for ( ContentKey contentToApprove : contentKeySet )
                {
                    command.addContentToApprove( contentToApprove );
                }
                menuItemService.execute( command );
            }
            else
            {
                final UnapproveContentsInSectionCommand command = new UnapproveContentsInSectionCommand();
                command.setSection( menuItemKey );
                command.setUnapprover( user.getKey() );
                for ( ContentKey contentToUnapprove : contentKeySet )
                {
                    command.addContentToUnapprove( contentToUnapprove );
                }
                menuItemService.execute( command );
            }
        }

        invalidatePageCache( menuItemKey );

        session.setAttribute( "sectionxml", XMLTool.documentToString( doc ) );

        MultiValueMap queryParams = new MultiValueMap();
        queryParams.put( "page", formItems.get( "page" ) );
        queryParams.put( "op", "browse" );
        queryParams.put( "sec", formItems.get( "sec" ) );
        queryParams.put( "menukey", formItems.get( "menukey" ) );
        queryParams.put( "menuitemkey", menuItemKey.toInt() );
        if ( ordered )
        {
            queryParams.put( "keepxml", "yes" );
            queryParams.put( "reordered", "true" );
        }
        redirectClientToAdminPath( "adminpage", queryParams, request, response );
    }

    public void addContent( HttpServletRequest request, HttpServletResponse response, AdminService admin, ExtendedMap formItems )
        throws VerticalAdminException, VerticalEngineException
    {

        User user = securityService.getLoggedInAdminConsoleUser();
        MenuItemKey sectionKey = new MenuItemKey( formItems.getInt( "sec", -1 ) );
        String[] contentKeys = formItems.getStringArray( "key" );

        for ( String contentKeyStr : contentKeys )
        {
            ContentKey contentKey = new ContentKey( contentKeyStr );
            Document doc = XMLTool.createDocument( "sections" );
            Element sectionsElem = doc.getDocumentElement();
            Element sectionElem = XMLTool.createElement( doc, doc.getDocumentElement(), "section" );
            sectionElem.setAttribute( "key", sectionKey.toString() );
            sectionsElem.setAttribute( "contentkey", contentKey.toString() );

            // add content to section as unapproved
            AddContentToSectionCommand command = new AddContentToSectionCommand();
            command.setAddOnTop( false );
            command.setContributor( user.getKey() );
            command.setSection( sectionKey );
            command.setContent( contentKey );
            command.setApproveInSection( false );
            menuItemService.execute( command );
        }

        MultiValueMap parameters = new MultiValueMap();
        parameters.put( "page", formItems.get( "page" ) );
        parameters.put( "op", "browse" );
        parameters.put( "sec", sectionKey.toString() );
        parameters.put( "menukey", formItems.get( "menukey" ) );
        parameters.put( "menuitemkey", admin.getMenuItemKeyBySection( sectionKey ).toString() );

        redirectClientToAdminPath( "adminpage", parameters, request, response );
    }

    public void handlerWizard( HttpServletRequest request, HttpServletResponse response, HttpSession session, AdminService admin,
                               ExtendedMap formItems, ExtendedMap parameters, User user, String wizardName )
        throws VerticalAdminException, VerticalEngineException, TransformerException, IOException, MessagingException
    {
        if ( "publish".equals( wizardName ) )
        {
            Wizard publishWizard = Wizard.getInstance( admin, applicationContext, this, session, formItems, WIZARD_CONFIG_PUBLISH );
            publishWizard.processRequest( request, response, session, admin, formItems, parameters, user );
        }
        else
        {
            super.handlerWizard( request, response, session, admin, formItems, parameters, user, wizardName );
        }
    }

    public void handlerBrowse( HttpServletRequest request, HttpServletResponse response, HttpSession session, AdminService admin,
                               ExtendedMap formItems )
        throws VerticalAdminException
    {

        Document doc;
        User user = securityService.getLoggedInAdminConsoleUser();
        // int sectionKey = formItems.getInt("sec", -1);
        int menuKey = formItems.getInt( "menukey" );
        MenuItemKey menuItemKey = new MenuItemKey( formItems.getString( "menuitemkey" ) );
        MenuItemKey sectionKey = admin.getSectionKeyByMenuItemKey( menuItemKey );

        String keepXML = request.getParameter( "keepxml" );

        if ( formItems.containsKey( "browsemode" ) )
        {
            String deploymentPath = DeploymentPathResolver.getAdminDeploymentPath( request );
            CookieUtil.setCookie( response, "browsemode" + menuItemKey, formItems.getString( "browsemode" ), -1, deploymentPath );
        }
        else
        {
            Cookie c = CookieUtil.getCookie( request, "browsemode" + menuItemKey );
            if ( c != null && "menuitem".equals( c.getValue() ) )
            {
                // int sectionKey = admin.getSectionKeyByMenuItemKey(menuItemKey);
                // redirectToSectionBrowse(request, response, siteKey, menuKey, sectionKey,
                // formItems.getBoolean("reload", false));
//                ( "SwitchMode section" );
            }
        }

        final String cookieName = "sectionBrowseItemsPerPage";
        int index = formItems.getInt( "index", 0 );
        int count = ListCountResolver.resolveCount( request, formItems, cookieName );
        CookieUtil.setCookie( response, cookieName, Integer.toString( count ), COOKIE_TIMEOUT,
                              DeploymentPathResolver.getAdminDeploymentPath( request ) );

        if ( keepXML != null && keepXML.equals( "yes" ) )
        {
            doc = XMLTool.domparse( (String) session.getAttribute( "sectionxml" ) );
        }
        else
        {
            doc = getSectionDocument( admin, user, index, count, sectionKey, menuKey, menuItemKey );
            Document supportedPageTemplatesOfSite = getSupportedPageTemplatesOfSite( menuKey, PageTemplateType.CONTENT );
            XMLTool.mergeDocuments( doc, supportedPageTemplatesOfSite, true );
            session.setAttribute( "sectionxml", XMLTool.documentToString( doc ) );
        }

        try
        {
            DOMSource xmlSource = new DOMSource( doc );

            Source xslSource = AdminStore.getStylesheet( session, "section_browse.xsl" );

            // Parameters
            HashMap<String, Object> parameters = formItems;
            parameters.put( "index", index );
            parameters.put( "count", count );
            parameters.put( "menuitemkey", menuItemKey.toString() );
            parameters.put( "sec", sectionKey.toString() );
            parameters.put( "debugpath", MenuHandlerServlet.getSiteUrl( request, menuKey ) );
            addCommonParameters( admin, user, request, parameters, -1, menuKey );
            transformXML( session, response.getWriter(), xmlSource, xslSource, parameters );
        }

        catch ( IOException ioe )
        {
            String message = "Failed to get response writer: %t";
            VerticalAdminLogger.errorAdmin( message, ioe );
        }
        catch ( TransformerException te )
        {
            String message = "Failed to transmform XML document: %t";
            VerticalAdminLogger.errorAdmin( message, te );
        }
    }

    private void moveContentUp( HttpServletRequest request, HttpServletResponse response, HttpSession session, ExtendedMap formItems )
        throws VerticalAdminException
    {

        Document doc = XMLTool.domparse( (String) session.getAttribute( "sectionxml" ) );
        Element parent = doc.getDocumentElement();
        String key = formItems.getString( "key" );

        NodeList approvedContents =
            XMLTool.selectNodes( doc, "/contenttitles/contenttitle[@approved = 'true' and not(@removed = 'true')]" );

        // elem is the node we are going to move
        Element elem = null;
        // next is the element that 'elem' should be inserted before
        Element next = null;
        for ( int i = approvedContents.getLength() - 1; i >= 0; i-- )
        {
            Element current = (Element) approvedContents.item( i );

            // If we have not found the node we are going to move
            if ( elem == null )
            {
                if ( key.equals( current.getAttribute( "key" ) ) )
                {
                    elem = current;
                }
            }
            // If we have found the node, it should be inserted before the current
            else
            {
                next = current;
                break;
            }
        }
        if ( next == null )
        {
            // The element should be inserted last (wrapping)
            parent.insertBefore( elem, approvedContents.item( approvedContents.getLength() - 1 ).getNextSibling() );
        }
        else
        {
            parent.insertBefore( elem, next );
        }

        session.setAttribute( "sectionxml", XMLTool.documentToString( doc ) );

        MultiValueMap queryParams = new MultiValueMap();
        queryParams.put( "page", formItems.get( "page" ) );
        queryParams.put( "op", "browse" );
        queryParams.put( "sec", formItems.get( "sec" ) );
        queryParams.put( "keepxml", "yes" );
        queryParams.put( "reordered", "true" );
        queryParams.put( "menukey", formItems.get( "menukey" ) );
        queryParams.put( "menuitemkey", formItems.getString( "menuitemkey" ) );
        redirectClientToAdminPath( "adminpage", queryParams, request, response );
    }

    private void moveContentDown( HttpServletRequest request, HttpServletResponse response, HttpSession session, ExtendedMap formItems )
        throws VerticalAdminException
    {

        Document doc = XMLTool.domparse( (String) session.getAttribute( "sectionxml" ) );
        Element parent = doc.getDocumentElement();
        String key = formItems.getString( "key" );

        NodeList approvedContents =
            XMLTool.selectNodes( doc, "/contenttitles/contenttitle[@approved = 'true' and not(@removed = 'true')]" );

        boolean insertFirst = false;
        // elem is the node we are going to move
        Element elem = null;
        // next is the element that 'elem' should be inserted before
        Element next = null;
        for ( int i = 0; i < approvedContents.getLength(); i++ )
        {
            Element current = (Element) approvedContents.item( i );

            // If we have not found the node we are going to move
            if ( elem == null )
            {
                if ( key.equals( current.getAttribute( "key" ) ) )
                {
                    elem = current;
                    if ( i == approvedContents.getLength() - 1 )
                    {
                        // If this is the last element in the list, it should be inserted first
                        insertFirst = true;
                    }
                }
            }
            // If we have found the node, take the next element
            else
            {
                next = (Element) current.getNextSibling();
                break;
            }
        }
        if ( insertFirst )
        {
            // wrapping
            parent.insertBefore( elem, approvedContents.item( 0 ) );
        }
        else
        {
            // Next can be null, but then elem is inserted last (which is correct)
            parent.insertBefore( elem, next );
        }

        session.setAttribute( "sectionxml", XMLTool.documentToString( doc ) );

        MultiValueMap queryParams = new MultiValueMap();
        queryParams.put( "page", formItems.get( "page" ) );
        queryParams.put( "op", "browse" );
        queryParams.put( "sec", formItems.get( "sec" ) );
        queryParams.put( "keepxml", "yes" );
        queryParams.put( "reordered", "true" );
        queryParams.put( "menukey", formItems.get( "menukey" ) );
        queryParams.put( "menuitemkey", formItems.getString( "menuitemkey" ) );
        redirectClientToAdminPath( "adminpage", queryParams, request, response );
    }

    public void approveContent( HttpServletRequest request, HttpServletResponse response, HttpSession session, AdminService admin,
                                ExtendedMap formItems )
    {

        User user = securityService.getLoggedInAdminConsoleUser();
        MenuItemKey sectionKey = new MenuItemKey( formItems.getString( "sec" ) );
        ContentKey contentKey = new ContentKey( formItems.getInt( "key" ) );
        int menuKey = formItems.getInt( "menukey", -1 );
        if ( menuKey == -1 )
        {
            menuKey = admin.getMenuKeyBySection( sectionKey );
        }

        boolean topLevel = "true".equals( formItems.getString( "toplevel", "" ) );
        Document doc;
        MenuItemKey menuItemKey = admin.getMenuItemKeyBySection( sectionKey );

        if ( topLevel )
        {
            doc = getSectionDocument( admin, user, 0, 20, sectionKey, menuKey, menuItemKey );
        }
        else
        {
            doc = XMLTool.domparse( (String) session.getAttribute( "sectionxml" ) );
        }

        Element sectionElem = XMLTool.getElement( doc.getDocumentElement(), "section" );
        boolean ordered = Boolean.valueOf( sectionElem.getAttribute( "ordered" ) );

        // Set approved attribute = true
        String xpath = "/contenttitles/contenttitle[@key = '" + contentKey + "']";
        Element elem = (Element) XMLTool.selectNode( doc, xpath );
        elem.setAttribute( "approved", "true" );
        elem.setAttribute( "modified", "true" );

        if ( ordered )
        {
            // Move the element to the top in the xml
            Element parent = (Element) elem.getParentNode();
            elem = (Element) parent.removeChild( elem );
            doc.importNode( elem, true );
            parent.insertBefore( elem, parent.getFirstChild() );
        }
        else
        {
            ApproveContentInSectionCommand command = new ApproveContentInSectionCommand();
            command.setApprover( user.getKey() );
            command.setSection( menuItemKey );
            command.setContentToApprove( contentKey );
            menuItemService.execute( command );
        }
        session.setAttribute( "sectionxml", XMLTool.documentToString( doc ) );

        invalidatePageCache( menuItemKey );

        String useRedirect = formItems.getString( "useredirect", null );
        if ( "referer".equals( useRedirect ) && !ordered )
        {
            redirectClientToReferer( request, response );
        }
        else
        {
            MultiValueMap queryParams = new MultiValueMap();
            queryParams.put( "page", formItems.get( "page" ) );
            queryParams.put( "op", "browse" );
            queryParams.put( "menukey", formItems.get( "menukey", null ) );
            queryParams.put( "menuitemkey", menuItemKey.toString() );
            if ( !topLevel || ordered )
            {
                queryParams.put( "sec", sectionKey );
            }
            if ( ordered )
            {
                queryParams.put( "keepxml", "yes" );
                queryParams.put( "reordered", "true" );
            }
            redirectClientToAdminPath( "adminpage", queryParams, request, response );
        }
    }

    public void unapproveContent( HttpServletRequest request, HttpServletResponse response, HttpSession session, AdminService admin,
                                  ExtendedMap formItems )
    {

        User user = securityService.getLoggedInAdminConsoleUser();
        Document doc = XMLTool.domparse( (String) session.getAttribute( "sectionxml" ) );
        Element sectionElem = XMLTool.getElement( doc.getDocumentElement(), "section" );
        boolean ordered = Boolean.valueOf( sectionElem.getAttribute( "ordered" ) );
        MenuItemKey sectionKey = new MenuItemKey( formItems.getString( "sec" ) );
        ContentKey contentKey = new ContentKey( Integer.parseInt( formItems.getString( "key" ) ) );
        MenuItemKey menuItemKey = admin.getMenuItemKeyBySection( sectionKey );

        // Set approved attribute = false
        String xpath = "/contenttitles/contenttitle[@key = '" + contentKey + "']";
        Element elem = (Element) XMLTool.selectNode( doc, xpath );
        elem.setAttribute( "approved", "false" );
        elem.setAttribute( "modified", "true" );

        if ( ordered )
        {
            NodeList unapprovedContents =
                XMLTool.selectNodes( doc, "/contenttitles/contenttitle[@approved = 'false' and not(@removed = 'true')]" );

            Element parent = doc.getDocumentElement();
            String title = XMLTool.getElementText( elem );

            Element next = null;
            for ( int i = 0; i < unapprovedContents.getLength(); i++ )
            {
                Element current = (Element) unapprovedContents.item( i );
                if ( XMLTool.getElementText( current ).compareTo( title ) > 0 )
                {
                    next = current;
                    break;
                }
            }

            if ( next != null )
            {
                parent.insertBefore( elem, next );
            }
            else
            {
                parent.insertBefore( elem, unapprovedContents.item( unapprovedContents.getLength() - 1 ).getNextSibling() );
            }
        }
        else
        {
            UnapproveContentsInSectionCommand command = new UnapproveContentsInSectionCommand();
            command.setUnapprover( user.getKey() );
            command.setSection( sectionKey );
            command.addContentToUnapprove( contentKey );
            menuItemService.execute( command );
        }

        session.setAttribute( "sectionxml", XMLTool.documentToString( doc ) );

        invalidatePageCache( menuItemKey );

        String useRedirect = formItems.getString( "useredirect", null );
        if ( "referer".equals( useRedirect ) && !ordered )
        {
            redirectClientToReferer( request, response );
        }
        else
        {
            MultiValueMap queryParams = new MultiValueMap();
            queryParams.put( "page", formItems.get( "page" ) );
            queryParams.put( "op", "browse" );
            queryParams.put( "sec", formItems.get( "sec" ) );
            queryParams.put( "menukey", formItems.get( "menukey" ) );
            queryParams.put( "menuitemkey", menuItemKey.toString() );
            if ( ordered )
            {
                queryParams.put( "keepxml", "yes" );
                queryParams.put( "reordered", "true" );
            }
            redirectClientToAdminPath( "adminpage", queryParams, request, response );
        }
    }

    public void saveContents( HttpServletRequest request, HttpServletResponse response, HttpSession session, ExtendedMap formItems )
    {
        User user = securityService.getLoggedInAdminConsoleUser();
        Document doc = XMLTool.domparse( (String) session.getAttribute( "sectionxml" ) );
        MenuItemKey menuItemKey = new MenuItemKey( Integer.parseInt( formItems.getString( "sec" ) ) );
        MenuItemEntity section = menuItemDao.findByKey( menuItemKey );
        assert ( section.isSection() );
        long pageLoadedTimestamp = Long.parseLong( formItems.getString( "timestamp" ) );

        final RemoveContentsFromSectionCommand removeContentsCommand = new RemoveContentsFromSectionCommand();
        removeContentsCommand.setRemover( user.getKey() );
        removeContentsCommand.setSection( section.getKey() );

        final UnapproveContentsInSectionCommand unapproveContentsCommand = new UnapproveContentsInSectionCommand();
        unapproveContentsCommand.setUnapprover( user.getKey() );
        unapproveContentsCommand.setSection( section.getKey() );

        final ApproveContentsInSectionCommand approveContentsCommand = new ApproveContentsInSectionCommand();
        approveContentsCommand.setApprover( user.getKey() );
        approveContentsCommand.setSection( section.getKey() );

        final OrderContentsInSectionCommand orderContentsInSectionCommand =
            approveContentsCommand.createAndReturnOrderContentsInSectionCommand();

        Element[] contents = XMLTool.getElements( doc.getDocumentElement(), "contenttitle" );
        for ( Element content : contents )
        {
            ContentKey contentKey = new ContentKey( content.getAttribute( "key" ) );
            if ( "true".equals( content.getAttribute( "removed" ) ) )
            {
                removeContentsCommand.addContentToRemove( contentKey );
            }
            else if ( "true".equals( content.getAttribute( "approved" ) ) )
            {
                approveContentsCommand.addContentToApprove( contentKey );
                orderContentsInSectionCommand.addContent( contentKey );
            }
            else
            {
                unapproveContentsCommand.addContentToUnapprove( contentKey );
            }
        }

        if ( section.getLastUpdatedSectionContentTimestamp().after( new Date( pageLoadedTimestamp ) ) )
        {
            throw new IllegalStateException( "Content in this section has been changed. Please reload and try again." );
        }

        final List<MenuItemServiceCommand> menuItemServiceCommands = Lists.newArrayList();
        if ( removeContentsCommand.hasContentToRemove() )
        {
            menuItemServiceCommands.add( removeContentsCommand );
        }
        if ( unapproveContentsCommand.hasContentToUnapprove() )
        {
            menuItemServiceCommands.add( unapproveContentsCommand );
        }
        if ( approveContentsCommand.hasContentToApprove() )
        {
            menuItemServiceCommands.add( approveContentsCommand );
        }

        menuItemService.execute( menuItemServiceCommands.toArray( new MenuItemServiceCommand[menuItemServiceCommands.size()] ) );

        invalidatePageCache( menuItemKey );

        MultiValueMap queryParams = new MultiValueMap();
        queryParams.put( "page", formItems.get( "page" ) );
        queryParams.put( "op", "browse" );
        queryParams.put( "sec", formItems.get( "sec" ) );
        queryParams.put( "menukey", formItems.get( "menukey" ) );
        queryParams.put( "menuitemkey", menuItemKey.toString() );

        redirectClientToAdminPath( "adminpage", queryParams, request, response );
    }

    public void removeContent( HttpServletRequest request, HttpServletResponse response, HttpSession session, ExtendedMap formItems )
        throws VerticalAdminException, VerticalRemoveException, VerticalSecurityException
    {

        User user = securityService.getLoggedInAdminConsoleUser();
        MenuItemKey sectionKey = new MenuItemKey( Integer.parseInt( formItems.getString( "sec" ) ) );
        MenuItemKey menuItemKey = new MenuItemKey( formItems.getString( "menuitemkey" ) );
        ContentKey contentKey = new ContentKey( Integer.parseInt( formItems.getString( "key" ) ) );
        boolean topLevel = "true".equals( formItems.getString( "toplevel", "" ) );
        boolean reordered = "true".equals( formItems.getString( "reordered", "" ) );
        boolean ordered = false;

        RemoveContentsFromSectionCommand command = new RemoveContentsFromSectionCommand();
        command.setRemover( user.getKey() );
        command.setSection( sectionKey );
        command.addContentToRemove( contentKey );

        if ( topLevel )
        {
            menuItemService.execute( command );
        }
        else
        {
            Document doc = XMLTool.domparse( (String) session.getAttribute( "sectionxml" ) );
            Element sectionElem = XMLTool.getElement( doc.getDocumentElement(), "section" );
            ordered = Boolean.valueOf( sectionElem.getAttribute( "ordered" ) );

            // Set approved attribute = false
            String xpath = "/contenttitles/contenttitle[@key = '" + contentKey + "']";
            Element elem = (Element) XMLTool.selectNode( doc, xpath );
            elem.setAttribute( "removed", "true" );

            if ( !( ordered && reordered ) )
            {
                menuItemService.execute( command );
            }
            session.setAttribute( "sectionxml", XMLTool.documentToString( doc ) );
        }
        int siteKey = formItems.getInt( "menukey", -1 );
        if ( siteKey == -1 )
        {
            siteKey = menuItemDao.findByKey( menuItemKey ).getSite().getKey().toInt();
        }

        invalidatePageCache( menuItemKey );

        String useRedirect = formItems.getString( "useredirect", null );
        if ( "referer".equals( useRedirect ) && !( ordered && reordered ) )
        {
            redirectClientToReferer( request, response );
        }
        else
        {
            MultiValueMap queryParams = new MultiValueMap();
            queryParams.put( "page", formItems.get( "page" ) );
            queryParams.put( "op", "browse" );
            queryParams.put( "menukey", siteKey );
            queryParams.put( "menuitemkey", menuItemKey.toString() );
            if ( !topLevel )
            {
                queryParams.put( "sec", sectionKey );
            }
            if ( ordered && reordered )
            {
                queryParams.put( "keepxml", "yes" );
                queryParams.put( "reordered", "true" );
            }
            redirectClientToAdminPath( "adminpage", queryParams, request, response );
        }
    }

    private Document getSectionDocument( AdminService admin, User user, int index, int count, MenuItemKey sectionKey, int menuKey,
                                         MenuItemKey menuItemKey )
        throws VerticalAdminException
    {

        Document doc;
        SectionCriteria section = new SectionCriteria();
        section.setSectionKey( sectionKey.toInt() );
        section.setAppendAccessRights( true );
        section.setIncludeChildCount( true );
        section.setIncludeAll( true );

        Document sectionDoc = admin.getSections( user, section ).getAsDOMDocument();
        Element sectionElem = XMLTool.getElement( sectionDoc.getDocumentElement(), "section" );
        long timestamp = admin.getSectionContentTimestamp( sectionKey );
        sectionElem.setAttribute( "timestamp", Long.toString( timestamp ) );
        boolean ordered = Boolean.valueOf( sectionElem.getAttribute( "ordered" ) );

        if ( ordered )
        {
            doc = admin.getContentTitlesBySection( sectionKey, null, 0, Integer.MAX_VALUE, false, false ).getAsDOMDocument();
        }
        else
        {
            doc = admin.getContentTitlesBySection( sectionKey, null, index, count, true, false ).getAsDOMDocument();
        }

        // Get contenttypes and categories
        Element[] contentTitleElems = XMLTool.getElements( doc.getDocumentElement() );
        final Set<Integer> contentTypeKeys = Sets.newHashSet();

        Set<Integer> categoryKeys = new LinkedHashSet<Integer>();
        for ( Element contentTitleElem : contentTitleElems )
        {
            contentTypeKeys.add( Integer.parseInt( contentTitleElem.getAttribute( "contenttypekey" ) ) );
            categoryKeys.add( Integer.parseInt( contentTitleElem.getAttribute( "categorykey" ) ) );
        }
        Document contentTypeDoc = admin.getData( user, Types.CONTENTTYPE, Ints.toArray( contentTypeKeys ) ).getAsDOMDocument();
        XMLTool.mergeDocuments( doc, contentTypeDoc, true );
        if ( categoryKeys.size() > 0 )
        {
            CategoryCriteria categoryCriteria = new CategoryCriteria();
            categoryCriteria.addCategoryKeys( Lists.newArrayList( categoryKeys ) );
            Document categoriesDoc = admin.getMenu( user, categoryCriteria ).getAsDOMDocument();
            XMLTool.mergeDocuments( doc, categoriesDoc, false );
        }

        // Import the sections to the content doc
        doc.getDocumentElement().appendChild( doc.importNode( sectionElem, true ) );

        final UserEntity userEntity = securityService.getUser( user.getKey() );
        final Document newMenusDoc = buildModelForBrowse( userEntity, menuKey, menuItemKey );

        XMLTool.mergeDocuments( doc, newMenusDoc, true );

        return doc;
    }

    private Document buildModelForBrowse( final UserEntity user, final int menuKey, MenuItemKey menuItemKey )
    {
        final SiteKey siteKey = new SiteKey( menuKey );

        final MenuBrowseModelFactory menuBrowseModelFactory =
            new MenuBrowseModelFactory( securityService, siteDao, menuItemDao, sitePropertiesService );
        final MenuBrowseContentModel model = menuBrowseModelFactory.createContentModel( user, siteKey, menuItemKey );
        return model.toXML().getAsDOMDocument();
    }

    public void handlerRemove( HttpServletRequest request, HttpServletResponse response, HttpSession session, AdminService admin,
                               ExtendedMap formItems, int key )
        throws VerticalRemoveException, VerticalSecurityException, VerticalAdminException
    {

        User user = securityService.getLoggedInAdminConsoleUser();

        // First, get supersection key
        SectionCriteria section = new SectionCriteria();
        section.setSectionKey( key );
        Document sectionDoc = admin.getSections( user, section ).getAsDOMDocument();
        Element sectionElem = (Element) sectionDoc.getDocumentElement().getFirstChild();
        String superSectionKey = sectionElem.getAttribute( "supersectionkey" );

        // Remove all sections recursive. This will silently skip sections
        // that the user is not allowed to remove.
        admin.removeSection( key, true );

        URL redirectURL = new URL( request.getHeader( "referer" ) );
        if ( superSectionKey != null && superSectionKey.length() > 0 )
        {
            redirectURL.setParameter( "sec", superSectionKey );
        }
        else
        {
            redirectURL.removeParameter( "sec" );
        }
        redirectURL.setParameter( "reload", "true" );

        redirectClientToURL( redirectURL, response );
    }

    /**
     * Copy section to another section.
     */
    public void handlerCopy( HttpServletRequest request, HttpServletResponse response, HttpSession session, AdminService admin,
                             ExtendedMap formItems, User user, int key )
        throws VerticalSecurityException, VerticalAdminException
    {
        // First, get supersection key
        SectionCriteria section = new SectionCriteria();
        section.setSectionKey( key );
        Document sectionDoc = admin.getSections( user, section ).getAsDOMDocument();
        Element sectionElem = (Element) sectionDoc.getDocumentElement().getFirstChild();
        String superSectionKey = sectionElem.getAttribute( "supersectionkey" );

        admin.copySection( key );

        URL redirectURL = new URL( request.getHeader( "referer" ) );
        if ( superSectionKey != null && superSectionKey.length() > 0 )
        {
            redirectURL.setParameter( "sec", superSectionKey );
        }
        else
        {
            redirectURL.removeParameter( "sec" );
        }

        redirectURL.setParameter( "reload", "true" );
        redirectClientToURL( redirectURL, response );
    }

    public void handlerPreview( HttpServletRequest request, HttpServletResponse response, HttpSession session, AdminService admin,
                                ExtendedMap formItems )
        throws VerticalAdminException, VerticalEngineException
    {

        // The preview button is currently disabled in sectionoperations.xsl (dunno why), so this method should not be
        // in use..

        HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper( request );
        requestWrapper.setParamsMasked( false );
        requestWrapper.setParameter( "op", "preview" );
        requestWrapper.setParameter( "subop", "frameset" );
        ServletRequestAccessor.setRequest( request );

        User user = securityService.getLoggedInAdminConsoleUser();

        // get content
        int contentKey = formItems.getInt( "contentkey" );
        int contentTypeKey = admin.getContentTypeKey( contentKey );
        Document doc = admin.getContent( user, contentKey, 1, 1, 0 ).getAsDOMDocument();
        requestWrapper.setParameter( "contentkey", String.valueOf( contentKey ) );
        requestWrapper.setParameter( "page", String.valueOf( 999 + contentTypeKey ) );
        session.setAttribute( "_xml", XMLTool.documentToString( doc ) );

        requestWrapper.setParameter( "menukey", formItems.getString( "menukey" ) );
        if ( formItems.containsKey( "pagetemplatekey" ) )
        {
            requestWrapper.setParameter( "pagetemplatekey", formItems.getString( "pagetemplatekey" ) );
        }

        String servletPath = "/adminpage";
        forwardRequest( servletPath, requestWrapper, response );
    }

    private void invalidatePageCache( MenuItemKey menuItemKey )
    {
        MenuItemEntity menuItem = menuItemDao.findByKey( menuItemKey );
        SiteEntity site = menuItem.getSite();
        PageCache pageCache = pageCacheService.getPageCacheService( site.getKey() );
        pageCache.removeEntriesByMenuItem( menuItemKey );
    }
}
