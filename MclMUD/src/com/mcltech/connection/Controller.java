package com.mcltech.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.logging.Logger;

import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.InvalidTelnetOptionException;
import org.apache.commons.net.telnet.SuppressGAOptionHandler;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TerminalTypeOptionHandler;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class Controller implements Runnable
{
   private static final Logger log = Logger.getLogger(Controller.class.getName());
   private TelnetClient client;
   private boolean isConnected;
   private OutputStream outputStream;
   StyledText text;
   Color[] colors = new Color[10];

   public Controller(StyledText text)
   {
      isConnected = false;
      this.text = text;
   }
   
   public boolean init(Display display)
   {
      AnsiParser.init(display);
   
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
         log.severe("Error registering Option handler: " + e.getMessage());
         System.out.println("Could not initiate Telnet.");
         return false;
      }
      catch (IOException e)
      {
         log.severe("Error registering Option handler: " + e.getMessage());
         System.out.println("Could not initiate Telnet.");
         return false;
      }
      
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
         // TODO put this higher in the class
         outputStream.write(line.getBytes());
         outputStream.flush();
//         System.out.println("Writing: " + line);
      }
      catch (IOException e)
      {
         log.severe("Command not sent {" + line + "}: " + e.getMessage());
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
         System.out.println("Could not connect to {" + hostname + ":" + port + "}. Connection already in use.");
         return false;
      }
      
      try
      {
         client.connect(hostname, port);
      }
      catch (SocketException e)
      {
         log.severe("Error connecting to {" + hostname + ":" + port + "}: " + e.getMessage());
         System.out.println("Could not connect to {" + hostname + ":" + port + "}");
         return false;
      }
      catch (IOException e)
      {
         log.severe("Error connecting to {" + hostname + ":" + port + "}: " + e.getMessage());
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
         log.info("Disconnect.");
         System.out.println("Disconnect.");
      }
      catch (IOException e)
      {
         log.severe("Could not disconnect: " + e.getMessage());
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
            ret_read = in.read(buff);
            if (ret_read > 0)
            {
               AnsiParser.parseBytes(text, buff, ret_read); 
//               System.out.print(new String(buff, 0, ret_read, "cp1252")); //IBM850,cp1252);
            }
         }
         while (ret_read >= 0);
         
         in.close();
         isConnected = false;
      }
      catch (IOException e)
      {
         log.severe("Error reading connection: " + e.getMessage());
         System.out.println("Could no longer read connection.");
         isConnected = false;
      }
   }
   
}
