package com.mcltech.ai.mume;

import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.swt.custom.StyleRange;

import com.mcltech.ai.AIInterface;

public class MumeFormatter implements AIInterface
{

   private static final Pattern pathPattern = Pattern.compile("\\A\\w+, the (\\d+)\\w+ of (\\w+), Year");
   private static final Pattern waterPattern = Pattern.compile("\\A\\w+, the (\\d+)\\w+ of (\\w+), Year");
   
   @Override
   public String format(String line, List<StyleRange> ranges)
   {
      if (line.startsWith("Exits: "))
      {
         
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
      // TODO Implement trigger
      throw new UnsupportedOperationException("trigger Not Implemented");
   }

   @Override
   public boolean command(String line)
   {
      // TODO Implement command
      throw new UnsupportedOperationException("command Not Implemented");
   }

}
