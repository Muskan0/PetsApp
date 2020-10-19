package com.example.android.pets.Data;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import com.example.android.pets.Data.PetContract.PetEntry;
import com.example.android.pets.Data.PetDbHelper;

/**
 * {@link ContentProvider} for Pets app.
 */
public class PetProvider extends ContentProvider {
    /** URI matcher code for the content URI for the pets table */


    private static final int PETS = 100;

    /** URI matcher code for the content URI for a single pet in the pets table */

    private static final int PET_ID = 101;

    /**

     * UriMatcher object to match a content URI to a corresponding code.

     * The input passed into the constructor represents the code to return for the root URI.

     * It's common to use NO_MATCH as the input for this case.

     */

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

// Static initializer. This is run the first time anything is called from this class.

    static {

        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS, PETS);
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS + "/#" , PET_ID);

    }
    /** Tag for the log messages */
    public static final String LOG_TAG = PetProvider.class.getSimpleName();
    /* Database helper object */
    private PetDbHelper mDbHelper;
    /**
     * Initialize the provider and the database helper object.
     */
    @Override
    public boolean onCreate() {
        mDbHelper= new PetDbHelper(getContext());
        return true;
    }
    /**
     * Perform the query for the given URI. Use the given projection, selection, selection arguments, and sort order.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,String[] selectionArgs, String sortOrder)
    {
//Get readable database
        SQLiteDatabase database= mDbHelper.getReadableDatabase();
//This cursor will hold the result of the query
        Cursor cursor;
// Figure out if the URI matcher can match the URI to a specific code
        int match= sUriMatcher.match(uri);
        switch(match){
            case PETS:
                cursor = database.query(PetEntry.TABLE_NAME, projection,  selection, selectionArgs, null, null, sortOrder);

                break;
            case PET_ID:
                selection= PetEntry._ID+"=?";
                selectionArgs= new String[] { String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(PetEntry.TABLE_NAME, projection,  selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(),uri);
        return cursor;
    }/**
     * Insert new data into the provider with the given ContentValues.
     */
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match= sUriMatcher.match(uri);
        switch(match)
        {
            case PETS:
                return insertPet(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for "  +uri);
        }
    }

    private Uri insertPet(Uri uri, ContentValues contentValues){
        String name = contentValues.getAsString(PetEntry.COLUMN_PET_NAME);
        if(name == null){
            throw new IllegalArgumentException("Pet requires a name");
        }
        Integer gender = contentValues.getAsInteger(PetEntry.COLUMN_PET_GENDER);
        if(gender==null || !PetEntry.isValidGender(gender)){
            throw new IllegalArgumentException("Pet requires valid gender");
        }
        Integer weight= contentValues.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);
        if(weight!=null && weight<0){
            throw new IllegalArgumentException("Pet requires valid weight");
        }
//Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        long id= database.insert(PetEntry.TABLE_NAME, null, contentValues);
        if(id==-1){
            Log.e(LOG_TAG, "Failed to insert row for" + uri);
            return null;
        }
        getContext().getContentResolver().notifyChange(uri,null);
//return the new URI with the ID(of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(uri, id);
    }
    /**
     * Updates the data at the given selection and selection arguments, with the new ContentValues.
     */
    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return updatePet(uri, contentValues, selection, selectionArgs);
            case PET_ID:
                // For the PET_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updatePet(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }
    private int updatePet(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        if (values.containsKey(PetEntry.COLUMN_PET_NAME)){
            String name= values.getAsString(PetEntry.COLUMN_PET_NAME);
            if(name==null){
                throw new IllegalArgumentException("Pet requires a name");
            }
        }
        if(values.containsKey(PetEntry.COLUMN_PET_GENDER)){
            Integer gender= values.getAsInteger(PetEntry.COLUMN_PET_GENDER);
            if(gender==null || !PetEntry.isValidGender(gender)){
                throw new IllegalArgumentException("Pet requires a valid gender");
            }
        }
        if(values.containsKey(PetEntry.COLUMN_PET_WEIGHT)){
            Integer weight= values.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);
            if(weight!=null && weight<0){
                throw new IllegalArgumentException("Pet requires a valid weight");
            }
        }
        //If there are no values on the database don't try to update the database
        if(values.size()==0){
            return 0;
        }
        SQLiteDatabase database= mDbHelper.getWritableDatabase();
        int rowsUpdated= database.update(PetEntry.TABLE_NAME, values, selection,selectionArgs);
        if(rowsUpdated!=0){
            getContext().getContentResolver().notifyChange(uri,null);
        }
        return rowsUpdated;
    }

    /**
     * Delete the data at the given selection and selection arguments.
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int rowsDeleted;
        SQLiteDatabase database= mDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match){
            case PETS:
                rowsDeleted=database.delete(PetEntry.TABLE_NAME,selection,selectionArgs);
                break;
            case PET_ID:
                selection= PetEntry._ID+"=?";
                selectionArgs= new String[] {String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted=database.delete(PetEntry.TABLE_NAME, selection, selectionArgs);
                break;
                default:
                    throw new IllegalArgumentException("Deletion is not supported for "+ uri);
        }
        if(rowsDeleted!=0){
            getContext().getContentResolver().notifyChange(uri,null);
        }
        return rowsDeleted;
    }
    /**
     * Returns the MIME type of data for the content URI.
     */
    @Override
    public String getType(Uri uri) {
        final int match= sUriMatcher.match(uri);
        switch (match){
            case PETS:
                return  PetEntry.CONTENT_LIST_TYPE;
            case PET_ID:
                return PetEntry.CONTENT_ITEM_TYPE;
                default:
                    throw new IllegalArgumentException("Unknown URI "+ uri+" with match "+match);
        }
    }
}
