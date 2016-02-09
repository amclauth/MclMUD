package com.mcltech.ai.mume;

import java.util.List;

import org.eclipse.swt.custom.StyleRange;

import com.mcltech.ai.AIInterface;
import com.mcltech.connection.MudFrame;

public class MumeTriggers implements AIInterface
{
   private MumeAI mumeAI;
   
   public MumeTriggers(MumeAI mumeAI)
   {
      this.mumeAI = mumeAI;
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
      if (line.equals("MUME: Multi Users in Middle-earth, version VIII."))
      {
         MudFrame.getInstance().writeCommand(";change xml on;change height 60;sl");
         mumeAI.startConnected();
      }
      else if (line.equals("ZBLAM"))
      {
         // stand;ride
      }
   }

   @Override
   public boolean command(String command)
   {
      throw new UnsupportedOperationException("command Not Implemented");
   }

}
