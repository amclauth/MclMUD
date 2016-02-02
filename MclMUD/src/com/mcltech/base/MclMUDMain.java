package com.mcltech.base;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mcltech.connection.MudFrame;

public class MclMUDMain
{

   public static void main(String[] args)
   {
      initLogger();

      (new MudFrame()).init();
   }

   private static void initLogger()
   {
      Handler handler = null;
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
      try
      {
         handler = new FileHandler("logs/mclmud.log", 10 * 1024 * 1024, 5);
      }
      catch (SecurityException e)
      {
         System.err.println("Error creating log file: logs/mclmud.log: " + e.getMessage());
         System.exit(1);
         return;
      }
      catch (IOException e)
      {
         System.err.println("Error creating log file: logs/mclmud.log: " + e.getMessage());
         System.exit(1);
         return;
      }

      handler.setLevel(Level.ALL);
      Logger.getLogger("").addHandler(handler);
      Logger.getLogger("").setUseParentHandlers(false);
   }

}
