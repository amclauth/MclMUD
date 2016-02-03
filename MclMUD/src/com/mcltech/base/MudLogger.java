package com.mcltech.base;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple singleton logger that won't block and can take multi-threaded input and
 * writes in its own time to the log
 * @author andymac
 *
 */
public class MudLogger implements Runnable
{
   // singleton class holder pattern
   private static final class holder
   {
      static final MudLogger INSTANCE = new MudLogger();
   }

   public static MudLogger getInstance()
   {
      return holder.INSTANCE;
   }

   MudLogger()
   {
      queue = new LinkedBlockingQueue<>();
      
      // make sure the folders exist
      File logFolder = new File("logs");
      File configFolder = new File("config");
      if (!logFolder.exists())
      {
         logFolder.mkdirs();
      }
      if (!configFolder.exists())
      {
         configFolder.mkdirs();
      }
      
      // set up the logger
      Handler handler = null;
      try
      {
         handler = new FileHandler("logs/mclmud.log", 10 * 1024 * 1024, 5);
      }
      catch (IOException e)
      {
         System.err.println("Error creating log file: logs/mclmud.log: " + e.getMessage());
         System.exit(1);
         return;
      }

      handler.setLevel(Level.ALL);
      Logger.getLogger("").addHandler(handler);
//      Logger.getLogger("").setUseParentHandlers(false);
   }

   /**
    * A holder class to hold the normal logging info for the queue
    * @author andymac
    */
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

   // where we'll actually log to
   private static final Logger log = Logger.getLogger(MudLogger.class.getName());
   private static LinkedBlockingQueue<LogInfo> queue;

   /**
    * Add this to be logged
    * @param level
    * @param message
    */
   public void add(Level level, String message)
   {
      add(level, message, null);
   }

   /**
    * Add this to be logged
    * @param level
    * @param message
    * @param thrown
    */
   public void add(Level level, String message, Throwable thrown)
   {
      queue.add(new LogInfo(level, message, thrown));
   }

   /**
    * Thread to log everything in the queue
    */
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
