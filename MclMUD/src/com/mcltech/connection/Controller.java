package com.mcltech.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;

import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.InvalidTelnetOptionException;
import org.apache.commons.net.telnet.SuppressGAOptionHandler;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TerminalTypeOptionHandler;
import org.eclipse.swt.graphics.Color;

import com.mcltech.base.MudLogger;

/**
 * Handles the telnet connection. Use this by calling init to set up the terminal options
 * and then calling "connect". It will start a reader thread that passes information to
 * the AnsiParser and then to the screen. Send commands through "write". To stop it,
 * just use "disconnect".
 * @author andymac
 *
 */
public class Controller implements Runnable //, TelnetNotificationHandler, TelnetInputListener
{
   private static final MudLogger log = MudLogger.getInstance();
   private TelnetClient client;
   private MudFrame frame;
   private boolean isConnected;
   private OutputStream outputStream;
   Color[] colors = new Color[10];

   public Controller(MudFrame frame)
   {
      isConnected = false;
      this.frame = frame;
   }

   /**
    * Set up the terminal
    * @return
    */
   public boolean init()
   {
      TerminalTypeOptionHandler ttopt = new TerminalTypeOptionHandler("VT100", false, false, false, false);
      EchoOptionHandler echoopt = new EchoOptionHandler(true, false, true, false);
      SuppressGAOptionHandler gaopt = new SuppressGAOptionHandler(true, true, true, true);
      client = new TelnetClient();

      try
      {
         client.addOptionHandler(ttopt);
         client.addOptionHandler(echoopt);
         client.addOptionHandler(gaopt);
      }
      catch (InvalidTelnetOptionException e)
      {
         log.add(Level.SEVERE, "Error registering Option handler", e);
         return false;
      }
      catch (IOException e)
      {
         log.add(Level.SEVERE, "Error registering Option handler", e);
         return false;
      }

//      client.registerNotifHandler(this);
//      client.registerInputListener(this);

      return true;
   }

   /**
    * Check if it's connected
    * @return is currently connected to a host
    */
   public boolean isConnected()
   {
      return isConnected;
   }

   /**
    * Write a string to the terminal and send it if connected.
    * @param line
    * @return false if not connected or error sending
    */
   public boolean write(String line)
   {
      if (!isConnected)
      {
         return false;
      }
      try
      {
         outputStream.write(line.getBytes());
         outputStream.flush();
      }
      catch (IOException e)
      {
         log.add(Level.SEVERE, "Command not sent {" + line + "}", e);
         frame.writeToTextBox("\n\n***Could not send command! Disconnecting. Error is logged.***\n\n", null);
         disconnect(); // clean up
         return false;
      }
      return true;
   }

   /**
    * Connect to a specific host / port and start the reader thread.
    * @param hostname
    * @param port
    * @return
    */
   public boolean connect(String hostname, int port)
   {
      if (isConnected)
      {
         log.add(Level.INFO,
               "Could not connect to {" + hostname + ":" + port + "}. A connection is already in use.");
         frame.writeToTextBox(
               "\n\n***Could not connect to {" + hostname + ":" + port + "}. A connection is already in use.***\n\n", null);
         return false;
      }

      try
      {
         client.connect(hostname, port);
      }
      catch (IOException e)
      {
         log.add(Level.SEVERE, "Error connecting to {" + hostname + ":" + port + "}", e);
         frame.writeToTextBox(
               "\n\n***Could not connect to {" + hostname + ":" + port + "}. Error is logged.***\n\n", null);
         return false;
      }

      Thread reader = new Thread(this);
      reader.start();
      outputStream = client.getOutputStream();
      isConnected = true;
      return true;
   }

   /**
    * Disconnect.
    * @return true if disconnected or disconnecting succeeds
    */
   public boolean disconnect()
   {
      if (!isConnected)
      {
         return true;
      }

      try
      {
         client.disconnect();
         log.add(Level.INFO, "Disconnect.");
         frame.writeToTextBox("\nDisconnected.\n", null);
      }
      catch (IOException e)
      {
         log.add(Level.SEVERE, "Could not disconnect: ", e);
         frame.writeToTextBox("\n\n***Could not disconnect. Error is logged.***\n\n", null);
         isConnected = false;
         return false;
      }

      isConnected = false;
      frame.stop();
      return true;
   }

   /**
    * Reader thread. Read the input stream. Disconnect if the socket is closed. Pass
    * information to the AnsiParser
    */
   @Override
   public void run()
   {
      try (InputStream in = client.getInputStream())
      {
         byte[] buff = new byte[1024];
         int ret_read = 0;

         do
         {
            // stream is finished sending (for now), print to screen
            if (in.available() == 0)
            {
               AnsiParser.flush();
            }
            ret_read = in.read(buff);
            if (ret_read > 0)
            {
               AnsiParser.parseBytes(buff, ret_read);
               // System.out.print(new String(buff, 0, ret_read, "cp1252")); //IBM850,cp1252);
            }
            // read zero bytes. Flush the output to the screen.
            else if (ret_read == 0)
            {
               AnsiParser.flush();
            }
         }
         while (ret_read >= 0 && isConnected);

         in.close();
         isConnected = false;
         frame.writeToTextBox("Disconnected.", null);
      }
      catch (IOException e)
      {
         log.add(Level.SEVERE, "Could no longer read connection. ", e);
         frame.writeToTextBox("\n\n***Could no longer read connection. Disconnecting. Error is logged.***\n\n", null);
         disconnect();
      }
   }

//   @Override
//   public void receivedNegotiation(int negotiation_code, int option_code)
//   {
//      // System.out.println("Neg: " + negotiation_code + " Opt: " + option_code);
//   }
//
//   @Override
//   public void telnetInputAvailable()
//   {
//      // System.out.println("Input available!");
//   }

}
