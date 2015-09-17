package com.enonic.vertical.adminweb.wizard;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.enonic.esl.containers.ExtendedMap;
import com.enonic.esl.util.DateUtil;
import com.enonic.esl.xml.XMLTool;
import com.enonic.vertical.engine.CategoryAccessRight;
import com.enonic.vertical.engine.MenuItemAccessRight;
import com.enonic.vertical.engine.SectionCriteria;
import com.enonic.vertical.engine.Types;
import com.enonic.vertical.engine.VerticalEngineException;
import com.enonic.vertical.engine.handlers.MenuHandler;

import com.enonic.cms.framework.util.TIntArrayList;
import com.enonic.cms.framework.xml.XMLDocument;
import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.CmsDateAndTimeFormats;
import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentLocation;
import com.enonic.cms.core.content.ContentLocationSpecification;
import com.enonic.cms.core.content.ContentLocations;
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.content.ContentStatus;
import com.enonic.cms.core.content.ContentVersionKey;
import com.enonic.cms.core.content.command.UnassignContentCommand;
import com.enonic.cms.core.content.command.UpdateContentCommand;
import com.enonic.cms.core.mail.ApproveAndRejectMailTemplate;
import com.enonic.cms.core.mail.MailRecipient;
import com.enonic.cms.core.mail.SendMailService;
import com.enonic.cms.core.portal.cache.PageCache;
import com.enonic.cms.core.portal.cache.PageCacheService;
import com.enonic.cms.core.security.SecurityService;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.service.AdminService;
import com.enonic.cms.core.structure.SiteEntity;
import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.core.structure.SiteProperties;
import com.enonic.cms.core.structure.SitePropertiesService;
import com.enonic.cms.core.structure.SiteService;
import com.enonic.cms.core.structure.SiteXmlCreator;
import com.enonic.cms.core.structure.menuitem.AddContentToSectionCommand;
import com.enonic.cms.core.structure.menuitem.MenuItemAccessResolver;
import com.enonic.cms.core.structure.menuitem.MenuItemAccessType;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.core.structure.menuitem.MenuItemKey;
import com.enonic.cms.core.structure.menuitem.MenuItemService;
import com.enonic.cms.core.structure.menuitem.MenuItemServiceCommand;
import com.enonic.cms.core.structure.menuitem.MenuItemSpecification;
import com.enonic.cms.core.structure.menuitem.MenuItemType;
import com.enonic.cms.core.structure.menuitem.MenuItemXMLCreatorSetting;
import com.enonic.cms.core.structure.menuitem.MenuItemXmlCreator;
import com.enonic.cms.core.structure.menuitem.OrderContentsInSectionCommand;
import com.enonic.cms.core.structure.menuitem.RemoveContentsFromSectionCommand;
import com.enonic.cms.core.structure.menuitem.SetContentHomeCommand;
import com.enonic.cms.core.structure.page.PageSpecification;
import com.enonic.cms.core.structure.page.template.PageTemplateKey;
import com.enonic.cms.core.structure.page.template.PageTemplateSpecification;
import com.enonic.cms.core.structure.page.template.PageTemplateType;
import com.enonic.cms.store.dao.ContentDao;
import com.enonic.cms.store.dao.GroupDao;
import com.enonic.cms.store.dao.MenuItemDao;

public class PublishWizard
    extends Wizard
{
    private static final long serialVersionUID = 1200012L;

    protected static final int[] EXCLUDED_TYPE_KEYS_IN_PREVIEW = new int[]{1, 2, 3, 4, 6};

    @Autowired
    private transient MenuItemService menuItemService;

    @Autowired
    private transient ContentService contentService;

    @Autowired
    private transient SecurityService securityService;

    @Autowired
    private transient MenuItemDao menuItemDao;

    @Autowired
    private transient SendMailService sendMailService;

    @Autowired
    private transient PageCacheService pageCacheService;

    @Autowired
    private transient SiteService siteService;

    @Autowired
    private transient SitePropertiesService sitePropertiesService;

    @Autowired
    private transient GroupDao groupDao;

    @Autowired
    private transient ContentDao contentDao;

    @Autowired
    private MenuHandler menuHandler;

    private Document sectionsDoc;

    private static enum Action
    {
        none,
        add,
        remove
    }

    public PublishWizard()
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
    }

    protected boolean validateState( WizardState wizardState, HttpSession session, AdminService admin, ExtendedMap formItems )
    {
        return true;
    }

    protected boolean evaluate( WizardState wizardState, HttpSession session, AdminService admin, ExtendedMap formItems,
                                String testCondition )
        throws WizardException
    {
        User user = securityService.getLoggedInAdminConsoleUser();
        boolean result;
        if ( "moreOrder".equals( testCondition ) )
        {
            result = moreOrder( user, wizardState, admin );
        }
        else if ( "noSites".equals( testCondition ) )
        {
            result = noSites( wizardState );
        }
        else
        {
            String message = "Unknown test condition: {0}";
            WizardLogger.errorWizard( message, testCondition );
            result = false;
        }

        return result;
    }

    protected void processWizardData( WizardState wizardState, HttpSession session, AdminService admin, ExtendedMap formItems,
                                      User user, Document dataDoc )
        throws WizardException, VerticalEngineException, IOException, MessagingException
    {
        Step step = wizardState.getCurrentStep();
        String finishName = step.getName();

        try
        {
            int contentKey;
            int versionKey = formItems.getInt( "versionkey", -1 );
            if ( versionKey < 0 )
            {
                contentKey = formItems.getInt( "contentkey" );
                versionKey = admin.getCurrentVersionKey( contentKey );
                formItems.put( "versionkey", versionKey );
            }
            else
            {
                contentKey = admin.getContentKeyByVersionKey( versionKey );
                formItems.put( "contentkey", contentKey );
            }

            if ( "finish0".equals( finishName ) )
            {
                processWizardData0( wizardState, user, contentKey, versionKey );
            }
            else
            {
                processWizardData0( wizardState, user, contentKey, versionKey );
                processWizardData1( wizardState, admin, user );
            }
        }
        catch ( ParseException pe )
        {
            String message = "Failed to parse a date: %t";
            WizardLogger.errorWizard( message, pe );
        }
    }

    private void processWizardData0( WizardState wizardState, User user, int contentKey, int versionKey )
        throws ParseException, IOException, MessagingException
    {
        Document stateDoc = wizardState.getFirstStepState().getStateDoc();
        Element rootElem = stateDoc.getDocumentElement();
        Element statusElem = XMLTool.getElement( rootElem, "status" );
        int status;
        if ( statusElem != null )
        {
            status = Integer.valueOf( XMLTool.getElementText( statusElem ) );
        }
        else
        {
            status = -1;
        }
        ContentEntity content = contentDao.findByKey( new ContentKey( contentKey ) );
        int originalStatus = content.getVersion( new ContentVersionKey( versionKey ) ).getStatus().getKey();

        UpdateContentCommand command = UpdateContentCommand.updateExistingVersion2( new ContentVersionKey( versionKey ) );
        command.setContentKey( new ContentKey( contentKey ) );
        command.setModifier( securityService.getUser( user ) );
        command.setSyncAccessRights( false );
        command.setSyncRelatedContent( false );
        // Keep comment since this is an update of existing content
        command.setChangeComment( content.getMainVersion().getChangeComment() );

        switch ( status )
        {
            case 0:
            {
                if ( originalStatus == 1 )
                {
                    // reject approval
                    sendMessage( user, stateDoc, contentKey, originalStatus );
                    command.setStatus( ContentStatus.DRAFT );
                    command.setUpdateAsMainVersion( false );
                    command.setAvailableFrom( content.getAvailableFrom() );
                    command.setAvailableTo( content.getAvailableTo() );
                    contentService.updateContent( command );
                }
                break;
            }
            case 1:
            {
                throw new IllegalArgumentException( "Unexpected status: " + status );
            }
            case 2:
            {
                Date from = null;
                Date to = null;

                // approve content/keep approval
                final Element publishingElem = XMLTool.getElement( rootElem, "publishing" );
                if ( publishingElem != null )
                {
                    final String fromStr = publishingElem.getAttribute( "from" );
                    if ( fromStr != null && fromStr.length() > 0 )
                    {
                        from = DateUtil.parseISODateTime( fromStr );
                    }
                    final String toStr = publishingElem.getAttribute( "to" );
                    if ( toStr != null && toStr.length() > 0 )
                    {
                        to = DateUtil.parseISODateTime( toStr );
                    }
                }

                command.setStatus( ContentStatus.APPROVED );
                command.setUpdateAsMainVersion( true );
                command.setAvailableFrom( from );
                command.setAvailableTo( to );
                contentService.updateContent( command );

                UnassignContentCommand unassignCommand = new UnassignContentCommand();
                unassignCommand.setContentKey( new ContentKey( contentKey ) );
                unassignCommand.setUnassigner( user.getKey() );
                contentService.unassignContent( unassignCommand );

                break;
            }
            case 3:
            {
                // archive content
                command.setStatus( ContentStatus.ARCHIVED );
                command.setUpdateAsMainVersion( false );
                command.setAvailableFrom( content.getAvailableFrom() );
                command.setAvailableTo( content.getAvailableTo() );
                contentService.updateContent( command );

                UnassignContentCommand unassignCommand = new UnassignContentCommand();
                unassignCommand.setContentKey( new ContentKey( contentKey ) );
                unassignCommand.setUnassigner( user.getKey() );
                contentService.unassignContent( unassignCommand );

                break;
            }


        }
    }


    private void sendMessage( User user, Document stateDoc, int contentKey, int originalStatus )
        throws IOException, MessagingException
    {

        Element rootElem = stateDoc.getDocumentElement();
        Element recipientsElem = XMLTool.getElement( rootElem, "recipients" );
        Element[] recipientElems = XMLTool.getElements( recipientsElem );
        if ( recipientElems.length > 0 )
        {

            Element messageElem = XMLTool.getElement( rootElem, "message" );
            if ( messageElem != null )
            {

                final UserEntity userEntity = securityService.getUser( user );

                String body = XMLTool.getElementText( messageElem );

                ApproveAndRejectMailTemplate mailCreator =
                    new ApproveAndRejectMailTemplate( body, new ContentKey( contentKey ), userEntity );

                //reject
                if ( originalStatus == 1 )
                {
                    mailCreator.setReject( true );
                }

                //send to approval
                if ( originalStatus == 0 )
                {
                    mailCreator.setReject( false );
                }

                mailCreator.setFrom( new MailRecipient( user.getDisplayName(), user.getEmail() ) );

                for ( Element recipientElem : recipientElems )
                {
                    String recipientName = recipientElem.getAttribute( "name" );
                    String recipientEmail = recipientElem.getAttribute( "email" );
                    mailCreator.addRecipient( new MailRecipient( recipientName, recipientEmail ) );
                }

                sendMailService.sendMail( mailCreator );
            }
        }
    }

    private void processWizardData1( WizardState wizardState, AdminService admin, User user )
    {
        Document firstStepState = wizardState.getFirstStepState().getStateDoc();
        Element elem = XMLTool.getElement( firstStepState.getDocumentElement(), "content" );
        int contentKey = Integer.parseInt( elem.getAttribute( "key" ) );

        Document sectionsDoc = XMLTool.createDocument( "sections" );
        Element sectionsElem = sectionsDoc.getDocumentElement();
        sectionsElem.setAttribute( "contentkey", String.valueOf( contentKey ) );

        Document step1State = wizardState.getStepState( "step1" ).getStateDoc();
        Element[] menuElems = XMLTool.getElements( step1State.getDocumentElement(), "menu" );
        Map<SiteKey, List<MenuItemKey>> listOfMenuItemKeysBySiteKey = new HashMap<SiteKey, List<MenuItemKey>>();
        int manualOrderIndex = 0;

        List<MenuItemServiceCommand> menuItemServiceCommands = Lists.newArrayList();
        List<AddContentToSectionCommand> addContentToSectionCommands = Lists.newArrayList();
        for ( Element menuElem : menuElems )
        {
            int menuKey = Integer.parseInt( menuElem.getAttribute( "key" ) );
            SiteKey siteKey = new SiteKey( menuKey );
            // set content home (menu item) and framework
            int categoryKey = admin.getCategoryKey( contentKey );
            CategoryAccessRight categoryAccessRight = admin.getCategoryAccessRight( user, categoryKey );
            if ( categoryAccessRight.getPublish() )
            {
                Element homeElem = XMLTool.getElement( menuElem, "home" );
                if ( homeElem != null )
                {
                    Element pageTemplateElem = XMLTool.getElement( menuElem, "pagetemplate" );
                    PageTemplateKey pageTemplateKey = null;
                    if ( pageTemplateElem != null )
                    {
                        pageTemplateKey = new PageTemplateKey( Integer.parseInt( pageTemplateElem.getAttribute( "key" ) ) );
                    }
                    final int homeKey = Integer.parseInt( homeElem.getAttribute( "key" ) );
                    SetContentHomeCommand setContentHomeCommand = new SetContentHomeCommand();
                    setContentHomeCommand.setSetter( user.getKey() );
                    setContentHomeCommand.setContent( new ContentKey( contentKey ) );
                    setContentHomeCommand.setSection( new MenuItemKey( homeKey ) );
                    setContentHomeCommand.setPageTemplate( pageTemplateKey );
                    menuItemServiceCommands.add( setContentHomeCommand );
                }
            }

            // iterate list of menus
            Element[] menuItemElems = XMLTool.getElements( menuElem, "menuitem" );
            for ( Element menuItemElem : menuItemElems )
            {
                boolean manuallyOrder = Boolean.valueOf( menuItemElem.getAttribute( "manuallyOrder" ) );
                boolean ordered = Boolean.valueOf( menuItemElem.getAttribute( "ordered" ) );
                MenuItemKey menuItemKey = new MenuItemKey( menuItemElem.getAttribute( "key" ) );
                MenuItemKey sectionKey = admin.getSectionKeyByMenuItemKey( menuItemKey );

                final Action action = Action.valueOf( menuItemElem.getAttribute( "action" ) );

                switch ( action )
                {
                    case add:
                        final AddContentToSectionCommand addContentToSectionCommand = new AddContentToSectionCommand();
                        addContentToSectionCommand.setSection( menuItemKey );
                        addContentToSectionCommand.setContent( new ContentKey( contentKey ) );
                        addContentToSectionCommand.setContributor( user.getKey() );

                        List<MenuItemKey> menuItemKeysBySiteKey = listOfMenuItemKeysBySiteKey.get( siteKey );
                        if ( menuItemKeysBySiteKey == null )
                        {
                            menuItemKeysBySiteKey = new ArrayList<MenuItemKey>();
                            listOfMenuItemKeysBySiteKey.put( siteKey, menuItemKeysBySiteKey );
                        }
                        menuItemKeysBySiteKey.add( menuItemKey );

                        Element sectionElem = XMLTool.createElement( sectionsDoc, sectionsElem, "section" );
                        sectionElem.setAttribute( "key", String.valueOf( sectionKey ) );
                        MenuItemAccessRight menuItemAccessRight = admin.getMenuItemAccessRight( user, menuItemKey );
                        boolean approveInSection = menuItemAccessRight.getPublish();
                        sectionElem.setAttribute( "approved", String.valueOf( approveInSection ) );
                        sectionElem.setAttribute( "ordered", Boolean.toString( ordered ) );
                        sectionElem.setAttribute( "manuallyOrder", Boolean.toString( manuallyOrder ) );

                        addContentToSectionCommand.setApproveInSection( approveInSection );
                        if ( !approveInSection )
                        {
                            addContentToSectionCommand.setAddOnTop( false );
                        }

                        if ( ordered && manuallyOrder )
                        {
                            final OrderContentsInSectionCommand orderContentsInSectionCommand =
                                addContentToSectionCommand.createOrderContentsInSectionCommand();
                            final List<ContentKey> wantedOrder = new ArrayList<ContentKey>();

                            StepState stepState = wizardState.getStepState( "step1" );
                            int k = -1;
                            do
                            {
                                stepState = stepState.getNextStepState();
                                k++;
                            }
                            while ( k < manualOrderIndex );
                            Element contentsElem = XMLTool.createElement( sectionsDoc, sectionElem, "contents" );
                            Document tempStateDoc = stepState.getStateDoc();
                            Element tempSectionElem = XMLTool.getFirstElement( tempStateDoc.getDocumentElement() );
                            Element[] tempContentElems = XMLTool.getElements( tempSectionElem );
                            for ( Element tempContentElem : tempContentElems )
                            {
                                contentsElem.appendChild( sectionsDoc.importNode( tempContentElem, true ) );
                                wantedOrder.add( new ContentKey( tempContentElem.getAttribute( "key" ) ) );
                            }
                            manualOrderIndex++;

                            orderContentsInSectionCommand.setWantedOrder( wantedOrder );
                        }
                        else if ( ordered && !manuallyOrder && approveInSection )
                        {
                            addContentToSectionCommand.setAddOnTop( true );
                        }

                        addContentToSectionCommands.add( addContentToSectionCommand );
                        break;

                    case remove:
                        final RemoveContentsFromSectionCommand removeCommand = new RemoveContentsFromSectionCommand();
                        removeCommand.setRemover( user.getKey() );
                        removeCommand.setSection( menuItemKey );
                        removeCommand.addContentToRemove( new ContentKey( contentKey ) );

                        menuItemServiceCommands.add( removeCommand );
                        break;

                    case none:
                        if ( ordered && manuallyOrder )
                        {
                            final List<ContentKey> wantedOrder = new ArrayList<ContentKey>();

                            StepState stepState = wizardState.getStepState( "step1" );
                            int k = -1;
                            do
                            {
                                stepState = stepState.getNextStepState();
                                k++;
                            }
                            while ( k < manualOrderIndex );
                            Document tempStateDoc = stepState.getStateDoc();
                            Element tempSectionElem = XMLTool.getFirstElement( tempStateDoc.getDocumentElement() );
                            Element[] tempContentElems = XMLTool.getElements( tempSectionElem );
                            for ( Element tempContentElem : tempContentElems )
                            {
                                wantedOrder.add( new ContentKey( tempContentElem.getAttribute( "key" ) ) );
                            }
                            manualOrderIndex++;

                            final OrderContentsInSectionCommand orderContentsInSectionCommand = new OrderContentsInSectionCommand();
                            orderContentsInSectionCommand.setSectionKey( sectionKey );
                            orderContentsInSectionCommand.setWantedOrder( wantedOrder );
                            menuItemServiceCommands.add( orderContentsInSectionCommand );
                        }
                        break;
                }
            }
        }

        menuItemServiceCommands.addAll( addContentToSectionCommands );
        menuItemService.execute( menuItemServiceCommands.toArray( new MenuItemServiceCommand[menuItemServiceCommands.size()] ) );

        for ( SiteKey siteKey : listOfMenuItemKeysBySiteKey.keySet() )
        {
            PageCache pageCache = pageCacheService.getPageCacheService( siteKey );
            List<MenuItemKey> menuItemKeys = listOfMenuItemKeysBySiteKey.get( siteKey );
            for ( MenuItemKey menuItemKeyToRemoveCacheEntriesFor : menuItemKeys )
            {
                pageCache.removeEntriesByMenuItem( menuItemKeyToRemoveCacheEntriesFor );
            }
        }
    }

    protected void appendCustomData( WizardState wizardState, HttpSession session, AdminService admin, ExtendedMap formItems,
                                     ExtendedMap parameters, User user, Document dataconfigDoc, Document wizarddataDoc )
        throws WizardException
    {
        if ( formItems.containsKey( "selectedunitkey" ) )
        {
            int unitKey = formItems.getInt( "selectedunitkey" );
            formItems.put( "unitname", admin.getUnitName( unitKey ) );
        }

        int categoryKey = formItems.getInt( "cat" );
        Document doc = admin.getSuperCategoryNames( categoryKey, false, true ).getAsDOMDocument();
        Element wizarddataElem = wizarddataDoc.getDocumentElement();
        wizarddataElem.appendChild( wizarddataDoc.importNode( doc.getDocumentElement(), true ) );

        int contentKey;
        int versionKey = formItems.getInt( "versionkey", -1 );
        if ( versionKey < 0 )
        {
            contentKey = formItems.getInt( "contentkey" );
            versionKey = admin.getCurrentVersionKey( contentKey );
            formItems.put( "versionkey", versionKey );
        }
        else
        {
            contentKey = admin.getContentKeyByVersionKey( versionKey );
            formItems.put( "contentkey", contentKey );
        }

        formItems.put( "contenttitle", admin.getContentTitle( versionKey ) );
        int contentTypeKey = admin.getContentTypeKey( contentKey );
        formItems.put( "contenttypekey", String.valueOf( contentTypeKey ) );

        Step currentStep = wizardState.getCurrentStep();
        if ( "step0".equals( currentStep.getName() ) )
        {
            appendCustomDataStep0( user, admin, wizardState, wizarddataDoc, parameters, categoryKey, contentKey, versionKey );
        }
        else if ( "step1".equals( currentStep.getName() ) )
        {
            appendCustomDataStep1( user, admin, wizardState, wizarddataDoc, contentKey, versionKey );
        }
        else if ( "step2".equals( currentStep.getName() ) )
        {
            appendCustomDataStep2( admin, wizardState, wizarddataDoc, formItems, versionKey );
        }
        else if ( "step3".equals( currentStep.getName() ) )
        {
            appendCustomDataStep3( user, admin, wizardState, wizarddataDoc, contentKey, versionKey );
        }
    }

    private void appendCustomDataStep0( User user, AdminService admin, WizardState wizardState, Document wizarddataDoc,
                                        ExtendedMap parameters, int categoryKey, int contentKey, int versionKey )
    {
        Element wizarddataElem = wizarddataDoc.getDocumentElement();

        // get content version
        Document doc = admin.getContentVersion( user, versionKey ).getAsDOMDocument();
        Element contentElem = XMLTool.getFirstElement( doc.getDocumentElement() );
        int originalStatus = Integer.valueOf( contentElem.getAttribute( "status" ) );
        wizarddataElem.appendChild( wizarddataDoc.importNode( doc.getDocumentElement(), true ) );

        // sites
        int contentTypeKey = admin.getContentTypeKey( contentKey );

        List<SiteEntity> sites = siteService.getSitesToPublishTo( contentTypeKey, user );
        SiteXmlCreator siteXmlCreator = new SiteXmlCreator( null, menuHandler );
        siteXmlCreator.setIncludeMenuItems( false );

        Map<SiteKey, SiteProperties> sitePropertyMap = new HashMap<SiteKey, SiteProperties>();

        for ( SiteEntity site : sites )
        {
            SiteProperties siteProperties = sitePropertiesService.getSiteProperties( site.getKey() );
            sitePropertyMap.put( site.getKey(), siteProperties );
        }

        XMLDocument sitesToPublishTo = siteXmlCreator.createLegacyGetMenus( sites, sitePropertyMap );

        doc = sitesToPublishTo.getAsDOMDocument();
        wizarddataElem.appendChild( wizarddataDoc.importNode( doc.getDocumentElement(), true ) );

        if ( !admin.isContentVersionApproved( versionKey ) )
        {
            Document stateDoc = wizardState.getCurrentStepState().getStateDoc();
            Element publishingElem = XMLTool.getElement( stateDoc.getDocumentElement(), "publishing" );
            if ( publishingElem == null )
            {
                // we need to keep the current publishFrom and publishTo dates if they are set
                publishingElem = XMLTool.createElement( stateDoc, stateDoc.getDocumentElement(), "publishing" );
                if ( !"".equals( contentElem.getAttribute( "publishfrom" ) ) )
                {
                    publishingElem.setAttribute( "from", contentElem.getAttribute( "publishfrom" ) );
                }
                else
                {
                    publishingElem.setAttribute( "from", CmsDateAndTimeFormats.printAs_STORE_DATE( ( new Date() ) ) );
                }
                if ( !"".equals( contentElem.getAttribute( "publishto" ) ) )
                {
                    publishingElem.setAttribute( "to", contentElem.getAttribute( "publishto" ) );
                }
            }
        }

        Document stateDoc = wizardState.getCurrentStepState().getStateDoc();
        Element statusElem = XMLTool.getElement( stateDoc.getDocumentElement(), "status" );
        int status = ( statusElem == null ? originalStatus : Integer.valueOf( XMLTool.getElementText( statusElem ) ) );
        if ( ( originalStatus == 0 && status == 1 ) || "loadRecipients".equals( wizardState.getCurrentStepState().getButtonPressed() ) )
        {
            doc = admin.getUsersWithPublishRight( categoryKey ).getAsDOMDocument();
            wizarddataElem.appendChild( wizarddataDoc.importNode( doc.getDocumentElement(), true ) );
            parameters.put( "notify", "sendtoapproval" );
        }
        else if ( ( originalStatus == 1 && status == 0 ) || "loadOwner".equals( wizardState.getCurrentStepState().getButtonPressed() ) )
        {
            doc = admin.getContentOwner( contentKey ).getAsDOMDocument();
            wizarddataElem.appendChild( wizarddataDoc.importNode( doc.getDocumentElement(), true ) );
            parameters.put( "notify", "reject" );
        }
    }

    // publishing

    private void appendCustomDataStep1( User user, AdminService admin, WizardState wizardState, Document wizarddataDoc, int contentKey,
                                        int versionKey )
    {
        Element wizarddataElem = wizarddataDoc.getDocumentElement();

        // get first step's selected menu keys
        Document stateDoc = wizardState.getFirstStepState().getStateDoc();
        Element stepstateElem = stateDoc.getDocumentElement();
        Element[] menuElems = XMLTool.getElements( stepstateElem, "menu" );
        int[] menuKeys = new int[0];
        if ( menuElems.length > 0 )
        {
            menuKeys = new int[menuElems.length];
            for ( int i = 0; i < menuElems.length; i++ )
            {
                menuKeys[i] = Integer.parseInt( menuElems[i].getAttribute( "key" ) );

                Document doc = admin.getPageTemplatesByMenu( menuKeys[i], EXCLUDED_TYPE_KEYS_IN_PREVIEW ).getAsDOMDocument();
                wizarddataElem.appendChild( wizarddataDoc.importNode( doc.getDocumentElement(), true ) );
            }

            final Document doc = wizardState.getStepState( "step1" ).getStateDoc();
            final Element rootElem = doc.getDocumentElement();
            final Set<String> menus = Sets.newHashSet();

            final boolean firstStep = "".equals( rootElem.getAttribute( "buttonpressed" ) );
            if ( !firstStep )
            {
                final Element[] menuitems = XMLTool.selectElements( rootElem, "/stepstate/menu/menuitem" );

                for ( final Element menuitem : menuitems )
                {
                    final String selected = menuitem.getAttribute( "selected" );
                    final String menukey = menuitem.getAttribute( "key" );

                    if ( selected.equals( "true" ) )
                    {
                        menus.add( menukey );
                    }
                }
            }

            // sites
            final List<MenuItemEntity> menuItems = getAccessibleMenuItems( user, menuKeys );
            Document menuItemsDoc = createElementsToList( menuItems, menus, firstStep );
            wizarddataElem.appendChild( wizarddataDoc.importNode( menuItemsDoc.getDocumentElement(), true ) );

            // sections
            SectionCriteria criteria = new SectionCriteria();
            criteria.setSiteKeys( menuKeys );
            criteria.setTreeStructure( false );
            criteria.setAppendAccessRights( false );
            criteria.setContentKeyExcludeFilter( contentKey );
            criteria.setMarkContentFilteredSections( true );
            int contentTypeKey = admin.getContentTypeKey( contentKey );
            criteria.setContentTypeKeyFilter( contentTypeKey );
            criteria.setIncludeSectionsWithoutContentTypeEvenWhenFilterIsSet( false );
            criteria.setIncludeSectionContentTypesInfo( false );
            Document sectionsDoc = admin.getSections( user, criteria ).getAsDOMDocument();
            wizarddataElem.appendChild( wizarddataDoc.importNode( sectionsDoc.getDocumentElement(), true ) );
        }

        // get content version
        Document doc = admin.getContentVersion( user, versionKey ).getAsDOMDocument();
        wizarddataElem.appendChild( wizarddataDoc.importNode( doc.getDocumentElement(), true ) );

        // determine home of content on site
        doc = admin.getContentHomes( contentKey ).getAsDOMDocument();
        final Element contentHomes = XMLTool.getElements( doc, "/contenthomes" )[0];

        // content does not have exactly home ? - resolve it
        if ( contentHomes.getChildNodes().getLength() == 0 )
        {
            for ( final int menuKey : menuKeys )
            {
                final ContentEntity content = contentDao.findByKey( new ContentKey( contentKey ) );

                final SiteKey siteKey = new SiteKey( menuKey );

                final ContentLocationSpecification contentLocationSpecification = new ContentLocationSpecification();
                contentLocationSpecification.setIncludeInactiveLocationsInSection( true );
                contentLocationSpecification.setSiteKey( siteKey );
                final ContentLocations contentLocations = content.getLocations( contentLocationSpecification );

                // <contenthome contentkey="3" menuitemkey="6" menukey="0"/>
                final ContentLocation homeLocation = contentLocations.getHomeLocation( siteKey );
                if ( homeLocation != null )
                {
                    final Element contentHomeElem = XMLTool.createElement( doc, contentHomes, "contenthome" );
                    contentHomeElem.setAttribute( "contentkey", "" + homeLocation.getContent().getKey() );
                    contentHomeElem.setAttribute( "menuitemkey", "" + homeLocation.getMenuItem().getKey() );
                    contentHomeElem.setAttribute( "menukey", "" + menuKey );

                    contentHomes.appendChild( contentHomeElem );
                }

            }
        }

        wizarddataElem.appendChild( wizarddataDoc.importNode( doc.getDocumentElement(), true ) );
    }

    // position content in section

    private void appendCustomDataStep2( AdminService admin, WizardState wizardState, Document wizarddataDoc, ExtendedMap formItems,
                                        int versionKey )
    {
        StepState stepState = wizardState.getCurrentStepState();
        NormalStep step;
        int sectionIndex = -1;
        do
        {
            sectionIndex++;
            stepState = stepState.getPreviousStepState();
            step = stepState.getStep();
        }
        while ( !"step1".equals( step.getName() ) );
        formItems.put( "sectionnumber", String.valueOf( sectionIndex + 1 ) );

        MenuItemKey menuItemKey = null;
        Document stateDoc = stepState.getStateDoc();
        Element stepstateElem = stateDoc.getDocumentElement();
        Element[] menuElems = XMLTool.getElements( stepstateElem, "menu" );
        int idx = 0;
        outer:
        for ( Element menuElem : menuElems )
        {
            Element[] menuitemElems = XMLTool.getElements( menuElem, "menuitem" );
            for ( Element menuitemElem : menuitemElems )
            {
                boolean manuallyOrder = Boolean.valueOf( menuitemElem.getAttribute( "manuallyOrder" ) );
                boolean ordered = Boolean.valueOf( menuitemElem.getAttribute( "ordered" ) );
                if ( manuallyOrder && ordered )
                {
                    if ( idx == sectionIndex )
                    {
                        menuItemKey = new MenuItemKey( Integer.parseInt( menuitemElem.getAttribute( "key" ) ) );
                        break outer;
                    }
                    else
                    {
                        idx++;
                    }
                }
            }
        }
        formItems.putInt( "menuitemkey", menuItemKey.toInt() );
        String path = admin.getPathString( Types.MENUITEM, menuItemKey.toInt() );
        formItems.put( "path", path );

        Element wizarddataElem = wizarddataDoc.getDocumentElement();
        StepState currentStepState = wizardState.getCurrentStepState();
        String buttonPressed = currentStepState.getButtonPressed();
        if ( "moveup".equals( buttonPressed ) || "movedown".equals( buttonPressed ) )
        {
            stateDoc = currentStepState.getStateDoc();
            stepstateElem = stateDoc.getDocumentElement();

            // get content index (which content to move)
            Element contentidxElem = XMLTool.getElement( stepstateElem, "contentidx" );
            int contentIdx = Integer.parseInt( contentidxElem.getAttribute( "value" ) );

            // move content up/down
            Element sectionElem = XMLTool.getElement( stepstateElem, "section" );
            Element[] contentElems = XMLTool.getElements( sectionElem );
            if ( "moveup".equals( buttonPressed ) )
            {
                sectionElem.removeChild( contentElems[contentIdx] );
                if ( contentIdx > 0 )
                {
                    sectionElem.insertBefore( contentElems[contentIdx], contentElems[contentIdx - 1] );
                    Element tempElem = contentElems[contentIdx];
                    contentElems[contentIdx] = contentElems[contentIdx - 1];
                    contentElems[contentIdx - 1] = tempElem;
                }
                else
                {
                    sectionElem.appendChild( contentElems[0] );
                    Element tempElem = contentElems[0];
                    System.arraycopy( contentElems, 1, contentElems, 0, contentElems.length - 1 );
                    contentElems[contentElems.length - 1] = tempElem;
                }
            }
            else
            {
                sectionElem.removeChild( contentElems[contentIdx] );
                if ( contentIdx < contentElems.length - 1 )
                {
                    sectionElem.insertBefore( contentElems[contentIdx], contentElems[contentIdx + 1] );
                    Element tempElem = contentElems[contentIdx];
                    contentElems[contentIdx] = contentElems[contentIdx + 1];
                    contentElems[contentIdx + 1] = tempElem;
                }
                else
                {
                    sectionElem.insertBefore( contentElems[contentElems.length - 1], contentElems[0] );
                    Element tempElem = contentElems[contentElems.length - 1];
                    for ( int j = contentElems.length - 2; j >= 0; j-- )
                    {
                        contentElems[j + 1] = contentElems[j];
                    }
                    contentElems[0] = tempElem;
                }
            }

            int[] contentKeys = new int[contentElems.length];
            for ( int j = 0; j < contentElems.length; j++ )
            {
                contentKeys[j] = Integer.parseInt( contentElems[j].getAttribute( "key" ) );
            }

            Document doc = admin.getContentTitles( contentKeys ).getAsDOMDocument();
            wizarddataElem.appendChild( wizarddataDoc.importNode( doc.getDocumentElement(), true ) );
        }
        else
        {
            stateDoc = currentStepState.getStateDoc();
            stepstateElem = stateDoc.getDocumentElement();
            Element[] contentElems = XMLTool.getElements( stepstateElem, "content" );

            Element rootElem;
            if ( contentElems.length > 0 )
            {
                int[] contentKeys = new int[contentElems.length];
                for ( int j = 0; j < contentElems.length; j++ )
                {
                    contentKeys[j] = Integer.parseInt( contentElems[j].getAttribute( "key" ) );
                }

                Document doc = admin.getContentTitles( contentKeys ).getAsDOMDocument();
                rootElem = (Element) wizarddataDoc.importNode( doc.getDocumentElement(), true );
            }
            else
            {
                // get section contents
                MenuItemKey sectionKey = admin.getSectionKeyByMenuItemKey( menuItemKey );
                Document doc =
                    admin.getContentTitlesBySection( sectionKey, null, 0, Integer.MAX_VALUE, false, true ).getAsDOMDocument();

                // get content to add
                Document tempDoc = admin.getContentTitleXML( versionKey ).getAsDOMDocument();
                Element contenttitleElem = XMLTool.getFirstElement( tempDoc.getDocumentElement() );

                // add content to section contents
                rootElem = (Element) wizarddataDoc.importNode( doc.getDocumentElement(), true );

                final Element elem = XMLTool.getFirstElement( rootElem );
                final Node newChild = wizarddataDoc.importNode( contenttitleElem, true );

                final String selector = "//contenttitle[@key = '" + contenttitleElem.getAttribute( "key" ) + "']";
                final Element exist = XMLTool.selectElement( doc.getDocumentElement(), selector );

                if ( exist == null )
                {
                    if ( elem != null )
                    {
                        rootElem.insertBefore( newChild, elem ); // insert before first
                    }
                    else
                    {
                        rootElem.appendChild( newChild );
                    }
                }

            }
            wizarddataElem.appendChild( rootElem );
        }
    }

    // confirm publishing

    private void appendCustomDataStep3( User user, AdminService admin, WizardState wizardState, Document wizarddataDoc, int contentKey,
                                        int versionKey )
    {
        Element wizarddataElem = wizarddataDoc.getDocumentElement();

        // get content version
        final Document doc = admin.getContentVersion( user, versionKey ).getAsDOMDocument();
        wizarddataElem.appendChild( wizarddataDoc.importNode( doc.getDocumentElement(), true ) );

        // get step 1's menu and section keys
        Document stateDoc = wizardState.getStepState( "step1" ).getStateDoc();
        Element stepstateElem = stateDoc.getDocumentElement();
        Element[] menuElems = XMLTool.getElements( stepstateElem, "menu" );
        TIntArrayList menuKeyList = new TIntArrayList();
        if ( menuElems.length > 0 )
        {
            for ( Element menuElem : menuElems )
            {
                int menuKey = Integer.parseInt( menuElem.getAttribute( "key" ) );
                menuKeyList.add( menuKey );

                Element pagetemplateElem = XMLTool.getElement( menuElem, "pagetemplate" );
                if ( pagetemplateElem != null )
                {
                    int pageTemplateKey = Integer.parseInt( pagetemplateElem.getAttribute( "key" ) );
                    Document tempDoc = XMLTool.domparse( admin.getPageTemplate( pageTemplateKey ) );
                    Element pagetemplatesElem = XMLTool.getElement( wizarddataElem, "pagetemplates" );
                    if ( pagetemplatesElem != null )
                    {
                        pagetemplatesElem.appendChild(
                            wizarddataDoc.importNode( XMLTool.getFirstElement( tempDoc.getDocumentElement() ), true ) );
                    }
                    else
                    {
                        wizarddataElem.appendChild( wizarddataDoc.importNode( tempDoc.getDocumentElement(), true ) );
                    }
                }
            }

            // Added menu items
            final TIntArrayList menuItemKeys = getSelectedMenuItemKeys( stateDoc );
            final TIntArrayList displayMenuItemKeys = new TIntArrayList();

            final List<MenuItemEntity> menuItemList = getAccessibleMenuItems( user, menuKeyList.toArray() );

            for ( MenuItemEntity entity : menuItemList )
            {
                if ( !menuItemKeys.contains( entity.getKey().toInt() ) )
                {
                    menuItemKeys.add( entity.getKey().toInt() );
                    displayMenuItemKeys.add( entity.getKey().toInt() );
                }
            }

            // Added sections
            SectionCriteria criteria = new SectionCriteria();
            criteria.setMenuItemKeys( menuItemKeys.toArray() );
            criteria.setTreeStructure( false );
            criteria.setAppendAccessRights( false );
            criteria.setMarkContentFilteredSections( true );
            final int contentTypeKey = admin.getContentTypeKey( contentKey );
            criteria.setContentTypeKeyFilter( contentTypeKey );
            criteria.setIncludeSectionsWithoutContentTypeEvenWhenFilterIsSet( false );
            criteria.setIncludeSectionContentTypesInfo( false );
            final XMLDocument xmlSectionsDocument = admin.getSections( user, criteria );
            wizarddataElem.appendChild( wizarddataDoc.importNode( xmlSectionsDocument.getAsDOMDocument().getDocumentElement(), true ) );
            wizarddataElem.appendChild( wizarddataDoc.importNode( this.sectionsDoc.getDocumentElement(), true ) );

            final Map<Integer, Boolean> keys = Maps.newLinkedHashMap();
            final Map<String, Set<String>> siteToMenus = getSitesToMenusMap( this.sectionsDoc, wizardState );

            final TIntArrayList allKeysSet = new TIntArrayList();
            for ( final Set<String> menuItems : siteToMenus.values() )
            {
                for ( final String menuItem : menuItems )
                {
                    allKeysSet.add( Integer.parseInt( menuItem ) );
                }
            }
            final Set<Integer> previousMenuItemKeysSet = allKeysSet.toLinkedHashSet();
            allKeysSet.add( menuItemKeys.toArray() );
            final Set<Integer> currentMenuItemKeysSet = menuItemKeys.toLinkedHashSet();

            for ( final int key : allKeysSet.toArray() )
            {
                keys.put( key, !previousMenuItemKeysSet.contains( key ) || currentMenuItemKeysSet.contains( key ) );
            }

            final Document elementsToListDoc = createElementsToList( keys, displayMenuItemKeys.toLinkedHashSet() );
            wizarddataElem.appendChild( wizarddataDoc.importNode( elementsToListDoc.getDocumentElement(), true ) );
        }
    }

    // Previous sections
    private Document readSectionsWhereContentIsPublished( final AdminService admin, final User user, final int contentKey )
    {
        final SectionCriteria criteria = new SectionCriteria();
        criteria.setTreeStructure( false );
        criteria.setAppendAccessRights( false );
        criteria.setContentKey( contentKey );
        criteria.setMarkContentFilteredSections( true );
        criteria.setIncludeSectionsWithoutContentTypeEvenWhenFilterIsSet( false );
        criteria.setIncludeSectionContentTypesInfo( false );
        return admin.getSections( user, criteria ).getAsDOMDocument();
    }

    protected void saveState( WizardState wizardState, HttpServletRequest request, HttpServletResponse response, AdminService admin,
                              User user, ExtendedMap formItems )
        throws WizardException
    {
        // get step state document
        StepState stepState = wizardState.getCurrentStepState();
        Document stepstateDoc = stepState.getStateDoc();

        try
        {
            Step currentStep = wizardState.getCurrentStep();
            if ( "step0".equals( currentStep.getName() ) )
            {
                int contentKey;
                int versionKey = formItems.getInt( "versionkey", -1 );
                if ( versionKey < 0 )
                {
                    contentKey = formItems.getInt( "contentkey" );
                    versionKey = admin.getCurrentVersionKey( contentKey );
                    formItems.put( "versionkey", versionKey );
                }
                else
                {
                    contentKey = admin.getContentKeyByVersionKey( versionKey );
                    formItems.put( "contentkey", contentKey );
                }
                saveStateStep0( admin, stepstateDoc, formItems, contentKey, versionKey );
            }
            else if ( "step1".equals( currentStep.getName() ) )
            {
                final int versionKey = formItems.getInt( "versionkey", -1 );
                final int contentKey =
                    versionKey < 0 ? formItems.getInt( "contentkey" ) : admin.getContentKeyByVersionKey( versionKey );

                this.sectionsDoc = readSectionsWhereContentIsPublished( admin, user, contentKey );

                final Map<String, Set<String>> siteToMenus = getSitesToMenusMap( this.sectionsDoc, wizardState );

                saveStateStep1( wizardState, admin, stepstateDoc, formItems, siteToMenus );
            }
            else if ( "step2".equals( currentStep.getName() ) )
            {
                saveStateStep2( admin, stepstateDoc, formItems );
            }
        }
        catch ( ParseException pe )
        {
            String message = "Failed to parse a date: %t";
            WizardLogger.errorWizard( message, pe );
        }
    }

    private Map<String, Set<String>> getSitesToMenusMap( final Document sectionsDoc, final WizardState wizardState )
    {
        // get step 1's menu and section keys
        Document stateDoc = wizardState.getStepState( "step0" ).getStateDoc();
        Element stepstateElem = stateDoc.getDocumentElement();
        Element[] menuElems = XMLTool.getElements( stepstateElem, "menu" );
        TIntArrayList menuKeyList = new TIntArrayList();
        if ( menuElems.length > 0 )
        {
            for ( Element menuElem : menuElems )
            {
                int menuKey = Integer.parseInt( menuElem.getAttribute( "key" ) );
                menuKeyList.add( menuKey );
            }
        }

        final Element rootElem = sectionsDoc.getDocumentElement();
        final Element[] sections = XMLTool.getElements( rootElem, "section" );

        final Map<String, Set<String>> siteToMenus = Maps.newHashMap();
        for ( final Element section : sections )
        {
            final String siteKey = section.getAttribute( "menukey" );
            final String menuKey = section.getAttribute( "key" );

            final Set<Integer> sites = menuKeyList.toLinkedHashSet();

            // check that site was selected on step 0
            if ( sites.contains( Integer.parseInt( siteKey ) ) )
            {
                Set<String> menus = siteToMenus.get( siteKey );
                if ( menus == null )
                {
                    menus = Sets.newHashSet();
                    siteToMenus.put( siteKey, menus );
                }
                menus.add( menuKey );
            }
        }

        return siteToMenus;
    }

    // approval and site selection

    private void saveStateStep0( AdminService admin, Document stepstateDoc, ExtendedMap formItems, int contentKey, int versionKey )
        throws ParseException, WizardException
    {
        Element rootElem = stepstateDoc.getDocumentElement();

        Element contentElem = XMLTool.createElement( stepstateDoc, rootElem, "content" );
        contentElem.setAttribute( "key", String.valueOf( contentKey ) );

        // status
        int status = formItems.getInt( "status", -1 );
        if ( status >= 0 )
        {
            XMLTool.createElement( stepstateDoc, rootElem, "status", String.valueOf( status ) );
        }
        int originalStatus = admin.getContentStatus( versionKey );

        switch ( status )
        {
            case 0:
            {
                if ( originalStatus == 1 )
                {
                    // reject approval
                    saveRecipients( stepstateDoc, formItems );
                    saveMessage( stepstateDoc, formItems );
                }
                break;
            }
            case 1:
            {
                if ( originalStatus == 0 )
                {
                    // send to approval
                    saveRecipients( stepstateDoc, formItems );
                    saveMessage( stepstateDoc, formItems );
                }
                break;
            }
            case 2:
            {
                // publish from/to
                if ( formItems.containsKey( "datepublishfrom" ) || formItems.containsKey( "datepublishto" ) ||
                    formItems.containsKey( "publishfrom_now" ) )
                {
                    Element publishingElem = XMLTool.createElement( stepstateDoc, rootElem, "publishing" );
                    String date = formItems.getString( "datepublishfrom", null );
                    if ( date != null )
                    {
                        String time = formItems.getString( "timepublishfrom", null );
                        String datetime;
                        if ( time != null )
                        {
                            datetime = date + " " + time;
                        }
                        else
                        {
                            datetime = date + " 00:00";
                        }
                        Date publishFrom = DateUtil.parseDateTime( datetime );
                        publishingElem.setAttribute( "from", DateUtil.formatISODateTime( publishFrom ) );
                    }
                    date = formItems.getString( "datepublishto", null );
                    if ( date != null )
                    {
                        String time = formItems.getString( "timepublishto", null );
                        String datetime;
                        if ( time != null )
                        {
                            datetime = date + " " + time;
                        }
                        else
                        {
                            datetime = date + " 00:00";
                        }
                        Date publishto = DateUtil.parseDateTime( datetime );
                        publishingElem.setAttribute( "to", DateUtil.formatISODateTime( publishto ) );
                    }
                }
                break;
            }
            case 3:
            {
                // ignore, nothing to save
                break;
            }
            default:
            {
                if ( originalStatus != 2 )
                {
                    WizardLogger.errorWizard( "Unknown status: {0}", String.valueOf( status ) );
                }
                break;
            }
        }

        if ( status == 1 || status == 2 || ( originalStatus == 2 && status == -1 ) )
        {
            // sites
            String[] menuKeys = formItems.getStringArray( "menukey" );
            for ( String menuKey : menuKeys )
            {
                Element sectionElem = XMLTool.createElement( stepstateDoc, rootElem, "menu" );
                sectionElem.setAttribute( "key", menuKey );
            }
        }
    }

    private void saveRecipients( Document stepstateDoc, ExtendedMap formItems )
    {
        Element rootElem = stepstateDoc.getDocumentElement();
        Element recipientsElem = XMLTool.createElement( stepstateDoc, rootElem, "recipients" );
        String[] recipientKeys = formItems.getStringArray( "recipientkeys" );
        for ( String recipientKey : recipientKeys )
        {
            Element recipientElem = XMLTool.createElement( stepstateDoc, recipientsElem, "recipient" );
            recipientElem.setAttribute( "key", recipientKey );
            recipientElem.setAttribute( "name", formItems.getString( "name_" + recipientKey ) );
            recipientElem.setAttribute( "email", formItems.getString( "email_" + recipientKey ) );
        }
    }

    private void saveMessage( Document stepstateDoc, ExtendedMap formItems )
    {
//            if ( formItems.containsKey( "subject" )  )
//            {
        Element rootElem = stepstateDoc.getDocumentElement();
        Element messageElem = XMLTool.createElement( stepstateDoc, rootElem, "message" );
        //messageElem.setAttribute( "subject", formItems.getString( "subject" ) );
        if ( formItems.containsKey( "body" ) )
        {
            XMLTool.createCDATASection( stepstateDoc, messageElem, formItems.getString( "body" ) );
        }
//            }

    }

    // publishing

    private void saveStateStep1( WizardState wizardState, AdminService admin, Document stepstateDoc, ExtendedMap formItems,
                                 final Map<String, Set<String>> siteToMenus )
    {
        Document stateDoc = wizardState.getFirstStepState().getStateDoc();
        Element stepstateElem = stateDoc.getDocumentElement();
        Element[] menuElems = XMLTool.getElements( stepstateElem, "menu" );

        // select
        for ( Element menuElem1 : menuElems )
        {
            final String site = menuElem1.getAttribute( "key" );
            final int menuKey = Integer.parseInt( site );
            Element rootElem = stepstateDoc.getDocumentElement();
            Element menuElem = XMLTool.createElement( stepstateDoc, rootElem, "menu" );
            menuElem.setAttribute( "key", String.valueOf( menuKey ) );

            // framework
            if ( formItems.containsKey( "contentframework_" + menuKey ) )
            {
                Element pagetemplateElem = XMLTool.createElement( stepstateDoc, menuElem, "pagetemplate" );
                try
                {
                    int pageTemplateKey = Integer.parseInt( formItems.getString( "contentframework_" + menuKey ) );
                    pagetemplateElem.setAttribute( "key", String.valueOf( pageTemplateKey ) );
                }
                catch ( Exception e )
                {
                    ;
                }
            }

            String[] menuItemSelectedKeysAsArray = formItems.getStringArray( "menuitem_select_" + menuKey );
            List<String> toBePublishedMenuItems = Arrays.asList( menuItemSelectedKeysAsArray );

            String[] menuItemManuallyOrderKeysArray = formItems.getStringArray( "menuitem_manually_order_" + menuKey );
            List<String> menuItemManuallyOrderKeys = Arrays.asList( menuItemManuallyOrderKeysArray );

            Set<String> menuItems = Sets.newHashSet();
            final Set<String> alreadyPublishedMenuItems =
                Objects.firstNonNull( siteToMenus.get( site ), Collections.<String>emptySet() );
            menuItems.addAll( alreadyPublishedMenuItems );
            menuItems.addAll( toBePublishedMenuItems );

            for ( final String menuItem : menuItems )
            {
                final boolean isAmongAlreadyPublished = alreadyPublishedMenuItems.contains( menuItem );
                final boolean isAmongToBePublished = toBePublishedMenuItems.contains( menuItem );
                final boolean isChanged = isAmongAlreadyPublished ^ isAmongToBePublished;
                final boolean isPublish = !isAmongAlreadyPublished || isAmongToBePublished;

                final Action change = isAmongAlreadyPublished ? Action.remove : Action.add;
                final Action action = isChanged ? change : Action.none;

                final boolean manuallyOrder = menuItemManuallyOrderKeys.contains( menuItem );

                final Element menuItemElem = XMLTool.createElement( stepstateDoc, menuElem, "menuitem" );

                menuItemElem.setAttribute( "key", menuItem );
                menuItemElem.setAttribute( "publish", String.valueOf( isPublish ) ); // show on wizard page as checked
                menuItemElem.setAttribute( "selected", String.valueOf( isAmongToBePublished ) );
                menuItemElem.setAttribute( "action", String.valueOf( action ) );
                menuItemElem.setAttribute( "manuallyOrder", String.valueOf( manuallyOrder ) );

                MenuItemKey menuItemKey = new MenuItemKey( menuItem );
                MenuItemKey sectionKey = admin.getSectionKeyByMenuItemKey( menuItemKey );
                menuItemElem.setAttribute( "ordered", String.valueOf( admin.isSectionOrdered( sectionKey.toInt() ) ) );
            }

            // home
            String homeKey = formItems.getString( "menuitem_home_" + menuKey, null );
            if ( homeKey != null )
            {
                Element homeElem = XMLTool.createElement( stepstateDoc, menuElem, "home" );
                homeElem.setAttribute( "key", homeKey );
            }
        }
    }

    // position content in section

    private void saveStateStep2( AdminService admin, Document stepstateDoc, ExtendedMap formItems )
    {
        Element rootElem = stepstateDoc.getDocumentElement();
        Element sectionElem = XMLTool.createElement( stepstateDoc, rootElem, "section" );
        MenuItemKey menuItemKey = new MenuItemKey( formItems.getString( "menuitemkey" ) );
        MenuItemKey sectionKey = admin.getSectionKeyByMenuItemKey( menuItemKey );
        sectionElem.setAttribute( "key", sectionKey.toString() );

        if ( formItems.containsKey( "contentidx" ) )
        {
            Element elem = XMLTool.createElement( stepstateDoc, rootElem, "contentidx" );
            elem.setAttribute( "value", formItems.getString( "contentidx", null ) );
        }

        String[] contentKeys = formItems.getStringArray( "content" );
        for ( String contentKey : contentKeys )
        {
            Element contentElem = XMLTool.createElement( stepstateDoc, sectionElem, "content" );
            contentElem.setAttribute( "key", contentKey );
        }
    }

    private boolean moreOrder( User user, WizardState wizardState, AdminService admin )
    {
        StepState stepState = wizardState.getCurrentStepState();
        NormalStep step = stepState.getStep();
        int sectionIndex = 0;
        while ( !"step1".equals( step.getName() ) )
        {
            stepState = stepState.getPreviousStepState();
            step = stepState.getStep();
            sectionIndex++;
        }

        Document stateDoc = stepState.getStateDoc();
        Element[] menuElems = XMLTool.getElements( stateDoc.getDocumentElement(), "menu" );

        int idx = 0;
        for ( Element menuElem : menuElems )
        {
            Element[] menuitemElems = XMLTool.getElements( menuElem, "menuitem" );
            for ( Element menuitemElem : menuitemElems )
            {
                boolean manuallyOrder = Boolean.valueOf( menuitemElem.getAttribute( "manuallyOrder" ) );
                boolean ordered = Boolean.valueOf( menuitemElem.getAttribute( "ordered" ) );
                MenuItemKey menuItemKey = new MenuItemKey( Integer.parseInt( menuitemElem.getAttribute( "key" ) ) );
                MenuItemAccessRight menuItemAccessRight = admin.getMenuItemAccessRight( user, menuItemKey );
                if ( manuallyOrder && ordered && menuItemAccessRight.getPublish() )
                {
                    if ( idx == sectionIndex )
                    {
                        return true;
                    }
                    else
                    {
                        idx++;
                    }
                }
            }
        }
        return false;
    }

    private boolean noSites( WizardState wizardState )
    {
        Document stepstateDoc = wizardState.getCurrentStepState().getStateDoc();
        Element stepstateElem = stepstateDoc.getDocumentElement();
        Element[] menuElems = XMLTool.getElements( stepstateElem, "menu" );
        return menuElems.length == 0;
    }

    private Document createElementsToList( final Map<Integer, Boolean> menuItemKeys, final Set<Integer> displayMenuItemKeys )
    {
        final MenuItemXmlCreator creator = getMenuItemXmlCreator();

        final org.jdom.Document doc = new org.jdom.Document();
        final org.jdom.Element rootEl = new org.jdom.Element( "menus" );
        doc.setRootElement( rootEl );

        final Map<SiteKey, org.jdom.Element> siteElSiteKeyMap = new HashMap<SiteKey, org.jdom.Element>();

        for ( final Map.Entry<Integer, Boolean> entry : menuItemKeys.entrySet() )
        {
            final Integer menuItemKey = entry.getKey();
            final Boolean menuItemKeyChecked = entry.getValue();

            final MenuItemEntity menuItem = menuItemDao.findByKey( new MenuItemKey( menuItemKey ) );
            final SiteEntity site = menuItem.getSite();
            final SiteKey siteKey = site.getKey();

            final org.jdom.Element siteEl;
            if ( siteElSiteKeyMap.containsKey( siteKey ) )
            {
                siteEl = siteElSiteKeyMap.get( siteKey );
            }
            else
            {
                siteEl = createSiteElement( site );
                siteElSiteKeyMap.put( siteKey, siteEl );
                rootEl.addContent( siteEl );
            }

            final org.jdom.Element menuItemElement = creator.createMenuItemElement( menuItem );
            menuItemElement.setAttribute( "publish", menuItemKeyChecked.toString() );
            menuItemElement.setAttribute( "none", "" + displayMenuItemKeys.contains( menuItemKey ) );

            siteEl.addContent( menuItemElement );
        }

        return XMLDocumentFactory.create( doc ).getAsDOMDocument();
    }

    private Document createElementsToList( final List<MenuItemEntity> menuItems, final Set<String> menus, final boolean firstStep )
    {
        final MenuItemXmlCreator creator = getMenuItemXmlCreator();

        final org.jdom.Document doc = new org.jdom.Document();
        final org.jdom.Element rootEl = new org.jdom.Element( "menus" );
        doc.setRootElement( rootEl );

        final Map<SiteKey, org.jdom.Element> siteElSiteKeyMap = new HashMap<SiteKey, org.jdom.Element>();

        for ( final MenuItemEntity menuItem : menuItems )
        {
            final SiteEntity site = menuItem.getSite();
            final SiteKey siteKey = site.getKey();

            final org.jdom.Element siteEl;
            if ( siteElSiteKeyMap.containsKey( siteKey ) )
            {
                siteEl = siteElSiteKeyMap.get( siteKey );
            }
            else
            {
                siteEl = createSiteElement( site );
                siteElSiteKeyMap.put( siteKey, siteEl );
                rootEl.addContent( siteEl );
            }

            final org.jdom.Element menuItemElement = creator.createMenuItemElement( menuItem );

            if ( firstStep )
            {
                menuItemElement.setAttribute( "publish", "true" );
            }
            else
            {
                final String menuItemKey = String.valueOf( menuItem.getKey() );
                menuItemElement.setAttribute( "publish", String.valueOf( menus.contains( menuItemKey ) ) );
            }

            siteEl.addContent( menuItemElement );
        }

        return XMLDocumentFactory.create( doc ).getAsDOMDocument();
    }


    private MenuItemXmlCreator getMenuItemXmlCreator()
    {
        final MenuItemXMLCreatorSetting setting = new MenuItemXMLCreatorSetting();
        setting.includeTypeSpecificXML = false;
        final MenuItemXmlCreator creator = new MenuItemXmlCreator( setting, null );
        creator.setIncludePathInfo( true );
        return creator;
    }

    private org.jdom.Element createSiteElement( final SiteEntity site )
    {
        final org.jdom.Element siteEl = new org.jdom.Element( "menu" );
        siteEl.setAttribute( "key", site.getKey().toString() );
        siteEl.setAttribute( "name", site.getName() );
        return siteEl;
    }

    private List<MenuItemEntity> getAccessibleMenuItems( final User oldUser, final int[] menuKeys )
    {
        final List<MenuItemEntity> menuItemsOfTypeSection = new ArrayList<MenuItemEntity>();

        for ( final int menuKey : menuKeys )
        {
                /* Sections */
            final MenuItemSpecification specSection = new MenuItemSpecification();
            specSection.setSiteKey( new SiteKey( menuKey ) );
            specSection.setType( MenuItemType.SECTION );
            menuItemsOfTypeSection.addAll( menuItemDao.findBySpecification( specSection ) );

                /* Page Section */
            final MenuItemSpecification specSectionPage = new MenuItemSpecification();
            specSectionPage.setSiteKey( new SiteKey( menuKey ) );
            specSectionPage.setPageSpecification( new PageSpecification() );
            specSectionPage.getPageSpecification().setTemplateSpecification( new PageTemplateSpecification() );
            specSectionPage.getPageSpecification().getTemplateSpecification().setType( PageTemplateType.SECTIONPAGE );
            menuItemsOfTypeSection.addAll( menuItemDao.findBySpecification( specSectionPage ) );

                /* Newsletter Section */
            final MenuItemSpecification newsletterSectionSpec = new MenuItemSpecification();
            newsletterSectionSpec.setSiteKey( new SiteKey( menuKey ) );
            newsletterSectionSpec.setPageSpecification( new PageSpecification() );
            newsletterSectionSpec.getPageSpecification().setTemplateSpecification( new PageTemplateSpecification() );
            newsletterSectionSpec.getPageSpecification().getTemplateSpecification().setType( PageTemplateType.NEWSLETTER );
            menuItemsOfTypeSection.addAll( menuItemDao.findBySpecification( newsletterSectionSpec ) );
        }

        final UserEntity newUser = securityService.getUser( oldUser );
        final List<MenuItemEntity> accessibleMenuItems = new ArrayList<MenuItemEntity>();
        MenuItemAccessResolver menuItemAccessResolver = new MenuItemAccessResolver( groupDao );
        for ( final MenuItemEntity menuItem : menuItemsOfTypeSection )
        {
            if ( menuItemAccessResolver.hasAccess( newUser, menuItem, MenuItemAccessType.PUBLISH ) ||
                menuItemAccessResolver.hasAccess( newUser, menuItem, MenuItemAccessType.ADD ) )
            {
                accessibleMenuItems.add( menuItem );
            }
        }
        return accessibleMenuItems;
    }

    private TIntArrayList getSelectedMenuItemKeys( final Document stateDoc )
    {
        final TIntArrayList menuItemKeys = new TIntArrayList();
        final Element[] menuEls = XMLTool.getElements( stateDoc.getDocumentElement(), "menu" );
        for ( final Element menuEl : menuEls )
        {
            final Element[] menuItemEls = XMLTool.getElements( menuEl, "menuitem" );
            for ( final Element menuItemEl : menuItemEls )
            {
                final int menuItemKey = Integer.parseInt( menuItemEl.getAttribute( "key" ) );
                final boolean selected = Boolean.parseBoolean( menuItemEl.getAttribute( "selected" ) );
                if ( selected )
                {
                    menuItemKeys.add( menuItemKey );
                }
            }
        }
        return menuItemKeys;
    }
}