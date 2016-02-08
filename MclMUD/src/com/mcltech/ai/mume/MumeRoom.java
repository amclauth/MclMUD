package com.mcltech.ai.mume;

import java.util.regex.Pattern;

public class MumeRoom
{
   private String name;
   private char lastDirection;
   private Boolean northExit;
   private Boolean southExit;
   private Boolean eastExit;
   private Boolean westExit;
   private Boolean upExit;
   private Boolean downExit;
   private Pattern returnDirPattern;

   public MumeRoom()
   {
      
   }
   
   public void addExits(String exitString)
   {
//      System.out.println("Exit String: " + exitString);
   }
   
   public MumeRoom copy()
   {
      // don't copy returnDirPattern or lastDirection
      MumeRoom newRoom = new MumeRoom();
      newRoom.name = name;
      newRoom.northExit = northExit;
      newRoom.southExit = southExit;
      newRoom.eastExit = eastExit;
      newRoom.westExit = westExit;
      newRoom.upExit = upExit;
      newRoom.downExit = downExit;
      return newRoom;
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

   public Boolean isNorthExit()
   {
      return northExit;
   }

   public void setNorthExit(Boolean northExit)
   {
      this.northExit = northExit;
   }

   public Boolean isSouthExit()
   {
      return southExit;
   }

   public void setSouthExit(Boolean southExit)
   {
      this.southExit = southExit;
   }

   public Boolean isEastExit()
   {
      return eastExit;
   }

   public void setEastExit(Boolean eastExit)
   {
      this.eastExit = eastExit;
   }

   public Boolean isWestExit()
   {
      return westExit;
   }

   public void setWestExit(Boolean westExit)
   {
      this.westExit = westExit;
   }

   public Boolean isUpExit()
   {
      return upExit;
   }

   public void setUpExit(Boolean upExit)
   {
      this.upExit = upExit;
   }

   public Boolean isDownExit()
   {
      return downExit;
   }

   public void setDownExit(Boolean downExit)
   {
      this.downExit = downExit;
   }
   
   public Pattern getReturnDirPattern()
   {
      return returnDirPattern;
   }
}
