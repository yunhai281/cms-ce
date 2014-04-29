/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.enonic.esl.xml.XMLTool;

final public class VerticalSession
    extends HashMap<String, Object>
{
    public static final String VERTICAL_SESSION_OBJECT = "VERTICAL_SESSION_OBJECT";

    public void setAttribute( final String attributeName, final String value )
    {
        final String trimmedValue = value.replaceAll( "\\r", "" );
        put( attributeName, trimmedValue );
    }

    public void setAttribute( final String attributeName, final Document xmlDoc )
    {
        put( attributeName, xmlDoc );
    }

    public Object getAttribute( final String attributeName )
    {
        return get( attributeName );
    }

    public void removeAttribute( final String name )
    {
        remove( name );
    }

    public Document toXML()
    {
        final Document doc = XMLTool.createDocument( "sessions" );
        toXML( doc.getDocumentElement() );
        return doc;
    }

    private void toXML( final Element parent )
    {
        final Document doc = parent.getOwnerDocument();
        final Element sessionElem = XMLTool.createElement( doc, parent, "session" );

        for ( final Map.Entry<String, Object> entry : entrySet() )
        {
            final Element attributeElem = XMLTool.createElement( doc, sessionElem, "attribute" );
            attributeElem.setAttribute( "name", entry.getKey() );

            final Object value = entry.getValue();
            if ( value instanceof Document )
            {
                final Document xmlDoc = (Document) value;
                final Element rootElem = xmlDoc.getDocumentElement();
                attributeElem.appendChild( doc.importNode( rootElem, true ) );
            }
            else if ( value != null )
            {
                XMLTool.createTextNode( doc, attributeElem, value.toString() );
            }
        }

    }

    public String toString()
    {
        return XMLTool.documentToString( toXML() );
    }
}
