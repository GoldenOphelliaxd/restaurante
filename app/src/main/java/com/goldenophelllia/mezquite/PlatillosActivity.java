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
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class PlatillosActivity extends AppCompatActivity {

    private ListView listViewPlatillos;
    private EditText etBuscarPlatillo;
    private Button btnNuevoPlatillo, btnVolver, btnVerActivos, btnVerInactivos;

    private Database dbHelper;
    private SimpleCursorAdapter adapter;
    private Cursor cursorPlatillos;
    private int filtroEstado = 1; // 1=Activos, 0=Inactivos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_platillos);

        // Inicializar vistas
        listViewPlatillos = findViewById(R.id.listViewPlatillos);
        etBuscarPlatillo = findViewById(R.id.etBuscarPlatillo);
        btnNuevoPlatillo = findViewById(R.id.btnNuevoPlatillo);
        btnVolver = findViewById(R.id.btnVolverPlatillos);
        btnVerActivos = findViewById(R.id.btnVerActivos);
        btnVerInactivos = findViewById(R.id.btnVerInactivos);

        // Inicializar base de datos
        dbHelper = new Database(this);

        // Configurar colores iniciales de los botones
        actualizarColoresBotones();

        // Configurar botones
        btnNuevoPlatillo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialogoPlatillo(null);
            }
        });

        btnVolver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnVerActivos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filtroEstado = 1;
                cargarPlatillos();
                actualizarColoresBotones();
            }
        });

        btnVerInactivos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filtroEstado = 0;
                cargarPlatillos();
                actualizarColoresBotones();
            }
        });

        // Configurar búsqueda
        etBuscarPlatillo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarPlatillos(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Configurar lista
        configurarListView();

        // Cargar platillos iniciales
        cargarPlatillos();

        // Configurar clic largo para opciones
        listViewPlatillos.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mostrarMenuOpcionesPlatillo(id);
                return true;
            }
        });
    }

    private void actualizarColoresBotones() {
        if (filtroEstado == 1) {
            // Activos seleccionado
            btnVerActivos.setBackgroundColor(Color.parseColor("#388E3C")); // Verde oscuro
            btnVerActivos.setTextColor(Color.WHITE);

            btnVerInactivos.setBackgroundColor(Color.parseColor("#FFB74D")); // Naranja claro
            btnVerInactivos.setTextColor(Color.BLACK);
        } else {
            // Inactivos seleccionado
            btnVerActivos.setBackgroundColor(Color.parseColor("#81C784")); // Verde claro
            btnVerActivos.setTextColor(Color.BLACK);

            btnVerInactivos.setBackgroundColor(Color.parseColor("#F57C00")); // Naranja oscuro
            btnVerInactivos.setTextColor(Color.WHITE);
        }
    }

    private void configurarListView() {
        String[] from = {"nombre", "precio_base", "unidad_medida", "activo"};
        int[] to = {android.R.id.text1, android.R.id.text2};

        adapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_list_item_2,
                null,
                from,
                to,
                0
        ) {
            @Override
            public void bindView(View view, android.content.Context context, Cursor cursor) {
                super.bindView(view, context, cursor);

                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                String nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
                double precio = cursor.getDouble(cursor.getColumnIndexOrThrow("precio_base"));
                String unidad = cursor.getString(cursor.getColumnIndexOrThrow("unidad_medida"));
                int activo = cursor.getInt(cursor.getColumnIndexOrThrow("activo"));

                // Cambiar color según estado
                if (activo == 0) {
                    text1.setTextColor(Color.GRAY);
                    text2.setTextColor(Color.GRAY);
                    text1.setText(nombre + " (INACTIVO)");
                } else {
                    text1.setTextColor(Color.BLACK);
                    text2.setTextColor(Color.BLACK);
                    text1.setText(nombre);
                }

                text2.setText(String.format("Precio: $%.2f por %s", precio, unidad));
            }
        };

        listViewPlatillos.setAdapter(adapter);
    }

    private void cargarPlatillos() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query;
        if (filtroEstado == 1) {
            query = "SELECT producto_id as _id, nombre, precio_base, unidad_medida, activo " +
                    "FROM PRODUCTOS WHERE activo = 1 ORDER BY nombre ASC";
        } else {
            query = "SELECT producto_id as _id, nombre, precio_base, unidad_medida, activo " +
                    "FROM PRODUCTOS WHERE activo = 0 ORDER BY nombre ASC";
        }

        if (cursorPlatillos != null && !cursorPlatillos.isClosed()) {
            cursorPlatillos.close();
        }

        cursorPlatillos = db.rawQuery(query, null);
        adapter.changeCursor(cursorPlatillos);

        // Mostrar mensaje si no hay resultados
        if (cursorPlatillos.getCount() == 0) {
            String mensaje = (filtroEstado == 1) ?
                    "No hay platillos activos" : "No hay platillos inactivos";
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
        }
    }

    private void filtrarPlatillos(String texto) {
        if (texto.isEmpty()) {
            cargarPlatillos();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT producto_id as _id, nombre, precio_base, unidad_medida, activo " +
                "FROM PRODUCTOS WHERE nombre LIKE ? AND activo = ? " +
                "ORDER BY nombre ASC";

        String likeText = "%" + texto + "%";

        if (cursorPlatillos != null && !cursorPlatillos.isClosed()) {
            cursorPlatillos.close();
        }

        cursorPlatillos = db.rawQuery(query, new String[]{likeText, String.valueOf(filtroEstado)});
        adapter.changeCursor(cursorPlatillos);
    }

    private void mostrarDialogoPlatillo(final Integer platilloId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_platillo, null);

        builder.setView(dialogView);

        TextView tvTitulo = dialogView.findViewById(R.id.tvTituloDialogPlatillo);
        final EditText etNombre = dialogView.findViewById(R.id.etNombrePlatillo);
        final Spinner spinnerCategoria = dialogView.findViewById(R.id.spinnerCategoria);
        final EditText etPrecio = dialogView.findViewById(R.id.etPrecioPlatillo);
        final Spinner spinnerUnidad = dialogView.findViewById(R.id.spinnerUnidad);
        Button btnCancelar = dialogView.findViewById(R.id.btnCancelarPlatillo);
        Button btnGuardar = dialogView.findViewById(R.id.btnGuardarPlatillo);

        // Configurar spinner de categorías (simplificado por ahora)
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Pescados", "Ceviches", "Bebidas"}
        );
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoria.setAdapter(catAdapter);

        // Configurar spinner de unidades
        ArrayAdapter<CharSequence> unidadAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.unidades_array,
                android.R.layout.simple_spinner_item
        );
        unidadAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUnidad.setAdapter(unidadAdapter);

        // Si es edición, cargar datos
        if (platilloId != null) {
            tvTitulo.setText("Editar Platillo");
            cargarDatosPlatillo(platilloId, etNombre, etPrecio, spinnerUnidad);
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
                String nombre = etNombre.getText().toString().trim();
                String precioStr = etPrecio.getText().toString().trim();
                String unidad = spinnerUnidad.getSelectedItem().toString();

                // Determinar categoría según selección
                int categoriaId = spinnerCategoria.getSelectedItemPosition() + 1;
                if (categoriaId > 3) categoriaId = 1; // Por defecto

                guardarPlatillo(nombre, categoriaId, precioStr, unidad, platilloId);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void cargarDatosPlatillo(int platilloId, EditText etNombre, EditText etPrecio, Spinner spinnerUnidad) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT nombre, precio_base, unidad_medida FROM PRODUCTOS WHERE producto_id = ?",
                new String[]{String.valueOf(platilloId)}
        );

        if (cursor.moveToFirst()) {
            etNombre.setText(cursor.getString(0));
            etPrecio.setText(String.valueOf(cursor.getDouble(1)));

            // Seleccionar la unidad correcta
            String unidad = cursor.getString(2);
            ArrayAdapter adapter = (ArrayAdapter) spinnerUnidad.getAdapter();
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).equals(unidad)) {
                    spinnerUnidad.setSelection(i);
                    break;
                }
            }
        }

        cursor.close();
        db.close();
    }

    private void guardarPlatillo(String nombre, int categoriaId, String precioStr,
                                 String unidad, Integer platilloId) {
        // Validaciones
        if (nombre.isEmpty()) {
            Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show();
            return;
        }

        if (precioStr.isEmpty()) {
            Toast.makeText(this, "El precio es obligatorio", Toast.LENGTH_SHORT).show();
            return;
        }

        double precio;
        try {
            precio = Double.parseDouble(precioStr);
            if (precio <= 0) {
                Toast.makeText(this, "El precio debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Precio inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("nombre", nombre);
        values.put("categoria_id", categoriaId);
        values.put("precio_base", precio);
        values.put("unidad_medida", unidad);
        values.put("activo", 1); // Siempre activo al crear/editar

        if (platilloId == null) {
            // Nuevo platillo
            long resultado = db.insert("PRODUCTOS", null, values);

            if (resultado != -1) {
                Toast.makeText(this, " Platillo registrado", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, " Error al registrar", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Editar platillo existente
            int resultado = db.update("PRODUCTOS", values, "producto_id = ?",
                    new String[]{String.valueOf(platilloId)});

            if (resultado > 0) {
                Toast.makeText(this, " Platillo actualizado", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, " Error al actualizar", Toast.LENGTH_SHORT).show();
            }
        }

        db.close();
        cargarPlatillos(); // Refrescar lista
    }

    private void mostrarMenuOpcionesPlatillo(final long platilloId) {
        // Obtener estado actual del platillo
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT activo FROM PRODUCTOS WHERE producto_id = ?",
                new String[]{String.valueOf(platilloId)}
        );

        final int estadoActual;
        if (cursor.moveToFirst()) {
            estadoActual = cursor.getInt(0);
        } else {
            estadoActual = 1;
        }
        cursor.close();
        db.close();

        // Crear menú basado en el estado
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (estadoActual == 1) {
            // MENÚ PARA PLATILLOS ACTIVOS
            builder.setTitle("Platillo Activo - Opciones");
            builder.setItems(new CharSequence[]{"Editar", "Desactivar", "Cancelar"},
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0: // Editar
                                    mostrarDialogoPlatillo((int) platilloId);
                                    break;
                                case 1: // Desactivar
                                    cambiarEstadoPlatillo((int) platilloId, 0);
                                    break;
                                case 2: // Cancelar
                                    dialog.dismiss();
                                    break;
                            }
                        }
                    });
        } else {
            // MENÚ PARA PLATILLOS INACTIVOS
            builder.setTitle("Platillo Inactivo - Opciones");
            builder.setItems(new CharSequence[]{"Editar", "Activar", "Eliminar", "Cancelar"},
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0: // Editar
                                    mostrarDialogoPlatillo((int) platilloId);
                                    break;
                                case 1: // Activar
                                    cambiarEstadoPlatillo((int) platilloId, 1);
                                    break;
                                case 2: // Eliminar
                                    confirmarEliminarPlatillo((int) platilloId);
                                    break;
                                case 3: // Cancelar
                                    dialog.dismiss();
                                    break;
                            }
                        }
                    });
        }

        builder.show();
    }
    private void cambiarEstadoPlatillo(int platilloId, int nuevoEstado) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("activo", nuevoEstado);

        int resultado = db.update("PRODUCTOS", values, "producto_id = ?",
                new String[]{String.valueOf(platilloId)});

        if (resultado > 0) {
            String mensaje = (nuevoEstado == 1) ?
                    " Platillo activado" : " Platillo desactivado";
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, " Error", Toast.LENGTH_SHORT).show();
        }

        db.close();
        cargarPlatillos(); // Recargar lista con nuevo filtro
    }

    private void confirmarEliminarPlatillo(final int platilloId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(" Confirmar Eliminación");
        builder.setMessage("¿Eliminar permanentemente este platillo?\n\nEsta acción no se puede deshacer.");

        builder.setPositiveButton("ELIMINAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                eliminarPlatillo(platilloId);
            }
        });

        builder.setNegativeButton("CANCELAR", null);
        builder.show();
    }

    private void eliminarPlatillo(int platilloId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Primero verificar si tiene ventas
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM DETALLE_VENTA WHERE producto_id = ?",
                new String[]{String.valueOf(platilloId)}
        );

        cursor.moveToFirst();
        int tieneVentas = cursor.getInt(0);
        cursor.close();

        if (tieneVentas > 0) {
            // Si tiene ventas, solo marcar como inactivo
            ContentValues values = new ContentValues();
            values.put("activo", 0);

            int resultado = db.update("PRODUCTOS", values, "producto_id = ?",
                    new String[]{String.valueOf(platilloId)});

            if (resultado > 0) {
                Toast.makeText(this,
                        "El platillo tenía ventas registradas. Se marcó como INACTIVO.",
                        Toast.LENGTH_LONG).show();
            }
        } else {
            // No tiene ventas, eliminar permanentemente
            int resultado = db.delete("PRODUCTOS", "producto_id = ?",
                    new String[]{String.valueOf(platilloId)});

            if (resultado > 0) {
                Toast.makeText(this, " Platillo eliminado", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, " Error al eliminar", Toast.LENGTH_SHORT).show();
            }
        }

        db.close();
        cargarPlatillos();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cursorPlatillos != null && !cursorPlatillos.isClosed()) {
            cursorPlatillos.close();
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}