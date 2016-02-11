package com.mcltech.ai.mume;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.custom.StyleRange;

import com.mcltech.ai.AIInterface;
import com.mcltech.base.MudLogger;
import com.mcltech.connection.Configger;
import com.mcltech.connection.MudFrame;

/**
 * A time class that will inspect the outputs of "time" and "look clock" and 
 * start timers in the title bar that tell the time of day, whether it's day
 * or night outside, and the time till the next change.
 * @author andymac
 *
 */
public class MumeTime implements AIInterface
{
   private static MudLogger log = MudLogger.getInstance();
   MudFrame frame;
   
   Timer clockTimer;
   boolean timeKnown = false;
   int hour;
   int minute;
   int month = -1;
   int day = -1;
   
   Pattern monthPattern = Pattern.compile("\\A\\w+, the (\\d+)\\w+ of (\\w+), Year");
   Map<String,calendarMonth> monthMap = new HashMap<>();
   
   public MumeTime(MudFrame frame)
   {
      this.frame = frame;
      monthMap.put("Afteryule", new calendarMonth(0,8,19));
      monthMap.put("Solmath", new calendarMonth(1,9,18));
      monthMap.put("Rethe", new calendarMonth(2,8,19));
      monthMap.put("Astron", new calendarMonth(3,7,20));
      monthMap.put("Thrimidge", new calendarMonth(4,7,21));
      monthMap.put("Forelithe", new calendarMonth(5,6,21));
      monthMap.put("Afterlithe", new calendarMonth(6,5,22));
      monthMap.put("Wedmath", new calendarMonth(7,4,23));
      monthMap.put("Halimath", new calendarMonth(8,5,22));
      monthMap.put("Winterfilth", new calendarMonth(9,6,21));
      monthMap.put("Blotmath", new calendarMonth(10,7,21));
      monthMap.put("Foreyule", new calendarMonth(11,7,20));
      
      monthMap.put("Narwain", new calendarMonth(0,8,19));
      monthMap.put("Nínui", new calendarMonth(1,9,18));
      monthMap.put("Gwaeron", new calendarMonth(2,8,19));
      monthMap.put("Gwirith", new calendarMonth(3,7,20));
      monthMap.put("Lothron", new calendarMonth(4,7,21));
      monthMap.put("Nórui", new calendarMonth(5,6,21));
      monthMap.put("Cerveth", new calendarMonth(6,5,22));
      monthMap.put("Urui", new calendarMonth(7,4,23));
      monthMap.put("Ivanneth", new calendarMonth(8,5,22));
      monthMap.put("Narbeleth", new calendarMonth(9,6,21));
      monthMap.put("Hithui", new calendarMonth(10,7,21));
      monthMap.put("Girithron", new calendarMonth(11,7,20));
      
      monthMap.put("0", new calendarMonth(0,8,19));
      monthMap.put("1", new calendarMonth(1,9,18));
      monthMap.put("2", new calendarMonth(2,8,19));
      monthMap.put("3", new calendarMonth(3,7,20));
      monthMap.put("4", new calendarMonth(4,7,21));
      monthMap.put("5", new calendarMonth(5,6,21));
      monthMap.put("6", new calendarMonth(6,5,22));
      monthMap.put("7", new calendarMonth(7,4,23));
      monthMap.put("8", new calendarMonth(8,5,22));
      monthMap.put("9", new calendarMonth(9,6,21));
      monthMap.put("10", new calendarMonth(10,7,21));
      monthMap.put("11", new calendarMonth(11,7,20));
   }
   
   @Override
   public void start()
   {
      setClock();
      setCalendar();
   }
   
   @Override
   public void stop()
   {
      if (clockTimer != null)
         clockTimer.cancel();
   }
   
   void setCalendar(String line)
   {
      if (!timeKnown)
         return;
      Matcher monthMatcher = monthPattern.matcher(line);
      
      if (monthMatcher.find(0))
      {
         try
         {
            day = Integer.valueOf(monthMatcher.group(1)).intValue();
         }
         catch (@SuppressWarnings("unused") NumberFormatException e)
         {
            log.add(Level.WARNING,"Couldn't convert {" + monthMatcher.group(1) + "} to a day");
            return;
         }
         String monthName = monthMatcher.group(2);
         if (!monthMap.containsKey(monthName))
         {
            return;
         }
         
         
         long now = System.currentTimeMillis() / 1000;
         long newyear = now - monthMap.get(monthName).num * 30 * 24 * 60 - day * 24 * 60 - hour * 60 - minute;
         
         Configger.setProperty("MUMENEWYEAR", newyear + "");
         setCalendar(newyear);
      }
   }
   
   void setCalendar()
   {
      if (!timeKnown)
         return;
      String m = Configger.getProperty("MUMENEWYEAR", "");
      if (m.equals(""))
         return;
      
      try {
         setCalendar(Long.valueOf(m).longValue());
      }
      catch (@SuppressWarnings("unused") NumberFormatException e)
      {
         log.add(Level.WARNING,"Couldn't turn {" + m + "} into a long for setCalendar.");
      }
   }
   
   private void setCalendar(long newyear)
   {
      if (!timeKnown)
         return;
      
      long now = System.currentTimeMillis() / 1000 - newyear;
      long year = now / 360 / 24 / 60;
      month = (int) ((now - year * 360 * 24 * 60) / 30 / 24 / 60);
      day = (int) ((now - year * 360 * 24 * 60 - month * 30 * 24 * 60) / 24 / 60);
   }
   
   private class calendarMonth
   {
      public int num;
      public int dawn;
      public int dusk;
      
      public calendarMonth(int num,int dawn,int dusk)
      {
         this.num = num;
         this.dawn = dawn;
         this.dusk = dusk;
      }
   }
      
   
   /**
    * Take in the line of text "The current time is h+:m+ {a|p}m." and parse it.
    * This will update the config file's midnight setting.
    * @param line
    */
   void setClock(String line)
   {
      String time = line.substring(20).trim();
      // t is the number of seconds (real time) from midnight
      int t = 0;
      int h = 0;
      int m = 0;
      if (time.endsWith("pm."))
      {
         t += 12*60;
      }
      time = time.substring(0,time.length()-4);
      String[] pair = time.split(":");
      if (pair.length != 2)
         return;
      try
      {
         h = Integer.valueOf(pair[0]).intValue();
         m = Integer.valueOf(pair[1]).intValue();
         if (h == 12)
         {
            if (t > 0)
            {
               // 12pm
               h = 0; // adding 12 later
            }
            else
            {
               h = -12; // adding 12 later
            }
         }
         t += 60*h + m;
      }
      catch (@SuppressWarnings("unused") NumberFormatException e)
      {
         log.add(Level.WARNING,"Nope. Couldn't convert {" + time + "} to to integers.");
         return;
      }
      long midnight = System.currentTimeMillis() / 1000 - t;
      Configger.setProperty("MUMEMIDNIGHT", midnight+"");
      setClock(midnight);
   }
   
   /**
    * Set the clock based on what's in the config file
    */
   void setClock()
   {
      String m = Configger.getProperty("MUMEMIDNIGHT", "");
      if (m.equals(""))
         return;
      
      try {
         setClock(Long.valueOf(m).longValue());
      }
      catch (@SuppressWarnings("unused") NumberFormatException e)
      {
         log.add(Level.WARNING,"Couldn't turn {" + m + "} into a long for setClock.");
      }
   }
   
   /**
    * Set the clock based on a midnight long (seconds since epoch when a midnight occurred).
    * @param midnight
    */
   private void setClock(long midnight)
   {
      long now = System.currentTimeMillis();
      hour = (int) (((now/1000 - midnight) % (24*60))/60);
      minute = (int) (((now/1000 - midnight) % (24*60)) - hour*60);
      timeKnown = true;
//      int timeTillNextHour = 60000 - (int) ((now - midnight*1000) % (24*60*1000) - currentHour*60*1000);
      if (clockTimer != null)
      {
         clockTimer.cancel();
      }
      clockTimer = new Timer();
      clockTimer.schedule(new TimerTask()
      {
         @Override
         public void run()
         {
            minute++;
            if (minute == 60)
            {
               minute = 0;
               hour++;
            }
            if (hour == 24)
            {
               hour = 0;
               day++;
            }
            if (day == 30)
            {
               day = 0;
               month = (month+1)%12;
            }
            
            if (month >= 0 && day >= 0 && MumeInfoPanel.getInstance().isShown())
            {
               calendarMonth cMonth = monthMap.get(month + "");
               if (hour < cMonth.dawn)
               {
                  int tillDawn = cMonth.dawn*60 - hour*60 - minute;
                  MumeInfoPanel.getInstance().updateTime(
                        String.format("%02d:%02d", Integer.valueOf(hour), Integer.valueOf(minute)),
                        "NIGHT",
                        String.format("%02d:00", Integer.valueOf(cMonth.dawn)),
                        String.format("%02d:%02d", Integer.valueOf(tillDawn / 60), Integer.valueOf(tillDawn % 60)),
                        "DAWN"
                        );
               }
               else if (hour < cMonth.dusk)
               {
                  int tillDusk = cMonth.dusk*60 - hour*60 - minute;
                  MumeInfoPanel.getInstance().updateTime(
                        String.format("%02d:%02d", Integer.valueOf(hour), Integer.valueOf(minute)),
                        "DAY",
                        String.format("%02d:00", Integer.valueOf(cMonth.dusk)),
                        String.format("%02d:%02d", Integer.valueOf(tillDusk / 60), Integer.valueOf(tillDusk % 60)),
                        "DUSK"
                        );
               }
               else
               {
                  if (day == 29)
                     cMonth = monthMap.get(((month+1)%12) + "");
                  int tillDawn = cMonth.dawn*60 + 24*60 - hour*60 - minute;
                  MumeInfoPanel.getInstance().updateTime(
                        String.format("%02d:%02d", Integer.valueOf(hour), Integer.valueOf(minute)),
                        "NIGHT",
                        String.format("%02d:00", Integer.valueOf(cMonth.dawn)),
                        String.format("%02d:%02d", Integer.valueOf(tillDawn / 60), Integer.valueOf(tillDawn % 60)),
                        "DAWN"
                        );
                  
               }
            }
            else
            {
               MumeInfoPanel.getInstance().updateTime(
                     String.format("%02d:%02d", Integer.valueOf(hour), Integer.valueOf(minute)),
                     "NIGHT",
                     "",
                     "",
                     ""
                     );
            }
         }
      }, 0, 1000);
   }

   @Override
   public String format(String line, List<StyleRange> ranges)
   {
      return line;
   }

   @Override
   public void trigger(String line)
   {
      // clock parsing
      if (line.startsWith("The current time is "))
      {
         setClock(line);
      }
      else if (line.startsWith("<weather>") && line.endsWith("</weather>"))
      {
         if (line.contains("The day has begun."))
         {
            if (month > -1)
            {
               int currentHour = monthMap.get(Integer.valueOf(month)+"").dawn+1;
               setClock(System.currentTimeMillis() / 1000 - currentHour*60);
            }
         }
         else if (line.contains("The night has begun."))
         {
            if (month > -1)
            {
               int currentHour = monthMap.get(Integer.valueOf(month)+"").dusk;
               setClock(System.currentTimeMillis() / 1000 - currentHour*60);
            }
         }
         else if (line.contains("The sun rises") || line.contains("sunrise"))
         {
            if (month > -1)
            {
               int currentHour = monthMap.get(Integer.valueOf(month)+"").dawn;
               setClock(System.currentTimeMillis() / 1000 - currentHour*60);
            }
         }
         else if (line.contains("sunset") || line.contains("the sun sets"))
         {
            if (month > -1)
            {
               int currentHour = monthMap.get(Integer.valueOf(month)+"").dusk-1;
               setClock(System.currentTimeMillis() / 1000 - currentHour*60);
            }
         }
      }
      
      // day parsing
      if (monthPattern.matcher(line).find(0))
      {
         setCalendar(line);
      }
   }

   @Override
   public boolean command(String line)
   {
       return false;
   }

   @Override
   public boolean isFormatter()
   {
      return false;
   }

   @Override
   public boolean isTriggerer()
   {
      return true;
   }

   @Override
   public boolean isCommander()
   {
      return false;
   }
}
