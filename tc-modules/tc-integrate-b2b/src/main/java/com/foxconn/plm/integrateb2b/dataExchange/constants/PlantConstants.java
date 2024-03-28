package com.foxconn.plm.integrateb2b.dataExchange.constants;

public class PlantConstants {

    public static String MNT_L6="CHMB";
    public static String MNT_L10="CHMC";
    public static String MNT_L5="CHPA";


     public static boolean isContain(String plants,String plant)  throws Exception {

               if (plants == null || "".equalsIgnoreCase(plants.trim())) {
                   return false;
               }
               if (plant == null || "".equalsIgnoreCase(plant.trim())) {
                   return false;
               }
               String[] m = plants.split(",");
               for (String str : m) {
                   if (str.trim().equalsIgnoreCase(plant.trim())) {
                       return true;
                   }
               }

            return false;

     }




}
