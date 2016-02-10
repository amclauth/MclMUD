package com.mcltech.connection;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;

import com.mcltech.base.MudLogger;

/**
 * Parse the ansi input and escape sequences into string and style ranges. This is not a thread safe class,
 * but the input is serialized by the controller's read, so should be quite safe for this application.
 * 
 * @author andymac
 * 
 */
public class AnsiParser
{
   private static final MudLogger log = MudLogger.getInstance();

   static final byte esc = (byte) 0x1B;
   static final byte ctr = (byte) 0x5B;
   static final byte end = (byte) 0x6D;
   static final byte sep1 = (byte) 0x3A;
   static final byte sep2 = (byte) 0x3B;
   static final byte newline = (byte) 0x0A;
   static final byte formfeed = (byte) 0x0C;
   static final byte carriage = (byte) 0x0D;
   static final byte bell = (byte) 0x07;

   static final byte zero = (byte) 0x30;
   static final byte one = (byte) 0x31;
   static final byte two = (byte) 0x32;
   static final byte three = (byte) 0x33;
   static final byte four = (byte) 0x34;
   static final byte five = (byte) 0x35;
   static final byte six = (byte) 0x36;
   static final byte seven = (byte) 0x37;
   static final byte eight = (byte) 0x38;
   static final byte nine = (byte) 0x39;

   boolean inEscape;
   boolean inControl;
   boolean isCarriage;
   int sequence;
   int lbIdx;

   String lineBuffer;
   List<StyleRange> ranges;

   private static MudFrame mudFrame;

   // singleton class holder pattern
   private static final class holder
   {
      static final AnsiParser INSTANCE = new AnsiParser();
   }

   public static AnsiParser getInstance()
   {
      return holder.INSTANCE;
   }

   AnsiParser()
   {
      if (mudFrame == null)
         mudFrame = MudFrame.getInstance();
      inEscape = false;
      inControl = false;
      isCarriage = false;
      sequence = 0;
      ranges = new ArrayList<>();
      lineBuffer = "";
      lbIdx = 0;
   }

   public static void injectMudFrame(MudFrame frame)
   {
      mudFrame = frame;
   }

   /**
    * Given a byte string (presumed ansi), parse it for color and style sequences and print it out to the
    * styled text widget
    * 
    * @param text
    * @param bytes
    * @param ret_read
    */
   public void parseBytes(byte[] bytes, int len)
   {
      synchronized (this) // synchronize the main operations that will change the lineBuffer
                          // and style ranges internally / externally
      {
         byte[] out = new byte[bytes.length];
         int idx = 0;
         int startIdx = 0;

         for (int ii = 0; ii < bytes.length && ii < len; ii++)
         {
            byte b = bytes[ii];
            // ignore the double \r\n for conversion (treat as one), but don't skip \n\n or others
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
                  lineBuffer += (new String(out, startIdx, idx - startIdx, "cp1252")) + "\n";
                  startIdx = idx;
               }
               catch (UnsupportedEncodingException e)
               {
                  log.add(Level.SEVERE, "UnsupportedEncodingException: ", e);
               }
               flushLineBuffer();
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

               // otherwise set the control bit
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
                     log.add(Level.INFO,
                           "Ignoring sequence in this line: " + new String(bytes, 0, len, "cp1252"));
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
                  // TODO this won't work in a lot of systems ... might try flashing the screen instead.
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
            lineBuffer += (new String(out, startIdx, idx - startIdx, "cp1252"));
         }
         catch (UnsupportedEncodingException e1)
         {
            log.add(Level.SEVERE, "UnsupportedEncodingException: ", e1);
         }
      }
   }

   /**
    * Process the current string
    */
   public void flush()
   {
      synchronized (this) // synchronize the main operations that will change the lineBuffer
                          // and style ranges internally / externally
      {
         flushLineBuffer();
      }
   }

   private void flushLineBuffer()
   {

      String out = terminateStyleRanges(lineBuffer, ranges);
      if (out != null)
         mudFrame.writeToTextBox(out, ranges);
      // reset the line buffer and the ranges. We have the option to continue with
      // the "continuedSequnces" here, but I'm currently opting to end all sequences
      // with line termination (there are times where an escape sequence is missed,
      // and then the entire screen ends up staying a color we don't want)
      lineBuffer = "";
      lbIdx = 0;
      ranges.clear();
   }

   /**
    * Mostly for unit testing
    */
   public void reset()
   {
      lineBuffer = "";
      lbIdx = 0;
      ranges.clear();
      inEscape = false;
      sequence = 0;
      inControl = false;
      isCarriage = false;
   }

   /**
    * Take the linebuffer and print it, with styles, to the output text
    */
   static String terminateStyleRanges(String line, List<StyleRange> currentRanges)
   {
      // send the string to the listeners
      String out = mudFrame.getListener().processOutput(line, currentRanges);

      // formatters could choose not to print this string by sending null back
      // process style ranges if they exist and can be applied
      if (out != null && out.length() > 0 && currentRanges.size() != 0)
      {
         // change the length of each range that hasn't been terminated
         for (StyleRange r : currentRanges)
         {
            if (r.length == -1 || r.length + r.start > out.length())
            {
               r.length = out.length() - r.start - 1;
            }
         }
      }

      return out;
   }

   /**
    * Add a StyleRange or truncate StyleRanges depending on the control sequence
    * 
    * @param sequence
    * @param ranges
    * @param idx
    */
   static void setStyle(int sequence, int idx, List<StyleRange> ranges)
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
      StyleRange last = ranges.size() > 0 ? ranges.get(ranges.size() - 1) : null;
      StyleRange range = last != null && last.start == idx ? last : new StyleRange();
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
            range.foreground = mudFrame.getColor(sequence - 30);
            break;
         case 40:
         case 41:
         case 42:
         case 43:
         case 44:
         case 45:
         case 46:
         case 47:
            range.background = mudFrame.getColor(sequence - 40);
            break;
         default:
            return; // do nothing
      }

      if (last == null || !last.equals(range))
         ranges.add(range);
   }

   /**
    * Convert ansi bytes to numbers (since Java has a weird way of handling bytes)
    * 
    * @param b
    * @return
    */
   private static int convert(byte b)
   {
      switch (b)
      {
         case zero:
            return 0;
         case one:
            return 1;
         case two:
            return 2;
         case three:
            return 3;
         case four:
            return 4;
         case five:
            return 5;
         case six:
            return 6;
         case seven:
            return 7;
         case eight:
            return 8;
         case nine:
            return 9;
         default:
            return -1;
      }
   }
}
