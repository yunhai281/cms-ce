/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.adminweb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.lang.StringUtils;
import org.jdom.transform.JDOMSource;
import org.springframework.util.Assert;
import org.w3c.dom.Document;

import com.enonic.esl.containers.ExtendedMap;
import com.enonic.esl.net.URL;
import com.enonic.vertical.adminweb.wizard.Wizard;
import com.enonic.vertical.engine.VerticalEngineException;

import com.enonic.cms.framework.xml.XMLDocument;
import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.security.user.DeleteUserStoreCommand;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.userstore.DeleteUserStoreJob;
import com.enonic.cms.core.security.userstore.UserStoreEntity;
import com.enonic.cms.core.security.userstore.UserStoreKey;
import com.enonic.cms.core.security.userstore.UserStoreXmlCreator;
import com.enonic.cms.core.security.userstore.connector.synchronize.SynchronizeUserStoreJob;
import com.enonic.cms.core.security.userstore.connector.synchronize.SynchronizeUserStoreType;
import com.enonic.cms.core.service.AdminService;


public class UserStoreHandlerServlet
    extends AdminHandlerBaseServlet
{
    private static final String WIZARD_CONFIG_CREATE_UPDATE = "wizardconfig_create_update_userstore.xml";

    private User verifyAccessToEditUserStore()
        throws VerticalAdminException
    {

        UserEntity user = securityService.getLoggedInAdminConsoleUserAsEntity();
        if ( memberOfResolver.hasEnterpriseAdminPowers( user.getKey() ) )
        {
            return user;
        }

        VerticalAdminLogger.errorAdmin( "Not authorized." );
        return null;
    }

    public void handlerBrowse( HttpServletRequest request, HttpServletResponse response, HttpSession session, AdminService admin,
                               ExtendedMap formItems )
        throws VerticalAdminException
    {
//        verifyAccess( null );
        User user = securityService.getLoggedInAdminConsoleUser();
        UserEntity userEntity = userDao.findByKey( user.getKey() );

        final UserStoreXmlCreator xmlCreator = new UserStoreXmlCreator( userStoreService.getUserStoreConnectorConfigs() );

        final List<UserStoreEntity> userStores = securityService.getUserStores();

        final List<UserStoreEntity> validUserStores = new ArrayList<UserStoreEntity>();

        for ( UserStoreEntity userStoreEntity : userStores )
        {
            if ( memberOfResolver.hasUserStoreAdministratorPowers( userEntity, userStoreEntity.getKey() ) )
            {
                validUserStores.add( userStoreEntity );
            }
        }

        final org.jdom.Document userStoresXml = xmlCreator.createPagedDocument( validUserStores, 0, 100 );

        Source xslSource = AdminStore.getStylesheet( session, "userstore_browse.xsl" );

        // parameters
        ExtendedMap xslParams = new ExtendedMap();
        xslParams.put( "page", formItems.getString( "page" ) );
        addSortParamteres( "@name", "ascending", formItems, session, xslParams );
        addAccessLevelParameters( user, xslParams );

        if ( formItems.containsKey( "reload" ) )
        {
            xslParams.put( "reload", "true" );
        }
        try
        {
            transformXML( session, response.getWriter(), new JDOMSource( userStoresXml ), xslSource, xslParams );
        }
        catch ( IOException e )
        {
            VerticalAdminLogger.errorAdmin( "I/O error: %t", e );
        }
        catch ( TransformerException e )
        {
            VerticalAdminLogger.errorAdmin( "XSLT error: %t", e );
        }
    }

    public void handlerForm( HttpServletRequest request, HttpServletResponse response, HttpSession session, AdminService admin,
                             ExtendedMap formItems )
        throws VerticalAdminException, VerticalEngineException
    {

        verifyAccessToEditUserStore();

        URL url = new URL( request.getHeader( "referer" ) );
        url.setParameter( "reload", "true" );
        formItems.put( "redirect", url.toString() );
        ExtendedMap parameters = new ExtendedMap();
        User user = securityService.getLoggedInAdminConsoleUser();

        Wizard createUpdateWizard = Wizard.getInstance( admin, applicationContext, this, session, formItems, WIZARD_CONFIG_CREATE_UPDATE );
        createUpdateWizard.processRequest( request, response, session, admin, formItems, parameters, user );
    }

    public void handlerRemove( HttpServletRequest request, HttpServletResponse response, HttpSession session, AdminService admin,
                               ExtendedMap formItems, int key )
        throws VerticalAdminException, VerticalEngineException
    {
        User loggedInUser = securityService.getLoggedInAdminConsoleUser();

        Assert.isTrue( StringUtils.isNotEmpty( formItems.getString( "key", null ) ), "UserStore key required" );

        final DeleteUserStoreCommand command = new DeleteUserStoreCommand();
        command.setKey( new UserStoreKey( formItems.getString( "key" ) ) );
        command.setDeleter( loggedInUser.getKey() );

        final int batchSize = 20;
        final DeleteUserStoreJob job = new DeleteUserStoreJob( userStoreService, command, batchSize );
        job.start();

        if ( formItems.containsKey( "redirect_to" ) )
        {
            redirectClientToAdminPath( formItems.getString( "redirect_to" ), request, response );
        }
        else
        {
            ExtendedMap params = new ExtendedMap();
            params.put( "page", formItems.getString( "page" ) );
            params.put( "op", "browse" );
            params.put( "reload", "true" );
            redirectClientToAdminPath( "adminpage", params, request, response );
        }
    }

    public void handlerCustom( HttpServletRequest request, HttpServletResponse response, HttpSession session, AdminService admin,
                               ExtendedMap formItems, String operation )
        throws VerticalEngineException, VerticalAdminException
    {

        User user = securityService.getLoggedInAdminConsoleUser();

        SynchronizeUserStoreType syncType = null;

        if ( "synchronize_all".equals( operation ) )
        {
            syncType = SynchronizeUserStoreType.USERS_AND_GROUPS;
        }
        else if ( "synchronize_groups".equals( operation ) )
        {
            syncType = SynchronizeUserStoreType.GROUPS_ONLY;
        }
        else if ( "synchronize_users".equals( operation ) )
        {
            syncType = SynchronizeUserStoreType.USERS_ONLY;
        }

        if ( syncType != null )
        {
            final UserStoreKey userStoreKey = new UserStoreKey( formItems.getInt( "domainkey" ) );
            final SynchronizeUserStoreJob job = synchronizeUserStoreJobFactory.createSynchronizeUserStoreJob( userStoreKey, syncType, 5 );
            job.start();

            if ( formItems.containsKey( "redirect_to" ) )
            {
                String path = (String) formItems.get( "redirect_to" );
                redirectClientToAdminPath( path, request, response );
            }
            else
            {
                ExtendedMap params = new ExtendedMap();
                params.put( "page", formItems.getString( "page" ) );
                params.put( "op", "browse" );
                redirectClientToAdminPath( "adminpage", params, request, response );
            }
        }

        else if ( "page".equals( operation ) )
        {
            handlerPage( request, response, session, admin, formItems, operation );
        }
    }

    private void handlerPage( HttpServletRequest request, HttpServletResponse response, HttpSession session, AdminService admin,
                              ExtendedMap formItems, String operation )
        throws VerticalEngineException, VerticalAdminException
    {
        User user = securityService.getLoggedInAdminConsoleUser();

        UserStoreKey userStoreKey = new UserStoreKey( formItems.getInt( "key" ) );

        final UserStoreXmlCreator userStoreXmlCreator = new UserStoreXmlCreator( userStoreService.getUserStoreConnectorConfigs() );
        UserStoreEntity userStore = userStoreService.getUserStore( userStoreKey );
        XMLDocument userStoresXmlDoc = XMLDocumentFactory.create( userStoreXmlCreator.createUserStoresDocument( userStore ) );

        Document dataDoc = userStoresXmlDoc.getAsDOMDocument();
        try
        {
            boolean isUserStoreAdministrator = memberOfResolver.hasUserStoreAdministratorPowers( user.getKey(), userStoreKey );
            // parameters
            ExtendedMap xslParams = new ExtendedMap();
            xslParams.put( "page", formItems.getString( "page" ) );
            xslParams.put( "key", String.valueOf( userStoreKey ) );
            xslParams.put( "reload", formItems.getString( "reload", "" ) );
            xslParams.put( "userstorekey", userStoreKey.toString() );
            xslParams.put( "userstorename", userStore.getName() );
            addCommonParameters( admin, user, request, xslParams, -1, -1 );
            addAccessLevelParameters( user, xslParams );
            xslParams.put( "userstoreadmin", isUserStoreAdministrator );

            boolean canSyncUsers = false;
            boolean canSyncGroups = false;
            try
            {
                canSyncUsers = userStoreService.canSynchronizeUsers( userStoreKey );
                canSyncGroups = userStoreService.canSynchronizeGroups( userStoreKey );
            }
            catch ( final Exception e )
            {
                xslParams.put( "userStoreConfigError", e.getMessage() );
            }
            xslParams.put( "synchronizeUsers", canSyncUsers );
            xslParams.put( "synchronizeGroups", canSyncGroups );

            Source xslSource = AdminStore.getStylesheet( session, "userstore_page.xsl" );
            Source xmlSource = new DOMSource( dataDoc );
            transformXML( session, response.getWriter(), xmlSource, xslSource, xslParams );
        }
        catch ( TransformerException e )
        {
            VerticalAdminLogger.errorAdmin( "XSLT error: %t", e );
        }
        catch ( IOException e )
        {
            VerticalAdminLogger.errorAdmin( "I/O error: %t", e );
        }
    }
}
