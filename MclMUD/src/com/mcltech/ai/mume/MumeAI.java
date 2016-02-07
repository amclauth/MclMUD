package com.mcltech.ai.mume;

import java.util.ArrayList;
import java.util.List;
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
   
   List<AIInterface> services;
   List<AIInterface> triggerers;
   List<AIInterface> commanders;
   List<AIInterface> formatters;
   List<String> friendList;
   List<String> onlineFriends;
   
   Timer friendTimer;
   
   int silent_who_countdown = 0;

   public MumeAI(MudFrame frame)
   {
      services = new ArrayList<>();
      services.add(new MumeTime(frame));
      services.add(new MumeFormatter());
      services.add(new MumeTriggers(this));
      
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
      
      if (silent_who_countdown == -1 && line.trim().startsWith("Players"))
      {
         silent_who_countdown = 3;
         return null;
      }
      
      if (silent_who_countdown > 0)
      {
         if (silent_who_countdown < 3)
         {
            silent_who_countdown--;
            return null;
         }
         if (line.contains(" allies ") && line.trim().endsWith("on."))
         {
            silent_who_countdown--;
            return null;
         }
         

         if (line.length() < 7)
            return null;
         
         List<String> currentlyOnline = new ArrayList<>();
         for (String friend : friendList)
         {
            if (line.length() < 6+friend.length())
               continue;
            String comp = line.substring(6, 6+friend.length());
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
      
      String out = line;
      for (AIInterface service : formatters)
      {
         out = service.format(line, ranges);
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

}
