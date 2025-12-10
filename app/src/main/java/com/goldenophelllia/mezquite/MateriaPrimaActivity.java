package com.goldenophelllia.mezquite;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashMap;

public class MateriaPrimaActivity extends AppCompatActivity {

    private ListView listViewMateriaPrima;
    private EditText etBuscarMateria;
    private Button btnNuevaMateria, btnVolver, btnTodoStock, btnStockBajo, btnStockCritico;
    private TextView tvTotalItems, tvStockBajo, tvStockCritico;

    private Database dbHelper;
    private SimpleAdapter adapter;
    private ArrayList<HashMap<String, String>> listaMateria;
    private int filtroStock = 0; // 0=Todos, 1=Bajo, 2=Crítico

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_materia_prima);

        // Inicializar vistas
        listViewMateriaPrima = findViewById(R.id.listViewMateriaPrima);
        etBuscarMateria = findViewById(R.id.etBuscarMateria);
        btnNuevaMateria = findViewById(R.id.btnNuevaMateria);
        btnVolver = findViewById(R.id.btnVolverMateria);
        btnTodoStock = findViewById(R.id.btnTodoStock);
        btnStockBajo = findViewById(R.id.btnStockBajo);
        btnStockCritico = findViewById(R.id.btnStockCritico);
        tvTotalItems = findViewById(R.id.tvTotalItems);
        tvStockBajo = findViewById(R.id.tvStockBajo);
        tvStockCritico = findViewById(R.id.tvStockCritico);

        // Inicializar base de datos
        dbHelper = new Database(this);

        // Configurar botones
        btnNuevaMateria.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialogoMateriaPrima(null);
            }
        });

        btnVolver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnTodoStock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filtroStock = 0;
                actualizarFiltros();
                cargarMateriaPrima();
            }
        });

        btnStockBajo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filtroStock = 1;
                actualizarFiltros();
                cargarMateriaPrima();
            }
        });

        btnStockCritico.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filtroStock = 2;
                actualizarFiltros();
                cargarMateriaPrima();
            }
        });

        // Configurar búsqueda
        etBuscarMateria.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarMateriaPrima(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Configurar lista
        configurarListView();

        // Cargar materia prima inicial
        cargarMateriaPrima();

        // Configurar clic largo para editar/eliminar
        listViewMateriaPrima.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mostrarMenuOpcionesMateria(position);
                return true;
            }
        });

        // Actualizar colores de filtros
        actualizarFiltros();
    }

    private void actualizarFiltros() {
        // Restablecer todos los botones
        btnTodoStock.setBackgroundColor(Color.parseColor("#9E9E9E"));
        btnStockBajo.setBackgroundColor(Color.parseColor("#FF9800"));
        btnStockCritico.setBackgroundColor(Color.parseColor("#F44336"));

        // Resaltar el botón seleccionado
        switch (filtroStock) {
            case 0:
                btnTodoStock.setBackgroundColor(Color.parseColor("#616161"));
                break;
            case 1:
                btnStockBajo.setBackgroundColor(Color.parseColor("#F57C00"));
                break;
            case 2:
                btnStockCritico.setBackgroundColor(Color.parseColor("#D32F2F"));
                break;
        }
    }

    private void configurarListView() {
        listaMateria = new ArrayList<>();

        String[] from = {
                "nombre",
                "estado",
                "proveedor",
                "stock",
                "minimo",
                "precio",
                "valor"
        };

        int[] to = {
                R.id.tvMateriaNombre,
                R.id.tvMateriaEstado,
                R.id.tvMateriaProveedor,
                R.id.tvMateriaStock,
                R.id.tvMateriaMinimo,
                R.id.tvMateriaPrecio,
                R.id.tvMateriaValor
        };

        adapter = new SimpleAdapter(
                this,
                listaMateria,
                R.layout.item_materia_prima,
                from,
                to
        );

        listViewMateriaPrima.setAdapter(adapter);
    }

    private void cargarMateriaPrima() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query;
        String[] args = null;

        switch (filtroStock) {
            case 0: // Todos
                query = "SELECT m.materia_prima_id, m.nombre, m.precio_compra, " +
                        "m.unidad_medida, m.stock_actual, m.stock_minimo, " +
                        "COALESCE(p.nombre, 'Sin proveedor') as proveedor_nombre " +
                        "FROM MATERIA_PRIMA m " +
                        "LEFT JOIN PROVEEDORES p ON m.proveedor_id = p.proveedor_id " +
                        "ORDER BY m.nombre ASC";
                break;

            case 1: // Stock bajo (stock_actual <= stock_minimo * 1.5)
                query = "SELECT m.materia_prima_id, m.nombre, m.precio_compra, " +
                        "m.unidad_medida, m.stock_actual, m.stock_minimo, " +
                        "COALESCE(p.nombre, 'Sin proveedor') as proveedor_nombre " +
                        "FROM MATERIA_PRIMA m " +
                        "LEFT JOIN PROVEEDORES p ON m.proveedor_id = p.proveedor_id " +
                        "WHERE m.stock_actual <= m.stock_minimo * 1.5 " +
                        "AND m.stock_actual > m.stock_minimo * 0.5 " +
                        "ORDER BY m.stock_actual ASC";
                break;

            case 2: // Stock crítico (stock_actual <= stock_minimo * 0.5)
                query = "SELECT m.materia_prima_id, m.nombre, m.precio_compra, " +
                        "m.unidad_medida, m.stock_actual, m.stock_minimo, " +
                        "COALESCE(p.nombre, 'Sin proveedor') as proveedor_nombre " +
                        "FROM MATERIA_PRIMA m " +
                        "LEFT JOIN PROVEEDORES p ON m.proveedor_id = p.proveedor_id " +
                        "WHERE m.stock_actual <= m.stock_minimo * 0.5 " +
                        "ORDER BY m.stock_actual ASC";
                break;

            default:
                query = "SELECT m.materia_prima_id, m.nombre, m.precio_compra, " +
                        "m.unidad_medida, m.stock_actual, m.stock_minimo, " +
                        "COALESCE(p.nombre, 'Sin proveedor') as proveedor_nombre " +
                        "FROM MATERIA_PRIMA m " +
                        "LEFT JOIN PROVEEDORES p ON m.proveedor_id = p.proveedor_id " +
                        "ORDER BY m.nombre ASC";
        }

        Cursor cursor = db.rawQuery(query, args);

        listaMateria.clear();
        int totalItems = 0;
        int stockBajoCount = 0;
        int stockCriticoCount = 0;

        while (cursor.moveToNext()) {
            int materiaId = cursor.getInt(0);
            String nombre = cursor.getString(1);
            double precio = cursor.getDouble(2);
            String unidad = cursor.getString(3);
            double stockActual = cursor.getDouble(4);
            double stockMinimo = cursor.getDouble(5);
            String proveedor = cursor.getString(6);

            // Determinar estado
            String estado;
            int colorEstado;

            if (stockActual <= stockMinimo * 0.5) {
                estado = "CRÍTICO";
                colorEstado = Color.RED;
                stockCriticoCount++;
            } else if (stockActual <= stockMinimo * 1.5) {
                estado = "BAJO";
                colorEstado = Color.parseColor("#FF9800");
                stockBajoCount++;
            } else {
                estado = "NORMAL";
                colorEstado = Color.parseColor("#4CAF50");
            }

            totalItems++;

            // Calcular valor total en inventario
            double valorTotal = precio * stockActual;

            HashMap<String, String> materia = new HashMap<>();
            materia.put("id", String.valueOf(materiaId));
            materia.put("nombre", nombre);
            materia.put("estado", estado);
            materia.put("proveedor", proveedor);
            materia.put("stock", String.format("%.1f %s", stockActual, unidad));
            materia.put("minimo", String.format("%.1f %s", stockMinimo, unidad));
            materia.put("precio", String.format("$%.2f / %s", precio, unidad));
            materia.put("valor", String.format(" | Valor: $%.2f", valorTotal));

            listaMateria.add(materia);
        }

        cursor.close();
        db.close();

        // Actualizar resumen
        tvTotalItems.setText(String.valueOf(totalItems));
        tvStockBajo.setText(String.valueOf(stockBajoCount));
        tvStockCritico.setText(String.valueOf(stockCriticoCount));

        adapter.notifyDataSetChanged();

        if (listaMateria.isEmpty()) {
            String mensaje = "";
            switch (filtroStock) {
                case 0:
                    mensaje = "No hay materia prima registrada";
                    break;
                case 1:
                    mensaje = "No hay materia prima con stock bajo";
                    break;
                case 2:
                    mensaje = "No hay materia prima con stock crítico";
                    break;
            }
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
        }
    }

    private void filtrarMateriaPrima(String texto) {
        if (texto.isEmpty()) {
            cargarMateriaPrima();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT m.materia_prima_id, m.nombre, m.precio_compra, " +
                "m.unidad_medida, m.stock_actual, m.stock_minimo, " +
                "COALESCE(p.nombre, 'Sin proveedor') as proveedor_nombre " +
                "FROM MATERIA_PRIMA m " +
                "LEFT JOIN PROVEEDORES p ON m.proveedor_id = p.proveedor_id " +
                "WHERE m.nombre LIKE ? ";

        // Agregar filtro de stock si es necesario
        String[] args;
        if (filtroStock == 1) {
            query += "AND m.stock_actual <= m.stock_minimo * 1.5 " +
                    "AND m.stock_actual > m.stock_minimo * 0.5 ";
            args = new String[]{"%" + texto + "%"};
        } else if (filtroStock == 2) {
            query += "AND m.stock_actual <= m.stock_minimo * 0.5 ";
            args = new String[]{"%" + texto + "%"};
        } else {
            args = new String[]{"%" + texto + "%"};
        }

        query += "ORDER BY m.nombre ASC";

        Cursor cursor = db.rawQuery(query, args);

        listaMateria.clear();

        while (cursor.moveToNext()) {
            int materiaId = cursor.getInt(0);
            String nombre = cursor.getString(1);
            double precio = cursor.getDouble(2);
            String unidad = cursor.getString(3);
            double stockActual = cursor.getDouble(4);
            double stockMinimo = cursor.getDouble(5);
            String proveedor = cursor.getString(6);

            // Determinar estado
            String estado;

            if (stockActual <= stockMinimo * 0.5) {
                estado = "CRÍTICO";
            } else if (stockActual <= stockMinimo * 1.5) {
                estado = "BAJO";
            } else {
                estado = "NORMAL";
            }

            // Calcular valor total
            double valorTotal = precio * stockActual;

            HashMap<String, String> materia = new HashMap<>();
            materia.put("id", String.valueOf(materiaId));
            materia.put("nombre", nombre);
            materia.put("estado", estado);
            materia.put("proveedor", proveedor);
            materia.put("stock", String.format("%.1f %s", stockActual, unidad));
            materia.put("minimo", String.format("%.1f %s", stockMinimo, unidad));
            materia.put("precio", String.format("$%.2f / %s", precio, unidad));
            materia.put("valor", String.format(" | Valor: $%.2f", valorTotal));

            listaMateria.add(materia);
        }

        cursor.close();
        db.close();

        adapter.notifyDataSetChanged();
    }

    private void mostrarDialogoMateriaPrima(final Integer materiaId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_materia_prima, null);

        builder.setView(dialogView);

        TextView tvTitulo = dialogView.findViewById(R.id.tvTituloDialogMateria);
        final EditText etNombre = dialogView.findViewById(R.id.etNombreMateria);
        final Spinner spinnerProveedor = dialogView.findViewById(R.id.spinnerProveedor);
        final EditText etPrecio = dialogView.findViewById(R.id.etPrecioCompra);
        final Spinner spinnerUnidad = dialogView.findViewById(R.id.spinnerUnidadMateria);
        final EditText etStockActual = dialogView.findViewById(R.id.etStockActual);
        final EditText etStockMinimo = dialogView.findViewById(R.id.etStockMinimo);
        Button btnCancelar = dialogView.findViewById(R.id.btnCancelarMateria);
        Button btnGuardar = dialogView.findViewById(R.id.btnGuardarMateria);

        // Cargar proveedores en spinner
        cargarProveedoresEnSpinner(spinnerProveedor);

        // Cargar unidades en spinner
        ArrayAdapter<CharSequence> unidadAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.unidades_materia_array,
                android.R.layout.simple_spinner_item
        );
        unidadAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUnidad.setAdapter(unidadAdapter);

        // Si es edición, cargar datos
        if (materiaId != null) {
            tvTitulo.setText("Editar Materia Prima");
            cargarDatosMateria(materiaId, etNombre, spinnerProveedor, etPrecio, spinnerUnidad, etStockActual, etStockMinimo);
        }

        final AlertDialog dialog = builder.create();

        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarMateriaPrima(
                        etNombre.getText().toString(),
                        spinnerProveedor.getSelectedItemPosition(),
                        etPrecio.getText().toString(),
                        spinnerUnidad.getSelectedItem().toString(),
                        etStockActual.getText().toString(),
                        etStockMinimo.getText().toString(),
                        materiaId
                );
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void cargarProveedoresEnSpinner(Spinner spinner) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT proveedor_id, nombre FROM PROVEEDORES ORDER BY nombre",
                null
        );

        // Usar listas separadas para IDs y nombres
        ArrayList<Integer> proveedorIds = new ArrayList<>();
        ArrayList<String> proveedorNombres = new ArrayList<>();

        // Opción por defecto
        proveedorIds.add(0);
        proveedorNombres.add("Sin proveedor");

        while (cursor.moveToNext()) {
            proveedorIds.add(cursor.getInt(0));
            proveedorNombres.add(cursor.getString(1));
        }

        cursor.close();
        db.close();

        // Crear un adaptador simple de Strings
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                proveedorNombres
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void cargarDatosMateria(int materiaId, EditText etNombre, Spinner spinnerProveedor,
                                    EditText etPrecio, Spinner spinnerUnidad, EditText etStockActual,
                                    EditText etStockMinimo) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT m.nombre, m.proveedor_id, m.precio_compra, " +
                        "m.unidad_medida, m.stock_actual, m.stock_minimo " +
                        "FROM MATERIA_PRIMA m WHERE m.materia_prima_id = ?",
                new String[]{String.valueOf(materiaId)}
        );

        if (cursor.moveToFirst()) {
            etNombre.setText(cursor.getString(0));

            // Seleccionar proveedor
            int proveedorId = cursor.getInt(1);
            SQLiteDatabase db2 = dbHelper.getReadableDatabase();
            Cursor cursorProv = db2.rawQuery(
                    "SELECT COUNT(*) FROM PROVEEDORES WHERE proveedor_id < ? ORDER BY nombre",
                    new String[]{String.valueOf(proveedorId)}
            );
            if (cursorProv.moveToFirst()) {
                int posicion = cursorProv.getInt(0) + 1; // +1 por "Sin proveedor"
                if (posicion < spinnerProveedor.getCount()) {
                    spinnerProveedor.setSelection(posicion);
                }
            }
            cursorProv.close();
            db2.close();

            etPrecio.setText(String.valueOf(cursor.getDouble(2)));

            // Seleccionar unidad
            String unidad = cursor.getString(3);
            ArrayAdapter unidadAdapter = (ArrayAdapter) spinnerUnidad.getAdapter();
            for (int i = 0; i < unidadAdapter.getCount(); i++) {
                if (unidadAdapter.getItem(i).equals(unidad)) {
                    spinnerUnidad.setSelection(i);
                    break;
                }
            }

            etStockActual.setText(String.valueOf(cursor.getDouble(4)));
            etStockMinimo.setText(String.valueOf(cursor.getDouble(5)));
        }

        cursor.close();
        db.close();
    }

    private void guardarMateriaPrima(String nombre, int proveedorPos, String precioStr,
                                     String unidad, String stockActualStr, String stockMinimoStr,
                                     Integer materiaId) {
        // Validaciones básicas
        if (nombre.isEmpty()) {
            Toast.makeText(this, "Ingrese el nombre", Toast.LENGTH_SHORT).show();
            return;
        }

        if (precioStr.isEmpty()) {
            Toast.makeText(this, "Ingrese el precio", Toast.LENGTH_SHORT).show();
            return;
        }

        // Valores por defecto si están vacíos
        if (stockActualStr.isEmpty()) {
            stockActualStr = "0";
        }

        if (stockMinimoStr.isEmpty()) {
            stockMinimoStr = "0"; // Permitimos 0, luego ajustamos
        }

        double precio, stockActual, stockMinimo;
        try {
            precio = Double.parseDouble(precioStr);
            stockActual = Double.parseDouble(stockActualStr);
            stockMinimo = Double.parseDouble(stockMinimoStr);

            // Validaciones
            if (precio <= 0) {
                Toast.makeText(this, "El precio debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                return;
            }

            if (stockActual < 0) {
                Toast.makeText(this, "El stock actual no puede ser negativo", Toast.LENGTH_SHORT).show();
                return;
            }

            if (stockMinimo < 0) {
                Toast.makeText(this, "El stock mínimo no puede ser negativo", Toast.LENGTH_SHORT).show();
                return;
            }

            // Si stock mínimo es 0, calcular valor razonable
            if (stockMinimo == 0 && stockActual > 0) {
                // El mínimo será el 20% del stock actual, mínimo 1 unidad
                stockMinimo = Math.max(1, stockActual * 0.2);
                Toast.makeText(this,
                        "Stock mínimo ajustado a " + String.format("%.1f", stockMinimo),
                        Toast.LENGTH_SHORT).show();
            }

            // Solo advertencia si el stock actual es menor al mínimo
            if (stockActual < stockMinimo && stockMinimo > 0) {
                final double finalStockActual = stockActual;
                final double finalStockMinimo = stockMinimo;

                // Mostrar advertencia pero PERMITIR guardar
                new AlertDialog.Builder(this)
                        .setTitle("⚠️ Advertencia de Stock")
                        .setMessage("El stock actual (" + stockActual + ") " +
                                "está por debajo del mínimo recomendado (" + stockMinimo + ").\n\n" +
                                "¿Desea guardar de todos modos?")
                        .setPositiveButton("Sí, Guardar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                guardarEnBD(nombre, proveedorPos, precio, unidad,
                                        finalStockActual, finalStockMinimo, materiaId);
                            }
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
                return; // Salir aquí, se llamará a guardarEnBD desde el diálogo
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Valores numéricos inválidos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Si llegamos aquí, los valores son válidos
        guardarEnBD(nombre, proveedorPos, precio, unidad, stockActual, stockMinimo, materiaId);
    }

    // Método separado para guardar en BD (llamado desde el diálogo o directamente)
    private void guardarEnBD(String nombre, int proveedorPos, double precio, String unidad,
                             double stockActual, double stockMinimo, Integer materiaId) {

        // Obtener ID del proveedor seleccionado
        int proveedorId = 0;
        if (proveedorPos > 0) {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery(
                    "SELECT proveedor_id FROM PROVEEDORES ORDER BY nombre LIMIT 1 OFFSET ?",
                    new String[]{String.valueOf(proveedorPos - 1)}
            );

            if (cursor.moveToFirst()) {
                proveedorId = cursor.getInt(0);
            }
            cursor.close();
            db.close();
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("nombre", nombre);
        values.put("proveedor_id", proveedorId > 0 ? proveedorId : null);
        values.put("precio_compra", precio);
        values.put("unidad_medida", unidad);
        values.put("stock_actual", stockActual);
        values.put("stock_minimo", stockMinimo);

        if (materiaId == null) {
            // Nueva materia prima
            long resultado = db.insert("MATERIA_PRIMA", null, values);

            if (resultado != -1) {
                Toast.makeText(this, " Materia prima registrada", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, " Error al registrar", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Editar materia prima existente
            int resultado = db.update("MATERIA_PRIMA", values, "materia_prima_id = ?",
                    new String[]{String.valueOf(materiaId)});

            if (resultado > 0) {
                Toast.makeText(this, " Materia prima actualizada", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, " Error al actualizar", Toast.LENGTH_SHORT).show();
            }
        }

        db.close();
        cargarMateriaPrima(); // Refrescar lista
    }

    private void mostrarMenuOpcionesMateria(int position) {
        final String materiaIdStr = listaMateria.get(position).get("id"); // Cambié el nombre para claridad
        final String nombreMateria = listaMateria.get(position).get("nombre");

        // Convertir a int una sola vez
        final int materiaId = Integer.parseInt(materiaIdStr);

        final CharSequence[] opciones = {"Ver Detalles", "Editar", "Registrar Consumo", "Registrar Compra", "Eliminar", "Cancelar"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Opciones: " + nombreMateria);
        builder.setItems(opciones, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // Ver Detalles
                        mostrarDetallesMateria(materiaId);
                        break;
                    case 1: // Editar
                        mostrarDialogoMateriaPrima(materiaId); // CORREGIDO
                        break;
                    case 3: // Registrar Compra
                        registrarCompraMateria(materiaId, nombreMateria); // CORREGIDO
                        break;
                    case 4: // Eliminar
                        confirmarEliminarMateria(materiaId, nombreMateria); // CORREGIDO
                        break;
                    case 5: // Cancelar
                        dialog.dismiss();
                        break;
                }
            }
        });
        builder.show();
    }

    private void mostrarDetallesMateria(int materiaId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT m.nombre, m.precio_compra, m.unidad_medida, " +
                        "m.stock_actual, m.stock_minimo, COALESCE(p.nombre, 'Sin proveedor') as proveedor " +
                        "FROM MATERIA_PRIMA m " +
                        "LEFT JOIN PROVEEDORES p ON m.proveedor_id = p.proveedor_id " +
                        "WHERE m.materia_prima_id = ?",
                new String[]{String.valueOf(materiaId)}
        );

        if (cursor.moveToFirst()) {
            String nombre = cursor.getString(0);
            double precio = cursor.getDouble(1);
            String unidad = cursor.getString(2);
            double stockActual = cursor.getDouble(3);
            double stockMinimo = cursor.getDouble(4);
            String proveedor = cursor.getString(5);

            // Calcular valores
            double valorInventario = precio * stockActual;
            double porcentajeStock = stockMinimo > 0 ? (stockActual / stockMinimo) * 100 : 0;

            String estado;
            if (stockMinimo == 0) {
                estado = "SIN MÍNIMO ESTABLECIDO";
            } else if (stockActual <= stockMinimo * 0.5) {
                estado = "CRÍTICO (Urgente reabastecer)";
            } else if (stockActual <= stockMinimo * 1.5) {
                estado = "BAJO (Reabastecer pronto)";
            } else {
                estado = "NORMAL";
            }

            String recomendacion = "";
            if (stockMinimo > 0) {
                double necesario = stockMinimo - stockActual;
                if (necesario > 0) {
                    recomendacion = "\n\n📋 RECOMENDACIÓN:\n" +
                            "Comprar: " + String.format("%.1f", necesario) + " " + unidad + "\n" +
                            "Costo estimado: $" + String.format("%.2f", necesario * precio);
                } else {
                    recomendacion = "\n\n Stock suficiente";
                }
            }

            String mensaje = " DETALLES DE MATERIA PRIMA\n\n" +
                    "🔹 Nombre: " + nombre + "\n" +
                    "🔹 Proveedor: " + proveedor + "\n" +
                    "🔹 Precio: $" + String.format("%.2f", precio) + " por " + unidad + "\n" +
                    "🔹 Stock Actual: " + String.format("%.1f", stockActual) + " " + unidad + "\n" +
                    "🔹 Stock Mínimo: " + String.format("%.1f", stockMinimo) + " " + unidad + "\n";

            if (stockMinimo > 0) {
                mensaje += "🔹 Porcentaje: " + String.format("%.0f", porcentajeStock) + "% del mínimo\n";
            }

            mensaje += "🔹 Estado: " + estado + "\n" +
                    "🔹 Valor en Inventario: $" + String.format("%.2f", valorInventario) +
                    recomendacion;

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Detalles de Materia Prima");
            builder.setMessage(mensaje);
            builder.setPositiveButton("Aceptar", null);

            // Agregar botón para editar si es necesario
            if (stockMinimo == 0) {
                builder.setNegativeButton("Establecer Mínimo", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mostrarDialogoMateriaPrima(materiaId);
                    }
                });
            }

            builder.show();
        }

        cursor.close();
        db.close();
    }



    private void registrarCompraMateria(final int materiaId, final String nombreMateria) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(" Registrar Compra: " + nombreMateria);

        // Obtener información actual
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT m.stock_actual, m.unidad_medida, m.precio_compra, COALESCE(p.nombre, '') as proveedor " +
                        "FROM MATERIA_PRIMA m " +
                        "LEFT JOIN PROVEEDORES p ON m.proveedor_id = p.proveedor_id " +
                        "WHERE m.materia_prima_id = ?",
                new String[]{String.valueOf(materiaId)}
        );

        final double[] stockActual = {0};
        final String[] unidad = {""};
        final double[] precioActual = {0};
        final String[] proveedorActual = {""};

        if (cursor.moveToFirst()) {
            stockActual[0] = cursor.getDouble(0);
            unidad[0] = cursor.getString(1);
            precioActual[0] = cursor.getDouble(2);
            proveedorActual[0] = cursor.getString(3);
        }
        cursor.close();
        db.close();

        // Crear layout dinámico
        View layout = getLayoutInflater().inflate(R.layout.dialog_compra_materia, null);

        TextView tvStock = layout.findViewById(R.id.tvStockActualCompra);
        final EditText etCantidad = layout.findViewById(R.id.etCantidadCompra);
        final EditText etPrecio = layout.findViewById(R.id.etPrecioCompraActual);
        final EditText etProveedor = layout.findViewById(R.id.etProveedorCompra);
        final TextView tvTotal = layout.findViewById(R.id.tvTotalCompra);

        tvStock.setText(String.format("Stock actual: %.1f %s", stockActual[0], unidad[0]));
        etPrecio.setText(String.valueOf(precioActual[0]));
        if (!proveedorActual[0].isEmpty()) {
            etProveedor.setText(proveedorActual[0]);
        }

        // TextWatcher para calcular total
        TextWatcher calculadoraTotal = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calcularTotalCompra(etCantidad, etPrecio, tvTotal);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etCantidad.addTextChangedListener(calculadoraTotal);
        etPrecio.addTextChangedListener(calculadoraTotal);

        builder.setView(layout);

        builder.setPositiveButton("REGISTRAR COMPRA", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String cantidadStr = etCantidad.getText().toString().trim();
                String precioStr = etPrecio.getText().toString().trim();
                String proveedor = etProveedor.getText().toString().trim();

                if (cantidadStr.isEmpty()) {
                    Toast.makeText(MateriaPrimaActivity.this, "Ingrese la cantidad", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (precioStr.isEmpty()) {
                    Toast.makeText(MateriaPrimaActivity.this, "Ingrese el precio unitario", Toast.LENGTH_SHORT).show();
                    return;
                }

                double cantidad, precio;
                try {
                    cantidad = Double.parseDouble(cantidadStr);
                    precio = Double.parseDouble(precioStr);

                    if (cantidad <= 0) {
                        Toast.makeText(MateriaPrimaActivity.this, "La cantidad debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (precio <= 0) {
                        Toast.makeText(MateriaPrimaActivity.this, "El precio debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(MateriaPrimaActivity.this, "Valores numéricos inválidos", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Registrar la compra
                registrarCompraEnBD(materiaId, nombreMateria, cantidad, precio, proveedor, unidad[0]);
            }
        });

        builder.setNegativeButton("CANCELAR", null);
        builder.show();
    }

    private void calcularTotalCompra(EditText etCantidad, EditText etPrecio, TextView tvTotal) {
        try {
            String cantidadStr = etCantidad.getText().toString().trim();
            String precioStr = etPrecio.getText().toString().trim();

            if (!cantidadStr.isEmpty() && !precioStr.isEmpty()) {
                double cantidad = Double.parseDouble(cantidadStr);
                double precio = Double.parseDouble(precioStr);
                double total = cantidad * precio;

                tvTotal.setText(String.format("Total: $%.2f", total));
            } else {
                tvTotal.setText("Total: $0.00");
            }
        } catch (NumberFormatException e) {
            tvTotal.setText("Total: $0.00");
        }
    }

    private void registrarCompraEnBD(int materiaId, String nombreMateria, double cantidad,
                                     double precioUnitario, String proveedor, String unidad) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            db.beginTransaction();

            // Obtener stock actual
            Cursor cursor = db.rawQuery(
                    "SELECT stock_actual FROM MATERIA_PRIMA WHERE materia_prima_id = ?",
                    new String[]{String.valueOf(materiaId)}
            );

            double stockActual = 0;
            if (cursor.moveToFirst()) {
                stockActual = cursor.getDouble(0);
            }
            cursor.close();

            double nuevoStock = stockActual + cantidad;

            // Actualizar materia prima
            ContentValues values = new ContentValues();
            values.put("stock_actual", nuevoStock);
            values.put("precio_compra", precioUnitario);

            // Actualizar proveedor si se proporcionó
            if (!proveedor.isEmpty() && !proveedor.equals("Sin proveedor")) {
                int proveedorId = obtenerOcrearProveedor(proveedor, db);
                if (proveedorId > 0) {
                    values.put("proveedor_id", proveedorId);
                }
            }

            int updateResult = db.update("MATERIA_PRIMA", values,
                    "materia_prima_id = ?",
                    new String[]{String.valueOf(materiaId)});

            if (updateResult > 0) {
                // Registrar movimiento
                ContentValues valuesMov = new ContentValues();
                valuesMov.put("materia_prima_id", materiaId);
                valuesMov.put("cantidad", cantidad);
                valuesMov.put("precio_unitario", precioUnitario);
                valuesMov.put("tipo", "compra");
                valuesMov.put("fecha", System.currentTimeMillis());
                valuesMov.put("total", cantidad * precioUnitario);

                db.insert("MOVIMIENTOS_MATERIA", null, valuesMov);

                db.setTransactionSuccessful();

                Toast.makeText(this,
                        String.format(" Compra registrada\n+%.1f %s\nStock actual: %.1f %s",
                                cantidad, unidad, nuevoStock, unidad),
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, " Error al actualizar stock", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al registrar compra: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
            db.close();
        }

        cargarMateriaPrima();
    }

    private int obtenerOcrearProveedor(String nombreProveedor, SQLiteDatabase db) {
        // Buscar proveedor existente
        Cursor cursor = db.rawQuery(
                "SELECT proveedor_id FROM PROVEEDORES WHERE nombre = ?",
                new String[]{nombreProveedor}
        );

        if (cursor.moveToFirst()) {
            int id = cursor.getInt(0);
            cursor.close();
            return id;
        }
        cursor.close();

        // Crear nuevo proveedor
        ContentValues values = new ContentValues();
        values.put("nombre", nombreProveedor);
        values.put("fecha_registro", System.currentTimeMillis());

        long resultado = db.insert("PROVEEDORES", null, values);

        if (resultado != -1) {
            return (int) resultado;
        }

        return 0;
    }

    private void actualizarStockMateria(int materiaId, double cantidad, String nombreMateria) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            db.beginTransaction();

            // Obtener información actual
            Cursor cursor = db.rawQuery(
                    "SELECT stock_actual, unidad_medida, precio_compra FROM MATERIA_PRIMA WHERE materia_prima_id = ?",
                    new String[]{String.valueOf(materiaId)}
            );

            double stockActual = 0;
            String unidad = "";
            double precio = 0;

            if (cursor.moveToFirst()) {
                stockActual = cursor.getDouble(0);
                unidad = cursor.getString(1);
                precio = cursor.getDouble(2);
            }
            cursor.close();

            double nuevoStock = stockActual + cantidad;

            if (nuevoStock < 0) {
                Toast.makeText(this,
                        String.format(" No hay suficiente stock. Stock actual: %.1f %s", stockActual, unidad),
                        Toast.LENGTH_LONG).show();
                return;
            }

            // Actualizar stock
            ContentValues values = new ContentValues();
            values.put("stock_actual", nuevoStock);

            int resultado = db.update("MATERIA_PRIMA", values, "materia_prima_id = ?",
                    new String[]{String.valueOf(materiaId)});

            if (resultado > 0) {
                // Registrar movimiento si es consumo
                if (cantidad < 0) {
                    ContentValues valuesMov = new ContentValues();
                    valuesMov.put("materia_prima_id", materiaId);
                    valuesMov.put("cantidad", Math.abs(cantidad));
                    valuesMov.put("precio_unitario", precio);
                    valuesMov.put("tipo", "consumo");
                    valuesMov.put("fecha", System.currentTimeMillis());
                    valuesMov.put("total", Math.abs(cantidad) * precio);

                    db.insert("MOVIMIENTOS_MATERIA", null, valuesMov);
                }

                db.setTransactionSuccessful();

                String mensaje = cantidad < 0 ?
                        String.format(" Consumo registrado: %.1f %s\nStock actual: %.1f %s",
                                Math.abs(cantidad), unidad, nuevoStock, unidad) :
                        String.format(" Stock aumentado: +%.1f %s\nStock actual: %.1f %s",
                                cantidad, unidad, nuevoStock, unidad);

                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, " Error al actualizar stock", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
            db.close();
        }

        cargarMateriaPrima();
    }

    private void confirmarEliminarMateria(final int materiaId, String nombreMateria) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(" Confirmar Eliminación");
        builder.setMessage("¿Estás seguro de eliminar:\n\n" +
                nombreMateria + "\n\n" +
                "Esta acción no se puede deshacer.");

        builder.setPositiveButton("ELIMINAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                eliminarMateriaPrima(materiaId);
            }
        });

        builder.setNegativeButton("CANCELAR", null);
        builder.show();
    }

    private void eliminarMateriaPrima(int materiaId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            // Verificar si hay movimientos
            Cursor cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM MOVIMIENTOS_MATERIA WHERE materia_prima_id = ?",
                    new String[]{String.valueOf(materiaId)}
            );

            int countMovimientos = 0;
            if (cursor.moveToFirst()) {
                countMovimientos = cursor.getInt(0);
            }
            cursor.close();

            if (countMovimientos > 0) {
                // Hay movimientos, ofrecer opciones
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(" No se puede eliminar");
                builder.setMessage("Esta materia prima tiene " + countMovimientos +
                        " movimientos registrados.\n\n" +
                        "¿Qué deseas hacer?\n\n" +
                        "1. Desactivar (recomendado)\n" +
                        "2. Eliminar con todos los movimientos");

                builder.setPositiveButton("DESACTIVAR", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        desactivarMateriaPrima(materiaId);
                    }
                });

                builder.setNegativeButton("ELIMINAR TODO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        eliminarMateriaConMovimientos(materiaId);
                    }
                });

                builder.setNeutralButton("CANCELAR", null);
                builder.show();
                return;
            }

            // Si no hay movimientos, eliminar directamente
            int resultado = db.delete("MATERIA_PRIMA", "materia_prima_id = ?",
                    new String[]{String.valueOf(materiaId)});

            if (resultado > 0) {
                Toast.makeText(this, " Materia prima eliminada", Toast.LENGTH_SHORT).show();
                cargarMateriaPrima();
            } else {
                Toast.makeText(this, " Error al eliminar", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            db.close();
        }
    }

    private void desactivarMateriaPrima(int materiaId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("activo", 0); // 0 = desactivado

        int resultado = db.update("MATERIA_PRIMA", values, "materia_prima_id = ?",
                new String[]{String.valueOf(materiaId)});

        db.close();

        if (resultado > 0) {
            Toast.makeText(this, " Materia prima desactivada", Toast.LENGTH_SHORT).show();
            cargarMateriaPrima();
        } else {
            Toast.makeText(this, " Error al desactivar", Toast.LENGTH_SHORT).show();
        }
    }

    private void eliminarMateriaConMovimientos(int materiaId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            db.beginTransaction();

            // Eliminar movimientos primero
            db.delete("MOVIMIENTOS_MATERIA", "materia_prima_id = ?",
                    new String[]{String.valueOf(materiaId)});

            // Eliminar materia prima
            int resultado = db.delete("MATERIA_PRIMA", "materia_prima_id = ?",
                    new String[]{String.valueOf(materiaId)});

            if (resultado > 0) {
                db.setTransactionSuccessful();
                Toast.makeText(this, " Materia prima y registros eliminados", Toast.LENGTH_SHORT).show();
                cargarMateriaPrima();
            } else {
                Toast.makeText(this, " Error al eliminar", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarMateriaPrima();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}