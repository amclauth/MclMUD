package com.mcltech.ai;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.eclipse.swt.custom.StyleRange;

import com.mcltech.base.MudLogger;
import com.mcltech.connection.MudFrame;

public class AIListener implements Runnable
{
   // formatter needs to be serial. Pass writes to this first for commands and aliases. Triggers / scripts
   // should be handled in a different thread
   private static final MudLogger log = MudLogger.getInstance();

   protected LinkedBlockingQueue<String> lineQueue;
   protected boolean listening = false;
   private String name;
   private Map<String,String[]> aliases;
   private MudFrame frame;
   private Thread poller;
   private AIInterface ai;
   private Map<String,AIInterface> AIMap = new HashMap<>();

   public AIListener(MudFrame frame, String name)
   {
      AIMap.put("basic", new BasicAI());
      AIMap.put("m.u.m.e.", new MumeAI(frame));
      
      lineQueue = new LinkedBlockingQueue<>();
      ai = new BasicAI();
      this.frame = frame;
      this.name = name;
      loadAliases();
      poller = new Thread(this);
      poller.start();
   }

   /**
    * Deregister the listener (so it can be swapped)
    */
   public void deregister()
   {
      listening = false;
      poller.interrupt();
      ai.stop();
   }
   
   /**
    * Add an alias string
    * @param aliasString in the form: name:command1;command2;...
    * @return
    */
   protected boolean addAlias(String aliasString)
   {
      int idx = aliasString.indexOf(':');
      if (idx <= 0 || idx == aliasString.length() - 1)
      {
         log.add(Level.WARNING, "Alias is improperly formatted. {" + aliasString + "}");
         frame.writeToTextBox("Alias is improperly formatted. {" + aliasString + "}", null);
         return false;
      }
      String alias = aliasString.substring(0, idx);
      String[] commands = aliasString.substring(idx+2).split(";");
      // double check
      if (commands.length == 0 || alias.length() == 0)
      {
         log.add(Level.WARNING, "Alias is improperly formatted. {" + aliasString + "}");
         frame.writeToTextBox("Alias is improperly formatted. {" + aliasString + "}", null);
         return false;
      }
      aliases.put(alias, commands);
      writeAliases();
      return true;
   }
   
   /**
    * Write the alias file
    */
   private void writeAliases()
   {
      if (name == null || name.isEmpty())
         return;
      
      File aliasFile = new File("config/" + name + ".alias");
      if (!aliasFile.exists())
      {
         try
         {
            aliasFile.createNewFile();
         }
         catch (IOException e)
         {
            log.add(Level.WARNING, "Can't create alias file {" + aliasFile.getAbsolutePath() + "}", e);
         }
      }
      
      try (BufferedWriter bw = new BufferedWriter(new FileWriter(aliasFile)))
      {
         for (String alias : aliases.keySet())
         {
            bw.write(alias + ":" + String.join(";", aliases.get(alias)));
         }
         bw.close();
      }
      catch (IOException e)
      {
         log.add(Level.SEVERE,"Couldn't write alias file {" + aliasFile.getAbsolutePath() + "}", e);
         frame.writeToTextBox("Couldn't write alias file {" + aliasFile.getAbsolutePath() + "}", null);
      }
   }
   
   /**
    * Load alias file into aliases
    */
   private void loadAliases()
   {
      if (name == null || name.isEmpty())
         return;
      
      aliases = new HashMap<>();
      File aliasFile = new File("config/" + name.toLowerCase() + ".alias");
      if (!aliasFile.exists())
      {
         return;
      }
      
      try (BufferedReader br = new BufferedReader(new FileReader(aliasFile)))
      {
         for (String line; (line = br.readLine()) != null; )
         {
            int idx = line.indexOf(':');
            if (idx <= 0 || idx == line.length() - 1)
            {
               log.add(Level.WARNING, "Alias is improperly formatted. {" + line + "}");
               continue;
            }
            String alias = line.substring(0, idx);
            String[] commands = line.substring(idx+2).split(";");
            // double check
            if (commands.length == 0 || alias.length() == 0)
               continue;
            aliases.put(alias, commands);
         }
         br.close();
      }
      catch (@SuppressWarnings("unused") FileNotFoundException e)
      {
         // of course it's found, we just checked that it exists. Do nothing just in case.
      }
      catch (IOException e)
      {
         log.add(Level.SEVERE,"Couldn't read alias file {" + aliasFile.getAbsolutePath() + "}", e);
         frame.writeToTextBox("Couldn't read alias file {" + aliasFile.getAbsolutePath() + "}", null);
      }
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
               ai.trigger(line);
            }
         }
         catch (InterruptedException e)
         {
            log.add(Level.INFO, "Listener interrupted: ", e);
            listening = false;
            return;
         }
      }
   }
   
   private String[] handleAlias(String alias, String line)
   {
      if (line.equals(alias))
      {
         return aliases.get(alias);
      }
      String[] extras = line.substring(alias.length() + 1).split(" ");
      String[] aliasCommands = aliases.get(alias);
      String[] commands = new String[aliasCommands.length];
      int extraIdx = 0;
      for (int ii = 0; ii < commands.length; ii++)
      {
         commands[ii] = aliasCommands[ii];
         while (commands[ii].contains("%%"))
         {
            commands[ii].replaceFirst("%%",extras[extraIdx]);
         }
      }
      return commands;
   }

   /**
    * Add the line and styles for formatting updates / corrections
    * @param line
    * @param ranges
    * @return
    */
   public String processOutput(String line, List<StyleRange> ranges)
   {
      add(line);
      return ai.format(line,ranges);
   }
   
   /**
    * This handles things like alias creation
    * @param line
    * @return null or the command to be sent
    */
   public String[] processCommand(String line)
   {
      if (line.startsWith("alias"))
      {
         if (line.trim().equals("alias"))
         {
            StringBuilder buf = new StringBuilder();
            buf.append("\n\nAliases:\n");
            String[] keys = aliases.keySet().toArray(new String[0]);
            Arrays.sort(keys);
            for (String key : keys)
            {
               buf.append("  " + key + " -> " + String.join(";", aliases.get(key)) + "\n");
            }
            buf.append("\n");
            frame.writeToTextBox(buf.toString(), null);
            return null;
         }

         addAlias(line.substring(6));
         return null;
      }
      else if (line.startsWith("#loadAI ") && line.length() > 8)
      {
         swapAI(line.substring(8));
         return null;
      }
      for (String alias : aliases.keySet())
      {
         String in = line.trim();
         if (in.equals(alias) || in.startsWith(alias + " "))
         {
            return handleAlias(alias,in);
         }
      }
      return ai.command(line);
   }
   
   /**
    * Swap out the AI
    * @param aiName
    */
   public boolean swapAI(String aiName)
   {
      if (ai != null)
         ai.stop();
      
      AIInterface newAI = AIMap.get(aiName.toLowerCase());
      if (newAI != null)
      {
         ai = newAI;
         name = aiName;
         frame.writeToTextBox("Now using AI: " + name, null);
         ai.start();
         return true;
      }

      frame.writeToTextBox("AI by name {" + aiName + "} not found.", null);
      frame.writeToTextBox("Currently registered AI's: " + Arrays.toString(AIMap.keySet().toArray(new String[0])), null);
      frame.writeToTextBox("Currently using AI: " + name, null);
      return false;
   }
}
