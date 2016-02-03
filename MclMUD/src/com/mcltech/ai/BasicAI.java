package com.mcltech.ai;

import java.util.List;

import org.eclipse.swt.custom.StyleRange;

public class BasicAI implements AIInterface
{
   public BasicAI() {}
   
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
   public boolean command(String line)
   {
      return false;
   }

}
