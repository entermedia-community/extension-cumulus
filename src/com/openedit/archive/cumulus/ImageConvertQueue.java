/*
 * Created on Sep 1, 2006
 */
package com.openedit.archive.cumulus;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ImageConvertQueue
{
	protected Timer fieldQueue;
	private static final Log log = LogFactory.getLog(ImageConvertQueue.class);
	
	protected int fieldCount;
	protected boolean fieldHasHitMax;
	
	
	public Timer getQueue()
	{
		if( fieldQueue == null)
		{
			fieldQueue = new Timer(true);
		}
		return fieldQueue;
	}

	public void setQueue(Timer inQueue)
	{
		fieldQueue = inQueue;
	}
	protected void reduce()
	{
		fieldCount--;
		if( fieldHasHitMax && fieldCount == 0)
		{
			log.info("Image Queue is now empty");
			fieldHasHitMax = false;
		}

	}
	protected void increase()
	{
		fieldCount++;
	}
	
	public void add(final Runnable inTask)
	{		
		if(( fieldCount > 300)) //We dont want this queue to get too big in case they cancel 
		{
			fieldHasHitMax = true;
			inTask.run();
			return;
		}
		increase();
		log.debug("Adding " + inTask);
		TimerTask task = new TimerTask()
		{
			public void run()
			{
				try
				{
					reduce();
					log.debug("Running " + inTask);
					inTask.run();
				}
				catch ( Throwable ex)
				{
					log.error("Image convert failed " + ex.getMessage() + " on  " + inTask );
				}
			}
		};
		getQueue().schedule(task, 0);
	}
}
