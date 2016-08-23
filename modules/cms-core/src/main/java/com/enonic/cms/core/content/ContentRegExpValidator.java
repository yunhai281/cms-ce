package com.enonic.cms.core.content;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.enonic.cms.core.content.contentdata.ContentData;
import com.enonic.cms.core.content.contentdata.custom.CustomContentData;
import com.enonic.cms.core.content.contentdata.custom.DataEntry;
import com.enonic.cms.core.content.contentdata.custom.GroupDataEntry;
import com.enonic.cms.core.content.contentdata.custom.stringbased.TextDataEntry;
import com.enonic.cms.core.content.contenttype.ContentTypeConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.DataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.TextDataEntryConfig;

public class ContentRegExpValidator
{

    public static void validate( ContentData contentData )
    {
        if ( contentData instanceof CustomContentData )
        {
            CustomContentData ccData = (CustomContentData) contentData;

            final List<DataEntry> dataEntries = ccData.getEntries();
            validateDataEntries( ccData.getContentTypeConfig(), dataEntries );
        }
    }

    private static void validateDataEntries( final ContentTypeConfig contentTypeConfig, final List<DataEntry> dataEntries )
    {
        for ( DataEntry dataEntry : dataEntries )
        {
            if ( dataEntry instanceof TextDataEntry )
            {
                final TextDataEntry textDataEntry = (TextDataEntry) dataEntry;
                String textDataEntryName = textDataEntry.getName();
                DataEntryConfig inputConfig = contentTypeConfig.getInputConfig( textDataEntryName );
                if ( inputConfig instanceof TextDataEntryConfig )
                {
                    String regExp = ( (TextDataEntryConfig) inputConfig ).getRegExp();

                    if ( StringUtils.isNotEmpty( regExp ) )
                    {
                        String value = textDataEntry.getValue();
                        Boolean isRequired = inputConfig.isRequired();

                        if ( ( isRequired || StringUtils.isNotEmpty( value ) ) && !value.matches( regExp ) )
                        {
                            throw new IllegalArgumentException(
                                "The value: " + textDataEntry.getValue() + ", does not match the validation reg exp: " + regExp );
                        }
                    }
                }
            }

            if ( dataEntry instanceof GroupDataEntry )
            {
                final GroupDataEntry groupDataEntry = (GroupDataEntry) dataEntry;
                validateDataEntries( contentTypeConfig, groupDataEntry.getEntries() );
            }
        }
    }
}
