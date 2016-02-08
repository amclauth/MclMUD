package com.mcltech.ai.mume;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.custom.StyleRange;

import com.mcltech.ai.AIInterface;
import com.mcltech.connection.MudFrame;

public class MumeFormatter implements AIInterface
{

   private static final Pattern pathPattern = Pattern.compile("-\\w+-");
   private static final Pattern roadPattern = Pattern.compile("=\\w+=");
   private static final Pattern waterPattern = Pattern.compile("~\\w+~");
   
   @Override
   public String format(String line, List<StyleRange> ranges)
   {
      // color the exits
      if (line.startsWith("Exits: "))
      {
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
               r.foreground = MudFrame.colors[3];
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
               r.foreground = MudFrame.colors[8];
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
               r.foreground = MudFrame.colors[6];
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
               r.foreground = MudFrame.colors[2];
               ranges.add(r);
            }
         }
      }
      return line;
   }

   @Override
   public void start()
   {
      
   }

   @Override
   public void stop()
   {
      
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
