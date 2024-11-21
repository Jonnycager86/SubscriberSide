package com.example.mapslabdemo

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.android.gms.maps.model.LatLng

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "LocationData.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_NAME = "locations"
        const val COLUMN_ID = "id"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_STUDENT_ID = "student_id"
        const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create the locations table
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_LATITUDE REAL NOT NULL,
                $COLUMN_LONGITUDE REAL NOT NULL,
                $COLUMN_STUDENT_ID TEXT NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL
            )
        """
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop the table if it exists and recreate it
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // Insert location data into the table
    fun insertLocation(latitude: Double, longitude: Double, studentId: String, timestamp: Long): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_LATITUDE, latitude)
            put(COLUMN_LONGITUDE, longitude)
            put(COLUMN_STUDENT_ID, studentId)
            put(COLUMN_TIMESTAMP, timestamp)
        }
        return db.insert(TABLE_NAME, null, values)
    }

    // Retrieve all locations as a list of CustomMarkerPoints
    fun getAllLocations(): List<CustomMarkerPoints> {
        val db = readableDatabase
        val cursor = db.query(TABLE_NAME, null, null, null, null, null, null)
        val locations = mutableListOf<CustomMarkerPoints>()

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE))
            val longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE))
            val studentId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STUDENT_ID))
            val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))

            val point = LatLng(latitude, longitude)
            locations.add(CustomMarkerPoints(id, point, studentId, timestamp))
        }
        cursor.close()
        return locations
    }

    // Optional: Clear all locations (for testing or debugging)
    fun clearAllLocations() {
        val db = writableDatabase
        db.delete(TABLE_NAME, null, null)
    }
}
