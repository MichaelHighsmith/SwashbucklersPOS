package com.satyrlabs.swashbucklerspos;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.starmicronics.stario.StarPrinterStatus;

@SuppressWarnings({"UnusedParameters", "UnusedAssignment", "WeakerAccess"})
public class Communication {
    @SuppressWarnings("unused")
    public enum Result {
        Success,
        ErrorUnknown,
        ErrorOpenPort,
        ErrorBeginCheckedBlock,
        ErrorEndCheckedBlock,
        ErrorWritePort,
        ErrorReadPort,
    }

    public static void sendCommandsDoNotCheckCondition(Object lock, byte[] commands, String portName, String portSettings, int timeout, Context context, SendCallback callback) {
        SendCommandDoNotCheckConditionThread thread = new SendCommandDoNotCheckConditionThread(lock, commands, portName, portSettings, timeout, context, callback);
        thread.start();
    }

    interface SendCallback {
        void onStatus(boolean result, Communication.Result communicateResult);
    }

}

class SendCommandDoNotCheckConditionThread extends Thread {
    private final Object mLock;
    private Communication.SendCallback mCallback;
    private byte[] mCommands;

    private StarIOPort mPort;

    private String  mPortName = null;
    private String  mPortSettings;
    private int     mTimeout;
    private Context mContext;

    SendCommandDoNotCheckConditionThread(Object lock, byte[] commands, StarIOPort port, Communication.SendCallback callback) {
        mLock     = lock;
        mCommands = commands;
        mPort     = port;
        mCallback = callback;
    }

    SendCommandDoNotCheckConditionThread(Object lock, byte[] commands, String portName, String portSettings, int timeout, Context context, Communication.SendCallback callback) {
        mLock         = lock;
        mCommands     = commands;
        mPortName     = portName;
        mPortSettings = portSettings;
        mTimeout      = timeout;
        mContext      = context;
        mCallback     = callback;
    }

    @Override
    public void run() {
        Communication.Result communicateResult = Communication.Result.ErrorOpenPort;
        boolean result = false;

        synchronized (mLock) {
            try {
                if (mPort == null) {

                    if (mPortName == null) {
                        resultSendCallback(false, communicateResult, mCallback);
                        return;
                    } else {
                        mPort = StarIOPort.getPort(mPortName, mPortSettings, mTimeout, mContext);
                    }
                }
                if (mPort == null) {
                    communicateResult = Communication.Result.ErrorOpenPort;
                    resultSendCallback(false, communicateResult, mCallback);
                    return;
                }

//              // When using USB interface with mPOP(F/W Ver 1.0.1), you need to send the following data.
//              byte[] dummy = {0x00};
//              mPort.writePort(dummy, 0, dummy.length);

                StarPrinterStatus status;

                communicateResult = Communication.Result.ErrorWritePort;

                status = mPort.retreiveStatus();

                if (status.rawLength == 0) {
                    throw new StarIOPortException("Unable to communicate with printer.");
                }

                communicateResult = Communication.Result.ErrorWritePort;

                mPort.writePort(mCommands, 0, mCommands.length);

//                if (status.coverOpen) {
//                    throw new StarIOPortException("Printer cover is open");
//                } else if (status.receiptPaperEmpty) {
//                    throw new StarIOPortException("Receipt paper is empty");
//                } else if (status.offline) {
//                    throw new StarIOPortException("Printer is offline");
//                }

                communicateResult = Communication.Result.Success;
                result = true;
            } catch (StarIOPortException e) {
                // Nothing
            }

            if (mPort != null && mPortName != null) {
                try {
                    StarIOPort.releasePort(mPort);
                } catch (StarIOPortException e) {
                    // Nothing
                }
                mPort = null;
            }

            resultSendCallback(result, communicateResult, mCallback);
        }
    }

    private static void resultSendCallback(final boolean result, final Communication.Result communicateResult, final Communication.SendCallback callback) {
        if (callback != null) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onStatus(result, communicateResult);
                }
            });
        }
    }
}
