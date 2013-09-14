/*
Berengar / FileTool
Copyright (c) 2013 Bradipao <bradipao@gmail.com>
http://gplus.to/Bradipao

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package it.bradipao.berengar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import android.os.Environment;

public class FileTool {

   public static final String LOGTAG = "FILETOOL";

   
   //---- STRINGWRITE ---------------------------------------------------------
   
   public static boolean stringWrite(String so,File outfile,boolean bGzip) throws IOException {
      // if not Gzip then write simple txt file
      if (!bGzip) {
         FileWriter fw = new FileWriter(outfile);
         BufferedWriter bw = new BufferedWriter(fw);
         bw.write(so);
         bw.close();
      }
      // else Gzip
      else {
         FileOutputStream fos = new FileOutputStream(outfile+".gz");
         GZIPOutputStream gzos = new GZIPOutputStream(fos);
         gzos.write(so.getBytes());
         gzos.finish();
         gzos.close();
      }
      
      return true;
   }
   
   //---- STRINGREAD ---------------------------------------------------------
   
   public static String stringRead(File infile,boolean bGzip) throws IOException {
      // if not Gzip then write simple txt file
      if (!bGzip) {
         StringBuilder sb = new StringBuilder(1024);
         String line;
         FileReader fr = new FileReader(infile);
         BufferedReader br = new BufferedReader(fr);
         while ((line=br.readLine())!=null) {
            sb.append(line);
         }
         br.close();
         return sb.toString();
      }
      // else Gzip
      else {       
         FileInputStream fis = new FileInputStream(infile+".gz");
         GZIPInputStream gzis = new GZIPInputStream(fis);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         byte[] buf = new byte[1024];
         int decompressCount = gzis.read(buf,0,1024);
         while(decompressCount>0) {
            bos.write(buf,0,decompressCount);
            decompressCount = gzis.read(buf,0,1024);
         }
         gzis.close();
         return new String(bos.toByteArray());
      }
      
   }
   
   //---- CHECKEXTERNALSTORAGE ------------------------------------------------
   
   public static boolean checkExternalStorage() {
      // check external storage for r/w
      String state = Environment.getExternalStorageState();
      if (Environment.MEDIA_MOUNTED.equals(state)) return true;
      else return false;
   }

}
