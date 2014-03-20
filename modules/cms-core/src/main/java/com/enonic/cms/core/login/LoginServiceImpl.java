/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.login;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.enonic.cms.core.security.RememberedLoginEntity;
import com.enonic.cms.core.security.RememberedLoginKey;
import com.enonic.cms.core.security.user.UserKey;
import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.core.time.TimeService;
import com.enonic.cms.store.dao.RememberedLoginDao;

@Service
public class LoginServiceImpl
    implements LoginService
{
    private RememberedLoginDao rememberedLoginDao;

    private TimeService timeService;

    private long autologinTimeoutInMilliSeconds;


    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public String rememberLogin( UserKey userKey, SiteKey siteKey, boolean resetGUID )
    {

        RememberedLoginEntity rememberedLogin = rememberedLoginDao.findByUserKeyAndSiteKey( userKey, siteKey );
        String guid;

        if ( rememberedLogin == null )
        {
            guid = createCookieSafeUID();
            RememberedLoginKey key = new RememberedLoginKey();
            key.setSiteKey( siteKey );
            key.setUserKey( userKey );
            rememberedLogin = new RememberedLoginEntity();
            rememberedLogin.setKey( key );
            rememberedLogin.setCreatedAt( timeService.getNowAsDateTime().toDate() );
            rememberedLogin.setGuid( guid );
            rememberedLoginDao.store( rememberedLogin );
        }
        else
        {
            if ( resetGUID )
            {
                guid = createCookieSafeUID();
                rememberedLogin.setGuid( guid );
            }
            else
            {
                guid = rememberedLogin.getGuid();
            }

            rememberedLogin.setCreatedAt( timeService.getNowAsDateTime().toDate() );
        }

        return guid;
    }

    public UserKey getRememberedLogin( String guid, SiteKey siteKey )
    {
        RememberedLoginEntity rememberedLogin = rememberedLoginDao.findByGuidAndSite( guid, siteKey );
        if ( rememberedLogin == null )
        {
            return null;
        }

        long now = timeService.getNowAsMilliseconds();
        long loginRememberedAt = rememberedLogin.getCreatedAt().getTime();
        long timeRemembered = now - loginRememberedAt;

        if ( timeRemembered < autologinTimeoutInMilliSeconds )
        {
            return rememberedLogin.getKey().getUserKey();
        }

        return null;
    }

    private String createCookieSafeUID()
    {
        return UUID.randomUUID().toString().replace( "-", "" );
    }

    @Autowired
    public void setRememberedLoginDao( RememberedLoginDao value )
    {
        this.rememberedLoginDao = value;
    }

    @Autowired
    public void setTimeService( TimeService value )
    {
        this.timeService = value;
    }

    @Value("${com.enonic.vertical.presentation.autologinTimeout}")
    public void setAutologinTimeoutInDays( Integer value )
    {
        this.autologinTimeoutInMilliSeconds = (long) 1000 * 60 * 60 * 24 * value;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void removeRememberedLogin( final UserKey userKey )
    {
        this.rememberedLoginDao.removeUsage( userKey );
    }
}
