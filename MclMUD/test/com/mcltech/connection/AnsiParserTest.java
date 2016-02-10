package com.mcltech.connection;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.widgets.Display;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;

import com.mcltech.ai.AIListener;
import com.mcltech.base.MudLogger;

public class AnsiParserTest
{

   MudFrame frame;

   @Before
   public void init()
   {
      // return linebuffer when asking for processOutput
      AIListener mockListener = Mockito.mock(AIListener.class);
      Mockito.when(mockListener.processOutput(Mockito.anyString(), Mockito.anyObject()))
            .then(AdditionalAnswers.returnsFirstArg());

      MudLogger.injectEmptyLogger();

      frame = Mockito.mock(MudFrame.class);
      // return linebuffer when asking for processOutput
      Mockito.when(frame.getListener()).thenReturn(mockListener);
      Mockito.when(frame.getColor(0)).thenReturn(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
      Mockito.when(frame.getColor(1)).thenReturn(Display.getDefault().getSystemColor(SWT.COLOR_RED));
      Mockito.when(frame.getColor(2)).thenReturn(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));
      Mockito.when(frame.getColor(3)).thenReturn(Display.getDefault().getSystemColor(SWT.COLOR_YELLOW));
      Mockito.when(frame.getColor(4)).thenReturn(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
      Mockito.when(frame.getColor(5)).thenReturn(Display.getDefault().getSystemColor(SWT.COLOR_MAGENTA));
      Mockito.when(frame.getColor(6)).thenReturn(Display.getDefault().getSystemColor(SWT.COLOR_CYAN));
      Mockito.when(frame.getColor(7)).thenReturn(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
      Mockito.when(frame.getColor(8)).thenReturn(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));

      AnsiParser.injectMudFrame(frame);
   }

   @Test
   public void testSetStyle()
   {
      // AnsiParser parser = AnsiParser.getInstance();
      List<StyleRange> ranges = new ArrayList<>();

      int size = 0;
      // add a style at various indexes for color, bold, underline, background color

      AnsiParser.setStyle(1, size, ranges);
      Assert.assertEquals(size + 1, ranges.size());
      Assert.assertEquals(0, ranges.get(size).start);
      Assert.assertEquals(-1, ranges.get(size).length);
      Assert.assertEquals(null, ranges.get(size).foreground);
      Assert.assertEquals(null, ranges.get(size).background);
      Assert.assertFalse(ranges.get(size).underline);
      Assert.assertEquals(SWT.BOLD, ranges.get(size).fontStyle);
      size++;

      AnsiParser.setStyle(4, size, ranges);
      Assert.assertEquals(size + 1, ranges.size());
      Assert.assertEquals(size, ranges.get(size).start);
      Assert.assertEquals(-1, ranges.get(size).length);
      Assert.assertEquals(null, ranges.get(size).foreground);
      Assert.assertEquals(null, ranges.get(size).background);
      Assert.assertTrue(ranges.get(size).underline);
      Assert.assertEquals(SWT.NORMAL, ranges.get(size).fontStyle);
      size++;

      for (int ii = 0; ii <= 7; ii++)
      {
         AnsiParser.setStyle(30 + ii, size, ranges);
         Assert.assertEquals(size + 1, ranges.size());
         Assert.assertEquals(size, ranges.get(size).start);
         Assert.assertEquals(-1, ranges.get(size).length);
         Assert.assertEquals(frame.getColor(ii), ranges.get(size).foreground);
         Assert.assertEquals(null, ranges.get(size).background);
         Assert.assertFalse(ranges.get(size).underline);
         Assert.assertEquals(SWT.NORMAL, ranges.get(size).fontStyle);
         size++;
      }

      for (int ii = 0; ii <= 7; ii++)
      {
         AnsiParser.setStyle(40 + ii, size, ranges);
         Assert.assertEquals(size + 1, ranges.size());
         Assert.assertEquals(size, ranges.get(size).start);
         Assert.assertEquals(-1, ranges.get(size).length);
         Assert.assertEquals(null, ranges.get(size).foreground);
         Assert.assertEquals(frame.getColor(ii), ranges.get(size).background);
         Assert.assertFalse(ranges.get(size).underline);
         Assert.assertEquals(SWT.NORMAL, ranges.get(size).fontStyle);
         size++;
      }

      AnsiParser.setStyle(0, size, ranges);
      for (StyleRange r : ranges)
      {
         Assert.assertEquals(size, r.length + r.start);
      }

      // add multiples of styles to the same index, verify they're overwritten or included

      ranges = new ArrayList<>();

      AnsiParser.setStyle(1, 0, ranges);
      Assert.assertEquals(1, ranges.size());
      Assert.assertEquals(0, ranges.get(0).start);
      Assert.assertEquals(-1, ranges.get(0).length);
      Assert.assertEquals(null, ranges.get(0).foreground);
      Assert.assertEquals(null, ranges.get(0).background);
      Assert.assertFalse(ranges.get(0).underline);
      Assert.assertEquals(SWT.BOLD, ranges.get(0).fontStyle);

      AnsiParser.setStyle(4, 0, ranges);
      Assert.assertEquals(1, ranges.size());
      Assert.assertEquals(0, ranges.get(0).start);
      Assert.assertEquals(-1, ranges.get(0).length);
      Assert.assertEquals(null, ranges.get(0).foreground);
      Assert.assertEquals(null, ranges.get(0).background);
      Assert.assertTrue(ranges.get(0).underline);
      Assert.assertEquals(SWT.BOLD, ranges.get(0).fontStyle);

      AnsiParser.setStyle(31, 0, ranges);
      Assert.assertEquals(1, ranges.size());
      Assert.assertEquals(0, ranges.get(0).start);
      Assert.assertEquals(-1, ranges.get(0).length);
      Assert.assertEquals(frame.getColor(1), ranges.get(0).foreground);
      Assert.assertEquals(null, ranges.get(0).background);
      Assert.assertTrue(ranges.get(0).underline);
      Assert.assertEquals(SWT.BOLD, ranges.get(0).fontStyle);

      AnsiParser.setStyle(41, 0, ranges);
      Assert.assertEquals(1, ranges.size());
      Assert.assertEquals(0, ranges.get(0).start);
      Assert.assertEquals(-1, ranges.get(0).length);
      Assert.assertEquals(frame.getColor(1), ranges.get(0).foreground);
      Assert.assertEquals(frame.getColor(1), ranges.get(0).background);
      Assert.assertTrue(ranges.get(0).underline);
      Assert.assertEquals(SWT.BOLD, ranges.get(0).fontStyle);
   }

   @SuppressWarnings("static-method")
   @Test
   public void testTerminateStyleRanges()
   {
      List<StyleRange> ranges = new ArrayList<>();

      // verify null can be output (pass in null for string in this case rather than
      // rewriting the Mockito.when)
      Assert.assertNull(AnsiParser.terminateStyleRanges(null, ranges));

      // verify no style ranges with positive lengths are changed
      for (int ii = 0; ii <= 5; ii++)
      {
         StyleRange r = new StyleRange();
         r.start = ii;
         r.length = ii;
         ranges.add(r);
      }
      Assert.assertEquals("Hi there. String longer than 10",
            AnsiParser.terminateStyleRanges("Hi there. String longer than 10", ranges));
      for (int ii = 0; ii <= 5; ii++)
      {
         Assert.assertEquals(ii, ranges.get(ii).start);
         Assert.assertEquals(ii, ranges.get(ii).length);
      }

      // verify all length==-1 style ranges are terminated at the end of the line and all long
      // ranges are terminated at the end of the line
      for (int ii = 6; ii < 9; ii++)
      {
         StyleRange r = new StyleRange();
         r.start = ii;
         r.length = -1;
         ranges.add(r);
      }
      Assert.assertEquals("Hi there.",
            AnsiParser.terminateStyleRanges("Hi there.", ranges));
      for (int ii = 0; ii < 5; ii++)
      {
         Assert.assertEquals(ii, ranges.get(ii).start);
         Assert.assertEquals(ii, ranges.get(ii).length);
      }
      for (int ii = 5; ii < 9; ii++)
      {
         int len = new String("Hi there.").length() - ii - 1;
         Assert.assertEquals(ii, ranges.get(ii).start);
         Assert.assertEquals(len, ranges.get(ii).length);
      }
   }

   @SuppressWarnings("static-method")
   @Test
   public void testFlush()
   {
      AnsiParser parser = AnsiParser.getInstance();
      parser.flush();
      List<StyleRange> ranges = new ArrayList<StyleRange>();

      // verify that flush resets line and ranges and lbIdx
      parser.lineBuffer = "Hi there.";
      parser.lbIdx = 9;
      parser.ranges = new ArrayList<>();
      ranges.add(new StyleRange());
      
      parser.flush();
      Assert.assertEquals("",parser.lineBuffer);
      Assert.assertEquals(0,parser.lbIdx);
      Assert.assertEquals(0, parser.ranges.size());
      
   }

   @SuppressWarnings("static-method")
   @Test
   public void testParseBytes()
   {
      AnsiParser parser = AnsiParser.getInstance();
      parser.flush();
      
      byte[] bytes = new byte[20];
      bytes[0] = AnsiParser.zero;
      bytes[1] = AnsiParser.one;
      bytes[2] = AnsiParser.two;
      bytes[3] = AnsiParser.three;
      bytes[4] = AnsiParser.four;
      bytes[5] = AnsiParser.five;
      bytes[6] = AnsiParser.six;
      bytes[7] = AnsiParser.seven;
      bytes[8] = AnsiParser.eight;
      bytes[9] = AnsiParser.nine;
      
      // pass in a string, verify linebuffer has that string
      parser.parseBytes(bytes, 10);
      Assert.assertEquals("0123456789",parser.lineBuffer);
      Assert.assertEquals(0, parser.sequence);
      Assert.assertEquals(10, parser.lbIdx);
      Assert.assertEquals(0, parser.ranges.size());
      Assert.assertFalse(parser.inEscape);
      Assert.assertFalse(parser.inControl);
      Assert.assertFalse(parser.isCarriage);
      parser.flush();
      
      // pass in a string with a terminal in the middle, verify linebuffer has only 
      // the next string
      bytes[5] = AnsiParser.carriage;
      parser.parseBytes(bytes, 10);
      Assert.assertEquals("6789",parser.lineBuffer);
      Assert.assertEquals(0, parser.sequence);
      Assert.assertEquals(4, parser.lbIdx);
      Assert.assertEquals(0, parser.ranges.size());
      Assert.assertFalse(parser.inEscape);
      Assert.assertFalse(parser.inControl);
      Assert.assertFalse(parser.isCarriage);
      parser.flush();
      
      bytes[5] = AnsiParser.newline;
      parser.parseBytes(bytes, 10);
      Assert.assertEquals("6789",parser.lineBuffer);
      Assert.assertEquals(0, parser.sequence);
      Assert.assertEquals(4, parser.lbIdx);
      Assert.assertEquals(0, parser.ranges.size());
      Assert.assertFalse(parser.inEscape);
      Assert.assertFalse(parser.inControl);
      Assert.assertFalse(parser.isCarriage);
      parser.flush();
      
      bytes[5] = AnsiParser.formfeed;
      parser.parseBytes(bytes, 10);
      Assert.assertEquals("6789",parser.lineBuffer);
      Assert.assertEquals(0, parser.sequence);
      Assert.assertEquals(4, parser.lbIdx);
      Assert.assertEquals(0, parser.ranges.size());
      Assert.assertFalse(parser.inEscape);
      Assert.assertFalse(parser.inControl);
      Assert.assertFalse(parser.isCarriage);
      parser.flush();

      // escape sequence partial enters into isEscape with no change to lineBuffer
      bytes[5] = AnsiParser.five;
      bytes[10] = AnsiParser.esc;
      parser.parseBytes(bytes, 11);
      Assert.assertEquals("0123456789",parser.lineBuffer);
      Assert.assertEquals(0, parser.sequence);
      Assert.assertEquals(10, parser.lbIdx);
      Assert.assertEquals(0, parser.ranges.size());
      Assert.assertTrue(parser.inEscape);
      Assert.assertFalse(parser.inControl);
      Assert.assertFalse(parser.isCarriage);
      parser.flush();

      // add a sequence, verify sequence
      bytes[11] = AnsiParser.one;
      parser.parseBytes(bytes, 12);
      Assert.assertEquals("0123456789",parser.lineBuffer);
      Assert.assertEquals(0, parser.sequence);
      Assert.assertEquals(10, parser.lbIdx);
      Assert.assertEquals(0, parser.ranges.size());
      Assert.assertFalse(parser.inEscape);
      Assert.assertFalse(parser.inControl);
      Assert.assertFalse(parser.isCarriage);
      parser.flush();
      
      bytes[11] = AnsiParser.ctr;
      parser.parseBytes(bytes, 12);
      Assert.assertEquals("0123456789",parser.lineBuffer);
      Assert.assertEquals(0, parser.sequence);
      Assert.assertEquals(10, parser.lbIdx);
      Assert.assertEquals(0, parser.ranges.size());
      Assert.assertFalse(parser.inEscape);
      Assert.assertTrue(parser.inControl);
      Assert.assertFalse(parser.isCarriage);
      parser.flush();
      
      bytes[12] = AnsiParser.one;
      parser.parseBytes(bytes, 13);
      Assert.assertEquals("0123456789",parser.lineBuffer);
      Assert.assertEquals(1, parser.sequence);
      Assert.assertEquals(10, parser.lbIdx);
      Assert.assertEquals(0, parser.ranges.size());
      Assert.assertFalse(parser.inEscape);
      Assert.assertTrue(parser.inControl);
      Assert.assertFalse(parser.isCarriage);
      parser.flush();
      
      bytes[13] = AnsiParser.end;
      parser.parseBytes(bytes, 14);
      Assert.assertEquals("0123456789",parser.lineBuffer);
      Assert.assertEquals(0, parser.sequence);
      Assert.assertEquals(10, parser.lbIdx);
      Assert.assertEquals(1, parser.ranges.size());
      Assert.assertEquals(SWT.BOLD, parser.ranges.get(0).fontStyle);
      Assert.assertEquals(10, parser.ranges.get(0).start);
      Assert.assertEquals(-1, parser.ranges.get(0).length);
      Assert.assertFalse(parser.inEscape);
      Assert.assertFalse(parser.inControl);
      Assert.assertFalse(parser.isCarriage);
      parser.flush();
      
      bytes[13] = AnsiParser.sep1;
      bytes[14] = AnsiParser.four;
      bytes[15] = AnsiParser.sep2;
      bytes[16] = AnsiParser.three;
      bytes[17] = AnsiParser.one;
      bytes[18] = AnsiParser.end;
      parser.parseBytes(bytes, 19);
      Assert.assertEquals("0123456789",parser.lineBuffer);
      Assert.assertEquals(0, parser.sequence);
      Assert.assertEquals(10, parser.lbIdx);
      Assert.assertEquals(1, parser.ranges.size());
      Assert.assertEquals(SWT.BOLD, parser.ranges.get(0).fontStyle);
      Assert.assertEquals(10, parser.ranges.get(0).start);
      Assert.assertEquals(-1, parser.ranges.get(0).length);
      Assert.assertTrue(parser.ranges.get(0).underline);
      Assert.assertEquals(frame.getColor(1), parser.ranges.get(0).foreground);
      Assert.assertFalse(parser.inEscape);
      Assert.assertFalse(parser.inControl);
      Assert.assertFalse(parser.isCarriage);
      parser.flush();
   }

}
