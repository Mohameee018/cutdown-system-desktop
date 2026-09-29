package Clothes_system;

import java.awt.Color;

public final class SettingsManager {
    private static String storeName="VYRA Clothing Store";
    private static String storeType="Clothing Store";
    private static String storePhone="01000000000";
    private static String storeAddress="Cairo, Egypt";
    private static String logoPath="";
    private static String invoiceFooter="Thank you for shopping with us.";
    private static String returnPolicy="Returns are accepted according to store policy.";
    private static String printer="";
    private static String paperSize="A4";
    private static boolean autoPrint=false;
    private static Color primaryColor=new Color(25,115,105);
    private static Color sidebarColor=new Color(10,45,45);
    private SettingsManager(){}
    public static String getStoreName(){return storeName;}
    public static void setStoreName(String v){storeName=v==null?"":v.trim();}
    public static String getStoreType(){return storeType;}
    public static void setStoreType(String v){storeType=v==null?"":v.trim();}
    public static String getStorePhone(){return storePhone;}
    public static void setStorePhone(String v){storePhone=v==null?"":v.trim();}
    public static String getStoreAddress(){return storeAddress;}
    public static void setStoreAddress(String v){storeAddress=v==null?"":v.trim();}
    public static String getLogoPath(){return logoPath;}
    public static void setLogoPath(String v){logoPath=v==null?"":v;}
    public static String getInvoiceFooter(){return invoiceFooter;}
    public static void setInvoiceFooter(String v){invoiceFooter=v==null?"":v;}
    public static String getReturnPolicy(){return returnPolicy;}
    public static void setReturnPolicy(String v){returnPolicy=v==null?"":v;}
    public static String getPrinter(){return printer;}
    public static void setPrinter(String v){printer=v==null?"":v;}
    public static String getPaperSize(){return paperSize;}
    public static void setPaperSize(String v){paperSize=v==null?"A4":v;}
    public static boolean isAutoPrint(){return autoPrint;}
    public static void setAutoPrint(boolean v){autoPrint=v;}
    public static Color getPrimaryColor(){return primaryColor;}
    public static void setPrimaryColor(Color v){if(v!=null)primaryColor=v;}
    public static Color getSidebarColor(){return sidebarColor;}
    public static void setSidebarColor(Color v){if(v!=null)sidebarColor=v;}
}
