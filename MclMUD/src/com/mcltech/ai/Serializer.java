package com.mcltech.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.swt.custom.StyleRange;

public class Serializer implements AIInterface
{
   private boolean running = false;
   protected Map<String, String> actionMap;
   protected Map<String, String> serialMap;
   private int min99Delay = 200;
   private int max99Delay = 2850;
   private int normalDelay = 750;
   private double leftScale = 1;
   private double rightScale = 1;
   private Random random;
   private String name;
   
   public Serializer(String name)
   {
      this.name = name;
      actionMap = new HashMap<>();
      serialMap = new HashMap<>();
   }

   private void writeCommand(String command)
   {
      // gaussian around minDelay with 95% within the range
      double r = random.nextGaussian()
            + normalDelay * (Math.min(max99Delay - normalDelay, normalDelay - min99Delay) / 2.0);

      // ensure safe values
      if (r < 0)
         r = 0;

      // scale the ranges
      if (r < normalDelay)
      {
         r = (r - normalDelay) * leftScale + normalDelay;
      }
      else
      {
         r = (r - normalDelay) * rightScale + normalDelay;
      }
      System.out.println("Delay: " + (int)r + ", Command: " + command);
      
      // split on delay and schedule multiple timers if there are delays built in

      Timer t = new Timer();
      t.schedule(new TimerTask()
      {
         @Override
         public void run()
         {
            System.out.println("  Command: " + command);
//            MudFrame.getInstance().writeCommand(command);
         }
      }, (int) r);

   }

   @Override
   public void start()
   {
      // TODO Add write to screen of name.on
      running = true;
      random = new Random(System.currentTimeMillis());
      if (max99Delay - normalDelay > normalDelay - min99Delay)
      {
         leftScale = 1;
         rightScale = (max99Delay - normalDelay) * 1.0 / (normalDelay - min99Delay);
      }
      else
      {
         rightScale = 1;
         leftScale = (normalDelay - min99Delay) * 1.0 / (max99Delay - normalDelay);
      }
      String startCommand = serialMap.get("***STARTSERIALACTIONS***");
      if (startCommand != null)
      {
         writeCommand("SIL_SERIAL;" + startCommand);
      }
   }

   @Override
   public void stop()
   {
   // TODO Add write to screen of name.off
      running = false;
   }
   
   @Override
   public boolean isRunning()
   {
      return running;
   }

   @Override
   public void trigger(String line)
   {
      if (!running)
         return;

      for (String action : actionMap.keySet())
      {
         if (line.contains(action))
         {
            writeCommand(actionMap.get(action));
         }
      }
      for (String action : serialMap.keySet())
      {
         if (line.contains(action))
         {
            writeCommand(actionMap.get(action));
         }
      }
   }

   @Override
   public String format(String line, List<StyleRange> ranges)
   {
      throw new UnsupportedOperationException("format Not Implemented");
   }

   @Override
   public boolean command(String command)
   {
      throw new UnsupportedOperationException("command Not Implemented");
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
