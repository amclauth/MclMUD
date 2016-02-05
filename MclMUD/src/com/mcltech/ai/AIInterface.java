package com.mcltech.ai;

import java.util.List;

import org.eclipse.swt.custom.StyleRange;

import com.mcltech.base.ServiceInterface;

public interface AIInterface extends ServiceInterface
{
   /**
    * Return these based on what the interface does, so it's easier
    * (more efficient) to call on only the interfaces needed.
    * @return
    */
   public boolean isFormatter();
   public boolean isTriggerer();
   public boolean isCommander();
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
    * This is basically to act as a listener on commands. An example of use would be 
    * to pass direction commands on to a mapper
    * or to trigger a timer when a command is given
    * @param line
    * @return true if no other action should be taken, false if the command should
    *         be sent on to the MUD
    */
   public boolean command(String command);
}
