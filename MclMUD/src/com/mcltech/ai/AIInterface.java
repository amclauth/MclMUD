package com.mcltech.ai;

import java.util.List;

import org.eclipse.swt.custom.StyleRange;

public interface AIInterface
{

   
   /**
    * Format the output line. Return null if it shouldn't be printed.
    * @param line
    * @param ranges
    * @return
    */
   public String format(String line, List<StyleRange> ranges);
   
   /**
    * Process any triggers or scripts based on an output line
    * @param line
    */
   public void trigger(String line);
   
   /**
    * Process an input line for commands / aliases / etc
    * @param line
    * @return true if no other action should be taken, false if the command should
    *         be sent on to the MUD
    */
   public boolean command(String line);
}
