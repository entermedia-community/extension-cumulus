/*
 * Created on Sep 1, 2006
 */
package com.openedit.archive.cumulus;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import com.openedit.OpenEditRuntimeException;
import com.openedit.util.FileUtils;

public class ImageToConvert
{
	protected int fieldWidth;	
	protected int fieldHeight;
	protected Image fieldImage;
	
	public int getWidth()
	{
		return fieldWidth;
	}
	public void setWidth(int inWidth)
	{
		fieldWidth = inWidth;
	}
	public int getHeight()
	{
		return fieldHeight;
	}
	public void setHeight(int inHeight)
	{
		fieldHeight = inHeight;
	}
	public Image getImage()
	{
		return fieldImage;
	}
	public void setImage(Image inImage)
	{
		fieldImage = inImage;
	}
	public void saveTo(OutputStream inOut)
	{
		BufferedImage scaledImage = new BufferedImage( getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB );
		Graphics2D scaledGraphics = scaledImage.createGraphics();
		scaledGraphics.setRenderingHint( RenderingHints.KEY_INTERPOLATION,
			RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		scaledGraphics.drawImage( getImage(), 0, 0, getWidth(),getHeight(), null );
		try
        {
        	//log.info("Saved: " + scaledImage.getWidth() );
	    	//out = new FileOutputStream(getOutput());
        	ImageIO.write(scaledImage, "jpg",  inOut);
        }
        catch (IOException ex)
        {
        	throw new OpenEditRuntimeException(ex);
        }
        finally
        {
    		FileUtils.safeClose(inOut);
        }
	}
}
