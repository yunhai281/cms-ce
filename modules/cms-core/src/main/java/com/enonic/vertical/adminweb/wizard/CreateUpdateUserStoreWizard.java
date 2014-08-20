package com.enonic.vertical.adminweb.wizard;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.enonic.esl.containers.ExtendedMap;
import com.enonic.esl.xml.XMLTool;
import com.enonic.vertical.adminweb.VerticalAdminLogger;
import com.enonic.vertical.engine.VerticalEngineException;

import com.enonic.cms.framework.util.JDOMUtil;
import com.enonic.cms.framework.xml.XMLDocument;
import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.api.plugin.ext.userstore.UserStoreConfig;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.userstore.StoreNewUserStoreCommand;
import com.enonic.cms.core.security.userstore.UpdateUserStoreCommand;
import com.enonic.cms.core.security.userstore.UserStoreEntity;
import com.enonic.cms.core.security.userstore.UserStoreKey;
import com.enonic.cms.core.security.userstore.UserStoreService;
import com.enonic.cms.core.security.userstore.UserStoreServiceImpl;
import com.enonic.cms.core.security.userstore.UserStoreXmlCreator;
import com.enonic.cms.core.security.userstore.config.UserStoreConfigParser;
import com.enonic.cms.core.security.userstore.connector.config.UserStoreConnectorConfig;
import com.enonic.cms.core.security.userstore.connector.config.UserStoreConnectorConfigXmlCreator;
import com.enonic.cms.core.service.AdminService;

public class CreateUpdateUserStoreWizard
    extends Wizard
{
    @Autowired
    private transient UserStoreService userStoreService;

    private static final String ERROR_CONNECTOR = "20";

    private static final String ERROR_XMLPARSING = "2";

    private static final String ERROR_NAMEILLEGALCHARS = "19";

    public CreateUpdateUserStoreWizard()
    {
        super();
    }


    protected void initialize( AdminService admin, Document wizardConfigDoc )
        throws WizardException
    {
    }

    private void readObject( ObjectInputStream in )
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        this.userStoreService = UserStoreServiceImpl.INSTANCE;
    }


    protected boolean validateState( WizardState wizardState, HttpSession session, AdminService admin, ExtendedMap formItems )
    {
        boolean state = true;
        final String userStoreType = formItems.getString( "userStoreType" );
        String connectorName = null;
        if ( userStoreType.equals( "remote" ) )
        {
            connectorName = formItems.getString( "remoteUserStoreConnector", null );

            final String errorMessageConnector = verifyConnector( connectorName );
            if ( errorMessageConnector != null )
            {
                wizardState.addError( ERROR_CONNECTOR, "connector", errorMessageConnector );
                state = false;
            }

        }
        if ( state )
        {
            final String errorMessageConfig = verifyConfig( formItems.getString( "config", null ), connectorName );
            if ( errorMessageConfig != null )
            {
                wizardState.addError( ERROR_XMLPARSING, "config", errorMessageConfig );
                state = false;
            }
        }

        final String userStoreName = formItems.getString( "name" );
        final String userStoreNameRegExp = "[a-zA-Z0-9_-]+";
        if ( !userStoreName.matches( userStoreNameRegExp ) )
        {
            wizardState.addError( ERROR_NAMEILLEGALCHARS, "name" );
            state = false;

        }
        return state;
    }


    protected boolean evaluate( WizardState wizardState, HttpSession session, AdminService admin, ExtendedMap formItems,
                                String testCondition )
        throws WizardException
    {
        return true;
    }


    protected void processWizardData( WizardState wizardState, HttpSession session, AdminService admin, ExtendedMap formItems, User user,
                                      Document dataDoc )
        throws WizardException, VerticalEngineException
    {
        if ( StringUtils.isNotEmpty( formItems.getString( "key", null ) ) )
        {
            updateUserStore( formItems, user, userStoreService );
        }
        else
        {
            storeNewUserStore( formItems, user, userStoreService );
        }
    }

    protected void appendCustomData( WizardState wizardState, HttpSession session, AdminService admin, ExtendedMap formItems,
                                     ExtendedMap parameters, User user, Document dataconfigDoc, Document wizarddataDoc )
        throws WizardException
    {
        Element wizarddataElem = wizarddataDoc.getDocumentElement();

        Step currentStep = wizardState.getCurrentStep();
        if ( "step0".equals( currentStep.getName() ) )
        {
            Map<String, UserStoreConnectorConfig> userStoreConnectorConfigs = userStoreService.getUserStoreConnectorConfigs();

            if ( formItems.containsKey( "key" ) )
            {
                int key = formItems.getInt( "key" );
                final UserStoreXmlCreator userStoreXmlCreator = new UserStoreXmlCreator( userStoreConnectorConfigs );
                UserStoreEntity userStore = userStoreService.getUserStore( new UserStoreKey( key ) );
                XMLDocument userStoresXmlDoc = XMLDocumentFactory.create( userStoreXmlCreator.createUserStoresDocument( userStore ) );
                wizarddataElem.appendChild( wizarddataDoc.importNode( userStoresXmlDoc.getAsDOMDocument().getDocumentElement(), true ) );
            }

            // Default User Store
            Element defaultUserStoreElem = XMLTool.createElement( wizarddataElem, "defaultuserstore" );
            UserStoreEntity defaultUserStore = userStoreService.getDefaultUserStore();
            if ( defaultUserStore != null )
            {
                defaultUserStoreElem.setAttribute( "key", defaultUserStore.getKey().toString() );
            }
            wizarddataElem.appendChild( wizarddataDoc.importNode( defaultUserStoreElem, true ) );

            // UserStore connector config names

            final XMLDocument userStoreConnectorConfigNamesXmlDoc = XMLDocumentFactory.create(
                UserStoreConnectorConfigXmlCreator.createUserStoreConnectorConfigsDocument( userStoreConnectorConfigs.values() ) );
            wizarddataElem.appendChild(
                wizarddataDoc.importNode( userStoreConnectorConfigNamesXmlDoc.getAsDOMDocument().getDocumentElement(), true ) );
        }
        else
        {
            String message = "Unknown step: {0}";
            VerticalAdminLogger.error( message, currentStep.getName(), null );
        }
    }


    protected void saveState( WizardState wizardState, HttpServletRequest request, HttpServletResponse response, AdminService admin,
                              User user, ExtendedMap formItems )
        throws WizardException
    {
        super.saveState( wizardState, request, response, admin, user, formItems );

        // get step state document
        StepState stepState = wizardState.getCurrentStepState();
        Document stepstateDoc = stepState.getStateDoc();
        Element rootElem = stepstateDoc.getDocumentElement();

        Step currentStep = wizardState.getCurrentStep();
        if ( "step0".equals( currentStep.getName() ) )
        {
            final Element userStoreElem = XMLTool.createElement( stepstateDoc, rootElem, "userstore" );

            if ( formItems.containsKey( "key" ) )
            {
                userStoreElem.setAttribute( "key", formItems.getString( "key" ) );
            }
            userStoreElem.setAttribute( "name", formItems.getString( "name" ) );
            userStoreElem.setAttribute( "default", formItems.getString( "defaultuserstore", "false" ) );

            final String userStoreType = formItems.getString( "userStoreType" );
            userStoreElem.setAttribute( "remote", userStoreType.equals( "remote" ) ? "true" : "false" );
            if ( userStoreType.equals( "remote" ) )
            {
                userStoreElem.setAttribute( "connector", formItems.getString( "remoteUserStoreConnector", null ) );
            }
            XMLTool.createElement( stepstateDoc, userStoreElem, "configRaw", formItems.getString( "config", "" ) );
        }
        else
        {
            String message = "Unknown step: {0}";
            WizardLogger.errorWizard( message, currentStep );
        }
    }

    private String verifyConnector( final String connectorName )
    {
        try
        {
            userStoreService.verifyUserStoreConnector( connectorName );
        }
        catch ( Exception e )
        {
            return e.getMessage();
        }
        return null;
    }

    private String verifyConfig( final String xmlConfigData, final String connectorName )
    {
        try
        {
            org.jdom.Element configEl = null;
            if ( StringUtils.isNotEmpty( xmlConfigData.trim() ) )
            {
                configEl = JDOMUtil.parseDocument( xmlConfigData ).getRootElement();
            }
            final UserStoreConfig config = UserStoreConfigParser.parse( configEl, connectorName != null );
            if ( connectorName != null )
            {
                userStoreService.verifyUserStoreConnectorConfig( config, connectorName );
            }
        }
        catch ( Exception e )
        {
            return e.getMessage();
        }
        return null;
    }

    private void updateUserStore( ExtendedMap formItems, User user, UserStoreService userStoreService )
    {
        final UserStoreKey userStoreKey = new UserStoreKey( formItems.getString( "key" ) );

        final boolean newDefaultUserStore = formItems.getBoolean( "defaultuserstore", false );
        final String userStoreType = formItems.getString( "userStoreType" );
        String connectorName = null;
        if ( userStoreType.equals( "remote" ) )
        {
            connectorName = formItems.getString( "remoteUserStoreConnector", null );
        }
        final String configXmlString = formItems.getString( "config", null );
        UserStoreConfig config = new UserStoreConfig();
        if ( configXmlString != null && configXmlString.trim().length() > 0 )
        {
            config = UserStoreConfigParser.parse( XMLDocumentFactory.create( configXmlString ).getAsJDOMDocument().getRootElement(),
                                                  connectorName != null );
        }

        final UpdateUserStoreCommand command = new UpdateUserStoreCommand();
        command.setUpdater( user.getKey() );
        command.setKey( userStoreKey );
        command.setName( formItems.getString( "name", null ) );
        if ( newDefaultUserStore )
        {
            command.setAsNewDefaultStore();
        }
        command.setConnectorName( connectorName );
        command.setConfig( config );

        userStoreService.updateUserStore( command );

        userStoreService.invalidateUserStoreCachedConfig( command.getKey() );
    }

    private void storeNewUserStore( ExtendedMap formItems, User user, UserStoreService userStoreService )
    {
        final String userStoreType = formItems.getString( "userStoreType" );
        String connectorName = null;
        if ( userStoreType.equals( "remote" ) )
        {
            connectorName = formItems.getString( "remoteUserStoreConnector", null );
        }
        final String configXmlString = formItems.getString( "config", null );
        UserStoreConfig config = new UserStoreConfig();
        if ( configXmlString != null && configXmlString.trim().length() > 0 )
        {
            config = UserStoreConfigParser.parse( XMLDocumentFactory.create( configXmlString ).getAsJDOMDocument().getRootElement(),
                                                  connectorName != null );
        }

        final StoreNewUserStoreCommand command = new StoreNewUserStoreCommand();
        command.setStorer( user.getKey() );
        command.setName( formItems.getString( "name", null ) );
        command.setDefaultStore( formItems.getBoolean( "defaultuserstore", false ) );
        command.setConnectorName( connectorName );
        command.setConfig( config );

        userStoreService.storeNewUserStore( command );
    }
}

