package com.enonic.vertical.adminweb.wizard;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.Sets;
import com.google.common.io.Files;

import com.enonic.esl.containers.ExtendedMap;
import com.enonic.esl.io.FileUtil;
import com.enonic.esl.util.DateUtil;
import com.enonic.esl.xml.XMLTool;
import com.enonic.vertical.adminweb.VerticalAdminException;
import com.enonic.vertical.adminweb.handlers.ContentBaseHandlerServlet;
import com.enonic.vertical.adminweb.handlers.ContentEnhancedImageHandlerServlet;
import com.enonic.vertical.adminweb.handlers.ContentFileHandlerServlet;
import com.enonic.vertical.engine.CategoryAccessRight;
import com.enonic.vertical.engine.ContentAccessRight;
import com.enonic.vertical.engine.VerticalEngineException;

import com.enonic.cms.core.content.AssignmentDataParser;
import com.enonic.cms.core.content.ContentAndVersion;
import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentParserService;
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.content.ContentStatus;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.PageCacheInvalidatorForContent;
import com.enonic.cms.core.content.UpdateContentResult;
import com.enonic.cms.core.content.access.ContentAccessEntity;
import com.enonic.cms.core.content.binary.BinaryData;
import com.enonic.cms.core.content.binary.BinaryDataAndBinary;
import com.enonic.cms.core.content.category.CategoryEntity;
import com.enonic.cms.core.content.category.CategoryKey;
import com.enonic.cms.core.content.category.CategoryService;
import com.enonic.cms.core.content.category.StoreNewCategoryCommand;
import com.enonic.cms.core.content.command.AssignContentCommand;
import com.enonic.cms.core.content.command.CreateContentCommand;
import com.enonic.cms.core.content.command.UnassignContentCommand;
import com.enonic.cms.core.content.command.UpdateContentCommand;
import com.enonic.cms.core.content.contenttype.ContentTypeEntity;
import com.enonic.cms.core.content.mail.ImportedContentAssignmentMailTemplate;
import com.enonic.cms.core.mail.MailRecipient;
import com.enonic.cms.core.mail.SendMailService;
import com.enonic.cms.core.portal.cache.PageCacheService;
import com.enonic.cms.core.security.SecurityService;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserKey;
import com.enonic.cms.core.service.AdminService;
import com.enonic.cms.core.util.LoggingUtil;
import com.enonic.cms.store.dao.CategoryDao;
import com.enonic.cms.store.dao.ContentDao;
import com.enonic.cms.store.dao.UserDao;

public abstract class ImportZipWizard
    extends Wizard
{
    @Autowired
    private transient ContentDao contentDao;

    @Autowired
    private transient UserDao userDao;

    @Autowired
    private transient CategoryDao categoryDao;

    @Autowired
    private transient SendMailService sendMailService;

    @Autowired
    private transient ContentService contentService;

    @Autowired
    private transient SecurityService securityService;

    @Autowired
    private transient ContentParserService contentParserService;

    @Autowired
    private transient PageCacheService pageCacheService;

    @Autowired
    private transient CategoryService categoryService;

    private int[] imageContentTypes;

    private int[] fileContentTypes;

    private String zipExcludePattern;

    @Value("${cms.admin.zipimport.excludePattern}")
    public void setZipExcludePattern( final String zipExcludePattern )
    {
        this.zipExcludePattern = zipExcludePattern;
    }

    protected void initialize( AdminService admin, Document wizardconfigDoc )
        throws WizardException
    {
        // fetch image and file content types
        imageContentTypes = admin.getContentTypeKeysByHandler( ContentEnhancedImageHandlerServlet.class.getName() );
        Arrays.sort( imageContentTypes );
        fileContentTypes = admin.getContentTypeKeysByHandler( ContentFileHandlerServlet.class.getName() );
        Arrays.sort( fileContentTypes );
    }

    private void readObject( ObjectInputStream in )
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
    }

    protected boolean evaluate( WizardState wizardState, HttpSession session, AdminService admin, ExtendedMap formItems,
                                String testCondition )
        throws WizardException
    {
        // no conditions defined
        return false;
    }

    protected void appendCustomData( WizardState wizardState, HttpSession session, AdminService admin, ExtendedMap formItems,
                                     ExtendedMap parameters, User user, Document dataconfigDoc, Document wizarddataDoc )
        throws WizardException
    {
        int categoryKey = formItems.getInt( "cat" );
        String categoryName = admin.getCategoryName( categoryKey );
        formItems.put( "categoryname", categoryName );
        int unitKey = admin.getUnitKey( categoryKey );
        formItems.put( "selectedunitkey", unitKey );
        Document xmlCat = admin.getSuperCategoryNames( categoryKey, false, true ).getAsDOMDocument();
        XMLTool.mergeDocuments( wizarddataDoc, xmlCat, true );
    }

    protected boolean validateState( WizardState wizardState, HttpSession session, AdminService admin, ExtendedMap formItems )
    {
        boolean validState;
        String currentStepName = wizardState.getCurrentStep().getName();
        Document stepstateDoc = wizardState.getCurrentStepState().getStateDoc();
        if ( "step0".equals( currentStepName ) )
        {
            if ( !wizardState.hasErrors() )
            {
                Element zipElem = XMLTool.getElement( stepstateDoc.getDocumentElement(), "zip" );
                if ( zipElem == null || !zipElem.hasAttribute( "dir" ) )
                {
                    validState = false;
                    wizardState.addError( "6", "zipfile" );
                }
                else
                {
                    validState = true;
                }
            }
            else
            {
                validState = false;
            }
        }
        else if ( "step1".equals( currentStepName ) )
        {
            validState = false;
            for ( Object o : formItems.keySet() )
            {
                String key = (String) o;
                String entryPrefix = "entry_0_";
                if ( key.startsWith( entryPrefix ) )
                {
                    validState = true;
                    break;
                }
            }
            if ( !validState )
            {
                wizardState.addError( "10", "files" );
            }
        }
        else
        {
            validState = true;
        }

        return validState;
    }

    protected void saveState( WizardState wizardState, HttpServletRequest request, HttpServletResponse response, AdminService admin,
                              User user, ExtendedMap formItems )
        throws WizardException
    {
        super.saveState( wizardState, request, response, admin, user, formItems );

        String stepName = wizardState.getCurrentStep().getName();
        // step 0: unzip zip file and create temporary directory
        if ( "step0".equals( stepName ) )
        {
            StepState stepState = wizardState.getCurrentStepState();
            Document stateDoc = stepState.getStateDoc();
            Element stepstateElem = stateDoc.getDocumentElement();
            try
            {
                FileItem zipFileItem = formItems.getFileItem( "zipfile", null );
                String zipFileName = zipFileItem.getName();
                if ( !zipFileName.endsWith( ".zip" ) )
                {
                    wizardState.addError( "11", "zipfile" );
                }

                File dir = Files.createTempDir();
                FileUtil.inflateZipFile( zipFileItem.getInputStream(), dir, zipExcludePattern );
                File[] files = dir.listFiles();
                Element zipElem = XMLTool.createElement( stateDoc, stepstateElem, "zip" );
                zipElem.setAttribute( "dir", dir.getAbsolutePath() );
                int categoryKey = formItems.getInt( "cat" );
                CategoryAccessRight categoryAccessRight = admin.getCategoryAccessRight( user, categoryKey );
                boolean adminRight = categoryAccessRight.getAdministrate();
                if ( adminRight )
                {
                    zipElem.setAttribute( "admin", "true" );
                }
                int contentTypeKey = admin.getContentTypeKeyByCategory( categoryKey );
                boolean someExists = filesToXML( zipElem, files, admin, user, categoryKey, adminRight, contentTypeKey );
                if ( !someExists )
                {
                    zipElem.setAttribute( "allchecked", "true" );
                }
            }
            catch ( IOException ioe )
            {
                String message = "Failed to inflate zip file: %t";
                WizardLogger.error( message, ioe );
                wizardState.addError( "12", "zipfile", LoggingUtil.formatCause( message, ioe ) );
            }
        }
        // update zip structure from first state
        else if ( "step1".equals( stepName ) )
        {
            Document firstStateDoc = wizardState.getFirstStepState().getStateDoc();
            Element firstZipElem = XMLTool.getFirstElement( firstStateDoc.getDocumentElement() );
            Document stateDoc = wizardState.getCurrentStepState().getStateDoc();
            Element zipElem = XMLTool.getFirstElement( stateDoc.getDocumentElement() );
            zipElem.setAttribute( "dir", firstZipElem.getAttribute( "dir" ) );

            boolean publish = ContentStatus.APPROVED.getKey() == Integer.parseInt( zipElem.getAttribute( "publish" ) );
            if ( publish )
            {
                if ( formItems.containsKey( "date_publishfrom" ) )
                {
                    Element publishfromElem = XMLTool.createElement( stateDoc, zipElem, "publishfrom" );
                    publishfromElem.setAttribute( "date", formItems.getString( "date_publishfrom" ) );
                    publishfromElem.setAttribute( "time", formItems.getString( "time_publishfrom" ) );

                }
                if ( formItems.containsKey( "date_publishto" ) )
                {
                    Element publishfromElem = XMLTool.createElement( stateDoc, zipElem, "publishto" );
                    publishfromElem.setAttribute( "date", formItems.getString( "date_publishto" ) );
                    publishfromElem.setAttribute( "time", formItems.getString( "time_publishto" ) );
                }
            }

            Map<String, Element> entryMap = new HashMap<String, Element>();
            for ( Object o : formItems.keySet() )
            {
                String key = (String) o;
                String entryPrefix = "entry_0_";
                if ( key.startsWith( entryPrefix ) )
                {
                    StringBuilder entryId = new StringBuilder( entryPrefix );
                    StringTokenizer positions = new StringTokenizer( key.substring( 8 ), "_" );

                    Element parentElem = zipElem;
                    Element[] entryElems = XMLTool.getElements( firstZipElem );
                    while ( positions.hasMoreTokens() )
                    {
                        int position = Integer.parseInt( positions.nextToken() ) - 1;
                        entryId.append( "_" );
                        entryId.append( position );
                        Element entryElem = entryMap.get( entryId.toString() );
                        if ( entryElem == null )
                        {
                            entryElem = XMLTool.createElement( stateDoc, parentElem, "entry" );
                            entryElem.setAttribute( "type", entryElems[position].getAttribute( "type" ) );
                            entryElem.setAttribute( "name", entryElems[position].getAttribute( "name" ) );
                            if ( entryElems[position].hasAttribute( "exists" ) )
                            {
                                entryElem.setAttribute( "exists", entryElems[position].getAttribute( "exists" ) );
                            }
                            entryMap.put( entryId.toString(), entryElem );
                        }

                        parentElem = entryElem;
                        entryElems = XMLTool.getElements( entryElems[position] );
                    }
                }
            }
        }
    }

    /**
     * @see com.enonic.vertical.adminweb.wizard.Wizard#cancelClicked(com.enonic.vertical.adminweb.wizard.Wizard.WizardState)
     */
    protected void cancelClicked( WizardState wizardState )
    {
        cleanup( wizardState );
    }

    private void cleanup( WizardState wizardState )
    {
        StepState firstStepState = wizardState.getFirstStepState();
        if ( firstStepState != null )
        {
            Document stateDoc = firstStepState.getStateDoc();
            Element zipElem = XMLTool.getFirstElement( stateDoc.getDocumentElement() );
            if ( zipElem != null )
            {
                File dir = new File( zipElem.getAttribute( "dir" ) );

                try
                {
                    FileUtils.deleteDirectory( dir );
                }
                catch ( final Exception e )
                {
                    // Do nothing
                }
            }
        }
    }

    private boolean filesToXML( Element root, File[] files, AdminService admin, User user, int categoryKey, boolean adminRight,
                                int contentTypeKey )
    {
        boolean someExists = false;
        for ( File file : files )
        {
            someExists |= fileToXML( root, file, admin, user, categoryKey, adminRight, contentTypeKey );
        }
        return someExists;
    }

    private boolean fileToXML( Element root, File file, AdminService admin, User user, int superCategoryKey, boolean superAdminRight,
                               int superContentTypeKey )
    {
        Document doc = root.getOwnerDocument();
        Element entryElem = XMLTool.createElement( doc, root, "entry" );
        String name = file.getName();
        entryElem.setAttribute( "name", name );
        boolean someExists;
        if ( file.isDirectory() )
        {
            entryElem.setAttribute( "type", "dir" );

            boolean adminRight;
            int contentTypeKey;
            int categoryKey = ( superCategoryKey >= 0 ? admin.getCategoryKey( superCategoryKey, name ) : -1 );
            if ( categoryKey >= 0 )
            {
                entryElem.setAttribute( "exists", "true" );
                contentTypeKey = admin.getContentTypeKeyByCategory( categoryKey );

                // check rights
                CategoryAccessRight categoryAccessRight = admin.getCategoryAccessRight( user, categoryKey );
                adminRight = categoryAccessRight.getAdministrate();
            }
            else
            {
                adminRight = superAdminRight;
                contentTypeKey = superContentTypeKey;
            }

            if ( adminRight )
            {
                entryElem.setAttribute( "admin", "true" );
            }
            String contentType = getContentTypeInternal( contentTypeKey );
            if ( contentType != null )
            {
                entryElem.setAttribute( "contenttype", contentType );
            }

            someExists = filesToXML( entryElem, file.listFiles(), admin, user, categoryKey, adminRight, contentTypeKey );
            if ( !someExists )
            {
                entryElem.setAttribute( "allchecked", "true" );
            }
        }
        else
        {
            entryElem.setAttribute( "type", "file" );
            if ( isFiltered( name ) )
            {
                entryElem.setAttribute( "filtered", "true" );
            }
            if ( superCategoryKey >= 0 && admin.contentExists( superCategoryKey, cropName( name ) ) )
            {
                entryElem.setAttribute( "exists", "true" );
                someExists = true;

                int contentKey = admin.getContentKey( superCategoryKey, cropName( name ) );
                ContentAccessRight contentAccessRight = admin.getContentAccessRight( user, contentKey );
                if ( contentAccessRight.getUpdate() )
                {
                    entryElem.setAttribute( "update", "true" );
                }
            }
            else
            {
                someExists = false;
                if ( superAdminRight )
                {
                    entryElem.setAttribute( "update", "true" );
                }
            }
        }
        return someExists;
    }

    protected abstract boolean isFiltered( String name );

    private String getContentTypeInternal( int contentTypeKey )
    {
        String contentType;
        if ( Arrays.binarySearch( imageContentTypes, contentTypeKey ) >= 0 )
        {
            contentType = "image";
        }
        else if ( Arrays.binarySearch( fileContentTypes, contentTypeKey ) >= 0 )
        {
            contentType = "file";
        }
        else
        {
            contentType = null;
        }
        return contentType;
    }

    protected void processWizardData( WizardState wizardState, HttpSession session, AdminService admin, ExtendedMap formItems, User user,
                                      Document dataDoc )
        throws VerticalAdminException, VerticalEngineException
    {
        Document stateDoc = wizardState.getStepState( "step1" ).getStateDoc();
        Element zipElem = XMLTool.getFirstElement( stateDoc.getDocumentElement() );
        File dir = new File( zipElem.getAttribute( "dir" ) );

        // if content is to be published, set publish dates
        final String importContentStatus = zipElem.getAttribute( "publish" );
        boolean doPublish = importContentStatus.equals( Integer.toString( ContentStatus.APPROVED.getKey() ) );

        if ( doPublish )
        {
            formItems.put( "importedContentStatus", 2 );
            formItems.put( "published", true );
            Element publishfromElem = XMLTool.getElement( zipElem, "publishfrom" );
            if ( publishfromElem != null )
            {
                formItems.put( "date_pubdata_publishfrom", publishfromElem.getAttribute( "date" ) );
                formItems.put( "time_pubdata_publishfrom", publishfromElem.getAttribute( "time" ) );
            }
            Element publishtoElem = XMLTool.getElement( zipElem, "publishto" );
            if ( publishtoElem != null )
            {
                formItems.put( "date_pubdata_publishto", publishtoElem.getAttribute( "date" ) );
                formItems.put( "time_pubdata_publishto", publishtoElem.getAttribute( "time" ) );
            }
        }
        else
        {
            formItems.put( "importedContentStatus", 0 );
        }

        // save files
        Element[] entryElems = XMLTool.getElements( zipElem, "entry" );
        int categoryKey = formItems.getInt( "cat" );
        int unitKey = admin.getUnitKey( categoryKey );
        formItems.put( "selectedunitkey", unitKey );
        ContentBaseHandlerServlet cbhServlet = (ContentBaseHandlerServlet) servlet;

        Set<ContentKey> assignedContent = Sets.newHashSet();

        AssignmentDataParser assignmentDataParser = new AssignmentDataParser( formItems );
        String assigneeKey = assignmentDataParser.getAssigneeKey();

        saveEntries( user, admin, formItems, cbhServlet, categoryKey, entryElems, dir, assignmentDataParser, assignedContent );

        if ( assignedContent.size() > 0 && StringUtils.isNotBlank( assigneeKey ) )
        {
            sendImportedContentAssignedMail( user, assignedContent, assignmentDataParser, assigneeKey );
        }

        // clean up
        cleanup( wizardState );
    }

    private StoreNewCategoryCommand createStoreNewCategoryCommand( User user, int superCategoryKey, String fileName )
    {
        CategoryEntity parentCategory = categoryDao.findByKey( new CategoryKey( superCategoryKey ) );

        StoreNewCategoryCommand command = new StoreNewCategoryCommand();
        command.setCreator( user.getKey() );
        command.setParentCategory( parentCategory.getKey() );
        command.setName( fileName );
        command.setDescription( null );
        command.setAutoApprove( parentCategory.getAutoMakeAvailableAsBoolean() );
        ContentTypeEntity contentType = parentCategory.getContentType();
        if ( contentType != null )
        {
            command.setContentType( contentType.getContentTypeKey() );
        }
        return command;
    }

    private void sendImportedContentAssignedMail( User user, Set<ContentKey> assignedContent, AssignmentDataParser assignmentDataParser,
                                                  String assigneeKey )
    {
        ImportedContentAssignmentMailTemplate mailTemplate = new ImportedContentAssignmentMailTemplate( assignedContent, contentDao );
        mailTemplate.setAssignmentDescription( assignmentDataParser.getAssignmentDescription() );
        mailTemplate.setAssignmentDueDate( assignmentDataParser.getAssignmentDueDate() );

        UserEntity assigner = userDao.findByKey( user.getKey() );
        mailTemplate.setAssigner( assigner );

        mailTemplate.setFrom( new MailRecipient( user.getDisplayName(), user.getEmail() ) );

        UserEntity assignee = userDao.findByKey( assigneeKey );

        mailTemplate.addRecipient( new MailRecipient( assignee.getName(), assignee.getEmail() ) );
        sendMailService.sendMail( mailTemplate );
    }

    private void saveEntries( User user, AdminService admin, ExtendedMap oldFormItems, ContentBaseHandlerServlet cbhServlet,
                              int superCategoryKey, Element[] entryElems, File parentDir, AssignmentDataParser assignmentDataParser,
                              Set<ContentKey> assignedContent )
    {
        oldFormItems.put( "newimage", true );

        for ( Element entryElem : entryElems )
        {
            ExtendedMap formItems = new ExtendedMap( oldFormItems );

            ContentKey storedContentKey = null;

            String fileName = entryElem.getAttribute( "name" );
            String fileType = entryElem.getAttribute( "type" );
            ContentStatus newVersionStatus = ContentStatus.get( formItems.getInt( "importedContentStatus" ) );

            boolean contentExists = Boolean.valueOf( entryElem.getAttribute( "exists" ) );
            boolean hasAssignee = assignmentDataParser.getAssigneeKey() != null;

            if ( "dir".equals( fileType ) )
            {
                File dir = new File( parentDir, fileName );
                int categoryKey;
                if ( contentExists )
                {
                    categoryKey = admin.getCategoryKey( superCategoryKey, fileName );
                }
                else
                {
                    StoreNewCategoryCommand command = createStoreNewCategoryCommand( user, superCategoryKey, fileName );
                    categoryKey = categoryService.storeNewCategory( command ).toInt();
                }
                saveEntries( user, admin, formItems, cbhServlet, categoryKey, XMLTool.getElements( entryElem ), dir, assignmentDataParser,
                             assignedContent );
            }
            else
            {
                File file = new File( parentDir, fileName );
                BinaryData[] binaries = getBinaries( cbhServlet, admin, formItems, file );

                formItems.put( cbhServlet.getContentXMLBuilder().getTitleFormKey(), cropName( fileName ) );

                formItems.put( "cat", superCategoryKey );
                if ( contentExists )
                {
                    int contentKey = admin.getContentKey( superCategoryKey, cropName( fileName ) );
                    int versionKey = admin.getCurrentVersionKey( contentKey );
                    formItems.put( "createnewversion", true );
                    formItems.put( "key", contentKey );
                    formItems.put( "versionkey", versionKey );
                    formItems.put( "_pubdata_created", admin.getContentCreatedTimestamp( contentKey ) );

                    boolean published = formItems.getBoolean( "published", false );
                    if ( !published )
                    {
                        Date publishFrom = admin.getContentPublishFromTimestamp( contentKey );
                        if ( publishFrom != null )
                        {
                            String dateString = DateUtil.formatDateTime( publishFrom );
                            formItems.put( "date_pubdata_publishfrom", dateString.substring( 0, 10 ) );
                            formItems.put( "time_pubdata_publishfrom", dateString.substring( 11 ) );

                            Date publishTo = admin.getContentPublishToTimestamp( contentKey );
                            if ( publishTo != null )
                            {
                                dateString = DateUtil.formatDateTime( publishTo );
                                formItems.put( "date_pubdata_publishto", dateString.substring( 0, 10 ) );
                                formItems.put( "time_pubdata_publishto", dateString.substring( 11 ) );
                            }
                        }
                    }

                    int versionState = admin.getContentVersionState( versionKey );
                    boolean setCurrentVersion = ( versionState < 2 ) || published;
                    String xmlData = cbhServlet.getContentXMLBuilder().buildXML( formItems, user, false, false, false );

                    UpdateContentResult result =
                        updateContent( user, xmlData, BinaryDataAndBinary.createNewFrom( binaries ), setCurrentVersion );

                    storedContentKey = result.getTargetedVersion().getContent().getKey();
                }
                else
                {
                    String xmlData = cbhServlet.getContentXMLBuilder().buildXML( formItems, user, true, false, false );
                    storedContentKey = storeNewContent( user, binaries, xmlData );
                }
            }

            if ( storedContentKey != null )
            {

                final boolean doAssignContent = newVersionStatus.equals( ContentStatus.DRAFT ) && hasAssignee;

                if ( doAssignContent )
                {
                    AssignContentCommand assignContentCommand = new AssignContentCommand();
                    assignContentCommand.setAssigneeKey( new UserKey( assignmentDataParser.getAssigneeKey() ) );
                    assignContentCommand.setAssignerKey( user.getKey() );
                    assignContentCommand.setContentKey( storedContentKey );
                    assignContentCommand.setAssignmentDescription( assignmentDataParser.getAssignmentDescription() );
                    assignContentCommand.setAssignmentDueDate( assignmentDataParser.getAssignmentDueDate() );

                    contentService.assignContent( assignContentCommand );

                    assignedContent.add( storedContentKey );
                }
                else
                {
                    ContentEntity storedContent = contentDao.findByKey( storedContentKey );

                    if ( storedContent.isAssigned() )
                    {
                        UnassignContentCommand unassignContentCommand = new UnassignContentCommand();
                        unassignContentCommand.setContentKey( storedContentKey );
                        unassignContentCommand.setUnassigner( user.getKey() );

                        contentService.unassignContent( unassignContentCommand );
                    }

                }
            }
        }
    }

    protected ContentKey storeNewContent( User oldUser, BinaryData[] binaries, String xmlData )
    {
        UserEntity runningUser = securityService.getUser( oldUser );
        List<BinaryDataAndBinary> binaryDataAndBinaries = BinaryDataAndBinary.createNewFrom( binaries );

        ContentAndVersion parsedContentAndVersion = contentParserService.parseContentAndVersion( xmlData, null, true );
        ContentEntity parsedContent = parsedContentAndVersion.getContent();
        ContentVersionEntity parsedVersion = parsedContentAndVersion.getVersion();

        CreateContentCommand createCommand = new CreateContentCommand();
        createCommand.setCreator( runningUser );
        createCommand.setAccessRightsStrategy( CreateContentCommand.AccessRightsStrategy.INHERIT_FROM_CATEGORY );

        /** Populate command with ContentEntity-data **/
        createCommand.populateCommandWithContentValues( parsedContent );
        createCommand.populateCommandWithContentVersionValues( parsedVersion );

        createCommand.setBinaryDatas( binaryDataAndBinaries );
        createCommand.setUseCommandsBinaryDataToAdd( true );

        return contentService.createContent( createCommand );
    }

    protected UpdateContentResult updateContent( User oldTypeUser, String xmlData, List<BinaryDataAndBinary> binariesToAdd,
                                                 boolean asCurrentVersion )
    {
        UserEntity runningUser = securityService.getUser( oldTypeUser );

        ContentAndVersion parsedContentAndVersion = contentParserService.parseContentAndVersion( xmlData, null, true );
        ContentEntity parsedContent = parsedContentAndVersion.getContent();

        // be sure to add existing content's access rights, else the content will loose them all
        ContentEntity persistedContent = contentDao.findByKey( parsedContent.getKey() );
        for ( ContentAccessEntity contentAccess : persistedContent.getContentAccessRights() )
        {
            parsedContent.addContentAccessRight( contentAccess.copy() );
        }

        UpdateContentCommand updateContentCommand =
            UpdateContentCommand.storeNewVersionEvenIfUnchanged( persistedContent.getMainVersion().getKey() );
        updateContentCommand.setModifier( runningUser );

        updateContentCommand.populateContentValuesFromContent( parsedContent );
        updateContentCommand.populateContentVersionValuesFromContentVersion( parsedContentAndVersion.getVersion() );

        updateContentCommand.setUpdateAsMainVersion( asCurrentVersion );

        // always as new version
        updateContentCommand.setSyncAccessRights( false );

        updateContentCommand.setBinaryDataToAdd( binariesToAdd );
        updateContentCommand.setUseCommandsBinaryDataToAdd( true );

        // always removing all the previous binaries
        updateContentCommand.setBinaryDataToRemove( persistedContent.getMainVersion().getContentBinaryDataKeys() );
        updateContentCommand.setUseCommandsBinaryDataToRemove( true );

        UpdateContentResult updateContentResult = contentService.updateContent( updateContentCommand );

        if ( updateContentResult.isAnyChangesMade() )
        {
            new PageCacheInvalidatorForContent( pageCacheService ).invalidateForContent( updateContentResult.getTargetedVersion() );
        }

        return updateContentResult;
    }

    protected String cropName( String name )
    {
        return name;
    }

    protected abstract BinaryData[] getBinaries( ContentBaseHandlerServlet cbhServlet, AdminService admin, ExtendedMap formItems,
                                                 File file )
        throws VerticalAdminException;

    public static class DummyFileItem
        implements FileItem
    {

        String fieldName;

        File file;

        DummyFileItem( File file )
        {
            this.file = file;
        }

        public void delete()
        {
        }

        public byte[] get()
        {
            return null;
        }

        public String getContentType()
        {
            return null;
        }

        public String getFieldName()
        {
            return fieldName;
        }

        public InputStream getInputStream()
            throws IOException
        {
            return new FileInputStream( file );
        }

        public String getName()
        {
            return file.getName();
        }

        public OutputStream getOutputStream()
            throws IOException
        {
            return null;
        }

        public long getSize()
        {
            return 0;
        }

        public String getString()
        {
            return null;
        }

        public String getString( String arg0 )
            throws UnsupportedEncodingException
        {
            return null;
        }

        public boolean isFormField()
        {
            return false;
        }

        public boolean isInMemory()
        {
            return false;
        }

        public void setFieldName( String arg0 )
        {
        }

        public void setFormField( boolean arg0 )
        {
        }

        public void write( File arg0 )
            throws Exception
        {
        }
    }
}
