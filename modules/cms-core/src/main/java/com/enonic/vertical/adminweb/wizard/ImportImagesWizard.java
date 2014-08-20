package com.enonic.vertical.adminweb.wizard;

import java.io.File;

import com.enonic.esl.containers.ExtendedMap;
import com.enonic.vertical.adminweb.VerticalAdminException;
import com.enonic.vertical.adminweb.handlers.ContentBaseHandlerServlet;

import com.enonic.cms.core.content.binary.BinaryData;
import com.enonic.cms.core.service.AdminService;

public class ImportImagesWizard
    extends ImportZipWizard
{
    private static final long serialVersionUID = 3400034L;

    protected String cropName( String name )
    {
        int dotIdx = name.lastIndexOf( '.' );
        if ( dotIdx > 0 )
        {
            return name.substring( 0, dotIdx );
        }
        else
        {
            return name;
        }
    }

    protected boolean isFiltered( String name )
    {
        int dotIdx = name.lastIndexOf( '.' );
        if ( dotIdx > 0 )
        {
            String extension = name.substring( dotIdx + 1 );
            return "jpg".equalsIgnoreCase( extension ) == false && "jpeg".equalsIgnoreCase( extension ) == false &&
                "png".equalsIgnoreCase( extension ) == false && "gif".equalsIgnoreCase( extension ) == false;
        }
        return true;
    }

    protected BinaryData[] getBinaries( ContentBaseHandlerServlet cbhServlet, AdminService admin, ExtendedMap formItems, File file )
        throws VerticalAdminException
    {
        formItems.put( "origimagefilename", new DummyFileItem( file ) );
        return cbhServlet.getContentXMLBuilder().getBinaries( formItems );
    }
}