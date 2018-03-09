package com.satyrlabs.swashbucklerspos;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                    case "Tax Location 1":
                        taxRate = .05f;
                        break;
                    case "Tax Location 2":
                        taxRate = .06f;
                        break;
                    case "Tax Location 3":
                        taxRate = .07f;
                        break;
                    case "Tax Location 4":
                        taxRate = .08f;
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


    public void print(View view) {
        SubmitOrderTask task = new SubmitOrderTask();
        task.execute("whatever");
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
            printReceipt();
            return "Task result";
        }

        @Override
        protected void onPostExecute(String result){
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

            byte[] title = "      Swashbucklers     ".getBytes();
            byte[] preTaxTotalByte = ("Pre-tax total:     " + String.format("%,.2f", preTaxPrice)).getBytes();
            byte[] taxTotalByte = ("Tax:       " + String.format("%,.2f", taxTotalPrice)).getBytes();
            byte[] totalPriceByte = ("Total:      " + String.format("%,.2f", totalPrice)).getBytes();

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
            Log.e("Log", "There was an error in the try", e);
            try{
                wait(1000);
            } catch (Exception error){
                Log.e("Couldn't wait", "error", e);
            }
            printReceipt();

        } finally {
            try {
                //Port close
                StarIOPort.releasePort(port);
            } catch (StarIOPortException e) {
                Log.i("Log", "Error closing the port");
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getMenuInflater().inflate(R.menu.main_menu, menu);

        return true;
    }
}



