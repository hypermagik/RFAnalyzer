package com.sdr.common;

import static android.app.PendingIntent.FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT;
import static android.app.PendingIntent.FLAG_MUTABLE;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;

import java.util.function.Function;

public class USBPermission {
    private static final String ACTION_USB_PERMISSION = "com.sdr.USB_PERMISSION";

    public static void request(Context context, UsbManager usbManager, UsbDevice usbDevice, Function<String, Void> callback) {
        if (usbManager.hasPermission(usbDevice)) {
            callback.apply(null);
        } else {
            registerNewBroadcastReceiver(context, usbDevice, callback);

            int flags = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                flags = FLAG_MUTABLE | FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags = FLAG_MUTABLE;
            }

            usbManager.requestPermission(usbDevice, PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), flags));
        }
    }

    private static void registerNewBroadcastReceiver(Context context, UsbDevice usbDevice, Function<String, Void> callback) {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (ACTION_USB_PERMISSION.equals(action)) {
                    synchronized (this) {
                        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                        if (usbDevice.equals(device)) {
                            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                                if (manager.hasPermission(device)) {
                                    callback.apply(null);
                                } else {
                                    callback.apply("Permissions were granted but can't access the device");
                                }
                            } else {
                                callback.apply("Extra permission was not granted");
                            }
                        } else {
                            callback.apply("Got a permission for an unexpected device " + device.getDeviceName());
                        }
                    }
                } else {
                    callback.apply("Unexpected action received");
                }

                context.unregisterReceiver(this);
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(broadcastReceiver, new IntentFilter(ACTION_USB_PERMISSION), Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(broadcastReceiver, new IntentFilter(ACTION_USB_PERMISSION));
        }
    }
}
