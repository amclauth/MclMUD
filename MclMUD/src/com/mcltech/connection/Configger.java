package com.mcltech.connection;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import com.mcltech.base.MudLogger;

/**
 * A static / synchronized class to read and write information from a config file on the fly. Overkill
 * here, but I had most of it from an earlier project.
 * @author amclauthlin
 *
 */
public class Configger
{
   private static final MudLogger log = MudLogger.getInstance();
   private final static String propFileName = "config/config.txt";
   
   private static ConcurrentHashMap<String,String> properties = new ConcurrentHashMap<>();
   
   /**
    * Get the property or the default value
    * @param key
    * @param defaultVal
    * @return
    */
   public static String getProperty(String key, String defaultVal)
   {
      if (properties.get(key) == null)
         sync();
      
      String val = properties.get(key);
      return val == null ? defaultVal : val;
   }
   
   /**
    * Set the key / value pair
    * @param key
    * @param val
    * @return
    */
   public static void setProperty(String key, String val)
   {
      properties.put(key,val);
      log.add(Level.FINE, "Saving " + key + " => " + val + " to config");
      sync();
   }
   
   /**
    * Sync values between the file and the current system information
    * @return
    */
   private static synchronized boolean sync()
   {
      log.add(Level.FINE, "Syncing config file");
      Properties propTmp = new Properties();
      
      boolean loaded = false;
      try (FileInputStream in = new FileInputStream(propFileName))
      {
         propTmp.load(in);
         in.close();
         loaded = true;
      }
      catch (FileNotFoundException e)
      {
         log.add(Level.INFO, "ERROR: Could not find file: " + propFileName + " to read. Will create a new one.", e);
      }
      catch (IOException e)
      {
         log.add(Level.SEVERE, "ERROR: Could not read file: " + propFileName, e);
         return false;
      }
      
      boolean isDirty = !loaded;
      // Compare values
      for (String key : properties.keySet())
      {
         if (!propTmp.containsKey(key))
         {
            propTmp.put(key, properties.get(key));
            isDirty = true;
         }
         else if (!propTmp.getProperty(key).equals(properties.get(key)))
         {
            propTmp.setProperty(key, properties.get(key));
            isDirty = true;
         }
      }
      for (Object key : propTmp.keySet())
      {
         if (!properties.containsKey(key))
         {
            properties.put((String)key, propTmp.getProperty((String) key));
            isDirty = true;
         }
      }
      
      if (isDirty)
      {
         log.add(Level.FINE, "Saving config file.");
         try (FileOutputStream out = new FileOutputStream(propFileName))
         {
            Date date = new Date();
            propTmp.store(out, "--- Saved on " + date.toString() + " ---");
            out.close();
         }
         catch (FileNotFoundException e)
         {
            log.add(Level.SEVERE, "ERROR: Could not find file to write: " + propFileName, e);
            return false;
         }
         catch (IOException e)
         {
            log.add(Level.SEVERE, "ERROR: Could not write to file: " + propFileName, e);
            return false;
         }
         
         return true;
      }
      
      return true;
   }
}
