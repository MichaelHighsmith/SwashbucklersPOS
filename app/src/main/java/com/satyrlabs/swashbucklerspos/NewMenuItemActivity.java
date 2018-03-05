package com.satyrlabs.swashbucklerspos;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import static com.satyrlabs.swashbucklerspos.MenuContract.CONTENT_URI;
import static com.satyrlabs.swashbucklerspos.MenuContract.ITEM_NAME;
import static com.satyrlabs.swashbucklerspos.MenuContract.ITEM_PRICE;

public class NewMenuItemActivity extends AppCompatActivity {

    EditText nameText, priceText;
    Button newItemButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_menu_item_layout);

        newItemButton = findViewById(R.id.new_item);
        nameText = findViewById(R.id.nameInput);
        priceText = findViewById(R.id.priceInput);
    }

    public void addMenuItem(View view){
        String itemName = nameText.getText().toString().trim();
        String itemPrice = priceText.getText().toString().trim();

        ContentValues values = new ContentValues();
        values.put(ITEM_NAME, itemName);
        values.put(ITEM_PRICE, itemPrice);

        Uri newUri = getContentResolver().insert(CONTENT_URI, values);

        if(newUri == null){
            Toast.makeText(this, "Error inserting the new item", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Menu Item added!", Toast.LENGTH_SHORT).show();
        }

        finish();
    }

}
