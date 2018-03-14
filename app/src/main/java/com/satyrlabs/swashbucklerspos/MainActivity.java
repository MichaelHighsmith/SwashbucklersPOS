package com.satyrlabs.swashbucklerspos;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.MenuItem;

import com.squareup.sdk.pos.ChargeRequest;
import com.squareup.sdk.pos.PosClient;
import com.squareup.sdk.pos.PosSdk;
import com.starmicronics.stario.PortInfo;
import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.starmicronics.stario.StarPrinterStatus;
import com.starmicronics.starioextension.ICommandBuilder;
import com.starmicronics.starioextension.StarIoExt;

import java.util.ArrayList;
import java.util.List;

import static com.satyrlabs.swashbucklerspos.MenuContract.CONTENT_URI;
import static com.satyrlabs.swashbucklerspos.MenuContract.ITEM_NAME;
import static com.satyrlabs.swashbucklerspos.MenuContract.ITEM_PRICE;
import static com.satyrlabs.swashbucklerspos.MenuContract._ID;
import static com.squareup.sdk.pos.CurrencyCode.USD;

public class MainActivity extends AppCompatActivity implements MenuItemAdapter.AdapterCallback{

    RecyclerView menuItemsRecyclerView;
    RecyclerView currentOrderRecyclerView;
    CurrentOrderAdapter currentOrderAdapter;
    MenuItemAdapter adapter;

    TextView preTaxTotalTV;
    TextView taxTotalTV;
    TextView totalPriceTV;

    //floats for all tax calculations
    float preTaxPrice = 0;
    float taxTotalPrice = 0;
    float totalPrice = 0;
    float taxRate;

    ArrayList<String> orderItems;
    ArrayList<Float> orderItemPrices;

    StarIOPort port = null;

    ProgressBar progress;
    Spinner taxSpinner;

    LoaderManager.LoaderCallbacks<Cursor> cursorLoader;

    private PosClient posClient;
    private static final int CHARGE_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        posClient = PosSdk.createClient(this, "CLIENT_ID_HERE");

        //id the views
        progress = findViewById(R.id.progressBar);
        taxSpinner = findViewById(R.id.tax_spinner);
        totalPriceTV = findViewById(R.id.total_price);
        taxTotalTV = findViewById(R.id.tax_price);
        preTaxTotalTV = findViewById(R.id.pretax_total_price);
        menuItemsRecyclerView = findViewById(R.id.menu_items_recyclerview);
        currentOrderRecyclerView = findViewById(R.id.current_order_recycler_view);

        //Create arrays for the current order items and prices (these grow simultaneously (could use dictionary))
        orderItems = new ArrayList<>();
        orderItemPrices = new ArrayList<>();

        currentOrderAdapter = new CurrentOrderAdapter(this, orderItems, orderItemPrices);
        currentOrderRecyclerView.setAdapter(currentOrderAdapter);

        menuItemsRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        currentOrderRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        //attach adapter to tax spinner
        setUpTaxSpinner();

        FloatingActionButton newItem = findViewById(R.id.new_item);
        newItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, NewMenuItemActivity.class);
                startActivity(intent);
            }
        });

        //Just one right now (content provider cursor loader)
        createLoaders();
        //Start cursorLoader
        getLoaderManager().initLoader(1, null, cursorLoader);
    }

    void setUpTaxSpinner(){
        ArrayAdapter taxAdapter = ArrayAdapter.createFromResource(this, R.array.array_tax_locations, R.layout.spinner_item);
        taxAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        taxSpinner.setAdapter(taxAdapter);

        taxSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                switch(selection){
                    case "Warrenton":
                        taxRate = .08975f;
                        break;
                    case "Unincorporated Warren County":
                        taxRate = .062250f;
                        break;
                    case "Marthasville":
                        taxRate = .07725f;
                        break;
                    case "Unincorporated St. Charles County":
                        taxRate = .0595f;
                        break;
                    case "Unincorporated Franklin County":
                        taxRate = .05975f;
                        break;
                    case "New Mellie":
                        taxRate = .0795f;
                        break;
                    case "Wentzville":
                        taxRate = .0845f;
                        break;
                    case "Ofallon":
                        taxRate = .0795f;
                        break;
                    case "St. Peters":
                        taxRate = .0795f;
                        break;
                    case "Wright City":
                        taxRate = .07975f;
                        break;
                    case "Washington":
                        taxRate = .0835f;
                        break;
                    case "Union":
                        taxRate = .08975f;
                        break;
                }
                taxTotalPrice = preTaxPrice * taxRate;
                taxTotalTV.setText(String.format("%,.2f", taxTotalPrice));
                totalPrice = preTaxPrice + taxTotalPrice;
                totalPriceTV.setText(String.format("%,.2f", totalPrice));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    public void creditCheckout(View view) {
        //convert the float to an underscore int
        float squareTotalFloat = totalPrice * 100;
        int squareTotal = (int) squareTotalFloat;

        //Square
        ChargeRequest request = new ChargeRequest.Builder(squareTotal, USD).build();
        try{
            Intent intent = posClient.createChargeIntent(request);
            startActivityForResult(intent, CHARGE_REQUEST_CODE);
        } catch (ActivityNotFoundException e){
            Toast.makeText(this, "Square Point of Sale app is not installed!", Toast.LENGTH_SHORT).show();
            posClient.openPointOfSalePlayStoreListing();
        }
    }

    //After the square payment is initiated, receive either success code or failure code.  If success then print receipt
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CHARGE_REQUEST_CODE){
            if(data == null){
                return;
            }

            if(resultCode == Activity.RESULT_OK){
                ChargeRequest.Success success = posClient.parseChargeSuccess(data);
                Toast.makeText(this, "Transaction successful!", Toast.LENGTH_SHORT).show();
                //print receipt
                SubmitOrderTask task = new SubmitOrderTask();
                task.execute("whatever");
            } else {
                ChargeRequest.Error error = posClient.parseChargeError(data);
                if(error.code == ChargeRequest.ErrorCode.TRANSACTION_ALREADY_IN_PROGRESS){
                    Toast.makeText(this, "A transaction is already in progress", Toast.LENGTH_SHORT).show();
                    posClient.launchPointOfSale();
                } else {
                    Toast.makeText(this, "Transaction error", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void cashCheckout(View view){
        Intent cashIntent = new Intent(MainActivity.this, CashCheckoutActivity.class);
        cashIntent.putExtra("total", totalPrice);
        startActivity(cashIntent);
    }

    //Async task for submitting the order to printer
    private class SubmitOrderTask extends AsyncTask<String, Void, String>{

        @Override
        protected void onPreExecute(){
            progress.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... urls){
            //print
            printReceipt();
            return "Task result";
        }

        @Override
        protected void onPostExecute(String result){

            //update total credit income
            SharedPreferences sharedPreferences = getSharedPreferences("myPref", 0);
            float totalCreditIncome = sharedPreferences.getFloat("totalCreditIncome", 0.0f);
            totalCreditIncome = totalCreditIncome + totalPrice;
            sharedPreferences.edit().putFloat("totalCreditIncome", totalCreditIncome).apply();

            //Set values back to 0
            resetOrder();

            progress.setVisibility(View.INVISIBLE);
        }
    }

    //Called in AsyncTask. mPoP has issues connecting sometimes, so on fail call recursive printReceipt until success
    public void printReceipt(){

        String portName = "";
        try {
            List<PortInfo> portList = StarIOPort.searchPrinter("BT:");

            PortInfo currentPort = portList.get(0);
            portName = currentPort.getPortName();
            port = StarIOPort.getPort(portName, "Portable", 10000, MainActivity.this);

            StarPrinterStatus status = port.beginCheckedBlock();

            byte[] title = "         Swashbucklers     ".getBytes();
            byte[] preTaxTotalByte = ("Pre-tax total:     " + String.format("%,.2f", preTaxPrice)).getBytes();
            byte[] taxTotalByte = ("Tax:                " + String.format("%,.2f", taxTotalPrice)).getBytes();
            byte[] totalPriceByte = ("Total:             " + String.format("%,.2f", totalPrice)).getBytes();
            byte[] signatureByte = "Sign Here: _______________________".getBytes();

            ICommandBuilder builder = StarIoExt.createCommandBuilder(StarIoExt.Emulation.StarPRNT);
            builder.appendLineFeed(title);
            builder.appendLineFeed();
            //Add a line for each item in the order (plus price)
            for(int i = 0; i < orderItems.size(); i++){
                String currentItemName = orderItems.get(i) + "        " + String.format("%,.2f", orderItemPrices.get(i));
                byte[] currentItemBytes = currentItemName.getBytes();
                builder.appendLineFeed(currentItemBytes);
            }
            builder.appendLineFeed();
            builder.appendLineFeed(preTaxTotalByte);
            builder.appendLineFeed(taxTotalByte);
            builder.appendLineFeed(totalPriceByte);
            builder.appendLineFeed();
            builder.appendLineFeed(signatureByte);
            builder.appendLineFeed();
            builder.appendLineFeed();
            builder.appendLineFeed();
            builder.appendCutPaper(ICommandBuilder.CutPaperAction.PartialCutWithFeed);
            builder.appendLineFeed();
            builder.appendLineFeed(preTaxTotalByte);
            builder.appendLineFeed(taxTotalByte);
            builder.appendLineFeed(totalPriceByte);
            builder.appendLineFeed();
            builder.appendLineFeed(signatureByte);
            builder.appendLineFeed();
            builder.appendLineFeed();
            builder.appendLineFeed();
            builder.appendCutPaper(ICommandBuilder.CutPaperAction.PartialCutWithFeed);
            builder.endDocument();

            port.writePort(builder.getCommands(), 0, builder.getCommands().length);

            status = port.endCheckedBlock();

            if (status.offline == false) {
                //Print successful end
            } else {
                //print failure
            }

        } catch (StarIOPortException e) {
            //Error
            try{
                wait(1000);
            } catch (Exception error){

            }
            printReceipt();

        } finally {
            try {
                //Port close
                StarIOPort.releasePort(port);
            } catch (StarIOPortException e) {

            }
        }
    }

    public void clearOrder(View view){
        resetOrder();
    }

    public void resetOrder(){
        preTaxPrice = 0;
        taxTotalPrice = 0;
        totalPrice = 0;
        preTaxTotalTV.setText(String.format("%,.2f", preTaxPrice));
        taxTotalTV.setText(String.format("%,.2f", taxTotalPrice));
        totalPriceTV.setText(String.format("%,.2f", totalPrice));
        orderItems = new ArrayList<>();
        orderItemPrices = new ArrayList<>();

        currentOrderAdapter = new CurrentOrderAdapter(this, orderItems, orderItemPrices);
        currentOrderRecyclerView.setAdapter(currentOrderAdapter);
    }

    //Menu item's onClick
    @Override
    public void onItemClicked(String itemName, float itemPrice) {
        preTaxPrice += itemPrice;
        preTaxTotalTV.setText(String.format("%,.2f", preTaxPrice));
        taxTotalPrice = preTaxPrice * taxRate;
        taxTotalTV.setText(String.format("%,.2f", taxTotalPrice));
        totalPrice = preTaxPrice + taxTotalPrice;
        totalPriceTV.setText(String.format("%,.2f", totalPrice));

        orderItems.add(itemName);
        orderItemPrices.add(itemPrice);

        currentOrderAdapter.notifyDataSetChanged();
    }

    //Create cursor loader for content provider
    public void createLoaders(){
        cursorLoader = new LoaderManager.LoaderCallbacks<Cursor>(){
            @Override
            public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
                String[] projection = {
                        _ID,
                        ITEM_NAME,
                        ITEM_PRICE
                };

                return new CursorLoader(getApplicationContext(),
                        CONTENT_URI,
                        projection,
                        null, null, null);
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
                adapter = new MenuItemAdapter(MainActivity.this, cursor, MainActivity.this);
                menuItemsRecyclerView.setAdapter(adapter);
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
                adapter.notifyDataSetChanged();
            }
        };
    }

    public void openRegister(View view){
        String portName = "";
        try{
            List<PortInfo> portList = StarIOPort.searchPrinter("BT:");

            PortInfo currentPort = portList.get(0);
            portName = currentPort.getPortName();
            port = StarIOPort.getPort(portName, "Portable", 10000, MainActivity.this);
        } catch (StarIOPortException e){

        }


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

        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.see_totals:
                Intent newIntent = new Intent(MainActivity.this, TotalsActivity.class);
                startActivity(newIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}



