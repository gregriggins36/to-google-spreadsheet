package com.greg.updatespreadsheet;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.util.ServiceException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This AsyncTask is executed when a token is successfully retrieved from GetAuthToken.
 * It get the spreadsheet and worksheet specified by the user and write the information specified
 * in the main activity.
 */
public class MySpreadsheetIntegration extends AsyncTask<String,Void,Void> {
    private ProgressDialog dialog;
    private String mSpreadSheetName;
    private Map<String,String> mValues = new HashMap<String, String>();
    private boolean spreadsheetSet = true;

    private static final String APPLICATION_NAME = "UpdateSpreadsheet";
    private static final String TAG = MySpreadsheetIntegration.class.getSimpleName()+" Tag ID";
    private static final String GOOGLE_SPREADSHEETS_FEED_URL = "https://spreadsheets.google.com/feeds/spreadsheets/private/full";

    @Override
    protected void onPostExecute(Void nothing) {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @Override
    protected Void doInBackground( String... strings ) {
        // We do not write to the spreadsheet if none is setup as that would generate an error.
        if ( spreadsheetSet ) {
            // Retrieve arguments passed by GetAuthToken and store them in mValues for further use.
            String subCategory = strings[0];
            String amount = strings[1];
            String description = strings[2];

            String category = subCategory.split(" : ")[1];
            subCategory = subCategory.split(" : ")[0];

            mValues.put("subCategory", subCategory);
            mValues.put("category", category);
            mValues.put("amount", amount);
            mValues.put("description", description);

            try {
                // Create a SpreadsheetService to access the Google Spreadsheets data API
                SpreadsheetService service = new SpreadsheetService(APPLICATION_NAME);
                service.setProtocolVersion(SpreadsheetService.Versions.V3);
                service.setAuthSubToken(strings[3]);

                // Fetch the spreadsheet
                SpreadsheetEntry spreadsheet = getSpreadsheet(service);
                // Fetch the cells from the spreadsheet
                List<CellEntry> cells = getCells(spreadsheet, service);

                if (cells == null)
                    cannotGetCells();
                else
                    writeToSpreadsheet(cells);
            } catch (ServiceException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        Activity activity = MyActivity.myActivity;
        dialog = new ProgressDialog(activity);

        // Retrieve the name of the Spreadsheet if one is specified
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences( activity );
        mSpreadSheetName = sp.getString( activity.getString(R.string.select_sheet_key), "" );
        if ( mSpreadSheetName.equals("") ) {
            Toast.makeText(MyActivity.myActivity,
                           "Error: You did not setup the name of the spreadsheet to work with. Please go to settings.",
                           Toast.LENGTH_LONG).show();
            spreadsheetSet = false;
        }

        // Display progress message to let the user know the app is doing things!
        // It also prevent the user from clicking the OK button multiple times
        // which would result in creating the same amount of AsyncTask
        dialog.setMessage("Posting entries to spreadsheet, please wait...");
        dialog.show();
    }

    /**
     * Retrieve the spreadsheet specified by mSpreadsheetName.
     * @param service SpreadsheetService obtained with token to manipulate Google spreadsheets (created in doInBackground)
     * @return the spreadsheet specified by the user in Settings.
     * @throws IOException
     * @throws ServiceException
     */
    private SpreadsheetEntry getSpreadsheet( SpreadsheetService service )
            throws IOException, ServiceException {
        // Generate a URL that returns meta data for spreadsheets
        URL metafeedUrl = new URL( GOOGLE_SPREADSHEETS_FEED_URL );

        // Fetch spreadsheet feed
        SpreadsheetFeed spreadsheetFeed = service.getFeed(metafeedUrl, SpreadsheetFeed.class);

        // Find desired spreadsheet (identified by mSpreadSheetName) from list of spreadsheets
        List<SpreadsheetEntry> spreadsheets = spreadsheetFeed.getEntries();
        SpreadsheetEntry spreadsheet = null;
        for ( SpreadsheetEntry entry : spreadsheets ) {
            if ( entry.getTitle().getPlainText().equals(mSpreadSheetName) ) {
                spreadsheet = entry;
                break;
            }
        }

        return spreadsheet;
    }

    /**
     * Retrieve the cells of the worksheet designated by the current month from the spreadsheet
     * specified in the parameter spreadsheet argument.
     * For the specifics of my example, the empty cells of the sheet are retrieved as well.
     * @param spreadsheet the spreadsheet to read the cells from (obtained by running getSpreadsheet)
     * @param service SpreadsheetService obtained with token to manipulate Google spreadsheets (created in doInBackground)
     * @return the cells from the spreadsheet
     * @throws IOException
     * @throws URISyntaxException
     * @throws ServiceException
     */
    private List<CellEntry> getCells( SpreadsheetEntry spreadsheet, SpreadsheetService service )
            throws IOException, URISyntaxException, ServiceException {
        // Get all the worksheets from the spreadsheet
        WorksheetFeed worksheetFeed = service.getFeed(spreadsheet.getWorksheetFeedUrl(), WorksheetFeed.class);
        List<WorksheetEntry> worksheets = worksheetFeed.getEntries();

        // Fetch the desired worksheet (here corresponds to the current month)
        WorksheetEntry worksheet = null;
        Calendar cal = Calendar.getInstance();
        String monthShort =  new SimpleDateFormat("MMM").format(cal.getTime()).toLowerCase();
        for (WorksheetEntry entry : worksheets) {
            String worksheetTitle = entry.getTitle().getPlainText().toLowerCase();
            if ( worksheetTitle.startsWith(monthShort) ) {
                worksheet = entry;
                break;
            }
        }

        // Fetch the cell feed of the worksheet.
        if ( worksheet == null )
            return null;
        else {
            // For particular reasons linked to this example we also fetch the empty cells of the sheet
            // remove + "?return-empty=true" if you do not need those.
            URL cellFeedUrl = new URI(worksheet.getCellFeedUrl().toString() + "?return-empty=true").toURL();
            CellFeed cellFeed = service.getFeed(cellFeedUrl, CellFeed.class);

            return cellFeed.getEntries();
        }
    }

    /**
     * This method is specifically written to serve the purpose of my particular example.
     * It needs to be adapted to specific spreadsheets.
     * @param cells the cells of the spreadsheets, for the particular reasons of my example the empty cells
     *              are returned as well (see getCells function above if you need to modify that)
     * @return null
     * @throws IOException
     * @throws ServiceException
     */
    private Void writeToSpreadsheet( List<CellEntry> cells )
            throws IOException, ServiceException {
        String titleOfCellToUpdate = null;
        String titleOfCellDescription = null;
        boolean categoryFound = false;

        String columnOfCategory = "";

        final String amount = mValues.get( "amount" );
        String description  = mValues.get( "description" );

        // Iterate through each cell.
        for ( final CellEntry cell : cells ) {
            final String cellValue = cell.getCell().getInputValue();
            final String cellTitle = cell.getTitle().getPlainText();

            // We found the category (e.g. : HOME EXPENSES)
            if( cellValue.equals(mValues.get("category")) ) {
                categoryFound = true;
                columnOfCategory = cellTitle.substring(0, 1);
            }

            // We found the category, we now look for the subcategory and its column,
            // by doing so we solve the problem of subcategories present in more than one category
            if ( categoryFound ) {
                if ( cellValue.equals(mValues.get("subCategory")) && cellTitle.substring(0, 1).equals(columnOfCategory) ) {
                    // We found the correct subcategory, we want to locate the field under "Actual"
                    char column = (cellTitle.substring(0, 1)).toCharArray()[0];
                    String row  = cellTitle.substring(1);
                    String columnToEdit = String.valueOf((char) (column + 2));
                    titleOfCellToUpdate = columnToEdit + row;

                    // We get the field next to "Difference" to use as a description field
                    String columnForDescription = String.valueOf((char) (column + 4));
                    titleOfCellDescription = columnForDescription + row;
                }
            }
            // Update the content of the cell for the column "Actual"
            if ( titleOfCellToUpdate != null && cellTitle.equals(titleOfCellToUpdate) ) {
                if ( cellValue.equals("") ) // The cell is empty we want to add the = character first
                    cell.changeInputValueLocal("="+amount);
                else if ( !cellValue.startsWith("=") ) // The cell is not empty but doesn't start with '=' we add it
                    cell.changeInputValueLocal("="+cellValue+"+"+amount);
                else
                    cell.changeInputValueLocal(cellValue+"+"+amount);

                cell.update();

                MyActivity.myActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText( MyActivity.myActivity, amount + " added to " + cellValue, Toast.LENGTH_LONG ).show();
                    }
                });
            }

            // Update the cell with a description
            if ( titleOfCellDescription != null && cellTitle.equals(titleOfCellDescription) ) {
                if ( cellValue.isEmpty() )
                    cell.changeInputValueLocal(description);
                else
                    cell.changeInputValueLocal(cellValue+","+description);

                cell.update();

                return null;
            }
        }
        return null;
    }

    /**
     * Display error to the user. Needs to run this on a UI thread as doInBackground does not run in a UI thread
     * and as such cannot display UI widgets such as a Toast.
     */
    private void cannotGetCells() {
        MyActivity.myActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MyActivity.myActivity, "Error: cannot get cells from spreadsheet. Nothing was written to the spreadsheet", Toast.LENGTH_LONG).show();
            }
        });
    }
}