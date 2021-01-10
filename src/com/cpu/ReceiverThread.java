package com.cpu;

import android.content.BroadcastReceiver;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import com.status.*;
import com.tools.*;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.os.BatteryManager;
import android.os.Handler;
import com.cpu.init.ShellExecuter;
import com.status.*;

public class ReceiverThread extends BroadcastReceiver
{
      public static String mOutput = null;
      public static String mAmpere = null;
      public static String mVoltase = null;
      public static String mTempe = null;
      public static boolean run = true;
      private Context context;
      private ReceiverBoot receiver;
      private SharedPreferences settings;

      public Handler mHandler = new Handler();
      private Runnable mRefresh = new Runnable() {
	        public void run() {
            thread();
            mHandler.postDelayed(mRefresh, 5 * 1000);
	       }
      };
	
      @Override
      public void onReceive(Context xcontext, Intent intent)
      {
          context = xcontext;
          receiver = new ReceiverBoot();
          settings = context.getSharedPreferences("Settings", 0);
          
          if (run) {
                run = false;
                mHandler.postDelayed(mRefresh, 5 * 1000);
          }
         // if (runServiceBoot(context) == false) context.startService(new Intent(context, ServiceBoot.class));
         //if (runAudioPreview(context) == false) context.startService(new Intent(context, AudioPreview.class));
      }

      private void thread() {
          facebook();
          context.startService(new Intent(context, ServiceStatus.class));
          PowerMon mPower = new PowerMon();
          receiver.notifiBoot(context, mPower.mVoltase, mPower.mAmpere, mPower.mTempe, false);
      }

      public void threadStop(Context context) {
          context.stopService(new Intent(context, ServiceStatus.class));
          new ReceiverBoot().notifiBoot(context, "stop", "stop", "stop", true);
      }

      public void facebook() {
          if (settings.getBoolean("alert_fb", false)) {
              String data = new MainBrowser().webkitText(context, "http://free.facebook.com/home.php");
              String[] spt1 = data.split("Pesan");
              try {
                  String[] spt2 = spt1[1].split("Notifikasi");
                  //Toast.makeText(context, "spt2[0]:"+spt2[0]+ "\nspt2[1]:"+spt2[1], Toast.LENGTH_LONG).show();

                  if (!spt2[0].equals("")) {
                     Toast.makeText(context, "ada "+spt2[0]+" pesan", Toast.LENGTH_LONG).show();
                  }
              }catch(Exception e){}
          }
      }

      public boolean runServiceBoot(Context context) {
          ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
          for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
               if (ServiceBoot.class.getName().equals(service.service.getClassName())) {
                   return true;
               }
		  }
        return false;
      }

      public boolean runServiceStatus(Context context) {
          ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
          for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
               if (ServiceStatus.class.getName().equals(service.service.getClassName())) {
                    return true;
               }
		   }
        return false;
      }

      public boolean runAudioPreview(Context context) {
          ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
          for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
               if (AudioPreview.class.getName().equals(service.service.getClassName())) {
                    return true;
               }
		  }
        return false;
      }

}