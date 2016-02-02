package com.mcltech.ai;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.eclipse.swt.custom.StyleRange;

import com.mcltech.base.MudLogger;
import com.mcltech.connection.AnsiParser;

public abstract class AIListener implements Runnable
{
   // formatter needs to be serial. Pass writes to this first for commands and aliases. Triggers / scripts
   // should be handled in a different thread
   private static final MudLogger log = MudLogger.getInstance();

   protected LinkedBlockingQueue<String> lineQueue;
   protected boolean listening = false;

   public AIListener()
   {
      lineQueue = new LinkedBlockingQueue<>();
   }

   /**
    * Register this to the AnsiParser
    * TODO - if we change AnsiParser to implement a "Parser" interface, update this as well
    */
   public void register()
   {
      AnsiParser.registerListener(this);
      Thread poller = new Thread(this);
      poller.start();
   }

   /**
    * Deregister the listener
    */
   public void deregister()
   {
      listening = false;
      AnsiParser.deRegisterListener(this);
   }

   /**
    * Add the line to the queue for trigger / script processing
    * @param line
    */
   private void add(String line)
   {
      lineQueue.add(line);
   }

   /**
    * Poll the queue for processing triggers and scripts
    */
   @Override
   public void run()
   {
      listening = true;
      String line = null;
      while (listening)
      {
         try
         {
            line = lineQueue.poll(250, TimeUnit.MILLISECONDS);
            if (listening)
            {
               trigger(line);
            }
         }
         catch (InterruptedException e)
         {
            log.add(Level.WARNING, "Listener interrupted: ", e);
            listening = false;
            return;
         }
      }
   }

   /**
    * Add the line and styles for formatting updates / corrections
    * @param line
    * @param ranges
    * @return
    */
   public String process(String line, List<StyleRange> ranges)
   {
      add(line);
      return format(line,ranges);
   }
   
   /**
    * Format the line. Return null if it shouldn't be printed.
    * @param line
    * @param ranges
    * @return
    */
   abstract protected String format(String line, List<StyleRange> ranges);
   
   /**
    * Process any triggers or scripts based on this line
    * @param line
    */
   abstract protected void trigger(String line);
}
