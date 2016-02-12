package com.mcltech.ai.mume;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;

import com.mcltech.ai.AIInterface;
import com.mcltech.connection.MudFrame;

public class MumeFormatter implements AIInterface
{

   private static final Pattern pathPattern = Pattern.compile("-\\w+-");
   private static final Pattern roadPattern = Pattern.compile("=\\w+=");
   private static final Pattern waterPattern = Pattern.compile("~\\w+~");
   private static final Pattern cliffPattern = Pattern.compile("~\\\\\\w+\\/~");
   
   private boolean running = false;
   
   @Override
   public String format(String line, List<StyleRange> ranges)
   {
      // color the exits
      if (line.startsWith("Exits: "))
      {
         MumeFormatter.formatExits(line, ranges);
      }
      else if (line.contains("seems to have recovered"))
      {
         StyleRange r = new StyleRange();
         r.start = line.indexOf("seems to have recovered");
         r.length = line.indexOf("senses.") + 7 - r.start;
         r.underline = true;
         r.fontStyle = SWT.ITALIC;
         ranges.add(r);
      }
      else if (line.equals("You gain a level!"))
      {
         StyleRange r = new StyleRange();
         r.start = 0;
         r.length = line.length();
         r.background = MudFrame.getInstance().getColor(8);
         ranges.add(r);
      }
      return line;
   }
   
   public static void formatExits(String line, List<StyleRange> ranges)
   {
      if (line == null)
         return;
      Matcher m = null;
      int start = -1;
      if (MumeAI.currentRoom != null && MumeAI.currentRoom.getReturnDirPattern() != null)
      {
         m = MumeAI.currentRoom.getReturnDirPattern().matcher(line);
         
         if (m.find())
         {
            StyleRange r = new StyleRange();
            r.start = m.start();
            start = m.start();
            r.length = m.end() - m.start();
            r.foreground = MudFrame.getInstance().getColor(3);
            ranges.add(r);
         }
      }
      m = pathPattern.matcher(line);
      while (m.find())
      {
         if (m.start() != start && m.start() != start+1)
         {
            StyleRange r = new StyleRange();
            r.start = m.start();
            r.length = m.end() - m.start();
            r.foreground = MudFrame.getInstance().getColor(8);
            ranges.add(r);
         }
      }
      m = waterPattern.matcher(line);
      while (m.find())
      {
         if (m.start() != start && m.start() != start+1)
         {
            StyleRange r = new StyleRange();
            r.start = m.start();
            r.length = m.end() - m.start();
            r.foreground = MudFrame.getInstance().getColor(6);
            ranges.add(r);
         }
      }
      m = roadPattern.matcher(line);
      while (m.find())
      {
         if (m.start() != start && m.start() != start+1)
         {
            StyleRange r = new StyleRange();
            r.start = m.start();
            r.length = m.end() - m.start();
            r.foreground = MudFrame.getInstance().getColor(2);
            ranges.add(r);
         }
      }
   }

   @Override
   public void start()
   {
      running = true;
   }

   @Override
   public void stop()
   {
      running = false;
   }

   @Override
   public boolean isRunning()
   {
      return running;
   }
   
   @Override
   public boolean isFormatter()
   {
      return true;
   }

   @Override
   public boolean isTriggerer()
   {
      return false;
   }

   @Override
   public boolean isCommander()
   {
      return false;
   }

   @Override
   public void trigger(String line)
   {
      
   }

   @Override
   public boolean command(String line)
   {
      return false;
   }

}
