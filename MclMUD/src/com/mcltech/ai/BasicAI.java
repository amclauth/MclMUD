package com.mcltech.ai;

import java.util.List;

import org.eclipse.swt.custom.StyleRange;

public class BasicAI implements AIInterface
{
   public BasicAI() {}
   
   @Override
   public void stop()
   {
      
   }
   
   @Override
   public void start()
   {
      
   }
   
   @Override
   public String format(String line, List<StyleRange> ranges)
   {
      return line;
   }

   @Override
   public void trigger(String line)
   {
      
   }

   @Override
   public boolean command(String command)
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
      return false;
   }

   @Override
   public boolean isCommander()
   {
      return false;
   }

}
