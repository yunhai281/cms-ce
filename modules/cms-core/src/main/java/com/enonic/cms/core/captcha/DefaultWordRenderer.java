/*
 * adapted to use in Enonic CMS
 * changed YOFFSET only
 * based on James Childers work
 *
 * Copyright (c) 2008, James Childers
 * All rights reserved.
 */

package com.enonic.cms.core.captcha;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

import nl.captcha.text.renderer.WordRenderer;

/**
 * Renders the answer onto the image.
 */
final class DefaultWordRenderer
    implements WordRenderer
{
    private static final Random RAND = new SecureRandom();

    private static final double YOFFSET = 0.5;

    private static final double XOFFSET = 0.05;

    private final List<Color> colors;

    private final List<Font> fonts;

    public DefaultWordRenderer( final List<Color> colors, final List<Font> fonts )
    {
        this.colors = colors;
        this.fonts = fonts;
    }

    @Override
    public void render( final String word, BufferedImage image )
    {
        Graphics2D g = image.createGraphics();

        RenderingHints hints = new RenderingHints( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        hints.add( new RenderingHints( RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY ) );
        g.setRenderingHints( hints );

        FontRenderContext frc = g.getFontRenderContext();
        int xBaseline = (int) Math.round( image.getWidth() * XOFFSET );
        int yBaseline = image.getHeight() - (int) Math.round( image.getHeight() * YOFFSET );

        char[] chars = new char[1];
        for ( char c : word.toCharArray() )
        {
            chars[0] = c;

            g.setColor( this.colors.get( RAND.nextInt( this.colors.size() ) ) );

            int choiceFont = RAND.nextInt( this.fonts.size() );
            Font font = this.fonts.get( choiceFont );
            g.setFont( font );

            GlyphVector gv = font.createGlyphVector( frc, chars );
            g.drawChars( chars, 0, chars.length, xBaseline, yBaseline );

            int width = (int) ( gv.getVisualBounds().getWidth() * 1.3f );
            xBaseline = xBaseline + width;
        }
    }
}
