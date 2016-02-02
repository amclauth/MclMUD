package com.mcltech.connection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.mcltech.base.MudLogger;

/**
 * An SWT implementation that has a menu, output screen, and input text box
 * 
 * @author andymac
 *
 */
public class MudFrame
{
   Shell shell;
   private Display display;
   private static final MudLogger log = MudLogger.getInstance();
   StyledText outputText;
   Text inputText;
   Controller controller;

   public MudFrame()
   {

   }

   /**
    * Print a line of text to the output screen. This will accept a string input and is formatted by the style
    * ranges. It will also clear 10 lines of the output screen at a time if over 1000 lines of text are in the
    * screen to keep from overflowing any buffers.
    * 
    * @param line
    * @param ranges (start should be set with respect to the string itself)
    */
   public void writeToTextBox(String line, List<StyleRange> ranges)
   {
      List<StyleRange> rangeCopy = new ArrayList<>();
      if (ranges != null)
         rangeCopy.addAll(ranges);

      Display.getDefault().asyncExec(new Runnable()
      {
         @Override
         public void run()
         {
            int caret = outputText.getCharCount();
            outputText.append(line + "\n");
            for (StyleRange range : rangeCopy)
            {
               range.start += caret;
               try
               {
                  outputText.setStyleRange(range);
               }
               catch (@SuppressWarnings("unused")
               IllegalArgumentException e)
               {
                  // don't die if this happens somehow (update while trimming lines from above)
                  System.out.println(range.start + ":" + range.length + ":" + caret);
               }
            }

            while (outputText.getLineCount() > 1000 || (outputText.getTextLimit() > 0
                  && outputText.getCharCount() > outputText.getTextLimit() - 1000))
            {
               int offset = outputText.getOffsetAtLine(10);
               System.out.println("Idx: " + offset);
               if (offset < outputText.getCharCount() && offset > 0)
                  outputText.replaceTextRange(0, offset, "");
            }
         }
      });
   }

   /**
    * Create the various elements and start initialize the controller and AnsiParser
    */
   public void init()
   {
      display = new Display();
      shell = new Shell(display);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      shell.setLayout(gridLayout);
      shell.setText("MclMUD Client");

      createMenu();
      createOutputScreen();
      createInputScreen();

      AnsiParser.init(display, this);
      controller = new Controller(this);
      controller.init();

      // try to get the location and size from the Configger
      int xloc = 0;
      int yloc = 0;
      int height = 600;
      int width = 800;
      try
      {
         xloc = Integer.valueOf(Configger.getProperty("XLOC", "0")).intValue();
         yloc = Integer.valueOf(Configger.getProperty("YLOC", "0")).intValue();
         height = Integer.valueOf(Configger.getProperty("height", "600")).intValue();
         width = Integer.valueOf(Configger.getProperty("width", "800")).intValue();
      }
      catch (NumberFormatException e)
      {
         log.add(Level.WARNING, "Couldn't get Configger values: ", e);
      }

      shell.setLocation(xloc, yloc);
      shell.setSize(width, height);

      // Save resize information to the config file
      shell.addListener(SWT.Resize, new Listener()
      {
         @Override
         public void handleEvent(Event e)
         {
            Configger.setProperty("width", shell.getSize().x + "");
            Configger.setProperty("height", shell.getSize().y + "");
         }
      });

      // Save location information to the config file
      shell.addListener(SWT.Move, new Listener()
      {
         @Override
         public void handleEvent(Event e)
         {
            Configger.setProperty("XLOC", shell.getLocation().x + "");
            Configger.setProperty("YLOC", shell.getLocation().y + "");
         }
      });

      shell.open();

      writeToTextBox("Welcome to the MclMUD client\n\n", null);
      writeToTextBox("To join a MUD, use the \"Connect\" button in the menu above or add a new connection\n",
            null);
      writeToTextBox(
            "by entering the name:address:port below. For example, to create a MUME connection, enter:\nMUME:mume.org:4242\n",
            null);

      while (!shell.isDisposed())
      {
         if (!display.readAndDispatch())
            display.sleep();
      }
      display.dispose();
   }

   /**
    * Screen that holds the output from the terminal
    */
   private void createOutputScreen()
   {
      GridData gridData = new GridData();
      gridData.horizontalAlignment = GridData.FILL;
      gridData.grabExcessHorizontalSpace = true;
      gridData.grabExcessVerticalSpace = true;
      gridData.verticalAlignment = GridData.FILL;
      outputText = new StyledText(shell, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
      outputText.setLayoutData(gridData);
      Font mono = new Font(display, "Courier", 12, SWT.NONE);
      outputText.setFont(mono);
      outputText.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
      outputText.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
      // scroll the screen to the bottom on any new input
      outputText.addListener(SWT.Modify, new Listener()
      {
         @Override
         public void handleEvent(Event e)
         {
            outputText.setTopIndex(outputText.getLineCount() - 1);
         }
      });
      // move the focus to the input box if this screen is clicked (this screen will
      // often be clicked to give the app focus)
      outputText.addListener(SWT.MouseUp, new Listener()
      {
         @Override
         public void handleEvent(Event e)
         {
            inputText.setFocus();
         }
      });

   }

   /**
    * Input text box for sending commands to the terminal
    * 
    * TODO - maybe add a limited stack to track commands?
    */
   private void createInputScreen()
   {
      GridData gridData = new GridData();
      gridData.horizontalAlignment = GridData.FILL;
      gridData.verticalAlignment = GridData.FILL;
      gridData.grabExcessHorizontalSpace = true;
      gridData.grabExcessVerticalSpace = false;
      gridData.heightHint = 50;
      inputText = new Text(shell, SWT.SINGLE | SWT.BORDER | SWT.WRAP);
      inputText.setLayoutData(gridData);
      // handle the user pressing enter as the way to send commands
      inputText.addListener(SWT.Traverse, new Listener()
      {
         @Override
         public void handleEvent(Event e)
         {
            if (e.detail == SWT.TRAVERSE_RETURN)
            {
               if (!controller.isConnected())
               {
                  addConnection(inputText.getText());
               }
               controller.write(inputText.getText() + "\n");
               // keep the text there and selected, so just pressing enter
               // repeats the last command
               inputText.selectAll();
            }
         }
      });
   }

   /**
    * Create the menu for connecting and disconnecting
    */
   private void createMenu()
   {
      Menu menuBar = new Menu(shell, SWT.BAR);

      // connect main item
      MenuItem connectionMenuItem = new MenuItem(menuBar, SWT.CASCADE);
      connectionMenuItem.setText("&Connect");

      Menu connectionMenu = new Menu(shell, SWT.DROP_DOWN);
      connectionMenuItem.setMenu(connectionMenu);

      // add each mud in the config file
      List<String> muds = new ArrayList<>();
      for (String mud : Configger.getProperty("MUDS", "").split(":"))
      {
         if (!mud.equals(""))
            muds.add(mud);
      }
      Collections.sort(muds);

      for (String mud : muds)
      {
         String[] details = Configger.getProperty(mud, "").split(":");
         if (details.length != 2)
         {
            log.add(Level.WARNING, "Got bad data for mud: " + mud);
            writeToTextBox("Couldn't use this connection. Information is in bad format in config: {"
                  + Arrays.toString(details) + "}", null);
            continue;
         }
         MenuItem mumeItem = new MenuItem(connectionMenu, SWT.PUSH);
         mumeItem.setText("&" + mud);
         // connect when clicked
         mumeItem.addSelectionListener(new SelectionAdapter()
         {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
               controller.connect(details[0], Integer.valueOf(details[1]).intValue());
            }
         });
      }

      // disconnect button
      MenuItem disconnectMenuItem = new MenuItem(menuBar, SWT.CASCADE);
      disconnectMenuItem.setText("&Disconnect");
      disconnectMenuItem.addSelectionListener(new SelectionAdapter()
      {
         @Override
         public void widgetSelected(SelectionEvent e)
         {
            controller.disconnect();
         }
      });

      shell.setMenuBar(menuBar);
   }

   /**
    * Add a connection when the user is disconnected and enters a name:host:port command
    * @param line
    */
   void addConnection(String line)
   {
      int idx1 = line.indexOf(':');
      int idx2 = line.indexOf(':', idx1 + 1);
      if (idx1 > 0 && idx2 > 0)
      {
         String name = line.substring(0, idx1);
         String con = line.substring(idx1 + 1, idx2);
         String pString = line.substring(idx2 + 1);
         int port = 0;
         try
         {
            port = Integer.valueOf(pString).intValue();
         }
         catch (NumberFormatException e)
         {
            writeToTextBox("\nPort wasn't a number. Got name{" + name + "} address{" + con + "} port{"
                  + pString + "}\n", null);
            log.add(Level.WARNING, "Couldn't parse number: ", e);
            return;
         }

         writeToTextBox("\nAdding connection name{" + name + "} address{" + con + "} port{" + pString + "}\n",
               null);

         System.out.println(name + "," + con + "," + port);
         String muds = Configger.getProperty("MUDS", "");
         if (Configger.getProperty(name, "").equals(""))
         {
            // new one
            List<String> arr = new ArrayList<>();
            for (String s : muds.split(":"))
            {
               if (!s.equals(""))
                  arr.add(s);
            }
            arr.add(name);
            Configger.setProperty("MUDS", String.join(":", arr));
            Configger.setProperty(name, con + ":" + pString);
         }
         else
         {
            // replacement
            Configger.setProperty(name, con + ":" + pString);
         }

         createMenu();
         shell.update();
      }
      else
      {
         writeToTextBox("\nIf entering a connection, need that in name:address:port style.\n", null);
         return;
      }
   }
}
