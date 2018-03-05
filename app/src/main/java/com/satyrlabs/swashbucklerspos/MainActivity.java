package com.satyrlabs.swashbucklerspos;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.starmicronics.stario.PortInfo;
import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.starmicronics.stario.StarPrinterStatus;
import com.starmicronics.starioextension.ICommandBuilder;
import com.starmicronics.starioextension.StarIoExt;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    StarIOPort port = null;
    String currentItem = "Hello";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void chickenSandwich(View view) {
        currentItem = "Chicken Sandwich";
    }

    public void PoBoy(View view) {
        currentItem = "PoBoy";
    }

    public void print(View view) {

        String portName = "";

        try {

            List<PortInfo> portList = StarIOPort.searchPrinter("BT:");

            PortInfo currentPort = portList.get(0);
            Log.i("LOG", "Port Name:" + currentPort.getPortName());

            portName = currentPort.getPortName();

            port = StarIOPort.getPort(portName, "Portable", 10000, this);

            StarPrinterStatus status = port.beginCheckedBlock();


            byte[] b = currentItem.getBytes();
            byte[] yuuup = "yuuuup".getBytes();
            byte[] hey = "hey".getBytes();

            ICommandBuilder builder = StarIoExt.createCommandBuilder(StarIoExt.Emulation.StarPRNT);
            builder.appendLineFeed();
            builder.appendLineFeed(b);
            builder.appendLineFeed();
            builder.appendLineFeed(yuuup);
            builder.appendLineFeed();
            builder.appendLineFeed(hey);
            builder.appendLineFeed();
            builder.appendLineFeed(yuuup);
            builder.appendCutPaper(ICommandBuilder.CutPaperAction.PartialCutWithFeed);
            builder.endDocument();

            port.writePort(builder.getCommands(), 0, builder.getCommands().length);

            status = port.endCheckedBlock();

            if (status.offline == false) {
                //Print successful end
                Log.i("Log", "Printing is all good!");
            } else {
                Log.i("Log", "Printing is abnormal termination");
            }


        } catch (StarIOPortException e) {
            //Error
            Log.e("Log", "There was an error in the try");
        } finally {
            try {
                //Port close
                StarIOPort.releasePort(port);
            } catch (StarIOPortException e) {
                Log.i("Log", "Error closing the port");
            }
        }

    }
}



