package com.mcltech.ai.mume;

import java.util.List;

import org.eclipse.swt.custom.StyleRange;

import com.mcltech.ai.AIInterface;

public class MumeTriggers implements AIInterface
{
   private MumeAI mumeAI;
   private boolean running = false;
   
   public MumeTriggers(MumeAI mumeAI)
   {
      this.mumeAI = mumeAI;
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

   @Override
   public String format(String line, List<StyleRange> ranges)
   {
      throw new UnsupportedOperationException("format Not Implemented");
   }

   @Override
   public void trigger(String line)
   {
      if (line.equals("<xml>XML mode is now on."))
      {
         mumeAI.startConnected();
      }
   }

   @Override
   public boolean command(String command)
   {
      throw new UnsupportedOperationException("command Not Implemented");
   }

}
