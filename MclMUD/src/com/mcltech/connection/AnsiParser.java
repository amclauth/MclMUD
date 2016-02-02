package com.mcltech.connection;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import com.mcltech.ai.AIListener;
import com.mcltech.base.MudLogger;

public class AnsiParser
{
   private static final MudLogger log = MudLogger.get();
   private static Color[] colors = new Color[10];
   private static List<AIListener> listeners;

   private static final byte esc = (byte) 0x1B;
   private static final byte ctr = (byte) 0x5B;
   private static final byte end = (byte) 0x6D;
   private static final byte sep1 = (byte) 0x3A;
   private static final byte sep2 = (byte) 0x3B;
   private static final byte newline = (byte) 0x0A;
   private static final byte formfeed = (byte) 0x0C;
   private static final byte carriage = (byte) 0x0D;

   private static List<Integer> continuedSequences;
   private static boolean inEscape;
   private static boolean inControl;
   private static boolean isCarriage;
   private static int sequence;
   static List<StyleRange> ranges;
   private static int lbIdx;
   private static MudFrame frame;

   static String lineBuffer;

   public static void init(Display display, MudFrame mudframe)
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
      inEscape = false;
      inControl = false;
      isCarriage = false;
      sequence = 0;
      ranges = new ArrayList<>();
      lineBuffer = "";
      lbIdx = 0;
      frame = mudframe;
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
    * Given a byte string (presumed ansi), parse it for color and style sequences and print it out to the
    * styled text widget
    * 
    * @param text
    * @param bytes
    * @param ret_read
    */
   public static void parseBytes(byte[] bytes, int len)
   {
      byte[] out = new byte[bytes.length];
      int idx = 0;
      int startIdx = 0;

      // try
      // {
      // String s = new String(bytes,0,len,"cp1252");
      // System.out.println(s);
      // }
      // catch (@SuppressWarnings("unused") UnsupportedEncodingException e1)
      // {
      // // just for debug
      // }

      for (int ii = 0; ii < bytes.length && ii < len; ii++)
      {
         byte b = bytes[ii];
         if (b == newline || b == carriage || b == formfeed)
         {
            if (isCarriage)
            {
               isCarriage = false;
               continue;
            }
            isCarriage = true;
            try
            {
               lineBuffer += (new String(out, startIdx, idx - startIdx, "cp1252"));
               startIdx = idx;
            }
            catch (UnsupportedEncodingException e)
            {
               log.add(Level.SEVERE,"UnsupportedEncodingException: ", e);
               System.out.println("Unsupported Encoding Exception");
            }
            processString();
            continue;
         }
         isCarriage = false;
         if (inEscape && b != ctr)
         {
            // skip this character, then
            inEscape = false;
            continue;
         }
         if (b == esc)
         {
            inEscape = true;
            continue;
         }
         else if (inEscape && b == ctr)
         {
            inEscape = false;
            inControl = true;
            sequence = 0;
            continue;
         }
         else if (inControl)
         {
            if (b == sep1 || b == sep2 || b == end)
            {
               setStyle(sequence, lbIdx);
               sequence = 0;
               if (b == end)
               {
                  inControl = false;
               }
               continue;
            }
            if (sequence < 0)
            {
               continue;
            }
            int n = convert(b);
            if (n < 0)
            {
               sequence = -1;
               try
               {
                  log.add(Level.INFO,"Ignoring sequence in this line: " + new String(bytes, 0, len, "cp1252"));
               }
               catch (@SuppressWarnings("unused")
               UnsupportedEncodingException e)
               {
                  // ignore the exception when trying to log the string
               }
               continue;
            }
            sequence = sequence * 10 + n;
         }
         else
         {
            out[idx++] = b;
            lbIdx++;
         }
      }

      try
      {
         lineBuffer += (new String(out, startIdx, idx - startIdx, "cp1252"));
      }
      catch (UnsupportedEncodingException e1)
      {
         log.add(Level.SEVERE,"UnsupportedEncodingException: ", e1);
         System.out.println("Unsupported Encoding Exception");
      }

   }

   public static void flush()
   {
      processString();
   }

   private static void processString()
   {
      for (StyleRange r : ranges)
      {
         if (r.length == -1)
         {
            // TODO ... these should continue across the lines
            r.length = lineBuffer.length() - r.start - 1;
         }
      }

      for (AIListener listener : listeners)
      {
         listener.process(ranges, lineBuffer);
      }

      frame.writeToTextBox(lineBuffer, ranges);

      lineBuffer = "";
      lbIdx = 0;
      ranges.clear();
   }

   /**
    * Add a StyleRange or truncate StyleRanges depending on the control sequence
    * 
    * @param sequence
    * @param ranges
    * @param idx
    */
   private static void setStyle(int sequence, int idx)
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

      switch (sequence)
      {
         case 1: // bold
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
    * 
    * @param b
    * @return
    */
   private static int convert(byte b)
   {
      switch (b)
      {
         case (byte) 0x30:
            return 0;
         case (byte) 0x31:
            return 1;
         case (byte) 0x32:
            return 2;
         case (byte) 0x33:
            return 3;
         case (byte) 0x34:
            return 4;
         case (byte) 0x35:
            return 5;
         case (byte) 0x36:
            return 6;
         case (byte) 0x37:
            return 7;
         case (byte) 0x38:
            return 8;
         case (byte) 0x39:
            return 9;
         default:
            return -1;
      }
   }
}
