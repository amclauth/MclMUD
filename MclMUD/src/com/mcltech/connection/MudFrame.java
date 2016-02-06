package com.mcltech.connection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
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

import com.mcltech.ai.AIListener;
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
   static final MudLogger log = MudLogger.getInstance();
   StyledText outputText;
   Text inputText;
   Controller controller;
   AIListener ai;
   List<String> commandStack = new LinkedList<>();
   static int COMMAND_STACK_SIZE;
   int commandStackIdx = -1;
   public static Color[] colors = new Color[10];

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

      if (display.isDisposed())
         return;

      display.asyncExec(new Runnable()
      {
         @Override
         public void run()
         {
            int caret = outputText.getCharCount();
            outputText.append(line);
            log.add(Level.INFO, line);
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
               }
            }

            while (outputText.getLineCount() > 1000 || (outputText.getTextLimit() > 0
                  && outputText.getCharCount() > outputText.getTextLimit() - 1000))
            {
               int offset = outputText.getOffsetAtLine(10);
               if (offset < outputText.getCharCount() && offset > 0)
                  outputText.replaceTextRange(0, offset, "");
            }
         }
      });
   }

   public void updateTitle(String title)
   {
      // TODO this sometimes fails when closing down ... might check that display is active
      Display.getDefault().asyncExec(new Runnable()
      {
         @Override
         public void run()
         {
            if (title == null || title.trim().length() == 0)
               shell.setText("MclMUD Client");
            else
               shell.setText("MclMUD Client " + title.trim());
         }
      });
   }

   /**
    * Create the various elements and start initialize the controller and AnsiParser
    */
   public void init()
   {
      try
      {
         if (Configger.getProperty("COMMANDSTACKSIZE", "").equals(""))
         {
            Configger.setProperty("COMMANDSTACKSIZE", "20");
            COMMAND_STACK_SIZE = 20;
         }
         else
         {
            COMMAND_STACK_SIZE = Integer.valueOf(Configger.getProperty("COMMANDSTACKSIZE", "20")).intValue();
         }
      }
      catch (@SuppressWarnings("unused") NumberFormatException e)
      {
         log.add(Level.WARNING, "Couldn't convert COMMANDSTACKSIZE to an integer from the config file.");
         COMMAND_STACK_SIZE = 20;
      }
      display = new Display();
      // set up the system colors
      colors[0] = display.getSystemColor(SWT.COLOR_BLACK);
      colors[1] = display.getSystemColor(SWT.COLOR_RED);
      colors[2] = display.getSystemColor(SWT.COLOR_GREEN);
      colors[3] = display.getSystemColor(SWT.COLOR_YELLOW);
      colors[4] = display.getSystemColor(SWT.COLOR_BLUE);
      colors[5] = display.getSystemColor(SWT.COLOR_MAGENTA);
      colors[6] = display.getSystemColor(SWT.COLOR_CYAN);
      colors[7] = display.getSystemColor(SWT.COLOR_WHITE);
      colors[8] = display.getSystemColor(SWT.COLOR_GRAY);
      shell = new Shell(display);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      shell.setLayout(gridLayout);
      shell.setText("MclMUD Client");

      createMenu();
      createOutputScreen();
      createInputScreen();

      ai = new AIListener(this, "Basic");
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
      log.stop();
      display.dispose();
      stop();
   }

   /**
    * Stop ongoing processes in the ai and controller
    */
   public void stop()
   {
      ai.deregister();
      controller.disconnect();
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
      inputText.addKeyListener(new KeyListener()
      {

         @Override
         public void keyPressed(KeyEvent e)
         {
            if ((e.stateMask & SWT.CTRL) != 0 && (e.keyCode == 117 || e.keyCode == 108))
            {
               inputText.setText("");
            }

         }

         @Override
         public void keyReleased(KeyEvent e)
         {
            if (e.keyCode == SWT.ARROW_UP)
            {
               if (commandStackIdx > 0)
               {
                  commandStackIdx--;
                  inputText.setText(commandStack.get(commandStackIdx));
                  inputText.selectAll();
               }
            }
            else if (e.keyCode == SWT.ARROW_DOWN)
            {
               if (commandStackIdx < commandStack.size() - 1)
               {
                  commandStackIdx++;
                  inputText.setText(commandStack.get(commandStackIdx));
                  inputText.selectAll();
               }
            }
         }
      });
      // handle the user pressing enter as the way to send commands
      inputText.addListener(SWT.Traverse, new Listener()
      {
         @Override
         public void handleEvent(Event e)
         {
            if (e.detail == SWT.TRAVERSE_RETURN)
            {
               String input = inputText.getText().trim();

               commandStack.add(input);
               while (commandStack.size() > COMMAND_STACK_SIZE)
               {
                  commandStack.remove(0);
               }
               commandStackIdx = commandStack.size() - 1;

               if (!controller.isConnected())
               {
                  addConnection(input);
                  inputText.selectAll();
                  return;
               }
               List<String> commands = ai.processCommand(input);
               if (commands != null)
               {
                  String com = String.join(";", commands);
                  writeToTextBox(com + "\n", null);
                  for (String command : commands)
                  {
                     controller.write(command + "\n");
                     log.add(Level.INFO, command);
                  }
               }
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
      connectionMenuItem.setText("&Connection");

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
               // try to find an AI
               if (!ai.swapAI(mud))
               {
                  ai.swapAI("basic");
               }
            }
         });
      }

      @SuppressWarnings("unused")
      MenuItem menuItem = new MenuItem(connectionMenu, SWT.SEPARATOR);

      // disconnect button
      MenuItem disconnectMenuItem = new MenuItem(connectionMenu, SWT.PUSH);
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
    * 
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
         log.add(Level.INFO, "Adding connection name{" + name + "} address{" + con + "} port{" + port + "}");

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

   /**
    * Get the listener for this connection
    * 
    * @return
    */
   public AIListener getListener()
   {
      return ai;
   }
}
