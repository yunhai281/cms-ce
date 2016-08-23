package com.enonic.cms.core.content;

import java.util.List;

import com.enonic.cms.core.content.contentdata.ContentData;
import com.enonic.cms.core.content.contentdata.custom.CustomContentData;
import com.enonic.cms.core.content.contentdata.custom.DataEntry;
import com.enonic.cms.core.content.contentdata.custom.stringbased.TextDataEntry;
import com.enonic.cms.core.content.contenttype.dataentryconfig.DataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.TextDataEntryConfig;

public class ContentRegExpValidator
{

    public static void validate( ContentData contentData )
    {
        if ( contentData instanceof CustomContentData )
        {
            CustomContentData ccData = (CustomContentData) contentData;

            final List<DataEntryConfig> inputConfigs = ccData.getContentTypeConfig().getInputConfigs();
            for ( DataEntryConfig inputConfig : inputConfigs )
            {
                if ( inputConfig instanceof TextDataEntryConfig )
                {
                    String regExp = ( (TextDataEntryConfig) inputConfig ).getRegExp();
                    if ( ( regExp != null ) && ( !regExp.equals( "" ) ) )
                    {
                        String name = inputConfig.getName();
                        DataEntry dataEntry = ccData.getEntry( name );
                        if (!(dataEntry instanceof TextDataEntry)) {
                            throw new IllegalArgumentException( "The input field, " + name + " is expected to be a text input, but is not." );
                        }
                        TextDataEntry textDataEntry = (TextDataEntry) dataEntry;
                        String value = textDataEntry.getValue();
                        if ( !value.isEmpty() && !value.matches( regExp ) )
                        {
                            throw new IllegalArgumentException(
                                "The value: " + value + ", does not match the validation reg exp: " + regExp );
                        }
                    }
                }
            }
        }
    }
}
