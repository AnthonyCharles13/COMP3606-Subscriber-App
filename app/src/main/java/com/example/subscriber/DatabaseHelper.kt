package com.example.subscriber

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.getFloatOrNull

const val DB_NAME = "database.sql"
const val DB_VERSION = 1

class DatabaseHelper(context: Context, factory: SQLiteDatabase.CursorFactory?):SQLiteOpenHelper(context, DB_NAME,factory, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        val createLocationUpdatesTableQuery = ("CREATE TABLE LocationUpdates (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "stuid TEXT, " +
                "latitude REAL, " +
                "longitude REAL, " +
                "speed REAL, " +
                "dateTime TEXT, " +
                "timestamp INTEGER)")
        db.execSQL(createLocationUpdatesTableQuery)
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
        // Content similar to enterprise DB
    }

    fun insertLocationUpdate(locationData: LocationData) {
        val values = ContentValues()

        values.put("stuid", locationData.stuid)
        values.put("latitude", locationData.latitude)
        values.put("longitude", locationData.longitude)
        values.put("speed", locationData.speed)
        values.put("dateTime", locationData.dateTime)
        values.put("timestamp", System.currentTimeMillis())

        val db = this.writableDatabase
        db.insert("LocationUpdates", null, values)
        db.close()
    }

    fun getLast5MinutesUpdates(stuid: String): List<LocationData> {
        val result = mutableListOf<LocationData>()
        val db = this.readableDatabase

        val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)

        val cursor = db.rawQuery(
            "SELECT * FROM LocationUpdates WHERE stuid = ? AND timestamp >= ? ORDER BY timestamp DESC",
            arrayOf(stuid, fiveMinutesAgo.toString())
        )

        if (cursor.moveToFirst()) {
            do {
                val stuid = cursor.getString(cursor.getColumnIndexOrThrow("stuid"))
                val latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"))
                val longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))
                val speed = cursor.getFloatOrNull(cursor.getColumnIndexOrThrow("speed")) ?: 0f
                val dateTime = cursor.getString(cursor.getColumnIndexOrThrow("dateTime"))

                result.add(LocationData(stuid, latitude, longitude, speed, dateTime))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return result
    }

    fun getMinMaxSpeeds(stuid: String): Pair<Float, Float>? {
        val db = this.readableDatabase
        val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)

        val cursor = db.rawQuery(
            "SELECT MIN(speed) AS minSpeed, MAX(speed) AS maxSpeed FROM LocationUpdates WHERE stuid = ? AND timestamp >= ?",
            arrayOf(stuid, fiveMinutesAgo.toString())
        )

        var minSpeed: Float? = null
        var maxSpeed: Float? = null

        if (cursor.moveToFirst()) {
            minSpeed = cursor.getFloatOrNull(cursor.getColumnIndexOrThrow("minSpeed"))
            maxSpeed = cursor.getFloatOrNull(cursor.getColumnIndexOrThrow("maxSpeed"))
        }

        cursor.close()
        db.close()

        return if (minSpeed != null && maxSpeed != null) {
            Pair(minSpeed, maxSpeed)
        } else {
            null
        }
    }

//    fun getAllUpdatesByStudentID(stuid: String): List<LocationData> {
//        val result = mutableListOf<LocationData>()
//        val db = this.readableDatabase
//
//        // Query to fetch all updates for a given student
//        val cursor = db.rawQuery(
//            "SELECT * FROM LocationUpdates WHERE stuid = ? ORDER BY timestamp",
//            arrayOf(stuid)
//        )
//
//        if (cursor.moveToFirst()) {
//            do {
//                val latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"))
//                val longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))
//                val speed = cursor.getFloat(cursor.getColumnIndexOrThrow("speed"))
//                val dateTime = cursor.getString(cursor.getColumnIndexOrThrow("dateTime"))
//
//                result.add(LocationData(stuid, latitude, longitude, speed, dateTime))
//            } while (cursor.moveToNext())
//        }
//
//        cursor.close()
//        db.close()
//        return result
//    }

//    fun getIndiMinMaxSpeeds(stuid: String): Pair<Float, Float>? {
//        val db = this.readableDatabase
//
//        val cursor = db.rawQuery(
//            "SELECT MIN(speed) AS minSpeed, MAX(speed) AS maxSpeed FROM LocationUpdates WHERE stuid = ?",
//            arrayOf(stuid)
//        )
//
//        var minSpeed: Float? = null
//        var maxSpeed: Float? = null
//
//        if (cursor.moveToFirst()) {
//            minSpeed = cursor.getFloatOrNull(cursor.getColumnIndexOrThrow("minSpeed"))
//            maxSpeed = cursor.getFloatOrNull(cursor.getColumnIndexOrThrow("maxSpeed"))
//        }
//
//        cursor.close()
//        db.close()
//
//        return if (minSpeed != null && maxSpeed != null) {
//            Pair(minSpeed, maxSpeed)
//        } else {
//            null
//        }
//    }

}