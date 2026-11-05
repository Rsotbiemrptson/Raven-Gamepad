package com.odin2.ds4bridgehelper;
/** Root-only, read-only access to this app's own owner-user settings on firmware without content CLI. */
public final class BridgeContentReader {
 public static void main(String[] args) {
  try {
   if(android.os.Process.myUid()!=0||args.length!=1)throw new SecurityException("Root-only bridge read");
   java.io.File base=new java.io.File("/data/user_de/0/com.odin2.ds4bridgehelper");
   String kind=args[0];
   if(kind.equals("forwarding")) {
    java.io.File prefs=new java.io.File(base,"shared_prefs/controller_mode.xml");boolean all=false,transition=false;
    if(prefs.exists())try(java.io.InputStream in=new java.io.FileInputStream(prefs)){
     org.xmlpull.v1.XmlPullParser parser=android.util.Xml.newPullParser();parser.setInput(in,"UTF-8");int event;
     while((event=parser.next())!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT)if(event==org.xmlpull.v1.XmlPullParser.START_TAG&&"boolean".equals(parser.getName())){
      String name=parser.getAttributeValue(null,"name"),value=parser.getAttributeValue(null,"value");
      if("all_apps".equals(name))all="true".equals(value);if("transition".equals(name))transition="true".equals(value);
     }
    }
    System.out.println(transition?"off":all?"all":"selected");return;
   }
   if(!kind.equals("targets")&&!kind.equals("profile"))throw new SecurityException("Unsupported bridge read");
   java.io.File file=new java.io.File(base,"files/"+kind+".conf");if(!file.exists())return;
   if(!file.getCanonicalPath().equals(file.getAbsolutePath()))throw new SecurityException("Linked settings rejected");
   try(java.io.InputStream in=new java.io.FileInputStream(file)) {byte[] data=new byte[4096];int n,total=0;while((n=in.read(data))!=-1){if((total+=n)>65536)throw new java.io.IOException("Oversized settings");System.out.write(data,0,n);}System.out.flush();}
  }catch(Exception error){System.err.println("Bridge settings read failed: "+error);System.exit(1);}
 }
}