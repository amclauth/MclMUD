package com.mcltech.ai;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.mcltech.connection.AnsiParser;

public abstract class AIListener
{

   private static final Logger log = Logger.getLogger(AIListener.class.getName());
   
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
      while(listening)
      {
         try
         {
            line = lineQueue.poll(250, TimeUnit.MILLISECONDS);
            if (listening)
               process(line);
         }
         catch (InterruptedException e)
         {
            log.warning("Listener interrupted: " + e.getMessage());
            listening = false;
            return;
         }
      }
   }
   
   abstract protected void process(String line);
}
