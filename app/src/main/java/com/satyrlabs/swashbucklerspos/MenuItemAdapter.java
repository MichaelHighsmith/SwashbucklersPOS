package com.satyrlabs.swashbucklerspos;


import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import static com.satyrlabs.swashbucklerspos.MenuContract.ITEM_NAME;
import static com.satyrlabs.swashbucklerspos.MenuContract.ITEM_PRICE;

public class MenuItemAdapter extends RecyclerView.Adapter<MenuItemAdapter.MenuItemViewHolder>{

    private Context context;
    Cursor cursor;
    private AdapterCallback callback;

    public MenuItemAdapter(Context context, Cursor cursor, AdapterCallback callback){
        this.context = context;
        this.cursor = cursor;
        this.callback = callback;
    }

    @Override
    public MenuItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.menu_item, parent, false);
        MenuItemViewHolder holder = new MenuItemViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(MenuItemViewHolder holder, final int position) {
        //get the cursor position and pull the name and price of each item when clicked
        cursor.moveToPosition(position);
        int nameColumnIndex = cursor.getColumnIndex(ITEM_NAME);
        int priceColumnIndex = cursor.getColumnIndex(ITEM_PRICE);

        final String name = cursor.getString(nameColumnIndex);
        final float price = cursor.getFloat(priceColumnIndex);

        holder.itemName.setText(name);



        holder.itemName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callback.onItemClicked(name, price);
                Toast.makeText(context, "this item's price is $" + price, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return cursor.getCount();
    }

    class MenuItemViewHolder extends RecyclerView.ViewHolder{

        Button itemName;

        public MenuItemViewHolder(View itemView){
            super(itemView);
            itemName = itemView.findViewById(R.id.item_name);
        }

        public float bindCursor(Cursor cursor){
            int nameColumnIndex = cursor.getColumnIndex(ITEM_NAME);
            itemName.setText(cursor.getString(nameColumnIndex));
            int priceColumnIndex = cursor.getColumnIndex(ITEM_PRICE);
            return cursor.getFloat(priceColumnIndex);
        }

    }

    public interface AdapterCallback{
        void onItemClicked(String itemName, float itemPrice);
    }

}
