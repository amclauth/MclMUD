package mcltech.clickbot.gui;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import mcltech.clickbot.ClickBot;

public class ActivePanel extends JPanel implements MouseListener
{
   private enum STATE
   {
      READY, WAITING, RUNNING
   };
   
   private static final long   serialVersionUID = 1L;
   private final static Color  bgColor          = Color.BLACK;
   private final static Font   buttonFont       = new Font("Serif", Font.BOLD, 30);
   private final static String readyString      = "READY";
   private final static String steadyString     = "STEADY";
   private final static String activeString     = "ACTIVE";
   
   private STATE               state;
   private int                 divisions        = 100;
   private int                 timerCount       = 0;
   private int                 size             = 100;
   private List<Integer>       xPoints;
   private List<Integer>       yPoints;
   private Timer               timer;
   private Point               originalPoint;
   private static final int    maxMotion        = 10;
   private ClickBot            bot;
   private int                 timerDelay;
   int                         distance         = 0;
   
   private JButton             linkButton;
   final private URI           uri;
   
   public ActivePanel() throws URISyntaxException
   {
      this.setBackground(bgColor);
      setPreferredSize(new Dimension(300, 100));
      addMouseListener(this);
      makeCirclePoints();
      bot = new ClickBot();
      bot.setActivePanel(this);
      state = STATE.READY;
      timerDelay = 1000;
      
      uri = new URI("http://McLauthlinTech.com");
      
      linkButton = new JButton();
      linkButton.setText("<HTML><u><i>McLauthlinTech.com</i></u></HTML>");
      linkButton.setHorizontalAlignment(SwingConstants.RIGHT);
      linkButton.setBorderPainted(false);
      linkButton.setOpaque(false);
      linkButton.setBackground(Color.BLACK);
      linkButton.setToolTipText(uri.toString());
      linkButton.setForeground(Color.WHITE);
      linkButton.addActionListener(new OpenUrlAction());
      
      setLayout(new GridBagLayout());
      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 0;
      c.anchor = GridBagConstraints.LAST_LINE_END;
      c.weighty = 1;
      c.weightx = 1;
      add(linkButton, c);
   }
   
   class OpenUrlAction implements ActionListener
   {
      @Override
      public void actionPerformed(ActionEvent e)
      {
         if (Desktop.isDesktopSupported())
         {
            try
            {
               Desktop.getDesktop().browse(uri);
            }
            catch (IOException e1)
            { /* TODO: error handling */
            }
         }
         else
         { /* TODO: error handling */
         }
      }
   }
   
   private void makeCirclePoints()
   {
      xPoints = new ArrayList<Integer>();
      yPoints = new ArrayList<Integer>();
      for (int ii = 0; ii < divisions; ii++)
      {
         xPoints.add((int) (Math.sin(ii * 2.0 * Math.PI / divisions) * size / 2));
         yPoints.add((int) (Math.cos(ii * 2.0 * Math.PI / divisions) * size / 2));
      }
      xPoints.add(0);
      yPoints.add(size / 2);
   }
   
   private void drawCircularButton(Graphics2D g, int x, int y)
   {
      Color buttonColor = null;
      if (state == STATE.READY || state == STATE.WAITING)
         buttonColor = Color.green;
      else if (state == STATE.RUNNING)
         buttonColor = blendColor(Color.blue, Color.red, bot.getDistanceRatio());
      
      for (int ii = 0; ii < divisions; ii++)
      {
         Color stateColor = buttonColor;
         if (ii < timerCount)
            stateColor = Color.cyan;
         
         if (ii % 2 == 0)
            g.setColor(stateColor.brighter());
         else
            g.setColor(stateColor.darker());
         int xCen = size / 2 + x;
         int yCen = size / 2 + y;
         int[] xArr = { xCen, xPoints.get(ii) + xCen, xPoints.get(ii + 1) + xCen };
         int[] yArr = { yCen, yPoints.get(ii) + yCen, yPoints.get(ii + 1) + yCen };
         g.fillPolygon(xArr, yArr, 3);
      }
   }
   
   @Override
   public void paintComponent(Graphics g)
   {
      super.paintComponent(g);
      
      Graphics2D g2 = (Graphics2D) g;
      
      drawCircularButton(g2, 0, 0);
      
      // draw the text in the buttons
      FontRenderContext frc = g2.getFontRenderContext();
      g.setFont(buttonFont);
      
      String curString = "";
      switch (state)
      {
         case READY:
            curString = readyString;
            break;
         case RUNNING:
            curString = activeString;
            break;
         case WAITING:
            curString = steadyString;
            break;
         default:
            break;
      }
      Rectangle2D bounds = buttonFont.getStringBounds(curString, frc);
      g.setColor(Color.RED);
      g.drawString(curString, (int) (size / 2 + bounds.getCenterX()), size / 2 - (int) bounds.getCenterY());
   }
   
   private void runRobot()
   {
      state = STATE.RUNNING;
      timer.cancel();
      timer.purge();
      timerCount = 0;
      repaint();
      
      bot.startRobot();
   }
   
   public void reset()
   {
      timerDelay = 5000;
      timerCount = 0;
      bot.stopRobot();
      if (timer != null)
      {
         timer.cancel();
         timer.purge();
      }
      startWaiting();
   }
   
   private void startWaiting()
   {
      state = STATE.WAITING;
      timerCount = 0;
      
      originalPoint = MouseInfo.getPointerInfo().getLocation();
      timer = new Timer();
      timer.scheduleAtFixedRate(new TimerTask()
      {
         @Override
         public void run()
         {
            timerCount++;
            Point p = MouseInfo.getPointerInfo().getLocation();
            if (p.distance(originalPoint) > maxMotion)
            {
               originalPoint = p;
               timerCount = 0;
            }
            else if (timerCount >= divisions)
            {
               timerCount = 0;
               runRobot();
            }
            repaint();
         }
      }, 0, timerDelay / divisions);
      repaint();
   }
   
   private Color blendColor(Color c1, Color c2, double ratio)
   {
      // re-sample ratio
      ratio = (ratio - 0.5) * 2;
      if (ratio > 1)
         ratio = 1.0;
      else if (ratio < 0)
         ratio = 0;
      
      int i1 = c1.getRGB();
      int i2 = c2.getRGB();
      
      int a1 = (i1 >> 24 & 0xff);
      int r1 = ((i1 & 0xff0000) >> 16);
      int g1 = ((i1 & 0xff00) >> 8);
      int b1 = (i1 & 0xff);
      
      int a2 = (i2 >> 24 & 0xff);
      int r2 = ((i2 & 0xff0000) >> 16);
      int g2 = ((i2 & 0xff00) >> 8);
      int b2 = (i2 & 0xff);
      
      int a = (int) ((a1 * (1 - ratio)) + (a2 * ratio));
      int r = (int) ((r1 * (1 - ratio)) + (r2 * ratio));
      int g = (int) ((g1 * (1 - ratio)) + (g2 * ratio));
      int b = (int) ((b1 * (1 - ratio)) + (b2 * ratio));
      
      return new Color(a << 24 | r << 16 | g << 8 | b);
   }
   
   @Override
   public void mouseClicked(MouseEvent arg0)
   {
      // changing states
      if (state == STATE.READY)
      {
         startWaiting();
      }
      else if (state == STATE.RUNNING || state == STATE.WAITING)
      {
         state = STATE.READY;
         timerCount = 0;
         timerDelay = 1000;
         if (timer != null)
         {
            timer.cancel();
            timer.purge();
         }
         bot.stopRobot();
         repaint();
      }
   }
   
   @Override
   public void mouseEntered(MouseEvent arg0)
   {
   }
   
   @Override
   public void mouseExited(MouseEvent arg0)
   {
   }
   
   @Override
   public void mousePressed(MouseEvent arg0)
   {
   }
   
   @Override
   public void mouseReleased(MouseEvent arg0)
   {
   }
}
