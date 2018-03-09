package com.satyrlabs.swashbucklerspos;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.starmicronics.stario.PortInfo;
import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.starmicronics.starioextension.ICommandBuilder;
import com.starmicronics.starioextension.IConnectionCallback;
import com.starmicronics.starioextension.StarIoExt;
import com.starmicronics.starioextension.StarIoExtManager;

import java.util.List;

public class CashCheckoutActivity extends AppCompatActivity implements IConnectionCallback{

    float priceTotal;
    TextView cashTotalTV, changeTV;
    EditText cashPaidTV;
    StarIoExtManager manager;
    String portName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cash_layout);

        cashPaidTV = findViewById(R.id.cash_paid);
        changeTV = findViewById(R.id.change_tv);

        priceTotal = getIntent().getFloatExtra("total", 0.0f);
        cashTotalTV = findViewById(R.id.cash_total_tv);
        cashTotalTV.setText(String.format("%,.2f", priceTotal));
    }

    public void getChange(View view){
        if(cashPaidTV.getText() == null){
            return;
        }
        float cashPaid = Float.valueOf(cashPaidTV.getText().toString());
        float change = cashPaid - priceTotal;
        changeTV.setText(String.format("%,.2f", change));

        //Check the cash drawers current state
        StarIoExtManager.CashDrawerStatus cashDrawerStatus = manager.getCashDrawerOpenStatus();
        Log.d("cash drawer status", String.valueOf(cashDrawerStatus));

        //Build a command to open the register
        ICommandBuilder builder = StarIoExt.createCommandBuilder(StarIoExt.Emulation.StarPRNT);
        builder.beginDocument();
        builder.appendPeripheral(ICommandBuilder.PeripheralChannel.No1);
        builder.endDocument();
        byte[] data = builder.getCommands();
        //Send the command using the communication file (pieces taken from StarPRNT's sdk)
        Communication.sendCommandsDoNotCheckCondition(this, data, portName, "Portable", 10000, this, mCallback);
    }

    private final Communication.SendCallback mCallback = new Communication.SendCallback() {
        @Override
        public void onStatus(boolean result, Communication.Result communicateResult) {
            String msg;
            switch (communicateResult) {
                case Success :
                    msg = "Success!";
                    break;
                case ErrorOpenPort:
                    msg = "Fail to openPort";
                    break;
                case ErrorBeginCheckedBlock:
                    msg = "Printer is offline (beginCheckedBlock)";
                    break;
                case ErrorEndCheckedBlock:
                    msg = "Printer is offline (endCheckedBlock)";
                    break;
                case ErrorReadPort:
                    msg = "Read port error (readPort)";
                    break;
                case ErrorWritePort:
                    msg = "Write port error (writePort)";
                    break;
                default:
                    msg = "Unknown error";
                    break;
            }

            Log.d("onStatus", msg);
        }
    };

    @Override
    public void onStart(){
        super.onStart();
        try{
            List<PortInfo> portList = StarIOPort.searchPrinter("BT:");

            PortInfo currentPort = portList.get(0);
            portName = currentPort.getPortName();

            manager = new StarIoExtManager(StarIoExtManager.Type.Standard, portName, "Portable", 10000, this);
            manager.connect(this);

            //Check that the cash drawer's activehigh is ready to open
            manager.setCashDrawerOpenActiveHigh(true);
            boolean open = manager.getCashDrawerOpenActiveHigh();
            Log.d("active high status", String.valueOf(open));

        } catch (StarIOPortException e){
            Log.e("CashCheckout", "Error opening the cash drawer", e);
        }
    }

    public void cashFinish(View view){
        //disconnect the StarIOExtManager and end this activity
        manager.disconnect(this);
        finish();
    }


    @Override
    public void onConnected(ConnectResult connectResult) {
        Log.d("CashCheckout", "onConnected" + connectResult);
    }

    @Override
    public void onDisconnected() {
        Log.d("CashCheckout", "onDisconnected");
    }
}
