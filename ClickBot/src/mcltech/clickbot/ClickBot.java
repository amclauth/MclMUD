package mcltech.clickbot;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;

import mcltech.clickbot.gui.ActivePanel;

public class ClickBot
{
   private Robot robot;
   private boolean stopRobot = true;
   private int msDelay = 15;
   private double cps = 1000.0/(msDelay*2.2);
   private ActivePanel activePanel; // should change to an interface
   private double distance = 0;
   private final double maxDistance = 50.0;

   public ClickBot()
   {
      robot = null;
      try {
         robot = new Robot();
      } catch (AWTException e) {
         // TODO Auto-generated catch block
         System.out.println("ERROR: Could not initialize Robot");
         e.printStackTrace();
      }
   }
   
   public void startRobot()
   {
      stopRobot = false;
      Point original = MouseInfo.getPointerInfo().getLocation();

      long t1 = System.currentTimeMillis();
      int counter = 0;
      while (!stopRobot) {
         robot.delay(msDelay);
         if (moved(original)) {
            break;
         }
         robot.mousePress(InputEvent.BUTTON1_MASK);
         robot.delay(msDelay);
         robot.mouseRelease(InputEvent.BUTTON1_MASK);
         counter++;
         if (counter == 100)
         {
            long t2 = System.currentTimeMillis();
            cps = counter * 1000.0 / (t2-t1);
            t1 = t2;
            counter = 0;
         }
      }
      if (activePanel != null)
      {
         activePanel.reset();
      }
   }
   
   public void stopRobot()
   {
      stopRobot = true;
   }
   
   public void increaseDelay()
   {
      msDelay++;
      cps = 1000.0/(msDelay*2.2);
   }
   
   public void decreaseDelay()
   {
      if (msDelay > 0)
      {
         msDelay--;
         cps = 1000.0/(msDelay*2.2);
      }
   }
   
   public double getCPS()
   {
      return cps;
   }
   
   public double getDistanceRatio()
   {
      return distance / maxDistance;
   }

   private boolean moved(Point original) {
      Point p = MouseInfo.getPointerInfo().getLocation();
      double d = p.distance(original);
      if (d != distance && activePanel != null)
      {
         activePanel.repaint();
      }
      distance = d;
      return distance > maxDistance;
   }

   public void setActivePanel(ActivePanel activePanel)
   {
      this.activePanel = activePanel;
   }
}
