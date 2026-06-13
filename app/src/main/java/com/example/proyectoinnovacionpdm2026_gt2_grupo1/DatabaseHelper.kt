package com.example.proyectoinnovacionpdm2026_gt2_grupo1

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.text.Normalizer

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "traductor_v82.db", null, 82) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE traducciones (
                id INTEGER PRIMARY KEY AUTOINCREMENT, 
                idioma_origen TEXT, 
                palabra_origen TEXT, 
                palabra_espanol TEXT,
                palabra_busqueda TEXT
            )
        """.trimIndent())
        insertarDatosIniciales(db)
        Log.d("DatabaseHelper", "Base de datos v82 creada")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS traducciones")
        onCreate(db)
    }

    // Normalización idéntica a la de la Activity para evitar errores de búsqueda
    fun normalizar(texto: String?): String {
        if (texto == null) return ""
        val temp = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return temp.replace("[\\u0300-\\u036f]".toRegex(), "")
            .replace("[^a-zA-Z0-9\\s]".toRegex(), "")
            .lowercase()
            .trim()
    }

    private fun insertarDatosIniciales(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            // FRANCÉS (VERBOS Y FRASES CLAVE)
            val fr = "frances"
            insertarFila(db, fr, "Comprends", "Entiendo / Comprendo")
            insertarFila(db, fr, "Bonjour", "Buenos días / Hola")
            insertarFila(db, fr, "Merci", "Gracias")
            insertarFila(db, fr, "S'il vous plaît", "Por favor")
            insertarFila(db, fr, "Eau", "Agua")
            insertarFila(db, fr, "Manger", "Comer")
            insertarFila(db, fr, "Ami", "Amigo")
            insertarFila(db, fr, "École", "Escuela")
            insertarFila(db, fr, "Je comprends", "Yo entiendo")
            insertarFila(db, fr, "Bien", "Bien")

            // INGLÉS (AMPLIADO)
            val en = "ingles"
            insertarFila(db, en, "Understand", "Entender")
            insertarFila(db, en, "Hello", "Hola")
            insertarFila(db, en, "Good morning", "Buenos días")
            insertarFila(db, en, "Thank you", "Gracias")
            insertarFila(db, en, "School", "Escuela")
            insertarFila(db, en, "Water", "Agua")
            insertarFila(db, en, "Friend", "Amigo")

            // NÁHUAT (BASE)
            val nh = "nahuat"
            insertarFila(db, nh, "Yek peyna", "Buenos días")
            insertarFila(db, nh, "Padiush", "Gracias")
            insertarFila(db, nh, "At", "Agua")
            insertarFila(db, nh, "Mistun", "Gato")
            insertarFila(db, nh, "Chuchut", "Perro")

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun insertarFila(db: SQLiteDatabase, idioma: String, origen: String, espanol: String) {
        val values = ContentValues().apply {
            put("idioma_origen", normalizar(idioma))
            put("palabra_origen", origen.trim())
            put("palabra_espanol", espanol.trim())
            put("palabra_busqueda", normalizar(origen))
        }
        db.insert("traducciones", null, values)
    }
}
