package com.mcltech.connection;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import com.mcltech.ai.AIListener;

public class AnsiParser
{
   private static final Logger log = Logger.getLogger(AnsiParser.class.getName());
   private static Color[] colors = new Color[10];
   private static List<AIListener> listeners;
   
   private static final byte esc = (byte)0x1B;
   private static final byte ctr = (byte)0x5B;
   private static final byte end = (byte)0x6D;
   private static final byte sep1 = (byte)0x3A;
   private static final byte sep2 = (byte)0x3B;
   
   private static List<Integer> continuedSequences;
   
   public static void init(Display display)
   {
      colors[0] = display.getSystemColor(SWT.COLOR_BLACK);
      colors[1] = display.getSystemColor(SWT.COLOR_RED);
      colors[2] = display.getSystemColor(SWT.COLOR_GREEN);
      colors[3] = display.getSystemColor(SWT.COLOR_YELLOW);
      colors[4] = display.getSystemColor(SWT.COLOR_BLUE);
      colors[5] = display.getSystemColor(SWT.COLOR_MAGENTA);
      colors[6] = display.getSystemColor(SWT.COLOR_CYAN);
      colors[7] = display.getSystemColor(SWT.COLOR_WHITE);
      listeners = new ArrayList<>();
      continuedSequences = new ArrayList<>();
   }
   
   public static void registerListener(AIListener listener)
   {
      listeners.add(listener);
   }
   
   public static void deRegisterListener(AIListener listener)
   {
      listeners.remove(listener);
   }
   
   /**
    * Given a byte string (presumed ansi), parse it for color and style sequences
    * and print it out to the styled text widget
    * @param text
    * @param bytes
    * @param ret_read
    */
   public static void parseBytes(StyledText text, byte[] bytes, int len)
   {
      byte[] out = new byte[bytes.length];
      int idx = 0;
      
      try
      {
         String s = new String(bytes,0,len,"cp1252");
         System.out.println(s);
      }
      catch (@SuppressWarnings("unused") UnsupportedEncodingException e1)
      {
         // just for debug
      }
      List<StyleRange> ranges = new ArrayList<>();
      Integer[] sequences = continuedSequences.toArray(new Integer[0]);
      continuedSequences.clear();
      for (Integer sequence : sequences)
      {
         setStyle(sequence.intValue(), ranges, 0);
      }
      for (int ii = 0; ii < bytes.length && ii < len; ii++)
      {
         byte b = bytes[ii];
         if (b == esc && bytes.length > ii+1 && bytes[ii+1] == ctr)
         {
            int jj = 2;
            int sequence = 0;
            while (ii+jj < len)
            {
               b = bytes[ii+jj];
               if (b == sep1 || b == sep2 || b == end)
               {
                  setStyle(sequence, ranges, idx);
                  if (b == end)
                  {
                     ii += jj;
                     break;
                  }
                  sequence = 0;
                  jj++;
                  continue;
               }
               int n = convert(b);
               if (n < 0)
               {
                  sequence = -1;
                  jj++;
                  try
                  {
                     log.info("Ignoring sequence in this line: " + new String(bytes, 0, len, "cp1252"));
                  }
                  catch (@SuppressWarnings("unused") UnsupportedEncodingException e)
                  {
                     // ignore the exception when trying to log the string
                  }
                  continue;
               }
               sequence = sequence * 10 + n;
               jj++;
            }
         }
         else
         {
            out[idx++] = b;
         }
      }
      
      for (StyleRange r : ranges)
      {
         if (r.length == -1)
         {
            r.length = idx - r.start;
         }
      }
      
      try
      {
         String line = new String(out, 0, idx, "cp1252");
         for (AIListener listener : listeners)
         {
            listener.add(line);
         }
         Display.getDefault().asyncExec(new Runnable()
         {
            @Override
            public void run()
            {
               int caret = text.getCharCount();
               text.append(line);
               for (StyleRange range : ranges)
               {
                  range.start += caret;
                  text.setStyleRange(range);
               }

               while (text.getLineCount() > 1000 || 
                     (text.getTextLimit() > 0 && text.getCharCount() > text.getTextLimit() - 1000))
               {
                  text.replaceTextRange(0,text.getOffsetAtLine(10),"");
               }
            }
         });
      }
      catch (UnsupportedEncodingException e)
      {
         log.severe("UnsupportedEncodingException: " + e.getMessage());
         System.out.println("Unsupported Encoding Exception");
      }
   }
   
   /**
    * Add a StyleRange or truncate StyleRanges depending on the control sequence
    * @param sequence
    * @param ranges
    * @param idx
    */
   private static void setStyle(int sequence, List<StyleRange> ranges, int idx)
   {
      if (sequence == 0)
      {
         continuedSequences.clear();
         for (StyleRange range : ranges)
         {
            if (range.length == -1)
            {
               range.length = idx - range.start;
            }
         }
         return;
      }
      
      continuedSequences.add(Integer.valueOf(sequence));
      
      StyleRange range = new StyleRange();
      range.start = idx;
      range.length = -1;
      
      switch(sequence)
      {
         case 1: //bold
            range.fontStyle = SWT.BOLD;
            break;
         case 4: // underline
            range.fontStyle = SWT.UNDERLINE_SINGLE;
            break;
         case 30: // foreground
         case 31:
         case 32:
         case 33:
         case 34:
         case 35:
         case 36:
         case 37:
            range.foreground = colors[sequence - 30];
            break;
         case 40:
         case 41:
         case 42:
         case 43:
         case 44:
         case 45:
         case 46:
         case 47:
            range.background = colors[sequence - 40];
            break;
         default:
            return; // do nothing
      }
      
      ranges.add(range);
   }
   
   /**
    * Convert bytes to numbers (since Java has a weird way of handling bytes)
    * @param b
    * @return
    */
   private static int convert(byte b)
   {
      switch (b)
      {
         case (byte)0x30:
            return 0;
         case (byte)0x31:
            return 1;
         case (byte)0x32:
            return 2;
         case (byte)0x33:
            return 3;
         case (byte)0x34:
            return 4;
         case (byte)0x35:
            return 5;
         case (byte)0x36:
            return 6;
         case (byte)0x37:
            return 7;
         case (byte)0x38:
            return 8;
         case (byte)0x39:
            return 9;
         default:
            return -1;
      }
   }
}
