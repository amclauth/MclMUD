package com.mcltech.ai;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.eclipse.swt.custom.StyleRange;

import com.mcltech.ai.mume.MumeAI;
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
   private Map<String, String[]> aliases;
   private Thread poller;
   private AIInterface ai;
   private TreeMap<String, AIInterface> AIMap = new TreeMap<>(new DescendingWordLengthComparator());
   private Pattern percentPattern = Pattern.compile("\\%\\%");
   private Pattern percentPattern2 = Pattern.compile("\\%\\%\\ ");

   public AIListener(MudFrame frame, String name)
   {
      AIMap.put("basic", new BasicAI());
      AIMap.put("m.u.m.e.", new MumeAI(frame));

      lineQueue = new LinkedBlockingQueue<>();
      ai = new BasicAI();
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
    * 
    * @param aliasString in the form: name:command1;command2;...
    * @return true if action performed of some kind, false on error
    */
   protected boolean addAlias(String aliasString)
   {
      int idx = aliasString.indexOf(':');
      
      if (idx <= 0 && aliasString.length() > 0 && aliases.containsKey(aliasString))
      {
         MudFrame.writeToTextBox("\n  " + aliasString + " => " + String.join(";",aliases.get(aliasString)) + "\n",null);
         return true;
      }
      
      if (idx <= 0)
      {
         log.add(Level.WARNING, "Alias is improperly formatted. {" + aliasString
               + "} should be in format \"alias name:command;command;command;...\"");
         MudFrame.writeToTextBox("Alias is improperly formatted. {" + aliasString
               + "} should be in format \"alias name:command;command;command;...\"\n", null);
         return false;
      }

      String alias = aliasString.substring(0, idx);
      if (idx == aliasString.length()-1 && idx > 0)
      {
         // clear alias
         aliases.remove(alias);
         return true;
      }
      
      String[] commands = aliasString.substring(idx + 1).split(";");
      // double check
      if (commands.length == 0 || alias.length() == 0)
      {
         log.add(Level.WARNING, "Alias is improperly formatted. {" + aliasString
               + "} should be in format \"alias name:command;command;command;...\"");
         MudFrame.writeToTextBox("Alias is improperly formatted. {" + aliasString
               + "} should be in format \"alias name:command;command;command;...\"\n", null);
         return false;
      }
      aliases.put(alias, commands);
      writeAliases();
      MudFrame.writeToTextBox("\nAdded alias " + alias + " -> " + String.join(";", commands) + "\n", null);
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
            bw.write(alias + ":" + String.join(";", aliases.get(alias)) + "\n");
         }
         bw.close();
      }
      catch (IOException e)
      {
         log.add(Level.SEVERE, "Couldn't write alias file {" + aliasFile.getAbsolutePath() + "}", e);
         MudFrame.writeToTextBox("Couldn't write alias file {" + aliasFile.getAbsolutePath() + "}", null);
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
         for (String line; (line = br.readLine()) != null;)
         {
            int idx = line.indexOf(':');
            if (idx <= 0 || idx == line.length() - 1)
            {
               log.add(Level.WARNING, "Alias is improperly formatted. {" + line + "}");
               continue;
            }
            String alias = line.substring(0, idx);
            String[] commands = line.substring(idx + 1).split(";");
            // double check
            if (commands.length == 0 || alias.length() == 0)
               continue;
            aliases.put(alias, commands);
         }
         br.close();
      }
      catch (@SuppressWarnings("unused")
      FileNotFoundException e)
      {
         // of course it's found, we just checked that it exists. Do nothing just in case.
      }
      catch (IOException e)
      {
         log.add(Level.SEVERE, "Couldn't read alias file {" + aliasFile.getAbsolutePath() + "}", e);
         MudFrame.writeToTextBox("Couldn't read alias file {" + aliasFile.getAbsolutePath() + "}", null);
      }
   }
   
   /**
    * Add the line to the queue for trigger / script processing
    * 
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
               if (ai.isTriggerer())
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
   
   private List<String> expandAlias(String command)
   {
      String[] words = command.split("\\s+");
      String[] aliasedCommands = aliases.get(words[0]);
      List<String> commands = new ArrayList<>();
      if (aliasedCommands == null)
      {
         commands.add(command);
         return commands;
      }

      for (String c : aliasedCommands)
      {
         int extraIdx = 1;
         while(c.contains("%%"))
         {
            if (extraIdx >= words.length)
            {
               c = percentPattern2.matcher(c).replaceAll("");
            }
            else
            {
               c = percentPattern.matcher(c).replaceFirst(words[extraIdx++]);
            }
         }
         while (extraIdx < words.length)
            c += " " + words[extraIdx++];
         commands.addAll(expandAlias(c));
      }
      
      return commands;
   }

   /**
    * Add the line and styles for formatting updates / corrections
    * 
    * @param line
    * @param ranges
    * @return
    */
   public String processOutput(String line, List<StyleRange> ranges)
   {
      String out = line.trim();
      if (ai.isFormatter())
         out = ai.format(line, ranges);
      if (out != null)
         add(line.trim());
      return out;
   }

   /**
    * This handles things like alias creation
    * 
    * @param line
    * @return null or the command to be sent
    */
   public List<String> processCommand(String input)
   {
      // handle empty
      List<String> commands = new ArrayList<>();
      if (input.isEmpty())
      {
         commands.add("");
         return commands;
      }
      
      // handle alias and loadAI
      if (input.startsWith("alias"))
      {
         if (input.equals("alias"))
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
            MudFrame.writeToTextBox(buf.toString(), null);
         }
         else
         {
            addAlias(input.substring(6));
         }
         return null;
      }
      else if (input.startsWith("#loadAI ") && input.trim().length() > 8)
      {
         swapAI(input.trim().substring(8));
         return null;
      }
      
      // handle individual commands
      for (String line : input.split(";"))
      {
         line = line.trim();
         if (line.isEmpty())
         {
            commands.add("");
            continue;
         }

         if (ai.isCommander() && ai.command(line))
         {
            continue;
         }
         
         commands.addAll(expandAlias(line));
      }
      
      if (commands.size() == 0)
         return null;
      return commands;
   }

   /**
    * Swap out the AI
    * 
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
         MudFrame.writeToTextBox("Now using AI: " + name, null);
         ai.start();
         loadAliases();
         return true;
      }

      MudFrame.writeToTextBox("AI by name {" + aiName + "} not found.\n", null);
      MudFrame.writeToTextBox(
            "Currently registered AI's: " + Arrays.toString(AIMap.keySet().toArray(new String[0])) + "\n",
            null);
      MudFrame.writeToTextBox("Currently using AI: " + name + "\n\n", null);
      return false;
   }

   private class DescendingWordLengthComparator implements Comparator<String>
   {

      public DescendingWordLengthComparator()
      {

      }

      @Override
      public int compare(String o1, String o2)
      {
         if (o1.length() == o2.length())
         {
            return o2.compareTo(o1);
         }
         if (o2.length() > o1.length())
            return 1;
         return -1;
      }

   }
}
