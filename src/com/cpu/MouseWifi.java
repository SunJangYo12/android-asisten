package com.cpu;


import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.widget.Toast;
import android.os.*;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import com.cpu.R;

public class MouseWifi extends AccessibilityService {

    private static final String TAG = MouseWifi.class.getName();

    private View cursorView, toastView;
    private LayoutParams cursorLayout;
    private WindowManager windowManager;
    private Toast toast;
    private int x, y = 200;
    private DatagramSocket udpSocket;

    private static void logNodeHierachy(AccessibilityNodeInfo nodeInfo, int depth) {
        Rect bounds = new Rect();
        nodeInfo.getBoundsInScreen(bounds);

        StringBuilder sb = new StringBuilder();
        if (depth > 0) {
            for (int i=0; i<depth; i++) {
                sb.append("  ");
            }
            sb.append("\u2514 ");
        }
        sb.append(nodeInfo.getClassName());
        sb.append(" (" + nodeInfo.getChildCount() +  ")");
        sb.append(" " + bounds.toString());
        if (nodeInfo.getText() != null) {
            sb.append(" - \"" + nodeInfo.getText() + "\"");
        }
        Log.v(TAG, sb.toString());

        for (int i=0; i<nodeInfo.getChildCount(); i++) {
            AccessibilityNodeInfo childNode = nodeInfo.getChild(i);
            if (childNode != null) {
                logNodeHierachy(childNode, depth + 1);
            }
        }
    }

    private static AccessibilityNodeInfo findSmallestNodeAtPoint(AccessibilityNodeInfo sourceNode, int ax, int ay) {
        Rect bounds = new Rect();
        sourceNode.getBoundsInScreen(bounds);

        if (!bounds.contains(ax, ay)) {
            return null;
        }

        for (int i=0; i<sourceNode.getChildCount(); i++) {
            AccessibilityNodeInfo nearestSmaller = findSmallestNodeAtPoint(sourceNode.getChild(i), ax, ay);
            if (nearestSmaller != null) {
                return nearestSmaller;
            }
        }
        return sourceNode;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "onInterrupt");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Toast.makeText(this, "Service Mouse start", Toast.LENGTH_LONG).show();

        cursorView = View.inflate(getBaseContext(), R.layout.cursor, null);
        cursorLayout = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                LayoutParams.TYPE_SYSTEM_ERROR,
                LayoutParams.FLAG_DISMISS_KEYGUARD | LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        cursorLayout.gravity = Gravity.TOP | Gravity.LEFT;
        cursorLayout.x = 200;
        cursorLayout.y = 200;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        toast = new Toast(this);
        toast.setView(cursorView);
        toast.setGravity(0, x, y);
        toast.show();

        try {
            udpSocket = new DatagramSocket(9999);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buffer = new byte[1];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    while (true) {
                        try {
                            udpSocket.receive(packet);
                            String message = new String(packet.getData()).trim();
                            final int event = Integer.parseInt(message);
                            new Handler(getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    onMouseMove(new MouseEvent(event));
                                }
                            });
                        } catch (IOException e) {}
                    }
                }
            }).start();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, " Mouse stoped", Toast.LENGTH_LONG).show();
        if (windowManager != null && cursorView != null) {
            windowManager.removeView(cursorView);
        }
    }

    /*private void click() {
        Log.d(TAG, String.format("Click [%d, %d]", x, y));
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) return;
        AccessibilityNodeInfo nearestNodeToMouse = findSmallestNodeAtPoint(nodeInfo, x, y+50);
        if (nearestNodeToMouse != null) {
            logNodeHierachy(nearestNodeToMouse, 0);
            nearestNodeToMouse.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        nodeInfo.recycle();
    }*/

     private void click() {
        Log.d(TAG, String.format("Click [%d, %d]", cursorLayout.x, cursorLayout.y));
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) return;
        AccessibilityNodeInfo nearestNodeToMouse = findSmallestNodeAtPoint(nodeInfo, cursorLayout.x, cursorLayout.y + 50);
        if (nearestNodeToMouse != null) {
            logNodeHierachy(nearestNodeToMouse, 0);
            nearestNodeToMouse.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        nodeInfo.recycle();
    }

    public void onMouseMove(MouseEvent event) {
        /*switch (event.direction) {
            case MouseEvent.MOVE_LEFT:
                x -= 10;
                break;
            case MouseEvent.MOVE_RIGHT:
                x += 10;
                break;
            case MouseEvent.MOVE_UP:
                y -= 10;
                break;
            case MouseEvent.MOVE_DOWN:
                y += 10;
                break;
            case MouseEvent.LEFT_CLICK:
                click();
                break;
            default:
                break;
        }
        toast = new Toast(MouseWifi.this);
        toast.setView(cursorView);
        toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, x, y);
        toast.show();*/

        switch (event.direction) {
            case MouseEvent.MOVE_LEFT:
                cursorLayout.x -= 10;
                break;
            case MouseEvent.MOVE_RIGHT:
                cursorLayout.x += 10;
                break;
            case MouseEvent.MOVE_UP:
                cursorLayout.y -= 10;
                break;
            case MouseEvent.MOVE_DOWN:
                cursorLayout.y += 10;
                break;
            case MouseEvent.LEFT_CLICK:
                click();
                break;
            default:
                break;
        }
        windowManager.updateViewLayout(cursorView, cursorLayout);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        windowManager.addView(cursorView, cursorLayout);
        return START_STICKY;
    }

}

class MouseEvent {

    public static final int
            MOVE_UP = 0,
            MOVE_DOWN = 1,
            MOVE_LEFT = 2,
            MOVE_RIGHT = 3,
            LEFT_CLICK = 4;

    public final int direction;

    public MouseEvent(int direction) {
        this.direction = direction;
    }
}