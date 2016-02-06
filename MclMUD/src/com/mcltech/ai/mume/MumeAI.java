package com.mcltech.ai.mume;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.custom.StyleRange;

import com.mcltech.ai.AIInterface;
import com.mcltech.base.ServiceInterface;
import com.mcltech.connection.MudFrame;

public class MumeAI implements AIInterface
{
   MudFrame frame;
   MumeTime mumeTime;
   
   List<AIInterface> services;

   public MumeAI(MudFrame frame)
   {
      this.frame = frame;
      services = new ArrayList<>();
      services.add(new MumeTime(frame));
      services.add(new MumeFormatter());
      services.add(new MumeTriggers());
   }
   
   @Override
   public void start()
   {
      for (ServiceInterface service : services)
      {
         service.start();
      }
   }
   
   @Override
   public void stop()
   {
      for (ServiceInterface service : services)
      {
         service.stop();
      }
   }

   @Override
   public String format(String line, List<StyleRange> ranges)
   {
      if (line == null || line.isEmpty())
         return line;
      // color -*- as a road and ~*~ as water
      
      String out = line;
      for (AIInterface service : services)
      {
         if (service.isFormatter())
            out = service.format(line, ranges);
      }
      return out;
   }

   @Override
   public void trigger(String line)
   {
      if (line == null || line.isEmpty())
         return;
      
      for (AIInterface service : services)
      {
         if (service.isTriggerer())
            service.trigger(line);
      }
   }

   @Override
   public boolean command(String command)
   {
      boolean retval = false;
      for (AIInterface service : services)
      {
         if (service.isFormatter())
            retval |= service.command(command);
      }
      return retval;
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
   
   

}
