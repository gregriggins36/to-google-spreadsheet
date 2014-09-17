package com.greg.updatespreadsheet;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

/**
 * This AsyncTask is executed on click of the OK button in the main layout.
 * It tries to get a token to access the user's spreadsheets in her/his Google Drive account.
 * If a token is successfully obtained it will execute an AsyncTask to write to the
 * spreadsheet (done in MySpreadsheetIntegration).
 */
public class GetAuthToken extends AsyncTask<Void, Void, String> {
    private MyActivity mActivity;
    private String mEmail;
    private String mSubCategory;
    private String mAmount;
    private String mDescription;

    private static final String TAG = MyActivity.class.getSimpleName();
    private static final int USER_RECOVERABLE_AUTH = 5;

    public GetAuthToken( MyActivity mActivity, String mEmail, String subCategory, String amount, String desc ) {
        this.mActivity = mActivity;
        this.mEmail = mEmail;
        this.mSubCategory = subCategory;
        this.mAmount = amount;
        this.mDescription = desc;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            return GoogleAuthUtil.getToken(mActivity, mEmail, "oauth2:https://spreadsheets.google.com/feeds https://docs.google.com/feeds");
        } catch ( UserRecoverableAuthException userRecoverableException ) {
            mActivity.startActivityForResult( userRecoverableException.getIntent(),USER_RECOVERABLE_AUTH );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute( String result ) {
        // The token is successfully retrieved, we pass it to MySpreadsheetIntegration with the information
        // to write to the spreadsheet as well.
        if ( result != null && !result.isEmpty() ) {
            String[] param = {mSubCategory, mAmount, mDescription, result };
            new MySpreadsheetIntegration().execute(param);
        }
        else if ( result != null && result.isEmpty() )
            Log.d(TAG, "empty");
        else
            Log.d( TAG, "null" );
    }

}