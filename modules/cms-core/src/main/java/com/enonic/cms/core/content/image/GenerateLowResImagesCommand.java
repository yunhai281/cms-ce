package com.enonic.cms.core.content.image;

import org.springframework.util.Assert;

import com.enonic.cms.api.client.model.StandardImageSize;
import com.enonic.cms.core.content.command.BaseContentCommand;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserKey;

public class GenerateLowResImagesCommand
    extends BaseContentCommand
{
    /**
     * List categories in which all images should be (re)generated, or NULL to (re)generate low res images of all images in entire archive.
     */
    private Integer[] categoryKeys;

    /**
     * Set to specific size, or NULL, if all sizes should be (re)genenerated.
     */
    private StandardImageSize imageSize;

    /**
     * This operation is modifying content without changing ownership, so only an Enterprise Administrator is allowed to do it.
     */
    private UserKey modifier;


    public Integer[] getCategoryKeys()
    {
        return categoryKeys;
    }

    public StandardImageSize getImageSize()
    {
        return imageSize;
    }

    public UserKey getModifier()
    {
        return modifier;
    }

    public void setCategoryKeys( final Integer[] categoryKeys )
    {
        this.categoryKeys = categoryKeys;
    }

    public void setImageSize( final StandardImageSize imageSize )
    {
        this.imageSize = imageSize;
    }

    public void setModifier( UserEntity value )
    {
        Assert.notNull( value );
        this.modifier = value.getKey();
    }

    public void setModifier( UserKey value )
    {
        Assert.notNull( value );
        this.modifier = value;
    }


}
