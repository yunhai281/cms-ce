package com.enonic.cms.web.portal.services;

import java.rmi.RemoteException;
import java.util.Locale;
import java.util.TimeZone;

import org.joda.time.DateMidnight;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.enonic.esl.containers.ExtendedMap;

import com.enonic.cms.api.client.model.user.Gender;
import com.enonic.cms.api.plugin.ext.userstore.UserFieldType;
import com.enonic.cms.api.plugin.ext.userstore.UserFields;
import com.enonic.cms.api.plugin.ext.userstore.UserStoreConfig;
import com.enonic.cms.api.plugin.ext.userstore.UserStoreConfigField;
import com.enonic.cms.core.Attribute;
import com.enonic.cms.core.portal.httpservices.HttpServicesException;
import com.enonic.cms.core.security.PortalSecurityHolder;
import com.enonic.cms.core.security.SecurityService;
import com.enonic.cms.core.security.user.StoreNewUserCommand;
import com.enonic.cms.core.security.user.UserKey;
import com.enonic.cms.core.security.user.UserType;
import com.enonic.cms.core.security.userstore.StoreNewUserStoreCommand;
import com.enonic.cms.core.security.userstore.UserStoreKey;
import com.enonic.cms.core.security.userstore.UserStoreService;
import com.enonic.cms.core.servlet.ServletRequestAccessor;
import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.core.structure.SitePath;
import com.enonic.cms.itest.AbstractSpringTest;
import com.enonic.cms.itest.util.DomainFixture;
import com.enonic.cms.store.dao.UserDao;
import com.enonic.cms.web.portal.SiteRedirectHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.easymock.classextension.EasyMock.createMock;

public class UserServicesProcessorTest_operation_ModifyTest
    extends AbstractSpringTest
{
    @Autowired
    private UserDao userDao;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private UserStoreService userStoreService;

    @Autowired
    private DomainFixture fixture;

    private MockHttpServletRequest request = new MockHttpServletRequest();

    private MockHttpServletResponse response = new MockHttpServletResponse();

    private UserServicesProcessor userHandlerController;

    @Before
    public void setUp()
    {
        fixture.initSystemData();

        userHandlerController = new UserServicesProcessor();
        userHandlerController.setUserDao( userDao );
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

    @Test
    public void modify()
        throws RemoteException
    {
        UserStoreConfig userStoreConfig = new UserStoreConfig();
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.FIRST_NAME, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.LAST_NAME, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.INITIALS, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.MIDDLE_NAME, "" ) );

        createLocalUserStore( "myLocalStore", true, userStoreConfig );

        UserFields userFields = new UserFields();
        userFields.setFirstName( "Qhawe" );
        userFields.setLastName( "Skriubakken" );
        createNormalUser( "qhawe", "myLocalStore", userFields );

        loginPortalUser( "qhawe" );

        request.setAttribute( Attribute.ORIGINAL_SITEPATH, new SitePath( new SiteKey( 0 ), "/_services/user/modify" ) );

        ExtendedMap formItems = createExtendedMap();
        formItems.putString( "first_name", "Vier" );

        userHandlerController.handlerModify( request, response, formItems );

        assertEquals( "Vier", fixture.findUserByName( "qhawe" ).getUserFields().getFirstName() );
    }

    @Test
    public void modify_without_required_fields_on_local_user_store()
        throws Exception
    {
        UserStoreConfig userStoreConfig = new UserStoreConfig();
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.FIRST_NAME, "required" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.LAST_NAME, "required" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.INITIALS, "required" ) );
        createLocalUserStore( "myLocalStore", true, userStoreConfig );

        fixture.flushAndClearHibernateSession();

        UserFields userFields = new UserFields();
        userFields.setFirstName( "First name" );
        userFields.setLastName( "Last name" );
        userFields.setInitials( "INI" );
        createNormalUser( "testuser", "myLocalStore", userFields );

        // exercise
        request.setAttribute( Attribute.ORIGINAL_SITEPATH, new SitePath( new SiteKey( 0 ), "/_services/user/create" ) );
        ExtendedMap formItems = createExtendedMap();
        formItems.putString( "initials", "ABC" );

        loginPortalUser( "testuser" );

        userHandlerController.handlerModify( request, response, formItems );

        // verify
        assertEquals( "ABC", fixture.findUserByName( "testuser" ).getUserFields().getInitials() );
        assertEquals( "First name", fixture.findUserByName( "testuser" ).getUserFields().getFirstName() );
        assertEquals( "Last name", fixture.findUserByName( "testuser" ).getUserFields().getLastName() );
    }

    @Test
    public void modify_with_empty_required_fields_on_local_user_store_throws_exception()
        throws Exception
    {
        UserStoreConfig userStoreConfig = new UserStoreConfig();
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.FIRST_NAME, "required" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.LAST_NAME, "required" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.INITIALS, "required" ) );
        createLocalUserStore( "myLocalStore", true, userStoreConfig );

        fixture.flushAndClearHibernateSession();

        UserFields userFields = new UserFields();
        userFields.setFirstName( "First name" );
        userFields.setLastName( "Last name" );
        userFields.setInitials( "INI" );
        createNormalUser( "testuser", "myLocalStore", userFields );

        // exercise
        request.setAttribute( Attribute.ORIGINAL_SITEPATH, new SitePath( new SiteKey( 0 ), "/_services/user/create" ) );
        ExtendedMap formItems = createExtendedMap();
        formItems.putString( "initials", "" ); // field set but empty
        formItems.putString( "last_name", "Last name changed" );
        loginPortalUser( "testuser" );

        try
        {
            userHandlerController.handlerModify( request, response, formItems );
            fail( "Expected exception" );
        }
        catch ( Exception e )
        {
            assertTrue( e instanceof HttpServicesException );
            assertEquals( "Error in http services, error code: 400", e.getMessage() );
        }
    }

    @Test
    public void modify_with_blank_required_fields_on_local_user_store_throws_exception()
        throws Exception
    {
        UserStoreConfig userStoreConfig = new UserStoreConfig();
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.FIRST_NAME, "required" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.LAST_NAME, "required" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.INITIALS, "required" ) );
        createLocalUserStore( "myLocalStore", true, userStoreConfig );

        fixture.flushAndClearHibernateSession();

        UserFields userFields = new UserFields();
        userFields.setFirstName( "First name" );
        userFields.setLastName( "Last name" );
        userFields.setInitials( "INI" );
        createNormalUser( "testuser", "myLocalStore", userFields );

        // exercise
        request.setAttribute( Attribute.ORIGINAL_SITEPATH, new SitePath( new SiteKey( 0 ), "/_services/user/create" ) );
        ExtendedMap formItems = createExtendedMap();
        formItems.putString( "initials", "  " ); // field set but blank
        formItems.putString( "last_name", "Last name changed" );
        loginPortalUser( "testuser" );

        try
        {
            userHandlerController.handlerModify( request, response, formItems );
            fail( "Expected exception" );
        }
        catch ( Exception e )
        {
            assertTrue( e instanceof HttpServicesException );
            assertEquals( "Error in http services, error code: 400", e.getMessage() );
        }
    }

    @Test
    public void modify_given_non_textual_user_field_null_then_value_is_not_changed()
        throws Exception
    {
        UserStoreConfig userStoreConfig = new UserStoreConfig();
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.BIRTHDAY, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.INITIALS, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.GENDER, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.HTML_EMAIL, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.LOCALE, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.TIME_ZONE, "" ) );
        createLocalUserStore( "myLocalStore", true, userStoreConfig );

        fixture.flushAndClearHibernateSession();

        UserFields userFields = new UserFields();
        userFields.setBirthday( new DateMidnight( 2012, 12, 12 ).toDate() );
        userFields.setGender( Gender.FEMALE );
        userFields.setHtmlEmail( true );
        userFields.setInitials( "INI" );
        userFields.setLocale( Locale.FRENCH );
        userFields.setTimezone( TimeZone.getTimeZone( "UTC" ) );
        createNormalUser( "testuser", "myLocalStore", userFields );

        // setup: verify user
        userFields = fixture.findUserByName( "testuser" ).getUserFields();
        assertEquals( new DateMidnight( 2012, 12, 12 ).toDate(), userFields.getBirthday() );
        assertEquals( Gender.FEMALE, userFields.getGender() );
        assertEquals( Boolean.TRUE, userFields.getHtmlEmail() );
        assertEquals( "INI", userFields.getInitials() );
        assertEquals( Locale.FRENCH, userFields.getLocale() );
        assertEquals( TimeZone.getTimeZone( "UTC" ), userFields.getTimeZone() );

        // exercise
        request.setAttribute( Attribute.ORIGINAL_SITEPATH, new SitePath( new SiteKey( 0 ), "/_services/user/create" ) );
        ExtendedMap formItems = createExtendedMap();
        formItems.putString( "initials", "Initials changed" );
        formItems.putString( "locale", null );
        formItems.putString( "gender", null );
        formItems.putString( "html-email", null );
        formItems.putString( "birthday", null );
        formItems.putString( "time-zone", null );

        loginPortalUser( "testuser" );

        userHandlerController.handlerModify( request, response, formItems );

        // verify
        userFields = fixture.findUserByName( "testuser" ).getUserFields();
        assertEquals( "Initials changed", userFields.getInitials() );
        assertEquals( new DateMidnight( 2012, 12, 12 ).toDate(), userFields.getBirthday() );
        assertEquals( Gender.FEMALE, userFields.getGender() );
        assertEquals( Boolean.TRUE, userFields.getHtmlEmail() );
        assertEquals( Locale.FRENCH, userFields.getLocale() );
        assertEquals( TimeZone.getTimeZone( "UTC" ), userFields.getTimeZone() );
    }

    @Test
    public void modify_given_non_textual_user_field_empty_string_then_value_is_changed_to_null()
        throws Exception
    {
        UserStoreConfig userStoreConfig = new UserStoreConfig();
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.BIRTHDAY, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.INITIALS, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.FIRST_NAME, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.GENDER, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.LAST_NAME, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.HTML_EMAIL, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.LOCALE, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.TIME_ZONE, "" ) );
        createLocalUserStore( "myLocalStore", true, userStoreConfig );

        fixture.flushAndClearHibernateSession();

        UserFields userFields = new UserFields();
        userFields.setBirthday( new DateMidnight( 2012, 12, 12 ).toDate() );
        userFields.setFirstName( "First name" );
        userFields.setGender( Gender.FEMALE );
        userFields.setHtmlEmail( true );
        userFields.setInitials( "INI" );
        userFields.setLastName( "Last name" );
        userFields.setLocale( Locale.FRENCH );
        userFields.setTimezone( TimeZone.getTimeZone( "UTC" ) );
        createNormalUser( "testuser", "myLocalStore", userFields );

        // setup: verify user
        userFields = fixture.findUserByName( "testuser" ).getUserFields();
        assertEquals( new DateMidnight( 2012, 12, 12 ).toDate(), userFields.getBirthday() );
        assertEquals( Gender.FEMALE, userFields.getGender() );
        assertEquals( Boolean.TRUE, userFields.getHtmlEmail() );
        assertEquals( "INI", userFields.getInitials() );
        assertEquals( Locale.FRENCH, userFields.getLocale() );
        assertEquals( TimeZone.getTimeZone( "UTC" ), userFields.getTimeZone() );

        // exercise
        request.setAttribute( Attribute.ORIGINAL_SITEPATH, new SitePath( new SiteKey( 0 ), "/_services/user/create" ) );
        ExtendedMap formItems = createExtendedMap();
        formItems.putString( "first_name", "First name changed" );
        formItems.putString( "last_name", "Last name changed" );
        formItems.putString( "initials", "Initials changed" );
        formItems.putString( "locale", "" );
        formItems.putString( "gender", "" );
        formItems.putString( "html-email", "" );
        formItems.putString( "birthday", "" );
        formItems.putString( "time-zone", "" );

        loginPortalUser( "testuser" );

        userHandlerController.handlerModify( request, response, formItems );

        // verify
        userFields = fixture.findUserByName( "testuser" ).getUserFields();
        assertEquals( "Initials changed", userFields.getInitials() );
        assertEquals( null, userFields.getLocale() );
        assertEquals( null, userFields.getGender() );
        assertEquals( null, userFields.getHtmlEmail() );
        assertEquals( null, userFields.getBirthday() );
        assertEquals( null, userFields.getTimeZone() );
    }

    @Test
    public void modify_given_textual_user_field_null_then_value_is_not_changed()
        throws Exception
    {
        UserStoreConfig userStoreConfig = new UserStoreConfig();
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.FIRST_NAME, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.LAST_NAME, "" ) );
        createLocalUserStore( "myLocalStore", true, userStoreConfig );

        fixture.flushAndClearHibernateSession();

        UserFields userFields = new UserFields();
        userFields.setFirstName( "First name" );
        userFields.setLastName( "Last name" );
        createNormalUser( "testuser", "myLocalStore", userFields );

        // setup: verify user
        userFields = fixture.findUserByName( "testuser" ).getUserFields();
        assertEquals( "First name", userFields.getFirstName() );
        assertEquals( "Last name", userFields.getLastName() );

        // exercise
        request.setAttribute( Attribute.ORIGINAL_SITEPATH, new SitePath( new SiteKey( 0 ), "/_services/user/create" ) );
        ExtendedMap formItems = createExtendedMap();
        formItems.putString( "first-name", null );
        formItems.putString( "last-name", null );

        loginPortalUser( "testuser" );

        userHandlerController.handlerModify( request, response, formItems );

        // verify
        userFields = fixture.findUserByName( "testuser" ).getUserFields();
        assertEquals( "First name", userFields.getFirstName() );
        assertEquals( "Last name", userFields.getLastName() );
    }

    @Test
    public void modify_given_textual_user_field_empty_string_then_value_is_changed_to_empty()
        throws Exception
    {
        UserStoreConfig userStoreConfig = new UserStoreConfig();
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.FIRST_NAME, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.LAST_NAME, "" ) );
        createLocalUserStore( "myLocalStore", true, userStoreConfig );

        fixture.flushAndClearHibernateSession();

        UserFields userFields = new UserFields();
        userFields.setFirstName( "First name" );
        userFields.setLastName( "Last name" );
        createNormalUser( "testuser", "myLocalStore", userFields );

        // setup: verify user
        userFields = fixture.findUserByName( "testuser" ).getUserFields();
        assertEquals( "First name", userFields.getFirstName() );
        assertEquals( "Last name", userFields.getLastName() );

        // exercise
        request.setAttribute( Attribute.ORIGINAL_SITEPATH, new SitePath( new SiteKey( 0 ), "/_services/user/create" ) );
        ExtendedMap formItems = createExtendedMap();
        formItems.putString( "first-name", "" );
        formItems.putString( "last-name", "" );

        loginPortalUser( "testuser" );

        userHandlerController.handlerModify( request, response, formItems );

        // verify
        userFields = fixture.findUserByName( "testuser" ).getUserFields();
        assertEquals( "", userFields.getFirstName() );
        assertEquals( "", userFields.getLastName() );
    }

    @Test
    public void modify_given_non_textual_fields_not_sent_then_fields_must_not_be_changed()
        throws Exception
    {
        UserStoreConfig userStoreConfig = new UserStoreConfig();
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.BIRTHDAY, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.INITIALS, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.GENDER, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.HTML_EMAIL, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.LOCALE, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.TIME_ZONE, "" ) );
        createLocalUserStore( "myLocalStore", true, userStoreConfig );

        fixture.flushAndClearHibernateSession();

        UserFields userFields = new UserFields();
        userFields.setBirthday( new DateMidnight( 2012, 12, 12 ).toDate() );
        userFields.setGender( Gender.FEMALE );
        userFields.setHtmlEmail( true );
        userFields.setInitials( "INI" );
        userFields.setLocale( Locale.FRENCH );
        userFields.setTimezone( TimeZone.getTimeZone( "UTC" ) );
        createNormalUser( "testuser", "myLocalStore", userFields );

        // setup: verify user
        userFields = fixture.findUserByName( "testuser" ).getUserFields();
        assertEquals( "INI", userFields.getInitials() );
        assertNotNull( userFields.getBirthday() );
        assertEquals( new DateMidnight( 2012, 12, 12 ).toDate(), userFields.getBirthday() );
        assertEquals( Gender.FEMALE, userFields.getGender() );
        assertEquals( Boolean.TRUE, userFields.getHtmlEmail() );
        assertEquals( Locale.FRENCH, userFields.getLocale() );
        assertEquals( TimeZone.getTimeZone( "UTC" ), userFields.getTimeZone() );

        // exercise
        request.setAttribute( Attribute.ORIGINAL_SITEPATH, new SitePath( new SiteKey( 0 ), "/_services/user/create" ) );
        ExtendedMap formItems = createExtendedMap();
        formItems.putString( "initials", "Initials changed" );

        loginPortalUser( "testuser" );

        userHandlerController.handlerModify( request, response, formItems );

        // verify
        userFields = fixture.findUserByName( "testuser" ).getUserFields();
        assertEquals( "Initials changed", userFields.getInitials() );
        assertEquals( new DateMidnight( 2012, 12, 12 ).toDate(), userFields.getBirthday() );
        assertEquals( Gender.FEMALE, userFields.getGender() );
        assertEquals( Boolean.TRUE, userFields.getHtmlEmail() );
        assertEquals( Locale.FRENCH, userFields.getLocale() );
        assertEquals( TimeZone.getTimeZone( "UTC" ), userFields.getTimeZone() );
    }

    @Test
    public void modify_given_textual_user_field_not_sent_then_value_is_not_changed()
        throws Exception
    {
        UserStoreConfig userStoreConfig = new UserStoreConfig();
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.INITIALS, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.FIRST_NAME, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.LAST_NAME, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.ORGANIZATION, "" ) );
        userStoreConfig.addUserFieldConfig( createUserStoreUserFieldConfig( UserFieldType.LOCALE, "" ) );
        createLocalUserStore( "myLocalStore", true, userStoreConfig );

        fixture.flushAndClearHibernateSession();

        UserFields userFields = new UserFields();
        userFields.setFirstName( "First name" );
        userFields.setLastName( "Last name" );
        userFields.setInitials( "INI" );
        userFields.setOrganization( "Org" );
        userFields.setLocale( Locale.GERMAN );
        createNormalUser( "testuser", "myLocalStore", userFields );

        // setup: verify user
        userFields = fixture.findUserByName( "testuser" ).getUserFields();
        assertEquals( "First name", userFields.getFirstName() );
        assertEquals( "Last name", userFields.getLastName() );
        assertEquals( "INI", userFields.getInitials() );
        assertEquals( "Org", userFields.getOrganization() );
        assertEquals( Locale.GERMAN, userFields.getLocale() );

        // exercise
        request.setAttribute( Attribute.ORIGINAL_SITEPATH, new SitePath( new SiteKey( 0 ), "/_services/user/create" ) );
        ExtendedMap formItems = createExtendedMap();
        formItems.putString( "locale", Locale.FRENCH.toString() );

        loginPortalUser( "testuser" );

        userHandlerController.handlerModify( request, response, formItems );

        // verify
        userFields = fixture.findUserByName( "testuser" ).getUserFields();
        assertEquals( Locale.FRENCH, userFields.getLocale() );
        assertEquals( "First name", userFields.getFirstName() );
        assertEquals( "Last name", userFields.getLastName() );
        assertEquals( "INI", userFields.getInitials() );
        assertEquals( "Org", userFields.getOrganization() );
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

    private void loginPortalUser( String userName )
    {
        PortalSecurityHolder.setImpersonatedUser( fixture.findUserByName( userName ).getKey() );
        PortalSecurityHolder.setLoggedInUser( fixture.findUserByName( userName ).getKey() );
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

    private UserKey createNormalUser( String userName, String userStoreName, UserFields userFields )
    {
        StoreNewUserCommand command = new StoreNewUserCommand();
        command.setStorer( fixture.findUserByName( "admin" ).getKey() );
        command.setUsername( userName );
        command.setUserStoreKey( fixture.findUserStoreByName( userStoreName ).getKey() );
        command.setAllowAnyUserAccess( true );
        command.setEmail( userName + "@example.com" );
        command.setPassword( "password" );
        command.setType( UserType.NORMAL );
        command.setDisplayName( userName );
        command.setUserFields( userFields );

        return userStoreService.storeNewUser( command );
    }

    private ExtendedMap createExtendedMap()
    {
        return new ExtendedMap( true );
    }
}
