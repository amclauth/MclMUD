package com.mcltech.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class TestAliasExpansion
{

   // aliases are always the first "word" in the command string
   @SuppressWarnings("static-method")
   @Test
   public void testSimple()
   {
      AIListener listener = new AIListener();
      listener.aliases = new HashMap<>();
      listener.aliases.put("alias1",new String[]{"response1"});
      listener.aliases.put("alias2",new String[]{"response2 %% response2"});
      listener.aliases.put("alias3",new String[]{"alias2 middle"});
      listener.aliases.put("alias4",new String[]{"alias2 %%"});
      
      List<String> out = listener.expandAlias("command");
      Assert.assertEquals(1, out.size());
      Assert.assertEquals("command", out.get(0));
      
      out = listener.expandAlias("alias1");
      Assert.assertEquals(1, out.size());
      Assert.assertEquals("response1", out.get(0));
      
      out = listener.expandAlias("alias2");
      Assert.assertEquals(1, out.size());
      Assert.assertEquals("response2 response2", out.get(0));
      
      out = listener.expandAlias("alias2 middle");
      Assert.assertEquals(1, out.size());
      Assert.assertEquals("response2 middle response2", out.get(0));
      
      out = listener.expandAlias("alias3");
      Assert.assertEquals(1, out.size());
      Assert.assertEquals("response2 middle response2", out.get(0));
      
      out = listener.expandAlias("alias3 end");
      Assert.assertEquals(1, out.size());
      Assert.assertEquals("response2 middle response2 end", out.get(0));
      
//      out = listener.expandAlias("alias4");
//      Assert.assertEquals(1, out.size());
//      Assert.assertEquals("response2 response2", out.get(0));
      
      out = listener.expandAlias("alias4 middle");
      Assert.assertEquals(1, out.size());
      Assert.assertEquals("response2 middle response2", out.get(0));
      
      out = listener.expandAlias("alias4 middle end");
      Assert.assertEquals(1, out.size());
      Assert.assertEquals("response2 middle response2 end", out.get(0));
      
      out = listener.expandAlias("alias1 end");
      Assert.assertEquals(1, out.size());
      Assert.assertEquals("response1 end", out.get(0));
      
      out = listener.expandAlias("alias2   middle   end");
      Assert.assertEquals(1, out.size());
      Assert.assertEquals("response2 middle response2 end", out.get(0));
      
      out = listener.expandAlias("nonAlias   middle   end");
      Assert.assertEquals(1, out.size());
      Assert.assertEquals("nonAlias   middle   end", out.get(0));
   }
   
   @Test
   public void testChainExpansion()
   {
      AIListener listener = new AIListener();
      listener.aliases = new HashMap<>();
      listener.aliases.put("alias1",new String[]{"response1"});
      listener.aliases.put("alias2",new String[]{"response2 %% response2"});
      
      List<String> out = addAllFromSplit(listener, "command1; command2   ;command3");
      
      Assert.assertEquals(3, out.size());
      Assert.assertEquals("command1", out.get(0));
      Assert.assertEquals("command2", out.get(1));
      Assert.assertEquals("command3", out.get(2));
      
      out = addAllFromSplit(listener, "alias1; alias1   ;alias1");
      Assert.assertEquals(3, out.size());
      Assert.assertEquals("response1", out.get(0));
      Assert.assertEquals("response1", out.get(1));
      Assert.assertEquals("response1", out.get(2));
      
      // alias5:command1;command2 %%;command3
      // alias5 middle?
   }
   
   private List<String> addAllFromSplit(AIListener listener, String input)
   {
      List<String> commands = new ArrayList<>();
      // handle individual commands
      for (String line : input.split(";"))
      {
         line = line.trim();
         if (line.isEmpty())
         {
            commands.add("");
            continue;
         }
         
         commands.addAll(listener.expandAlias(line));
      }
      return commands;
   }
}
