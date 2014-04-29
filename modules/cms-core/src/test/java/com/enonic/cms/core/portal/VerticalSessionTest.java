package com.enonic.cms.core.portal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;
import org.w3c.dom.Document;

import junit.framework.Assert;

import com.enonic.esl.xml.XMLTool;

import com.enonic.cms.framework.util.JDOMUtil;

public class VerticalSessionTest
{
    @Test
    public void testAccessors()
    {
        final VerticalSession session = new VerticalSession();
        Assert.assertNull( session.getAttribute( "test" ) );

        session.setAttribute( "test", "hello" );
        Assert.assertEquals( "hello", session.getAttribute( "test" ) );

        session.removeAttribute( "test" );
        Assert.assertNull( session.getAttribute( "test" ) );

        final Document doc = XMLTool.createDocument();
        session.setAttribute( "doc", doc );
        Assert.assertSame( doc, session.getAttribute( "doc" ) );
    }

    @Test
    public void testSerialization_empty()
        throws Exception
    {
        final VerticalSession original = new VerticalSession();
        final byte[] serializedBytes = serialize( original );

        final VerticalSession deserialized = deserialize( serializedBytes );
        Assert.assertEquals( 0, deserialized.size() );
    }

    @Test
    public void testSerialization_mixed()
        throws Exception
    {
        final VerticalSession original = new VerticalSession();
        original.setAttribute( "simple", "value" );

        final Document doc = XMLTool.domparse( "<a><b/></a>" );
        original.setAttribute( "complex", doc );

        final byte[] serializedBytes = serialize( original );

        final VerticalSession deserialized = deserialize( serializedBytes );
        Assert.assertEquals( 2, deserialized.size() );
        Assert.assertEquals( "value", deserialized.getAttribute( "simple" ) );
        Assert.assertEquals( toXml( doc ), toXml( (Document) deserialized.getAttribute( "complex" ) ) );
    }

    @Test
    public void testToXml()
    {
        final VerticalSession session = new VerticalSession();
        Assert.assertEquals( readXml( "session_no_data.xml" ), toXml( session ) );

        session.setAttribute( "simple", "value" );
        session.setAttribute( "complex", XMLTool.domparse( "<a><b/></a>" ) );
        Assert.assertEquals( readXml( "session_mixed_data.xml" ), toXml( session ) );
    }

    private String toXml( final VerticalSession session )
    {
        final Document doc = session.toXML();
        return toXml( doc );
    }

    private String toXml( final Document doc )
    {
        return JDOMUtil.prettyPrintDocument( JDOMUtil.toDocument( doc ) );
    }

    private String readXml( final String name )
    {
        final InputStream in = getClass().getResourceAsStream( name );
        final Document doc = XMLTool.domparse( in );
        return toXml( doc );
    }

    private byte[] serialize( final VerticalSession object )
        throws Exception
    {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ObjectOutputStream oout = new ObjectOutputStream( out );

        oout.writeObject( object );

        out.close();
        return out.toByteArray();
    }

    private VerticalSession deserialize( final byte[] bytes )
        throws Exception
    {
        final ByteArrayInputStream in = new ByteArrayInputStream( bytes );
        final ObjectInputStream oin = new ObjectInputStream( in );

        final Object value = oin.readObject();

        in.close();
        return (VerticalSession) value;
    }
}
