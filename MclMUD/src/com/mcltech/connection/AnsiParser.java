package com.mcltech.connection;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.widgets.Display;

import com.mcltech.base.MudLogger;

/**
 * Parse the ansi input and escape sequences into string and style ranges. This is
 * not a thread safe class, but the input is serialized by the controller's read, so
 * should be quite safe for this application.
 * @author andymac
 * 
 */
public class AnsiParser
{
   private static final MudLogger log = MudLogger.getInstance();

   private static final byte esc = (byte) 0x1B;
   private static final byte ctr = (byte) 0x5B;
   private static final byte end = (byte) 0x6D;
   private static final byte sep1 = (byte) 0x3A;
   private static final byte sep2 = (byte) 0x3B;
   private static final byte newline = (byte) 0x0A;
   private static final byte formfeed = (byte) 0x0C;
   private static final byte carriage = (byte) 0x0D;
   private static final byte bell = (byte) 0x07;
   
   private boolean inEscape;
   private boolean inControl;
   private boolean isCarriage;
   private int sequence;
   private List<StyleRange> ranges;
   private int lbIdx;

   private String lineBuffer;
   
   // singleton class holder pattern
   private static final class holder
   {
      static final AnsiParser INSTANCE = new AnsiParser();
   }

   public static AnsiParser getInstance()
   {
      return holder.INSTANCE;
   }
   
   AnsiParser() {}

   /**
    * Initialize the parser
    * @param display
    * @param mudframe
    */
   public void init(Display display)
   {
      inEscape = false;
      inControl = false;
      isCarriage = false;
      sequence = 0;
      ranges = new ArrayList<>();
      lineBuffer = "";
      lbIdx = 0;
   }

   /**
    * Given a byte string (presumed ansi), parse it for color and style sequences and print it out to the
    * styled text widget
    * 
    * @param text
    * @param bytes
    * @param ret_read
    */
   public synchronized void parseBytes(byte[] bytes, int len)
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
         // ignore the double \r\n for conversion, but don't skip \n\n or others
         if (isCarriage)
         {
            if (b == newline)
            {
               isCarriage = false;
               continue;
            }
            isCarriage = false;
         }
         if (b == carriage)
         {
            isCarriage = true;
         }
         // on a newline, add to the string and put it onto the output text box
         if (b == newline || b == carriage || b == formfeed)
         {
            try
            {
               synchronized(this)
               {
                  lineBuffer += (new String(out, startIdx, idx - startIdx, "cp1252")) + "\n";
                  startIdx = idx;
               }
            }
            catch (UnsupportedEncodingException e)
            {
               log.add(Level.SEVERE,"UnsupportedEncodingException: ", e);
            }
            processString();
            continue;
         }

         if (inEscape)
         {
            // check for escape commands that we don't parse
            if (b != ctr)
            {
               inEscape = false;
               continue;
            }

            // otherwise  set the control bit
            inEscape = false;
            inControl = true;
            sequence = 0;
            continue;
         }
         else if (inControl)
         {
            // on a separator, parse the existing sequence command
            if (b == sep1 || b == sep2 || b == end)
            {
               setStyle(sequence, lbIdx, ranges);
               sequence = 0;
               if (b == end)
               {
                  inControl = false;
               }
               continue;
            }
            
            // if we can't figure out this sequence, continue to ignore it.
            if (sequence < 0)
            {
               continue;
            }
            
            // get the next integer bit
            int n = convert(b);
            // ignore non integers because we're not parsing those currently
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
            // check for escapes
            if (b == esc)
            {
               inEscape = true;
               continue;
            }
            else if (b == bell)
            {
               //TODO this won't work in a lot of systems ... might try flashing the screen instead.
               java.awt.Toolkit.getDefaultToolkit().beep();
               continue;
            }
            
            // or log it to the out bytes and set the index for the full string 
            out[idx++] = b;
            lbIdx++;
         }
      }

      // add to the lineBuffer when done parsing these bits
      try
      {
         synchronized(this)
         {
            lineBuffer += (new String(out, startIdx, idx - startIdx, "cp1252"));
         }
      }
      catch (UnsupportedEncodingException e1)
      {
         log.add(Level.SEVERE,"UnsupportedEncodingException: ", e1);
      }

   }

   /**
    * Process the current string
    */
   public void flush()
   {
      processString();
   }

   /**
    * Take the linebuffer and print it, with styles, to the output text
    */
   private void processString()
   {
      synchronized(this)
      {
         // condense the ranges
         List<StyleRange> condensedRanges = new ArrayList<>();
         for (int ii = 0; ii < ranges.size(); ii++)
         {
            if (ii == 0)
            {
               condensedRanges.add(ranges.get(0));
               continue;
            }
            
            StyleRange last = condensedRanges.get(condensedRanges.size() - 1);
            StyleRange curr = ranges.get(ii);
            
            if (last.start == curr.start)
            {
               if (curr.foreground != null)
                  last.foreground = curr.foreground;
               if (curr.background != null)
                  last.background = curr.background;
               if (curr.underline != last.underline)
                  last.underline = curr.underline;
               if (curr.fontStyle != last.fontStyle)
                  last.fontStyle = curr.fontStyle;
               continue;
            }
            
            if (last.start + last.length == curr.start + curr.length)
            {
               // carry through font styles until the break at length
               if (last.underline)
                  curr.underline = true;
               if (last.fontStyle != SWT.NORMAL && curr.fontStyle == SWT.NORMAL)
                  curr.fontStyle = last.fontStyle;
               
               // carry through colors on nulls
               if (curr.foreground == null)
                  curr.foreground = last.foreground;
               if (curr.background == null)
                  curr.background = last.background;
            }
            condensedRanges.add(curr);
         }
         // send the string to the listeners
         lineBuffer = MudFrame.getInstance().getListener().processOutput(lineBuffer, condensedRanges);
         
         // formatters could choose not to print this string by sending null back
         if (lineBuffer == null)
         {
            lineBuffer = "";
            lbIdx = 0;
            ranges.clear();
            return;
         }
         
         // process style ranges if they exist and can be applied
         if (lineBuffer.length() > 0 && condensedRanges.size() != 0)
         {
            // change the length of each range that hasn't been terminated
            for (StyleRange r : ranges)
            {
               if (r.length == -1)
               {
                  r.length = lineBuffer.length() - r.start - 1;
               }
            }
         }
   
         // and write
         MudFrame.getInstance().writeToTextBox(lineBuffer, condensedRanges);
   
         // reset the line buffer and the ranges. We have the option to continue with
         // the "continuedSequnces" here, but I'm currently opting to end all sequences
         // with line termination (there are times where an escape sequence is missed, 
         // and then the entire screen ends up staying a color we don't want)
         lineBuffer = "";
         lbIdx = 0;
         ranges.clear();
      }
   }

   /**
    * Add a StyleRange or truncate StyleRanges depending on the control sequence
    * 
    * @param sequence
    * @param ranges
    * @param idx
    */
   private void setStyle(int sequence, int idx, List<StyleRange> ranges)
   {
      synchronized(this)
      {
         // ignore bad sequences
         if (sequence < 0)
         {
            return;
         }
         else if (sequence == 0)
         {
            // set the length for all non-terminated sequences and return
            for (StyleRange range : ranges)
            {
               if (range.length == -1)
               {
                  range.length = idx - range.start;
               }
            }
            return;
         }
   
         // Create the new style range for this sequence
         StyleRange range = new StyleRange();
         range.start = idx; // relative to the string, not the bytes or the screen
         range.length = -1;
   
         switch (sequence)
         {
            case 1: // bold
               range.fontStyle = SWT.BOLD;
               break;
            case 4: // underline
               range.underline = true;
               break;
            case 30: // foreground
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
               range.foreground = MudFrame.colors[sequence - 30];
               break;
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
               range.background = MudFrame.colors[sequence - 40];
               break;
            default:
               return; // do nothing
         }
         
         ranges.add(range);
      }
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
