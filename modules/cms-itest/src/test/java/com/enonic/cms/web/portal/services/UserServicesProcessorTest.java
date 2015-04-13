/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.web.portal.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import com.enonic.esl.containers.ExtendedMap;

import com.enonic.cms.api.plugin.ext.userstore.UserFieldType;
import com.enonic.cms.api.plugin.ext.userstore.UserStoreConfig;
import com.enonic.cms.api.plugin.ext.userstore.UserStoreConfigField;
import com.enonic.cms.core.Attribute;
import com.enonic.cms.core.portal.httpservices.HttpServicesException;
import com.enonic.cms.core.security.PortalSecurityHolder;
import com.enonic.cms.core.security.SecurityService;
import com.enonic.cms.core.security.group.GroupEntity;
import com.enonic.cms.core.security.group.GroupKey;
import com.enonic.cms.core.security.group.GroupType;
import com.enonic.cms.core.security.user.UpdateUserCommand;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserType;
import com.enonic.cms.core.security.userstore.StoreNewUserStoreCommand;
import com.enonic.cms.core.security.userstore.UserStoreKey;
import com.enonic.cms.core.security.userstore.UserStoreService;
import com.enonic.cms.core.servlet.ServletRequestAccessor;
import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.core.structure.SitePath;
import com.enonic.cms.itest.AbstractSpringTest;
import com.enonic.cms.itest.util.DomainFixture;
import com.enonic.cms.store.dao.GroupDao;
import com.enonic.cms.store.dao.UserDao;
import com.enonic.cms.store.dao.UserStoreDao;
import com.enonic.cms.web.portal.SiteRedirectHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.easymock.classextension.EasyMock.createMock;

public class UserServicesProcessorTest
    extends AbstractSpringTest
{
    @Autowired
    private UserDao userDao;

    @Autowired
    private GroupDao groupDao;

    @Autowired
    private UserStoreDao userStoreDao;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private UserStoreService userStoreService;

    @Autowired
    private DomainFixture fixture;

    private MockHttpServletRequest request = new MockHttpServletRequest();

    private MockHttpServletResponse response = new MockHttpServletResponse();

    private MockHttpSession session = new MockHttpSession();

    private UserServicesProcessor userHandlerController;

    @Before
    public void setUp()
    {

        fixture.initSystemData();

        userHandlerController = new UserServicesProcessor();
        userHandlerController.setUserDao( userDao );
        userHandlerController.setGroupDao( groupDao );
        userHandlerController.setUserStoreDao( userStoreDao );
        userHandlerController.setSecurityService( securityService );
        userHandlerController.setUserStoreService( userStoreService );
        userHandlerController.setUserServicesRedirectHelper( new UserServicesRedirectUrlResolver() );

        // just need a dummy of the SiteRedirectHelper
        userHandlerController.setSiteRedirectHelper( createMock( SiteRedirectHelper.class ) );

        request.setRemoteAddr( "127.0.0.1" );
        ServletRequestAccessor.setRequest( request );

        PortalSecurityHolder.setAnonUser( fixture.findUserByName( "anonymous" ).getKey() );

    }

    @After
    public void after()
    {
        securityService.logoutPortalUser();
    }

//    JSI, 2015-04-01:
//    The following two tests do not seem to test what they should.  It seems it only test <code>userHandlerConteroller.addGroupsfromSetGroupsConfig</code>
//    which is an internal method in the class, where the other tests in this and related classes tests handlerXxx methods.  I think this test should test
//    the <code>handlerSetGroups</code> method.  That would make sense.
//
//    Until today, this test and the next one, were exactly equal.  The few commetns exising did not make sense, so I have tried to update this test
//    somewhat.  However, I run into problems when I tried to create a data-setup that allowed for real testing of the <code>handlerSetGroups</code>
//    method, so the test as it stands right now does still not seem to be very interesting.
    @Test
    public void testAddGroupsFromSetGroupsConfig()
    {
        final GroupEntity group1 = fixture.getFactory().createGroup( "open1", GroupType.GLOBAL_GROUP, false );
        fixture.save( group1 );
        final GroupEntity group2 = fixture.getFactory().createGroup( "open2", GroupType.GLOBAL_GROUP, false );
        fixture.save( group2 );
        final GroupEntity group3 = fixture.getFactory().createGroup( "open3", GroupType.USERSTORE_GROUP, false );
        fixture.save( group3 );
        final GroupEntity testUserGroup = fixture.getFactory().createGroup( "testUserGroup", GroupType.USER, true );
        fixture.save( testUserGroup );


        final UserEntity testUser = fixture.getFactory().createUser( "testUser", "Test User", UserType.NORMAL, "testuserstore", testUserGroup);
        fixture.save( testUser );

        final String group1Key = group1.getGroupKey().toString();
        final String group2Key = group2.getGroupKey().toString();
        final String group3Key = group3.getGroupKey().toString();

        ExtendedMap formItems = new ExtendedMap();

        // User is a member of group 1
        // Group 1 is in the allgroupskey, and should be removed since not in joingroupkey
        // Group 2 and 3 will be added
        formItems.put( UserServicesProcessor.ALLGROUPKEYS, group1Key );
        formItems.put( UserServicesProcessor.JOINGROUPKEY, new String[]{group2Key, group3Key} );

        UpdateUserCommand updateUserCommand = new UpdateUserCommand( null, null );

        userHandlerController.addGroupsFromSetGroupsConfig( formItems, updateUserCommand, testUser );

        List<GroupKey> expectedEntries = generateGroupKeyList( new String[]{group2Key, group3Key} );

        assertEquals( "Should have 2 groups", 2, updateUserCommand.getMemberships().size() );
        assertTrue( updateUserCommand.getMemberships().containsAll( expectedEntries ) );
    }

//    JSI, 2015-04-01:
//    This test is exactely what the previous test used to be.  Neither makes any sense.  The name of this seems to indicate it should test some kind
//    of negative condition, but it does not.  I would say this needs to be rewritten, but it is not clear to what, so for now, I will check this in
//    with these comments.
    @Test
    public void testAddRestricedGroupNotAllowed()
    {
        final GroupEntity group1 = fixture.getFactory().createGroup( "open1", GroupType.GLOBAL_GROUP, false );
        fixture.save( group1 );
        final GroupEntity group2 = fixture.getFactory().createGroup( "open2", GroupType.GLOBAL_GROUP, false );
        fixture.save( group2 );
        final GroupEntity group3 = fixture.getFactory().createGroup( "open3", GroupType.GLOBAL_GROUP, true );
        fixture.save( group3 );

        final String group1Key = group1.getGroupKey().toString();
        final String group2Key = group2.getGroupKey().toString();
        final String group3Key = group3.getGroupKey().toString();

        ExtendedMap formItems = new ExtendedMap();

        // User has group 1,2,7
        // Group 1 is in the allgroupskey, and should be removed since not in joingroupkey
        // Group 1 and 2 will be added, while group 3 will not since its restricted
        formItems.put( UserServicesProcessor.ALLGROUPKEYS, "1" );
        formItems.put( UserServicesProcessor.JOINGROUPKEY, new String[]{group1Key, group2Key, group3Key} );

        UpdateUserCommand updateUserCommand = new UpdateUserCommand( null, null );

        MyUserEntityMock user = new MyUserEntityMock();

        userHandlerController.addGroupsFromSetGroupsConfig( formItems, updateUserCommand, user );

        List<GroupKey> expectedEntries = generateGroupKeyList( new String[]{"2", "7", group1Key, group2Key} );

        assertEquals( "Should have 4 groups", 4, updateUserCommand.getMemberships().size() );
        assertTrue( updateUserCommand.getMemberships().containsAll( expectedEntries ) );
    }


    @Test
    public void create_without_required_fields_on_local_user_store_throws_exception()
        throws Exception
    {
        UserStoreConfig userStoreConfig = new UserStoreConfig();
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.FIRST_NAME, "required" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.LAST_NAME, "required" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.INITIALS, "required" ) );
        createLocalUserStore( "myLocalStore", true, userStoreConfig );

        fixture.flushAndClearHibernateSession();

        // exercise
        request.setAttribute( Attribute.ORIGINAL_SITEPATH, new SitePath( new SiteKey( 0 ), "/_services/user/create" ) );
        ExtendedMap formItems = createExtendedMap();
        formItems.putString( "username", "testcreate" );
        formItems.putString( "password", "password" );
        formItems.putString( "email", "test@test.com" );
        formItems.putString( "first_name", "First name" );
        formItems.putString( "last_name", "Last name" );

        try
        {
            userHandlerController.handlerCreate( request, response, session, formItems, null, null );
            fail( "Expected exception" );
        }
        catch ( Exception e )
        {
            assertTrue( e instanceof HttpServicesException );
            assertEquals( "Error in http services, error code: 400", e.getMessage() );
        }

    }

    @Test
    public void create_with_empty_required_fields_on_local_user_store_throws_exception()
        throws Exception
    {
        UserStoreConfig userStoreConfig = new UserStoreConfig();
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.FIRST_NAME, "required" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.LAST_NAME, "required" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.INITIALS, "required" ) );
        createLocalUserStore( "myLocalStore", true, userStoreConfig );

        fixture.flushAndClearHibernateSession();

        // exercise
        request.setAttribute( Attribute.ORIGINAL_SITEPATH, new SitePath( new SiteKey( 0 ), "/_services/user/create" ) );
        ExtendedMap formItems = createExtendedMap();
        formItems.putString( "username", "testcreate" );
        formItems.putString( "password", "password" );
        formItems.putString( "email", "test@test.com" );
        formItems.putString( "first_name", "First name" );
        formItems.putString( "last_name", "Last name" );
        formItems.putString( "initials", "" ); // field set but empty

        try
        {
            userHandlerController.handlerCreate( request, response, session, formItems, null, null );
            fail( "Expected exception" );
        }
        catch ( Exception e )
        {
            assertTrue( e instanceof HttpServicesException );
            assertEquals( "Error in http services, error code: 400", e.getMessage() );
        }

    }

    @Test
    public void create_with_blank_required_fields_on_local_user_store_throws_exception()
        throws Exception
    {
        UserStoreConfig userStoreConfig = new UserStoreConfig();
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.FIRST_NAME, "required" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.LAST_NAME, "required" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.INITIALS, "required" ) );
        createLocalUserStore( "myLocalStore", true, userStoreConfig );

        fixture.flushAndClearHibernateSession();

        // exercise
        request.setAttribute( Attribute.ORIGINAL_SITEPATH, new SitePath( new SiteKey( 0 ), "/_services/user/create" ) );
        ExtendedMap formItems = createExtendedMap();
        formItems.putString( "username", "testcreate" );
        formItems.putString( "password", "password" );
        formItems.putString( "email", "test@test.com" );
        formItems.putString( "first_name", "First name" );
        formItems.putString( "last_name", "Last name" );
        formItems.putString( "initials", "  " ); // field set but blank

        try
        {
            userHandlerController.handlerCreate( request, response, session, formItems, null, null );
            fail( "Expected exception" );
        }
        catch ( Exception e )
        {
            assertTrue( e instanceof HttpServicesException );
            assertEquals( "Error in http services, error code: 400", e.getMessage() );
        }

    }

    private UserStoreConfigField createUserStoreUserFieldConfig( UserFieldType type, String properties )
    {
        UserStoreConfigField fieldConfig = new UserStoreConfigField( type );
        fieldConfig.setRemote( properties.contains( "remote" ) );
        fieldConfig.setReadOnly( properties.contains( "read-only" ) );
        fieldConfig.setRequired( properties.contains( "required" ) );
        fieldConfig.setIso( properties.contains( "iso" ) );
        return fieldConfig;
    }

    private UserStoreKey createLocalUserStore( String name, boolean defaultStore, UserStoreConfig config )
    {
        StoreNewUserStoreCommand command = new StoreNewUserStoreCommand();
        command.setStorer( fixture.findUserByName( "admin" ).getKey() );
        command.setName( name );
        command.setDefaultStore( defaultStore );
        command.setConfig( config );
        return userStoreService.storeNewUserStore( command );
    }

    private ExtendedMap createExtendedMap()
    {
        return new ExtendedMap( true );
    }

    private List<GroupKey> generateGroupKeyList( String[] keys )
    {
        List<GroupKey> groupKeys = new ArrayList<GroupKey>();

        for ( String key : keys )
        {
            groupKeys.add( new GroupKey( key ) );
        }

        return groupKeys;
    }

    private class MyUserEntityMock
        extends UserEntity
    {
        private static final long serialVersionUID = -5767196099030559184L;

        @Override
        public Set<GroupEntity> getDirectMemberships()
        {

            GroupEntity group1 = new GroupEntity();
            group1.setKey( new GroupKey( "1" ) );

            GroupEntity group2 = new GroupEntity();
            group2.setKey( new GroupKey( "2" ) );

            GroupEntity group7 = new GroupEntity();
            group7.setKey( new GroupKey( "7" ) );

            Set<GroupEntity> groupEntities = new HashSet<GroupEntity>();

            groupEntities.add( group1 );

            groupEntities.add( group2 );

            groupEntities.add( group7 );

            return groupEntities;
        }
    }
}
