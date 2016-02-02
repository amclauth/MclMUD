package com.mcltech.ai;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.eclipse.swt.custom.StyleRange;

import com.mcltech.base.MudLogger;
import com.mcltech.connection.AnsiParser;

public abstract class AIListener
{
   // formatter needs to be serial. Pass writes to this first for commands and aliases. Triggers / scripts
   // should be handled in a different thread

   private static final MudLogger log = MudLogger.get();

   protected LinkedBlockingQueue<String> lineQueue;
   protected boolean listening = false;

   public AIListener()
   {
      lineQueue = new LinkedBlockingQueue<>();
   }

   public void register()
   {
      AnsiParser.registerListener(this);
      poll();
   }

   public void deregister()
   {
      listening = false;
      AnsiParser.deRegisterListener(this);
   }

   public void add(String line)
   {
      lineQueue.add(line);
   }

   protected void poll()
   {
      listening = true;
      String line = null;
      while (listening)
      {
         try
         {
            line = lineQueue.poll(250, TimeUnit.MILLISECONDS);
            // if (listening)
            // process(line);
         }
         catch (InterruptedException e)
         {
            log.add(Level.WARNING, "Listener interrupted: ", e);
            listening = false;
            return;
         }
      }
   }

   abstract public void process(List<StyleRange> ranges, String line);
}
