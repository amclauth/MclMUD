package com.mcltech.base;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MudLogger implements Runnable
{
   private static final class holder
   {
      static final MudLogger INSTANCE = new MudLogger();
   }

   private class LogInfo
   {
      public Level level;
      public String message;
      public Throwable thrown;

      public LogInfo(Level level, String message, Throwable thrown)
      {
         this.level = level;
         this.message = message;
         this.thrown = thrown;
      }
   }

   MudLogger()
   {
      queue = new LinkedBlockingQueue<>();
   }

   public static MudLogger get()
   {
      return holder.INSTANCE;
   }

   private static final Logger log = Logger.getLogger(MudLogger.class.getName());
   private static LinkedBlockingQueue<LogInfo> queue;

   public void add(Level level, String message)
   {
      add(level, message, null);
   }

   public void add(Level level, String message, Throwable thrown)
   {
      queue.add(new LogInfo(level, message, thrown));
   }

   @Override
   public void run()
   {
      boolean interrupted = false;
      do
      {
         try
         {
            LogInfo info = queue.poll(1, TimeUnit.SECONDS);
            if (info.thrown == null)
            {
               log.log(info.level, info.message);
            }
            else
            {
               log.log(info.level, info.message, info.thrown);
            }
         }
         catch (@SuppressWarnings("unused")
         InterruptedException e)
         {
            interrupted = true;
         }
      }
      while (!interrupted);
   }
}
