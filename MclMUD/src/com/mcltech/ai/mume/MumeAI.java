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
   List<AIInterface> triggerers;
   List<AIInterface> commanders;
   List<AIInterface> formatters;

   public MumeAI(MudFrame frame)
   {
      this.frame = frame;
      services = new ArrayList<>();
      services.add(new MumeTime(frame));
      services.add(new MumeFormatter());
      services.add(new MumeTriggers());
      
      triggerers = new ArrayList<>();
      commanders = new ArrayList<>();
      formatters = new ArrayList<>();
      for (AIInterface i : services)
      {
         if (i.isTriggerer())
            triggerers.add(i);
         if (i.isCommander())
            commanders.add(i);
         if (i.isFormatter())
            formatters.add(i);
      }
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
      for (AIInterface service : formatters)
      {
         out = service.format(line, ranges);
      }
      return out;
   }

   @Override
   public void trigger(String line)
   {
      if (line == null || line.isEmpty())
         return;
      
      for (AIInterface service : triggerers)
      {
         service.trigger(line);
      }
   }

   @Override
   public boolean command(String command)
   {
      boolean retval = false;
      for (AIInterface service : commanders)
      {
         retval |= service.command(command);
      }
      return retval;
   }

   @Override
   public boolean isFormatter()
   {
      return formatters.size() > 0;
   }

   @Override
   public boolean isTriggerer()
   {
      return triggerers.size() > 0;
   }

   @Override
   public boolean isCommander()
   {
      return commanders.size() > 0;
   }
   
   

}
