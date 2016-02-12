package com.mcltech.ai.mume;

import java.util.List;
import java.util.Map;

import org.eclipse.swt.custom.StyleRange;

import com.mcltech.ai.AIInterface;

public class MumeMap implements AIInterface
{
   private Map<String,MumeRoom> roomNameMap;
   private boolean running = false;
   
   public MumeMap()
   {
      
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
      // <movement dir=up/><room><name>Seagull Reception</name>
      // </room><exits>Exits: east, up\n</exits>
      // for unknown movement: <movment/>
      if (line.contains("</room>"))
      {
         MumeRoom thisRoom = MumeAI.currentRoom.copy();
      }
   }

   @Override
   public boolean command(String command)
   {
      throw new UnsupportedOperationException("format Not Implemented");
   }
}
