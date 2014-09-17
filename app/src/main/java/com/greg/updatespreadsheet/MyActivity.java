package com.greg.updatespreadsheet;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * This class only sets the main layout and check for the google play service status.
 * It gets a token on button click.
 */
public class MyActivity extends Activity {
    public static MyActivity myActivity;
    private static final int USER_RECOVERABLE_AUTH = 5;
    private String mSubCategory = "";
    private String mAmount      = "";
    private String mDescription = "";

    /****** Example of Categories and Subcategories to feed the auto complete widget ******/
    private static String[] myCategories = {"HOME EXPENSES","DAILY LIVING","TRANSPORTATION","ENTERTAINMENT","HEALTH",
            "OBLIGATIONS","CHARITY/GIFTS","SUBSCRIPTIONS","SAVINGS","MISCELLANEOUS"};
    private static String[][] mySubCategories = {{"Rent","Rental Insurance","Phone","Cable/Internet","Furnishings/Appliances","Parking","Utilities"},
            {"Groceries","Personal Supplies","Clothing","Laundry","Education/Lessons","Salon/Barber","Beer/ Wine","Other"},
            {"Vehicle Payments","Auto Insurance","Fuel","Bus/Taxi/Train Fare","Repairs/ Maintenance","Registration/License","Other - TOLLS"},
            {"Dining/Eating Out","Music","Movies/Theater","Concerts/Plays","Sports","Outdoor Recreation","Gadgets","Vacation/Travel"},
            {"Health Insurance","Doctor/Dentist","Medicine/Drugs","Gym Membership","Veterinarian/Pet Care","Other"},
            {"Taxes ","cc payments"},
            {"Gifts Given","Charitable Donations","Religious Donations","Other"},
            {"Newspaper","Magazines","Dues/Memberships","Other"},
            {"Transfer to Savings","Retirement (401k, IRA)"},
            {"Incidental","Speeding Tixx","Other"}};
    /**************************************************************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        myActivity = this;

        checkGooglePlayServiceStatus();
        initLayout();
    }

    public void checkGooglePlayServiceStatus() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        String baseMessage = "Google Play Service status : ";

        switch (status) {
            case ConnectionResult.SUCCESS:
                Toast.makeText(this, baseMessage+"Success", Toast.LENGTH_SHORT).show();
                break;
            case ConnectionResult.SERVICE_MISSING:
                Toast.makeText(this, baseMessage+"Missing", Toast.LENGTH_SHORT).show();
                break;
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                Toast.makeText(this, baseMessage+"Version Update Required",
                        Toast.LENGTH_SHORT).show();
                break;
            case ConnectionResult.SERVICE_DISABLED:
                Toast.makeText(this, baseMessage+"Disabled", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * Setup the main layout by updating the title of the main layout and setting up the widgets.
     * This function retrieves as well a token to access the user's spreadsheets on Google Drive
     */
    protected void initLayout() {
        // Update the title of the main activity layout to be the current month
        Calendar cal = Calendar.getInstance();
        String year = new SimpleDateFormat("yyyy").format(cal.getTime());
        String monthLong =  new SimpleDateFormat("MMMM").format(cal.getTime());
        String dateLong  = (monthLong +" "+year);
        TextView tv_title = (TextView)findViewById(R.id.tv_add_transaction);
        tv_title.setText(getString(R.string.add_transaction_title)+" "+dateLong);

        // Fill up multi auto complete with subcategories
        ArrayList<String> categoriesPlusSubcategories = new ArrayList<String>();
        for (int i=0;i<myCategories.length;i++ ) {
            for (String s : mySubCategories[i]) {
                categoriesPlusSubcategories.add(s+" : "+myCategories[i]);
            }
        }
        ArrayAdapter<String> categoriesAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line,categoriesPlusSubcategories);
        AutoCompleteTextView mactv = (AutoCompleteTextView)findViewById(R.id.autoCompleteTextView);
        mactv.setAdapter(categoriesAdapter);

        // Setup cancel button
        Button bCancel = (Button)findViewById(R.id.button_cancel);
        bCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // Setup ok button
        final Button bOk = (Button)findViewById(R.id.button_ok);
        bOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Collect the data entered by user
                AutoCompleteTextView actv = (AutoCompleteTextView)findViewById(R.id.autoCompleteTextView);
                EditText et_amount = (EditText)findViewById(R.id.et_amount);
                EditText et_description = (EditText)findViewById(R.id.et_description);

                if ( actv.getText() != null )
                    mSubCategory = actv.getText().toString();
                if ( et_amount.getText() != null)
                    mAmount = et_amount.getText().toString();
                if ( et_description.getText() != null)
                    mDescription = et_description.getText().toString();

                // We do not proceed if the user did not set up a category/subcategory and an amount as there
                // would be nothing to add to the spreadsheet.
                if ( !mSubCategory.isEmpty() && !mAmount.isEmpty() )
                    new GetAuthToken(myActivity, getEmail(), mSubCategory, mAmount, mDescription).execute();
                else
                    Toast.makeText( myActivity,
                            "Error: You need to select a category and an amount.",
                            Toast.LENGTH_LONG ).show();
            }
        });
    }

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        if ( requestCode == USER_RECOVERABLE_AUTH && resultCode == RESULT_OK ) {
            new GetAuthToken( this, getEmail(), mSubCategory, mAmount, mDescription ).execute();
        } else if ( requestCode == USER_RECOVERABLE_AUTH && resultCode == RESULT_CANCELED ) {
            Toast.makeText( this, "User rejected authorization.", Toast.LENGTH_SHORT ).show();
        }
    }

    /**
     * Retrieve email selected in the Settings or the main one if none has been selected.
     * @return the Google email address corresponding to the Google Account that should be used to fetch the spreadsheet
     */
    private String getEmail() {
        AccountManager accountManager = AccountManager.get( this );
        Account[] accounts = accountManager.getAccountsByType( GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE );

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences( this );
        // If no account has been selected in the settings we return the main one that stands in the 1st position
        return sp.getString( getString( R.string.select_google_account_key), accounts[0].name );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsView.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
