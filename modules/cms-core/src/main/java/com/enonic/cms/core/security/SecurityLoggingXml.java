package com.enonic.cms.core.security;

import org.jdom.Document;
import org.jdom.Element;

import com.google.common.base.Preconditions;

import com.enonic.cms.core.security.user.QualifiedUsername;

public class SecurityLoggingXml
{
    public static Document createUserStoreDataDoc( QualifiedUsername user )
    {
        Preconditions.checkArgument( user.getUsername().equals( "admin" ) ||
                                         ( user.getUserStoreKey() == null && user.getUserStoreName() != null ) ||
                                         ( user.getUserStoreName() == null && user.getUserStoreKey() != null ) );

        final Element rootElem = new Element( "data" );

        if ( user.getUserStoreKey() != null )
        {
            final Element userStoreElem = new Element( "userstorekey" );
            userStoreElem.setText( user.getUserStoreKey().toString() );
            rootElem.addContent( userStoreElem );
        }
        else if ( user.getUserStoreName() != null && !user.getUserStoreName().equals( "" ) )
        {
            final Element userStoreElem = new Element( "userstorename" );
            userStoreElem.setText( user.getUserStoreName() );
            rootElem.addContent( userStoreElem );
        }

        return new Document( rootElem );
    }

}
