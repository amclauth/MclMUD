package com.mcltech.ai.mume;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.mcltech.ai.AIInterface;
import com.mcltech.base.MudLogger;
import com.mcltech.connection.Configger;
import com.mcltech.connection.MudFrame;

public class MumeInfoPanel implements AIInterface
{
   static final MudLogger log = MudLogger.getInstance();
   private int fontSize;
   Shell shell;
   Display display;
   StyledText roomInfoText;
   StyledText timeText;
   
   Color RED;
   Color DARK_RED;
   Color BLUE;
   Color DARK_BLUE;
   Color GREEN;
   Color DARK_GREEN;
   Color GRAY;
   Color BLACK;
   
   boolean isShown = false;
   
   int hp_low, hp_high, hp_max, hp_wimpy;
   int mv_low, mv_high, mv_max;
   int mana_low, mana_high, mana_max;
   
   Canvas hpMpManaCanvas;
   
   // singleton class holder pattern
   private static final class holder
   {
      static final MumeInfoPanel INSTANCE = new MumeInfoPanel();
   }

   public static MumeInfoPanel getInstance()
   {
      return holder.INSTANCE;
   }
   
   //TODO
   // day / night / time / time till next
   // xp / tp
   // text above bars with numbers for hp / tp / mv / mana / xp / etc
   // room name
   // room exits
   // room enemies
   // turn all of these things into singletons like MudLogger
   public MumeInfoPanel()
   {
      hp_max = 100;
      mv_max = 100;
      mana_max = 100;
      hp_low = 50;
      hp_high = 70;
      hp_wimpy=30;
      mv_low = 40;
      mv_high = 60;
      mana_low = 30;
      mana_high = 80;
   }
   
   public boolean isShown()
   {
      return isShown;
   }

   @Override
   public void trigger(String line)
   {
      if (line.contains("</room>"))
      {
         updateRoomText();
      }
   }
   
   public void updateRoomText()
   {
      if (roomInfoText != null)
      {
         display.asyncExec(new Runnable()
         {
            List<StyleRange> ranges = new ArrayList<>();
            
            @Override
            public void run()
            {
               ranges.clear();
               roomInfoText.setText(MumeAI.currentRoom.getName() + "\n" + MumeAI.currentRoom.getExitsString());
               MumeFormatter.formatExits(MumeAI.currentRoom.getExitsString(), ranges);
               int len = MumeAI.currentRoom.getName().length() + 1;
               for (StyleRange r : ranges)
               {
                  r.start += len;
               }
               roomInfoText.setStyleRanges(ranges.toArray(new StyleRange[0]));
            }
         });
      }
   }
   
   public void updateTime(String time, String dayNight, String changeTime, String till, String dawnDusk)
   {
      if (timeText != null)
      {
         display.asyncExec(new Runnable()
         {
            StyleRange[] ranges = new StyleRange[2];
            
            @Override
            public void run()
            {
               ranges[0] = new StyleRange();
               ranges[1] = new StyleRange();
               ranges[0].underline = true;
               ranges[1].underline = true;
               
               //  DAY          DUSK
               //  10:54    19:00 (10:06)    
               StringBuilder text = new StringBuilder();
               text.append("  ");
               ranges[0].start = 2;
               if (dayNight.length() == 3)
               {
                  ranges[0].start = 3;
                  text.append(" ");
               }
               ranges[0].length = dayNight.length();
               text.append(dayNight);
               while (text.length() < 15)
                  text.append(" ");
               text.append(dawnDusk);
               text.append("\n  ");
               text.append(time);
               text.append("    ");
               text.append(changeTime + " (" + till + ")");
               timeText.setText(text.toString());
               ranges[1].start = 15;
               ranges[1].length = dawnDusk.length();
               ranges[1].underline = true;
               timeText.setStyleRanges(ranges);
            }
         });
      }
   }
   
   private void createHpMpManaBars()
   {
      hpMpManaCanvas = new Canvas(shell, SWT.NO_REDRAW_RESIZE);
      GridData gridData = new GridData();
      gridData.horizontalAlignment = GridData.FILL;
      gridData.grabExcessHorizontalSpace = true;
      gridData.grabExcessVerticalSpace = false;
      gridData.heightHint = 30;

      hpMpManaCanvas.setLayoutData(gridData);
      
      hpMpManaCanvas.addPaintListener(new PaintListener()
      {
         @Override
         public void paintControl(PaintEvent e)
         {
            int cHeight = hpMpManaCanvas.getClientArea().height;
            int cWidth = hpMpManaCanvas.getClientArea().width;
            e.gc.setBackground(BLACK);
            e.gc.fillRectangle(0,0,cWidth,cHeight);
            e.gc.setForeground(GRAY);
            e.gc.setBackground(DARK_RED);
            e.gc.fillRectangle(0, 0, (int)(1.0*hp_high/hp_max * cWidth), cHeight/3-1);
            e.gc.setBackground(RED);
            e.gc.fillRectangle(0, 0, (int)(1.0*hp_low/hp_max * cWidth), cHeight/3-1);
            e.gc.drawRectangle(0, 0, (int)(1.0*hp_low/hp_max * cWidth), cHeight/3-1);
            e.gc.drawLine((int)(1.0*hp_wimpy/hp_max * cWidth), 0, (int)(1.0*hp_wimpy/hp_max * cWidth), cHeight/3-1);
            e.gc.drawRectangle(0, 0, (int)(1.0*hp_high/hp_max * cWidth), cHeight/3-1);
            
            e.gc.setBackground(DARK_GREEN);
            e.gc.fillRectangle(0, cHeight/3, (int)(1.0*mv_high/mv_max * cWidth), cHeight/3-1);
            e.gc.setBackground(GREEN);
            e.gc.fillRectangle(0, cHeight/3, (int)(1.0*mv_low/mv_max * cWidth), cHeight/3-1);
            e.gc.drawRectangle(0, cHeight/3, (int)(1.0*mv_low/mv_max * cWidth), cHeight/3-1);
            e.gc.drawRectangle(0, cHeight/3, (int)(1.0*mv_high/mv_max * cWidth), cHeight/3-1);

            e.gc.setBackground(DARK_BLUE);
            e.gc.fillRectangle(0, 2*cHeight/3, (int)(1.0*mana_high/mana_max * cWidth), cHeight/3-1);
            e.gc.setBackground(BLUE);
            e.gc.fillRectangle(0, 2*cHeight/3, (int)(1.0*mana_low/mana_max * cWidth), cHeight/3-1);
            e.gc.drawRectangle(0, 2*cHeight/3, (int)(1.0*mana_low/mana_max * cWidth), cHeight/3-1);
            e.gc.drawRectangle(0, 2*cHeight/3, (int)(1.0*mana_high/mana_max * cWidth), cHeight/3-1);
         }
      });
   }
   
   private void createTimers()
   {
      
   }
   
   private void createRoomInfoBox()
   {
      GridData gridData = new GridData();
      gridData.horizontalAlignment = GridData.FILL;
      gridData.grabExcessHorizontalSpace = true;
      gridData.grabExcessVerticalSpace = true;
      gridData.verticalAlignment = GridData.FILL;
      roomInfoText = new StyledText(shell, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
      roomInfoText.setLayoutData(gridData);
      Font mono = new Font(display, "Courier", 12, SWT.NONE);
      roomInfoText.setFont(mono);
      roomInfoText.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
      roomInfoText.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
   }
   
   private void createTimeBox()
   {
      GridData gridData = new GridData();
      gridData.horizontalAlignment = GridData.FILL;
      gridData.grabExcessHorizontalSpace = true;
      gridData.grabExcessVerticalSpace = false;
      gridData.heightHint = 30;
      timeText = new StyledText(shell, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
      timeText.setLayoutData(gridData);
      Font mono = new Font(display, "Courier", 12, SWT.NONE);
      timeText.setFont(mono);
      timeText.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
      timeText.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
   }

   @Override
   public void start()
   {
      display = MudFrame.getInstance().getDisplay();
      try {
         int newFontSize = Integer.valueOf(Configger.getProperty("FONTSIZE", "12")).intValue();
         if (newFontSize != fontSize)
         {
            fontSize = newFontSize;
            Configger.setProperty("FONTSIZE", fontSize+"");
         }
      }
      catch (@SuppressWarnings("unused") NumberFormatException e)
      {
         log.add(Level.INFO, "Couldn't get font size from configger");
      }
      
      RED = display.getSystemColor(SWT.COLOR_RED);
      DARK_RED = display.getSystemColor(SWT.COLOR_DARK_RED);
      BLUE = display.getSystemColor(SWT.COLOR_BLUE);
      DARK_BLUE = display.getSystemColor(SWT.COLOR_DARK_BLUE);
      GREEN = display.getSystemColor(SWT.COLOR_GREEN);
      DARK_GREEN = display.getSystemColor(SWT.COLOR_DARK_GREEN);
      GRAY = display.getSystemColor(SWT.COLOR_GRAY);
      BLACK = display.getSystemColor(SWT.COLOR_BLACK);
      
      shell = new Shell(display);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      shell.setLayout(gridLayout);
      shell.setText("M.U.M.E. Info");

      // try to get the location and size from the Configger
      int xloc = 0;
      int yloc = 0;
      int height = 300;
      int width = 200;
      try
      {
         xloc = Integer.valueOf(Configger.getProperty("MUMEXLOC", "0")).intValue();
         yloc = Integer.valueOf(Configger.getProperty("MUMEYLOC", "0")).intValue();
         height = Integer.valueOf(Configger.getProperty("MUMEheight", "300")).intValue();
         width = Integer.valueOf(Configger.getProperty("MUMEwidth", "200")).intValue();
      }
      catch (NumberFormatException e)
      {
         log.add(Level.WARNING, "Couldn't get Configger values: ", e);
      }

      createHpMpManaBars();
      createTimers();
      createTimeBox();
      createRoomInfoBox();
      
      shell.setLocation(xloc, yloc);
      shell.setSize(width, height);

      // Save resize information to the config file
      shell.addListener(SWT.Resize, new Listener()
      {
         @Override
         public void handleEvent(Event e)
         {
            Configger.setProperty("MUMEwidth", shell.getSize().x + "");
            Configger.setProperty("MUMEheight", shell.getSize().y + "");
         }
      });

      // Save location information to the config file
      shell.addListener(SWT.Move, new Listener()
      {
         @Override
         public void handleEvent(Event e)
         {
            Configger.setProperty("MUMEXLOC", shell.getLocation().x + "");
            Configger.setProperty("MUMEYLOC", shell.getLocation().y + "");
         }
      });

      shell.open();
      isShown = true;

      shell.addListener(SWT.Close, new Listener()
      {
         @Override
         public void handleEvent(Event event)
         {
            System.out.println("Child Shell handling Close event, about to dispose this Shell");
            stop();
            log.stop();
            shell.dispose();
            isShown = false;
         }
      });
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
   public boolean command(String command)
   {
      throw new UnsupportedOperationException("command Not Implemented");
   }

}
