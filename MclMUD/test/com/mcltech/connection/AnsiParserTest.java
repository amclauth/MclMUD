package com.mcltech.connection;

import static org.junit.Assert.fail;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.mcltech.base.MudLogger;

public class AnsiParserTest
{

   @SuppressWarnings("static-method")
   @Before
   public void init()
   {
      MudLogger.injectEmptyLogger();
      
      MudFrame frame = Mockito.mock(MudFrame.class);
      // return linebuffer when asking for processOutput
      Mockito.when(frame.getListener().processOutput(Mockito.anyString(),Mockito.anyObject())).thenAnswer(new Answer<String>() {
         @Override
         public String answer(InvocationOnMock invocation) throws Throwable {
           Object[] args = invocation.getArguments();
           return (String) args[0];
         }
       });
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

   @SuppressWarnings("static-method")
   @Test
   public void test()
   {
      AnsiParser parser = AnsiParser.getInstance();
      
      fail("Not yet implemented");
   }

}
