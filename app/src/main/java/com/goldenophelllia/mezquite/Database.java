package com.goldenophelllia.mezquite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Database extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "el_mezquite.db";
    private static final int DATABASE_VERSION = 1;

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tabla Clientes
        String createClientes = "CREATE TABLE CLIENTES (" +
                "cliente_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT," +
                "telefono TEXT," +
                "email TEXT," +
                "fecha_registro TEXT)";
        db.execSQL(createClientes);

        // Tabla Proveedores
        String createProveedores = "CREATE TABLE PROVEEDORES (" +
                "proveedor_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT," +
                "contacto TEXT," +
                "telefono TEXT," +
                "email TEXT," +
                "direccion TEXT)";
        db.execSQL(createProveedores);

        // Tabla Categorías
        String createCategorias = "CREATE TABLE CATEGORIAS (" +
                "categoria_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT NOT NULL," +
                "descripcion TEXT)";
        db.execSQL(createCategorias);

        // Insertar categorías básicas
        db.execSQL("INSERT INTO CATEGORIAS (nombre, descripcion) VALUES ('Pescados', 'Productos de pescado')");
        db.execSQL("INSERT INTO CATEGORIAS (nombre, descripcion) VALUES ('Ceviches', 'Ceviches varios')");
        db.execSQL("INSERT INTO CATEGORIAS (nombre, descripcion) VALUES ('Bebidas', 'Refrescos y bebidas')");

        // Tabla Productos
        String createProductos = "CREATE TABLE PRODUCTOS (" +
                "producto_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT NOT NULL," +
                "categoria_id INTEGER," +
                "precio_base REAL," +
                "unidad_medida TEXT," +
                "activo INTEGER DEFAULT 1," +
                "FOREIGN KEY (categoria_id) REFERENCES CATEGORIAS(categoria_id))";
        db.execSQL(createProductos);

        // Tabla Ventas
        String createVentas = "CREATE TABLE VENTAS (" +
                "venta_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "cliente_id INTEGER," +
                "fecha_venta TEXT," +
                "total_venta REAL," +
                "tipo_venta TEXT," +
                "estado TEXT DEFAULT 'pendiente'," +
                "FOREIGN KEY (cliente_id) REFERENCES CLIENTES(cliente_id))";
        db.execSQL(createVentas);

        // Tabla Detalle Venta
        String createDetalleVenta = "CREATE TABLE DETALLE_VENTA (" +
                "detalle_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "venta_id INTEGER," +
                "producto_id INTEGER," +
                "cantidad REAL," +
                "peso REAL," +
                "precio_unitario REAL," +
                "subtotal REAL," +
                "notas TEXT," +
                "FOREIGN KEY (venta_id) REFERENCES VENTAS(venta_id)," +
                "FOREIGN KEY (producto_id) REFERENCES PRODUCTOS(producto_id))";
        db.execSQL(createDetalleVenta);

        // Tabla Materia Prima
        String createMateriaPrima = "CREATE TABLE MATERIA_PRIMA (" +
                "materia_prima_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT NOT NULL," +
                "proveedor_id INTEGER," +
                "precio_compra REAL," +
                "unidad_medida TEXT," +
                "stock_actual REAL," +
                "stock_minimo REAL," +
                "FOREIGN KEY (proveedor_id) REFERENCES PROVEEDORES(proveedor_id))";
        db.execSQL(createMateriaPrima);

        //  materia prima de ejemplo
        ContentValues materia1 = new ContentValues();
        materia1.put("nombre", "Mojarra Fresca");
        materia1.put("proveedor_id", 1);
        materia1.put("precio_compra", 85.0);
        materia1.put("unidad_medida", "kg");
        materia1.put("stock_actual", 25.5);
        materia1.put("stock_minimo", 10.0);
        db.insert("MATERIA_PRIMA", null, materia1);

        ContentValues materia2 = new ContentValues();
        materia2.put("nombre", "Camarón");
        materia2.put("proveedor_id", 1);
        materia2.put("precio_compra", 120.0);
        materia2.put("unidad_medida", "kg");
        materia2.put("stock_actual", 8.0);
        materia2.put("stock_minimo", 5.0);
        db.insert("MATERIA_PRIMA", null, materia2);

        ContentValues materia3 = new ContentValues();
        materia3.put("nombre", "Limón");
        materia3.put("proveedor_id", 2); // Carnicería La Especial
        materia3.put("precio_compra", 25.0);
        materia3.put("unidad_medida", "kg");
        materia3.put("stock_actual", 3.0);
        materia3.put("stock_minimo", 2.0);
        db.insert("MATERIA_PRIMA", null, materia3);

        ContentValues materia4 = new ContentValues();
        materia4.put("nombre", "Coca-Cola");
        materia4.put("proveedor_id", 3); // Bebidas Refrescantes S.A.
        materia4.put("precio_compra", 15.0);
        materia4.put("unidad_medida", "piezas");
        materia4.put("stock_actual", 48);
        materia4.put("stock_minimo", 24);
        db.insert("MATERIA_PRIMA", null, materia4);

        // proveedores de ejemplo:
        ContentValues proveedor1 = new ContentValues();
        proveedor1.put("nombre", "Pescados Frescos del Mar");
        proveedor1.put("contacto", "María González");
        proveedor1.put("telefono", "5559876543");
        proveedor1.put("email", "pescados@delmar.com");
        proveedor1.put("direccion", "Av. del Mar #456, Puerto");
        db.insert("PROVEEDORES", null, proveedor1);

        ContentValues proveedor2 = new ContentValues();
        proveedor2.put("nombre", "Carnicería La Especial");
        proveedor2.put("contacto", "Roberto Martínez");
        proveedor2.put("telefono", "5558765432");
        proveedor2.put("email", "carnes@laespecial.com");
        proveedor2.put("direccion", "Calle Hidalgo #789, Centro");
        db.insert("PROVEEDORES", null, proveedor2);

        ContentValues proveedor3 = new ContentValues();
        proveedor3.put("nombre", "Bebidas Refrescantes S.A.");
        proveedor3.put("contacto", "Laura Ramírez");
        proveedor3.put("telefono", "5557654321");
        proveedor3.put("email", "ventas@bebidasrefrescantes.com");
        proveedor3.put("direccion", "Blvd. Industrial #321, Zona Industrial");
        db.insert("PROVEEDORES", null, proveedor3);

        // Insertar productos básicos
        ContentValues valores = new ContentValues();
        valores.put("nombre", "Mojarra Preparada");
        valores.put("categoria_id", 1);
        valores.put("precio_base", 220.0);
        valores.put("unidad_medida", "kg");
        db.insert("PRODUCTOS", null, valores);

        valores.clear();
        valores.put("nombre", "Ceviche Tostada");
        valores.put("categoria_id", 2);
        valores.put("precio_base", 15.0);
        valores.put("unidad_medida", "pieza");
        db.insert("PRODUCTOS", null, valores);

        valores.clear();
        valores.put("nombre", "Refresco");
        valores.put("categoria_id", 3);
        valores.put("precio_base", 20.0);
        valores.put("unidad_medida", "pieza");
        db.insert("PRODUCTOS", null, valores);


        valores.clear();
        valores.put("nombre", "Tostada de Hueva");
        valores.put("categoria_id", 2); // Ceviches
        valores.put("precio_base", 20.0);
        valores.put("unidad_medida", "pieza");
        db.insert("PRODUCTOS", null, valores);

        valores.clear();
        valores.put("nombre", "Ceviche por Kilo");
        valores.put("categoria_id", 2); // Ceviches
        valores.put("precio_base", 120.0);
        valores.put("unidad_medida", "kg");
        db.insert("PRODUCTOS", null, valores);



    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS DETALLE_VENTA");
        db.execSQL("DROP TABLE IF EXISTS VENTAS");
        db.execSQL("DROP TABLE IF EXISTS PRODUCTOS");
        db.execSQL("DROP TABLE IF EXISTS CATEGORIAS");
        db.execSQL("DROP TABLE IF EXISTS MATERIA_PRIMA");
        db.execSQL("DROP TABLE IF EXISTS PROVEEDORES");
        db.execSQL("DROP TABLE IF EXISTS CLIENTES");
        onCreate(db);
    }

    // MÉTODOS PARA CLIENTES

    public long insertarCliente(String nombre, String telefono, String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("nombre", nombre);
        values.put("telefono", telefono);
        values.put("email", email);
        values.put("fecha_registro", getFechaActual());

        long resultado = db.insert("CLIENTES", null, values);
        db.close();
        return resultado;
    }

    public Cursor obtenerClientes() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT cliente_id as _id, nombre, telefono, email, fecha_registro " +
                "FROM CLIENTES ORDER BY nombre ASC", null);
    }

    public Cursor buscarClientes(String texto) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT cliente_id as _id, nombre, telefono, email, fecha_registro " +
                "FROM CLIENTES WHERE nombre LIKE ? OR telefono LIKE ? " +
                "ORDER BY nombre ASC";
        String likeText = "%" + texto + "%";
        return db.rawQuery(query, new String[]{likeText, likeText});
    }

    public int actualizarCliente(int id, String nombre, String telefono, String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("nombre", nombre);
        values.put("telefono", telefono);
        values.put("email", email);

        int resultado = db.update("CLIENTES", values, "cliente_id = ?",
                new String[]{String.valueOf(id)});
        db.close();
        return resultado;
    }

    public Cursor obtenerCategorias() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT categoria_id as _id, nombre FROM CATEGORIAS ORDER BY nombre", null);
    }

    // Para actualizar estado de producto
    public int actualizarEstadoProducto(int productoId, int activo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("activo", activo);
        return db.update("PRODUCTOS", values, "producto_id = ?",
                new String[]{String.valueOf(productoId)});
    }

    public int eliminarCliente(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int resultado = db.delete("CLIENTES", "cliente_id = ?",
                new String[]{String.valueOf(id)});
        db.close();
        return resultado;
    }

    public Cursor obtenerClientePorId(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM CLIENTES WHERE cliente_id = ?",
                new String[]{String.valueOf(id)});
    }

    public Cursor obtenerClientesParaSpinner() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT cliente_id, nombre FROM CLIENTES ORDER BY nombre", null);
    }

    // MÉTODOS PARA PRODUCTOS

    public Cursor obtenerProductos() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT producto_id as _id, nombre, precio_base, unidad_medida " +
                "FROM PRODUCTOS WHERE activo = 1 ORDER BY nombre", null);
    }

    public Cursor obtenerProductoPorId(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM PRODUCTOS WHERE producto_id = ?",
                new String[]{String.valueOf(id)});
    }

    // MÉTODOS PARA VENTAS

    public long insertarVenta(int clienteId, double total, String tipoVenta) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("cliente_id", clienteId > 0 ? clienteId : null);
        values.put("fecha_venta", getFechaHoraActual());
        values.put("total_venta", total);
        values.put("tipo_venta", tipoVenta);
        values.put("estado", "pagada");

        long ventaId = db.insert("VENTAS", null, values);
        db.close();
        return ventaId;
    }

    public long insertarDetalleVenta(long ventaId, int productoId, double cantidad,
                                     double peso, double precioUnitario, double subtotal) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("venta_id", ventaId);
        values.put("producto_id", productoId);
        values.put("cantidad", cantidad);
        values.put("peso", peso);
        values.put("precio_unitario", precioUnitario);
        values.put("subtotal", subtotal);

        long resultado = db.insert("DETALLE_VENTA", null, values);
        db.close();
        return resultado;
    }



    // Metodo para obtener ventas por rango de fechas
    public Cursor obtenerVentasPorFecha(String fechaDesde, String fechaHasta) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT v.venta_id, v.fecha_venta, v.total_venta, " +
                "COALESCE(c.nombre, 'Cliente General') as cliente " +
                "FROM VENTAS v " +
                "LEFT JOIN CLIENTES c ON v.cliente_id = c.cliente_id " +
                "WHERE DATE(v.fecha_venta) BETWEEN ? AND ? " +
                "ORDER BY v.fecha_venta DESC";
        return db.rawQuery(query, new String[]{fechaDesde, fechaHasta});
    }

    // Metodo para obtener detalles de una venta
    public Cursor obtenerDetalleVenta(int ventaId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT p.nombre, dv.cantidad, dv.peso, dv.precio_unitario, dv.subtotal " +
                "FROM DETALLE_VENTA dv " +
                "JOIN PRODUCTOS p ON dv.producto_id = p.producto_id " +
                "WHERE dv.venta_id = ?";
        return db.rawQuery(query, new String[]{String.valueOf(ventaId)});
    }

    // Metodo para obtener totales por período
    public double[] obtenerTotalesVentas(String fechaDesde, String fechaHasta) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*), COALESCE(SUM(total_venta), 0) " +
                "FROM VENTAS " +
                "WHERE DATE(fecha_venta) BETWEEN ? AND ?";

        Cursor cursor = db.rawQuery(query, new String[]{fechaDesde, fechaHasta});

        double[] resultados = new double[2];
        if (cursor.moveToFirst()) {
            resultados[0] = cursor.getInt(0); // Cantidad de ventas
            resultados[1] = cursor.getDouble(1); // Monto total
        }

        cursor.close();
        db.close();
        return resultados;
    }
    // Metodo para obtener todos los proveedores
    public Cursor obtenerProveedores() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM PROVEEDORES ORDER BY nombre ASC", null);
    }

    // Metodo para buscar proveedores
    public Cursor buscarProveedores(String texto) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM PROVEEDORES WHERE nombre LIKE ? OR contacto LIKE ? OR telefono LIKE ? ORDER BY nombre ASC";
        String likeText = "%" + texto + "%";
        return db.rawQuery(query, new String[]{likeText, likeText, likeText});
    }

    // Metodo para insertar proveedor
    public long insertarProveedor(String nombre, String contacto, String telefono, String email, String direccion) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("nombre", nombre);
        values.put("contacto", contacto);
        values.put("telefono", telefono);
        values.put("email", email);
        values.put("direccion", direccion);

        long resultado = db.insert("PROVEEDORES", null, values);
        db.close();
        return resultado;
    }



    private String getFechaActual() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date());
    }

    private String getFechaHoraActual() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        return sdf.format(new Date());
    }
}