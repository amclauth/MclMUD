package mcltech.clickbot.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.net.URISyntaxException;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class MainFrame extends JFrame
{
   private static final long serialVersionUID = 1L;
   /*
    * TODO - small frame with a flashing "start" button that disappears when
    * clicked. Replaced with
    * bar that "loads" as it's getting ready to go. It resets the bar if it
    * hasn't reached completion
    * so you can move around within the 1s window until you're where you need to
    * be. Click the bar again
    * for cancel (label it as such). The clicking stops (and resets the window)
    * if you've moved more than
    * 50 pixels after it starts.
    */
   private String            guiTitle         = "Clickbot 5000";
   private int               windowHeight;
   private int               windowWidth;
   private int               windowXPos;
   private int               windowYPos;
   
   
   public MainFrame()
   {
      initComponents();
      setVisible(true);
   }
   
   private void initComponents()
   {
      getSettings();
      
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      // setSize(windowWidth, windowHeight);
      // setPreferredSize(new Dimension(windowWidth, windowHeight));
      setLocation(windowXPos, windowYPos);
      setTitle(guiTitle);
      
      JPanel activePanel = null;
      try
      {
         activePanel = new ActivePanel();
      }
      catch (URISyntaxException e)
      {
         e.printStackTrace();
      }
      getContentPane().setLayout(new BorderLayout());
      getContentPane().add(activePanel, BorderLayout.CENTER);
      
      pack();
   }
   
   private void getSettings()
   {
      // Figure out some default values
      Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
      windowWidth = 400;
      
      windowHeight = 100;
      windowXPos = (dim.width - windowWidth) / 2;
      windowYPos = (dim.height - windowHeight) / 2;
   }
}
