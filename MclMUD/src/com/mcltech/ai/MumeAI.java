package com.mcltech.ai;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.swt.custom.StyleRange;

import com.mcltech.connection.Configger;
import com.mcltech.connection.MudFrame;

public class MumeAI implements AIInterface
{
   MudFrame frame;
   Timer clockTimer;

   public MumeAI(MudFrame frame)
   {
      System.out.println("Starting MumeAI");
      this.frame = frame;
   }
   
   @Override
   public void start()
   {
      setClock();
   }
   
   @Override
   public void stop()
   {
      clockTimer.cancel();
   }

   @Override
   public String format(String line, List<StyleRange> ranges)
   {
      return line;
   }

   @Override
   public void trigger(String line)
   {
      if (line == null || line.isEmpty())
         return;
      
      if (line.startsWith("The current time is "))
      {
         String time = line.substring(20,line.length()-1);
         System.out.println("Current Time: " + time);
         // t is the number of seconds (real time) from midnight
         int t = 0;
         int h = 0;
         int m = 0;
         if (time.endsWith("pm"))
         {
            t += 12*60;
         }
         time = time.substring(0,time.length()-3);
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
            System.out.println("Nope. Couldn't convert {" + time + "} to to integers.");
            return;
         }
         long midnight = System.currentTimeMillis() / 1000 - t;
         System.out.println("Got midnight as " + midnight + " and current time as " + h + ":" + m + " ... " + t);
         Configger.setProperty("MUMEMIDNIGHT", midnight+"");
         setClock(midnight);
      }
   }

   @Override
   public String[] command(String line)
   {
      String [] data = {line};
      return data;
   }
   
   private void setClock()
   {
      String m = Configger.getProperty("MUMEMIDNIGHT", "");
      if (m.equals(""))
         return;
      
      try {
         setClock(Long.valueOf(m).longValue());
      }
      catch (@SuppressWarnings("unused") NumberFormatException e)
      {
         System.out.println("Couldn't turn {" + m + "} into a long for setClock.");
      }
   }
   
   private void setClock(long midnight)
   {
      long now = System.currentTimeMillis();
      int currentHour = (int) (((now/1000 - midnight) % (24*60))/60);
      int timeTillNextHour = 60000 - (int) ((now - midnight*1000) % (24*60*1000) - currentHour*60*1000);
      System.out.println("Current Hour: " + currentHour + ", TTNH: " + timeTillNextHour);
      if (clockTimer != null)
      {
         clockTimer.cancel();
      }
      clockTimer = new Timer();
      clockTimer.schedule(new TimerTask()
      {
         int hour = currentHour;
         @Override
         public void run()
         {
            hour++;
            frame.writeToTextBox("Current Hour: " + ((hour)%24), null);
         }
      }, timeTillNextHour, 1000*60);
   }

}
