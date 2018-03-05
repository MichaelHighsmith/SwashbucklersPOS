package com.satyrlabs.swashbucklerspos;


import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static com.satyrlabs.swashbucklerspos.MenuContract.ITEM_NAME;
import static com.satyrlabs.swashbucklerspos.MenuContract.ITEM_PRICE;
import static com.satyrlabs.swashbucklerspos.MenuContract.TABLE_NAME;
import static com.satyrlabs.swashbucklerspos.MenuContract._ID;

public class MenuDbHelper extends SQLiteOpenHelper{

    public MenuDbHelper(Context context){
        super(context, TABLE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase database){

        try{
            String SQL_CREATE_ITEMS_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
                    + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + ITEM_NAME + " TEXT NOT NULL, "
                    + ITEM_PRICE + " FLOAT NOT NULL);";

            database.execSQL(SQL_CREATE_ITEMS_TABLE);
            Log.v("onCreate", "Table created successfully");
        } catch (SQLException e){
            Log.e("OnCreate", "Error making the table", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int i, int k){

    }

}
