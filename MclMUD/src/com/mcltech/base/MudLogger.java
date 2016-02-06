package com.mcltech.base;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * A simple singleton logger that won't block and can take multi-threaded input and
 * writes in its own time to the log
 * @author andymac
 *
 */
public class MudLogger implements Runnable
{
   private Thread writer;
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
         handler.setFormatter(new OneLineFormatter());
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
      
      writer = new Thread(this);
      writer.start();
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
    * Stop writing the log. Shut down the thread.
    */
   public void stop()
   {
      writer.interrupt();
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
            if (info != null)
            {
               if (info.thrown == null)
               {
                  log.log(info.level, info.message);
               }
               else
               {
                  log.log(info.level, info.message, info.thrown);
               }
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
   
   private final class OneLineFormatter extends Formatter
   {
      private final String LINE_SEPARATOR = System.getProperty("line.separator");
      
      public OneLineFormatter()
      {
         
      }

      @Override
      public String format(LogRecord record)
      {
         StringBuilder sb = new StringBuilder();

         sb.append(new Date(record.getMillis()))
             .append(":")
             .append(record.getLevel().getLocalizedName())
             .append(": ")
             .append(formatMessage(record));
         if (!record.getMessage().endsWith("\n"))
             sb.append(LINE_SEPARATOR);

         if (record.getThrown() != null) {
             try {
                 StringWriter sw = new StringWriter();
                 PrintWriter pw = new PrintWriter(sw);
                 record.getThrown().printStackTrace(pw);
                 pw.close();
                 sb.append(sw.toString());
             } catch (@SuppressWarnings("unused") Exception ex) {
                 // ignore
             }
         }

         return sb.toString();
      }
      
   }
}
