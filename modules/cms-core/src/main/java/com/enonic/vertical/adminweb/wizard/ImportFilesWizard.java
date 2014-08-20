package com.enonic.vertical.adminweb.wizard;

import java.io.File;

import com.enonic.esl.containers.ExtendedMap;
import com.enonic.vertical.adminweb.VerticalAdminException;
import com.enonic.vertical.adminweb.handlers.ContentBaseHandlerServlet;

import com.enonic.cms.core.content.binary.BinaryData;
import com.enonic.cms.core.service.AdminService;

public class ImportFilesWizard
    extends ImportZipWizard
{
    private static final long serialVersionUID = 2300023L;

    protected String cropName( String name )
    {
        return name;
    }

    protected boolean isFiltered( String name )
    {
        return false;
    }

    protected BinaryData[] getBinaries( ContentBaseHandlerServlet cbhServlet, AdminService admin, ExtendedMap formItems, File file )
        throws VerticalAdminException
    {
        formItems.put( "newfile", new DummyFileItem( file ) );
        return cbhServlet.getContentXMLBuilder().getBinaries( formItems );
    }
}
