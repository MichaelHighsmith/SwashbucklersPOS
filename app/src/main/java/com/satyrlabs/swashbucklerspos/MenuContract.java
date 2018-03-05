package com.satyrlabs.swashbucklerspos;


import android.net.Uri;
import android.provider.BaseColumns;

public class MenuContract {

    public static final String DATABASE_NAME = "menuItems.db";
    public static final String TABLE_NAME = "menuItems";

    public static final String CONTENT_AUTHORITY = "com.satyrlabs.swashbucklerspos";
    public static final String PATH_ITEMS= "items";
    public static final Uri BASE_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_URI, PATH_ITEMS);

    public static final String _ID = BaseColumns._ID;
    public static final String ITEM_NAME = "name";
    public static final String ITEM_PRICE = "price";
}
