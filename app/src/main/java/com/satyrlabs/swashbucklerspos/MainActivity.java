package com.satyrlabs.swashbucklerspos;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
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

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, MenuItemAdapter.AdapterCallback{

    RecyclerView menuItemsRecyclerView;
    MenuItemAdapter adapter;
    TextView priceTotal;
    float price = 0;

    ArrayList<String> orderItems;
    ArrayList<Float> orderItemPrices;

    StarIOPort port = null;
    String currentItem = "Hello";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        getLoaderManager().initLoader(1, null, this);
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
            byte[] title = "      Swashbucklers     ".getBytes();
            byte[] finalPrice = String.valueOf(price).getBytes();

            ICommandBuilder builder = StarIoExt.createCommandBuilder(StarIoExt.Emulation.StarPRNT);
            builder.appendLineFeed(title);
            builder.appendLineFeed();
            //Add a line for each item in the order (plus price)
            for(int i = 0; i < orderItems.size(); i++){
                String currentItemName = orderItems.get(i) + "        " + orderItemPrices.get(i).toString();
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
        adapter = new MenuItemAdapter(this, cursor, this);
        menuItemsRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClicked(String itemName, float itemPrice) {
        price += itemPrice;
        priceTotal.setText(String.valueOf(price));
        orderItems.add(itemName);
        orderItemPrices.add(itemPrice);
    }
}



