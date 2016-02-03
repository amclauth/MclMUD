package com.mcltech.ai.mume;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.custom.StyleRange;

import com.mcltech.ai.AIInterface;
import com.mcltech.ai.ServiceInterface;
import com.mcltech.connection.MudFrame;

public class MumeAI implements AIInterface
{
   MudFrame frame;
   MumeTime mumeTime;
   
   List<ServiceInterface> services;

   public MumeAI(MudFrame frame)
   {
      this.frame = frame;
      services = new ArrayList<>();
      services.add(new MumeTime(frame));
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
      return line;
   }

   @Override
   public void trigger(String line)
   {
      if (line == null || line.isEmpty())
         return;
      
      // clock parsing
      if (line.startsWith("The current time is "))
      {
         mumeTime.setClock(line);
      }
      
      // day parsing
      if (mumeTime.monthPattern.matcher(line).find(0))
      {
         mumeTime.setCalendar(line);
      }
   }

   @Override
   public String[] command(String line)
   {
      String [] data = {line};
      return data;
   }
   
   

}
