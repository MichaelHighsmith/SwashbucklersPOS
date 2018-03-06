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
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

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
    MenuItemAdapter adapter;
    TextView priceTotal;
    float price = 0;

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

        progress = findViewById(R.id.progressBar);

        taxSpinner = findViewById(R.id.tax_spinner);
        List<Float> taxes = new ArrayList<>();
        taxes.add(.055f);
        taxes.add(.055f);
        taxes.add(.055f);
        taxes.add(.065f);
        taxes.add(.075f);

        ArrayAdapter taxAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, taxes);
        taxSpinner.setAdapter(taxAdapter);

        priceTotal = findViewById(R.id.total_price);
        orderItems = new ArrayList<>();
        orderItemPrices = new ArrayList<>();

        FloatingActionButton newItem = findViewById(R.id.new_item);
        newItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, NewMenuItemActivity.class);
                startActivity(intent);
            }
        });

        menuItemsRecyclerView = findViewById(R.id.menu_items_recyclerview);
        menuItemsRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        createLoaders();

        getLoaderManager().initLoader(1, null, cursorLoader);
    }


    public void print(View view) {

        SubmitOrderTask task = new SubmitOrderTask();
        task.execute("whatever");

    }

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
            //reset the items
            price = 0;
            priceTotal.setText(String.format("%,.2f", price));
            orderItems = new ArrayList<>();
            orderItemPrices = new ArrayList<>();
            progress.setVisibility(View.INVISIBLE);
        }
    }

    public void printReceipt(){

        String portName = "";
        try {
            List<PortInfo> portList = StarIOPort.searchPrinter("BT:");

            PortInfo currentPort = portList.get(0);
            Log.i("LOG", "Port Name:" + currentPort.getPortName());

            portName = currentPort.getPortName();

            port = StarIOPort.getPort(portName, "Portable", 10000, MainActivity.this);

            StarPrinterStatus status = port.beginCheckedBlock();

            byte[] title = "      Swashbucklers     ".getBytes();
            byte[] finalPrice = String.format("%,.2f", price).getBytes();

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
            builder.appendLineFeed();
            builder.appendLineFeed(finalPrice);
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
        //reset the items
        price = 0;
        priceTotal.setText(String.format("%,.2f", price));
        orderItems = new ArrayList<>();
        orderItemPrices = new ArrayList<>();
    }

    @Override
    public void onItemClicked(String itemName, float itemPrice) {
        price += itemPrice;
        priceTotal.setText(String.format("%,.2f", price));
        orderItems.add(itemName);
        orderItemPrices.add(itemPrice);
    }

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
}



