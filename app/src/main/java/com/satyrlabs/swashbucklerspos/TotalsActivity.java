package com.satyrlabs.swashbucklerspos;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

public class TotalsActivity extends AppCompatActivity {

    TextView cashTotalTV, creditTotalTV;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.totals_layout);

        cashTotalTV = findViewById(R.id.cash_total_day);
        creditTotalTV = findViewById(R.id.credit_total_day);

        updateTvs();
    }

    public void resetToday(View view){
        //set both values back to 0
        SharedPreferences sharedPreferences = getSharedPreferences("myPref", 0);
        sharedPreferences.edit().putFloat("totalCashIncome", 0.0f).apply();
        sharedPreferences.edit().putFloat("totalCreditIncome", 0.0f).apply();
        updateTvs();

    }

    public void updateTvs(){
        SharedPreferences sharedPreferences = getSharedPreferences("myPref", 0);
        float totalCashIncome = sharedPreferences.getFloat("totalCashIncome", 0.0f);
        float totalCreditIncome = sharedPreferences.getFloat("totalCreditIncome", 0.0f);

        cashTotalTV.setText(String.format("%,.2f", totalCashIncome));
        creditTotalTV.setText(String.format("%,.2f", totalCreditIncome));
    }


}
