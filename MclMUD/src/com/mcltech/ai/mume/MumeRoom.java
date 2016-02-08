package com.mcltech.ai.mume;

import java.util.regex.Pattern;

public class MumeRoom
{
   private String name;
   private char lastDirection;
   private boolean northExit;
   private boolean southExit;
   private boolean eastExit;
   private boolean westExit;
   private boolean upExit;
   private boolean downExit;
   private Pattern returnDirPattern;

   public MumeRoom()
   {
      
   }
   
   public void addExits(String exitString)
   {
      System.out.println("Exit String: " + exitString);
   }

   public String getName()
   {
      return name;
   }

   public void setName(String name)
   {
      this.name = name;
   }

   public char getLastDirection()
   {
      return lastDirection;
   }

   public void setLastDirection(char c)
   {
      switch (c)
      {
         case 'e':
            returnDirPattern = Pattern.compile(" .?west.?[,.]");
            break;
         case 'w':
            returnDirPattern = Pattern.compile(" .?east.?[,.]");
            break;
         case 'n':
            returnDirPattern = Pattern.compile(" .?south.?[,.]");
            break;
         case 's':
            returnDirPattern = Pattern.compile(" .?north.?[,.]");
            break;
         case 'u':
            returnDirPattern = Pattern.compile(" .?down.?[,.]");
            break;
         case 'd':
            returnDirPattern = Pattern.compile(" .?up.?[,.]");
            break;
         default:
            returnDirPattern = null;
            break;
      }
      if (returnDirPattern != null)
         lastDirection = c;
   }

   public boolean isNorthExit()
   {
      return northExit;
   }

   public void setNorthExit(boolean northExit)
   {
      this.northExit = northExit;
   }

   public boolean isSouthExit()
   {
      return southExit;
   }

   public void setSouthExit(boolean southExit)
   {
      this.southExit = southExit;
   }

   public boolean isEastExit()
   {
      return eastExit;
   }

   public void setEastExit(boolean eastExit)
   {
      this.eastExit = eastExit;
   }

   public boolean isWestExit()
   {
      return westExit;
   }

   public void setWestExit(boolean westExit)
   {
      this.westExit = westExit;
   }

   public boolean isUpExit()
   {
      return upExit;
   }

   public void setUpExit(boolean upExit)
   {
      this.upExit = upExit;
   }

   public boolean isDownExit()
   {
      return downExit;
   }

   public void setDownExit(boolean downExit)
   {
      this.downExit = downExit;
   }
   
   public Pattern getReturnDirPattern()
   {
      return returnDirPattern;
   }
}
