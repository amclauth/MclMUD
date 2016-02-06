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
   private static final Pattern waterPattern = Pattern.compile("~\\w+~");
   private Pattern returnDirPattern;
   
   @Override
   public String format(String line, List<StyleRange> ranges)
   {
      if (line.startsWith("Exits: "))
      {

         Matcher m = null;
         int start = -1;
         if (returnDirPattern != null)
         {
            m = returnDirPattern.matcher(line);
            
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
            if (m.start() != start)
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
            if (m.start() != start)
            {
               StyleRange r = new StyleRange();
               r.start = m.start();
               r.length = m.end() - m.start();
               r.foreground = MudFrame.colors[6];
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
      return true;
   }

   @Override
   public boolean isCommander()
   {
      return true;
   }

   @Override
   public void trigger(String line)
   {
      if (line.startsWith("You flee "))
      {
         setLastDirection(line.substring(9,10));
      }
   }

   @Override
   public boolean command(String line)
   {

      // just process single character directions for simplicity
      if (line.length() == 1)
      {
         setLastDirection(line);
      }
      return false;
   }
   
   private void setLastDirection(String c)
   {
      switch (c)
      {
         case "e":
            returnDirPattern = Pattern.compile(" .?west.?[,.]");
            break;
         case "w":
            returnDirPattern = Pattern.compile(" .?east.?[,.]");
            break;
         case "n":
            returnDirPattern = Pattern.compile(" .?south.?[,.]");
            break;
         case "s":
            returnDirPattern = Pattern.compile(" .?north.?[,.]");
            break;
         case "u":
            returnDirPattern = Pattern.compile(" .?down.?[,.]");
            break;
         case "d":
            returnDirPattern = Pattern.compile(" .?up.?[,.]");
            break;
         default:
            returnDirPattern = null;
            break;
      }
   }

}
