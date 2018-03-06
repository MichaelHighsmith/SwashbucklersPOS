package com.satyrlabs.swashbucklerspos;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class CurrentOrderAdapter extends RecyclerView.Adapter<CurrentOrderAdapter.CurrentOrderViewHolder> {

    private Context context;
    private ArrayList<String> orderNames;
    private ArrayList<Float> orderPrices;

    CurrentOrderAdapter(Context context, ArrayList<String> orderNames, ArrayList<Float> orderPrices) {
        this.context = context;
        this.orderNames = orderNames;
        this.orderPrices = orderPrices;

    }

    @Override
    public CurrentOrderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.order_item, parent, false);
        return new CurrentOrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CurrentOrderViewHolder holder, int position) {
        holder.itemName.setText(orderNames.get(position));
        holder.itemPrice.setText(String.format("%,.2f", orderPrices.get(position)));
    }

    @Override
    public int getItemCount() {
        return orderNames.size();
    }

    class CurrentOrderViewHolder extends RecyclerView.ViewHolder {

        TextView itemName, itemPrice;

        CurrentOrderViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.current_order_item_name);
            itemPrice = itemView.findViewById(R.id.current_order_item_price);
        }
    }


}
