to-google-spreadsheet
=====================

Application example to write to a spreadsheet stored in Google Drive. It uses OAuth2 authentication to get a token to access user's spreadsheets.

This particular example is based on a particular spreadsheet to write to. I attached a sample of it in app/spreadsheetSample.xlsx.

If you only copy some of the source code make sure to add the dependencies to your build.gradle. I had some conflicts between the libraries imported by play-services and gdata:core, please check my build.gradle for the modules excluded when compiling gdata.
If you only copy some of the source code also make sure to update your Manifest (permissions and gms version)
