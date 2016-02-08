package com.mcltech.ai.mume;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;

import com.mcltech.ai.AIInterface;
import com.mcltech.base.MudLogger;
import com.mcltech.base.ServiceInterface;
import com.mcltech.connection.Configger;
import com.mcltech.connection.MudFrame;

public class MumeAI implements AIInterface
{
   
   static final MudLogger log = MudLogger.getInstance();
   
   public static MumeRoom currentRoom;
   
   List<AIInterface> services;
   List<AIInterface> triggerers;
   List<AIInterface> commanders;
   List<AIInterface> formatters;
   List<String> friendList;
   List<String> onlineFriends;
   
   Timer friendTimer;
   
   int silent_who_countdown = 0;
   private Map<String,String> tags;
   private Map<String,String> escapes;

   public MumeAI(MudFrame frame)
   {
      services = new ArrayList<>();
      services.add(new MumeTime(frame));
      services.add(new MumeFormatter());
      services.add(new MumeTriggers(this));
      services.add(new MumeMap());
      
      triggerers = new ArrayList<>();
      commanders = new ArrayList<>();
      formatters = new ArrayList<>();
      for (AIInterface i : services)
      {
         if (i.isTriggerer())
            triggerers.add(i);
         if (i.isCommander())
            commanders.add(i);
         if (i.isFormatter())
            formatters.add(i);
      }
      
      friendList = new ArrayList<>();
      onlineFriends = new ArrayList<>();
      String friendString = Configger.getProperty("MUMEFRIENDS", null);
      if (friendString != null)
      {
         for (String friend : friendString.split(";"))
         {
            friendList.add(friend);
         }
      }
      
      tags = new HashMap<>();
      tags.put("<prompt>","");
      tags.put("<room>", "");
      tags.put("<room><name>",""); 
      // can't use "<name>" because that's in the login screen. Jerks.
      tags.put("<description>","");
      tags.put("<status>","");
      tags.put("<terrain>","");
      tags.put("<magic>","");
      tags.put("<exits>","");
      tags.put("<tell>","");
      tags.put("<say>","");
      tags.put("<narrate>","");
      tags.put("<song>","");
      tags.put("<pray>","");
      tags.put("<shout>","");
      tags.put("<yell>","");
      tags.put("<emote>","");
      tags.put("<hit>","");
      tags.put("<damage>","");
      tags.put("<weather>","");
      tags.put("<highlight type=avoid_damage>", "");
      tags.put("<highlight type=damage>", "");
      tags.put("<highlight type=emote>", "");
      tags.put("<highlight type=enemy>", "");
      tags.put("<highlight type=exits>", "");
      tags.put("<highlight type=header>", "");
      tags.put("<highlight type=hit>", "");
      tags.put("<highlight type=name>", "");
      tags.put("<highlight type=magic>", "");
      tags.put("<highlight type=miss>", "");
      tags.put("<highlight type=narrate>", "");
      tags.put("<highlight type=weather>", "");
      tags.put("<highlight type=pray>", "");
      tags.put("<highlight type=prompt>", "");
      tags.put("<highlight type=description>", "");
      tags.put("<highlight type=say>", "");
      tags.put("<highlight type=shout>", "");
      tags.put("<highlight type=social>", "");
      tags.put("<highlight type=song>", "");
      tags.put("<highlight type=status>", "");
      tags.put("<highlight type=tell>", "");
      tags.put("<highlight type=yell>", "");
      
      tags.put("<movement/>","");
      
      tags.put("</prompt>","");
      tags.put("</room>","");
      tags.put("</name>","");
      tags.put("</description>","");
      tags.put("</status>","");
      tags.put("</terrain>","");
      tags.put("</magic>","");
      tags.put("</exits>","");
      tags.put("</tell>","");
      tags.put("</say>","");
      tags.put("</narrate>","");
      tags.put("</song>","");
      tags.put("</pray>","");
      tags.put("</shout>","");
      tags.put("</yell>","");
      tags.put("</emote>","");
      tags.put("</hit>","");
      tags.put("</damage>","");
      tags.put("</weather>","");
      tags.put("</highlight>", "");
      
      escapes = new HashMap<>();
      escapes.put("&lt;", "<");
      escapes.put("&gt;",">");
      escapes.put("&amp;","&");
      escapes.put("&nbsp;"," ");
      escapes.put("&quot;","\"");
   }
   
   public void startConnected()
   {
      if (friendList.size() > 0)
      {
         friendTimer = new Timer();
         friendTimer.scheduleAtFixedRate(new FriendTimer(), 0, 60*1000);
      }
   }
   
   @Override
   public void start()
   {
      for (ServiceInterface service : services)
      {
         service.start();
      }
   }
   
   @Override
   public void stop()
   {
      if (friendTimer != null)
         friendTimer.cancel();
      for (ServiceInterface service : services)
      {
         service.stop();
      }
   }

   @Override
   public String format(String line, List<StyleRange> ranges)
   {
      if (line == null || line.isEmpty())
         return line;
      
      String out = line;
      
      if (silent_who_countdown == -1 && out.trim().endsWith("<header>Players</header>"))
      {
         silent_who_countdown = 1;
         return null;
      }
      
      if (silent_who_countdown == 1)
      {
         if (out.startsWith("<prompt>"))
         {
            silent_who_countdown = 0;
            return null;
         }
         
         if (out.length() < 7)
            return null;
         
         List<String> currentlyOnline = new ArrayList<>();
         for (String friend : friendList)
         {
            if (out.length() < 6+friend.length())
               continue;
            String comp = out.substring(6, 6+friend.length());
            if (comp.toLowerCase().equals(friend))
            {
               currentlyOnline.add(friend);
               if (onlineFriends.contains(friend))
               {
                  continue;
               }
               String friendOnline = "\n" + comp + " has appeard online!\n";
               StyleRange range = new StyleRange();
               range.background = MudFrame.colors[2];
               range.foreground = MudFrame.colors[0];
               range.fontStyle = SWT.ITALIC;
               range.start = 0;
               range.length = friendOnline.length();
               List<StyleRange> rangeList = new ArrayList<>();
               rangeList.add(range);
               MudFrame.writeToTextBox(friendOnline, rangeList);
            }
         }
         onlineFriends = currentlyOnline;
         return null;
      }

      if (out.indexOf('<') > -1)
      {
         out = removeTags(out,ranges);
      }
      
      if (out.indexOf('&') > -1)
      {
         out = removeEscapes(out,ranges);
      }
      
      for (AIInterface service : formatters)
      {
         out = service.format(out, ranges);
      }
      
      return out;
   }

   @Override
   public void trigger(String line)
   {
      if (line == null || line.isEmpty())
         return;
      
      for (AIInterface service : triggerers)
      {
         service.trigger(line);
      }
   }

   @Override
   public boolean command(String command)
   {
      boolean retval = false;
      
      if (command.startsWith("friend"))
      {
         if (command.length() > 7)
         {
            String friend = command.substring(7).toLowerCase();
            friendList.add(friend);
            Configger.setProperty("MUMEFRIENDS", String.join(";", friendList));
            log.add(Level.INFO,"Adding {" + friend + "} to friends list");
            MudFrame.writeToTextBox("\nAdding {" + friend + "} to friends list\n", null);
            if (friendList.size() == 1)
            {
               friendTimer = new Timer();
               friendTimer.scheduleAtFixedRate(new FriendTimer(), 0, 60*1000);
            }
         }
         else
         {
            StringBuilder sb = new StringBuilder();
            sb.append("\nFriends:\n");
            for (String friend : friendList)
            {
               sb.append("   " + friend + "\n");
            }
            sb.append("\n");
            sb.append("  Add a friend with \"friend <name>\" and remove them with \"defriend <name>\"\n");
            MudFrame.writeToTextBox(sb.toString(), null);
         }
         return true;
      }
      else if (command.startsWith("defriend ") && command.length() > 9)
      {
         String friend = command.substring(9).toLowerCase();
         friendList.remove(friend);
         Configger.setProperty("MUMEFRIENDS", String.join(";", friendList));
         log.add(Level.INFO,"Removing {" + friend + "} from friends list");
         MudFrame.writeToTextBox("\nRemoving {" + friend + "} from friends list\n", null);
         if (friendList.size() == 0)
         {
            friendTimer.cancel();
         }
         return true;
      }
      
      
      for (AIInterface service : commanders)
      {
         
         retval |= service.command(command);
      }
      return retval;
   }

   @Override
   public boolean isFormatter()
   {
      return true; 
   }

   @Override
   public boolean isTriggerer()
   {
      return triggerers.size() > 0;
   }

   @Override
   public boolean isCommander()
   {
      return true;
   }
   
   private class FriendTimer extends TimerTask
   {
      public FriendTimer() {}
      @Override
      public void run()
      {
         silent_who_countdown = -1;
         MudFrame.writeCommand("SIL_WHO;who");
      }  
   }
   
   private String removeTags(String line, List<StyleRange> ranges)
   {
      String out = line;
      
      int startIdx = out.indexOf('<');
      while (startIdx >= 0 && startIdx < out.length())
      {
         int endIdx = out.indexOf('>', startIdx);
         if (endIdx == -1)
            break;
         
         String substr = out.substring(startIdx,endIdx+1);
         // special case for "room"
         if (substr.equals("<room>") && out.length() > endIdx+6 && out.substring(endIdx+1,endIdx+7).equals("<name>"))
         {
            substr = "<room><name>";
            endIdx += 6;
         }
         String replacement = tags.get(substr);
         if (replacement != null)
         {
            out = out.substring(0,startIdx) + out.substring(endIdx+1);
            for (StyleRange range : ranges)
            {
               if (range.start > startIdx)
               {
                  range.start -= (endIdx - startIdx) + 1;
               }
               else if (range.start + range.length > startIdx)
               {
                  range.length -= (endIdx - startIdx) + 1;
               }
            }
         }
         if (substr.equals("<room><name>"))
         {
            currentRoom = new MumeRoom();
            int roomEnd = out.indexOf('<',startIdx);
            if (roomEnd == -1)
            {
               currentRoom.setName(out.substring(startIdx).trim());
            }
            else
            {
               currentRoom.setName(out.substring(startIdx,roomEnd).trim());
            }
         }
         else if (substr.equals("<exits>"))
         {
            int exitEnd = out.indexOf('<',startIdx);
            if (exitEnd == -1)
            {
               currentRoom.addExits(out.substring(startIdx).trim());
            }
            else
            {
               currentRoom.addExits(out.substring(startIdx,exitEnd).trim());
            }
         }
         
         if (replacement == null)
         {
            startIdx = out.indexOf('<',startIdx+1);
         }
         else
         {
            startIdx = out.indexOf('<',startIdx);
         }
      }
      
      // movement direction
      startIdx = out.indexOf("<movement dir=");
      if (startIdx >= 0)
      {
         int endIdx = out.indexOf('/',startIdx+14);
         if (endIdx > 0 && endIdx+2 < out.length())
         {
            currentRoom.setLastDirection(out.charAt(startIdx+14));
            out = out.substring(0,startIdx) + out.substring(endIdx+2);
            for (StyleRange range : ranges)
            {
               if (range.start > startIdx)
               {
                  range.start -= (endIdx - startIdx) + 2;
               }
               else if (range.start + range.length > startIdx)
               {
                  range.length -= (endIdx - startIdx) + 2;
               }
            }
         }
      }
      return out;
   }
   
   private String removeEscapes(String line, List<StyleRange> ranges)
   {
      String out = line;
      
      int startIdx = out.indexOf('&');
      while (startIdx >= 0 && startIdx < out.length())
      {
         int endIdx = out.indexOf(';', startIdx);
         if (endIdx == -1)
            break;
         
         String substr = out.substring(startIdx,endIdx+1);
         String replacement = escapes.get(substr);
         if (replacement != null)
         {
            out = out.substring(0,startIdx) + out.substring(endIdx+1);
            for (StyleRange range : ranges)
            {
               if (range.start > startIdx)
               {
                  range.start -= (endIdx - startIdx) + 1;
               }
            }
         }
         startIdx = out.indexOf('&',startIdx+1);
      }
      
      return out;
   }

}
