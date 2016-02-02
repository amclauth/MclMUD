package com.mcltech.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.logging.Level;

import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.InvalidTelnetOptionException;
import org.apache.commons.net.telnet.SuppressGAOptionHandler;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TelnetInputListener;
import org.apache.commons.net.telnet.TelnetNotificationHandler;
import org.apache.commons.net.telnet.TerminalTypeOptionHandler;
import org.eclipse.swt.graphics.Color;

import com.mcltech.base.MudLogger;

public class Controller implements Runnable, TelnetNotificationHandler, TelnetInputListener
{
   private static final MudLogger log = MudLogger.get();
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
         System.out.println("Could not initiate Telnet.");
         return false;
      }
      catch (IOException e)
      {
         log.add(Level.SEVERE, "Error registering Option handler", e);
         System.out.println("Could not initiate Telnet.");
         return false;
      }

      client.registerNotifHandler(this);
      client.registerInputListener(this);

      return true;
   }

   public boolean isConnected()
   {
      return isConnected;
   }

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
         System.out.println("Command not sent {" + line + "}");
         isConnected = false;
         return false;
      }
      return true;
   }

   public boolean connect(String hostname, int port)
   {
      if (isConnected)
      {
         System.out
               .println("Could not connect to {" + hostname + ":" + port + "}. Connection already in use.");
         return false;
      }

      try
      {
         client.connect(hostname, port);
      }
      catch (SocketException e)
      {
         log.add(Level.SEVERE, "Error connecting to {" + hostname + ":" + port + "}", e);
         System.out.println("Could not connect to {" + hostname + ":" + port + "}");
         return false;
      }
      catch (IOException e)
      {
         log.add(Level.SEVERE, "Error connecting to {" + hostname + ":" + port + "}", e);
         System.out.println("Could not connect to {" + hostname + ":" + port + "}");
         return false;
      }

      Thread reader = new Thread(this);
      reader.start();
      outputStream = client.getOutputStream();
      isConnected = true;
      return true;
   }

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
         frame.writeToTextBox("Disconnected.", null);
      }
      catch (IOException e)
      {
         log.add(Level.SEVERE, "Could not disconnect: ", e);
         System.out.println("Could not disconnect.");
         isConnected = false;
         return false;
      }
      isConnected = false;
      return true;
   }

   @Override
   public void run()
   {
      try (InputStream in = client.getInputStream())
      {
         byte[] buff = new byte[1024];
         int ret_read = 0;

         do
         {
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
            else if (ret_read == 0)
            {
               AnsiParser.flush();
            }
         }
         while (ret_read >= 0);

         in.close();
         isConnected = false;
         frame.writeToTextBox("Disconnected.", null);
      }
      catch (IOException e)
      {
         log.add(Level.SEVERE, "Error reading connection: ", e);
         System.out.println("Could no longer read connection.");
         isConnected = false;
      }
   }

   @Override
   public void receivedNegotiation(int negotiation_code, int option_code)
   {
      // System.out.println("Neg: " + negotiation_code + " Opt: " + option_code);
   }

   @Override
   public void telnetInputAvailable()
   {
      // System.out.println("Input available!");
   }

}
