/*
 * Copyright 2000-2011 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.esl.activation;

/*
 * @(#)ByteArrayDataSource.java	1.1 00/01/30
 *
 * Copyright 1998-2000 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 *
 */
/*
License Agreement

SUN MICROSYSTEMS, INC. (``SUN'') IS WILLING TO LICENSE ITS JAVAMAIL(tm)
SOFTWARE (``SOFTWARE'') TO YOU ("CUSTOMER") ONLY UPON THE CONDITION
THAT YOU ACCEPT ALL OF THE TERMS CONTAINED IN THIS LICENSE AGREEMENT
("AGREEMENT"). READ THE TERMS AND CONDITIONS OF THE AGREEMENT
CAREFULLY BEFORE SELECTING THE "ACCEPT" BUTTON AT THE BOTTOM OF THIS
PAGE. BY SELECTING THE "ACCEPT" BUTTON YOU AGREE TO THE TERMS AND
CONDITIONS OF THE AGREEMENT. IF YOU ARE NOT WILLING TO BE BOUND BY
ITS TERMS, SELECT THE "DO NOT ACCEPT" BUTTON AT THE BOTTOM OF THIS PAGE
AND THE INSTALLATION PROCESS WILL NOT CONTINUE.

1.      License to Distribute. Customer is granted a royalty-free,
non-transferable right to reproduce and use the Software
for the purpose of developing applications which run in
conjunction with the Software.  Customer may not modify the Software
(including any APIs exposed by the Software) in any way.

2.      Restrictions. Software is confidential copyrighted information
of Sun and title to all copies is retained by Sun and/or its
licensors.  Except to the extent enforcement of this provision is
prohibited by applicable law, if at all, Customer shall not decompile,
disassemble, decrypt, extract, or otherwise reverse engineer Software.
Software is not designed or intended for use in on-line control of
aircraft, air traffic, aircraft navigation or aircraft communications;
or in the design, construction, operation or maintenance of any nuclear
facility. Customer warrants that it will not use or redistribute the
Software for such purposes.

3.      Trademarks and Logos. This Agreement does not authorize
Customer to use any Sun name, trademark or logo. Customer acknowledges
that Sun owns the Java trademark and all Java-related trademarks, logos
and icons including the Coffee Cup and Duke (``Java Marks'') and agrees
to: (i) comply with the Java Trademark Guidelines at
http://java.sun.com/trademarks.html; (ii) not do anything harmful to or
inconsistent with Sun's rights in the Java Marks; and (iii) assist Sun
in protecting those rights, including assigning to Sun any rights
acquired by Customer in any Java Mark.

4. Disclaimer of Warranty. Software is provided ``AS IS,'' without a
warranty of any kind. ALL EXPRESS OR IMPLIED REPRESENTATIONS AND
WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED.

5.Limitation of Liability.      IN NO EVENT WILL SUN OR ITS LICENSORS
BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR SPECIAL,
INDIRECT, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES HOWEVER CAUSED
AND REGARDLESS OF THE THEORY OF LIABILITY ARISING OUT OF THE
DOWNLOADING OF, USE OF, OR INABILITY TO USE, SOFTWARE, EVEN IF SUN HAS
BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

6.      Termination.  Customer may terminate this Agreement at any time
by destroying all copies of Software. This Agreement will terminate
immediately without notice from Sun if Customer fails to comply with
any provision of this Agreement.  Upon such termination, Customer must
destroy all copies of Software. Sections 4 and 5 above shall survive
termination of this Agreement.

7.      Export Regulations. Software, including technical data, is
subject to U.S. export control laws, including the U.S. Export
Administration Act and its associated regulations, and may be subject
to export or import regulations in other countries. Customer agrees to
comply strictly with all such regulations and acknowledges that it has
the responsibility to obtain licenses to export, re-export, or import
Software. Software may not be downloaded, or otherwise exported or
re-exported (i) into, or to a national or resident of, Cuba, Iraq,
Iran, North Korea, Libya, Sudan, Syria or any country to which the U.S.
has embargoed goods; or (ii) to anyone on the U.S. Treasury
Department's list of Specially Designated Nations or the U.S. Commerce
Department's Table of Denial Orders.

8.      Restricted Rights. Use, duplication or disclosure by the United
States government is subject to the restrictions as set forth in the
Rights in Technical Data and Computer Software Clauses in DFARS
252.227-7013(c) (1) (ii) and FAR 52.227-19(c) (2) as applicable.

9.      Governing Law. Any action related to this Agreement will be
governed by California law and controlling U.S. federal law. No choice
of law rules of any jurisdiction will apply.

10.     Severability. If any of the above provisions are held to be in
violation of applicable law, void, or unenforceable in any
jurisdiction, then such provisions are herewith waived or amended to
the extent necessary for the Agreement to be otherwise enforceable in
such jurisdiction.   However, if in Sun's opinion deletion or amendment
of any provisions of the Agreement by operation of this paragraph
unreasonably compromises the rights or increase the liabilities of Sun
or its licensors, Sun reserves the right to terminate the Agreement.
*/

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import javax.activation.DataSource;

public class ByteArrayDataSource
    implements DataSource
{
    private byte[] data; // data

    private String type; // content-type

    /* Create a DataSource from a byte array */

    public ByteArrayDataSource( byte[] data, String type, String encoding )
    {
        this.data = data;
        this.type = type + "; charset=" + encoding;
    }

    /* Create a DataSource from an input stream */

    public ByteArrayDataSource( InputStream is, String type, String encoding )
    {
        this.type = type + "; charset=" + encoding;
        try
        {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int ch;

            while ( ( ch = is.read() ) != -1 )
            // XXX - must be made more efficient by
            // doing buffered reads, rather than one byte reads
            {
                os.write( ch );
            }
            data = os.toByteArray();

        }
        catch ( IOException ioex )
        {
        }
    }

    /* Create a DataSource from a String */

    public ByteArrayDataSource( String data, String type, String encoding )
    {
        try
        {
            // Assumption that the string contains only ASCII
            // characters!  Otherwise just pass a charset into this
            // constructor and use it in getBytes()
            this.data = data.getBytes( encoding );
        }
        catch ( UnsupportedEncodingException uex )
        {
        }
        this.type = type + "; charset=" + encoding;
    }

    public String getContentType()
    {
        return type;
    }

    /**
     * Return an InputStream for the data. Note - a new stream must be returned each time.
     */
    public InputStream getInputStream()
        throws IOException
    {
        if ( data == null )
        {
            throw new IOException( "no data" );
        }
        return new ByteArrayInputStream( data );
    }

    public String getName()
    {
        return "dummy";
    }

    public OutputStream getOutputStream()
        throws IOException
    {
        throw new IOException( "cannot do this" );
    }
}
