// ProveedoresActivity.java
package com.goldenophelllia.mezquite;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashMap;

public class ProveedoresActivity extends AppCompatActivity {

    private ListView listViewProveedores;
    private EditText etBuscarProveedor;
    private Button btnNuevoProveedor, btnVolver;

    private Database dbHelper;
    private SimpleAdapter adapter;
    private ArrayList<HashMap<String, String>> listaProveedores;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proveedores);

        // Inicializar vistas
        listViewProveedores = findViewById(R.id.listViewProveedores);
        etBuscarProveedor = findViewById(R.id.etBuscarProveedor);
        btnNuevoProveedor = findViewById(R.id.btnNuevoProveedor);
        btnVolver = findViewById(R.id.btnVolverProveedores);

        // Inicializar base de datos
        dbHelper = new Database(this);

        // Configurar botones
        btnNuevoProveedor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialogoProveedor(null);
            }
        });

        btnVolver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Configurar búsqueda
        etBuscarProveedor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarProveedores(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Configurar lista
        configurarListView();

        // Cargar proveedores iniciales
        cargarProveedores();

        // Configurar clic largo para editar/eliminar
        listViewProveedores.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mostrarMenuOpcionesProveedor(position);
                return true;
            }
        });
    }

    private void configurarListView() {
        listaProveedores = new ArrayList<>();

        String[] from = {
                "nombre",
                "contacto",
                "telefono",
                "email",
                "direccion"
        };

        int[] to = {
                R.id.tvProveedorNombre,
                R.id.tvProveedorContacto,
                R.id.tvProveedorTelefono,
                R.id.tvProveedorEmail,
                R.id.tvProveedorDireccion
        };

        adapter = new SimpleAdapter(
                this,
                listaProveedores,
                R.layout.item_proveedor,
                from,
                to
        );

        listViewProveedores.setAdapter(adapter);
    }

    private void cargarProveedores() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT proveedor_id, nombre, contacto, telefono, email, direccion " +
                        "FROM PROVEEDORES ORDER BY nombre ASC",
                null
        );

        listaProveedores.clear();

        while (cursor.moveToNext()) {
            int proveedorId = cursor.getInt(0);
            String nombre = cursor.getString(1);
            String contacto = cursor.getString(2);
            String telefono = cursor.getString(3);
            String email = cursor.getString(4);
            String direccion = cursor.getString(5);

            HashMap<String, String> proveedor = new HashMap<>();
            proveedor.put("id", String.valueOf(proveedorId));
            proveedor.put("nombre", nombre);
            proveedor.put("contacto", contacto != null ? contacto : "No especificado");
            proveedor.put("telefono", telefono);
            proveedor.put("email", email != null ? email : "No especificado");
            proveedor.put("direccion", direccion != null ? direccion : "No especificada");

            listaProveedores.add(proveedor);
        }

        cursor.close();
        db.close();

        adapter.notifyDataSetChanged();

        if (listaProveedores.isEmpty()) {
            Toast.makeText(this, "No hay proveedores registrados", Toast.LENGTH_SHORT).show();
        }
    }

    private void filtrarProveedores(String texto) {
        if (texto.isEmpty()) {
            cargarProveedores();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT proveedor_id, nombre, contacto, telefono, email, direccion " +
                "FROM PROVEEDORES WHERE nombre LIKE ? OR contacto LIKE ? OR telefono LIKE ? " +
                "ORDER BY nombre ASC";

        String likeText = "%" + texto + "%";

        Cursor cursor = db.rawQuery(query, new String[]{likeText, likeText, likeText});

        listaProveedores.clear();

        while (cursor.moveToNext()) {
            int proveedorId = cursor.getInt(0);
            String nombre = cursor.getString(1);
            String contacto = cursor.getString(2);
            String telefono = cursor.getString(3);
            String email = cursor.getString(4);
            String direccion = cursor.getString(5);

            HashMap<String, String> proveedor = new HashMap<>();
            proveedor.put("id", String.valueOf(proveedorId));
            proveedor.put("nombre", nombre);
            proveedor.put("contacto", contacto != null ? contacto : "No especificado");
            proveedor.put("telefono", telefono);
            proveedor.put("email", email != null ? email : "No especificado");
            proveedor.put("direccion", direccion != null ? direccion : "No especificada");

            listaProveedores.add(proveedor);
        }

        cursor.close();
        db.close();

        adapter.notifyDataSetChanged();
    }

    private void mostrarDialogoProveedor(final Integer proveedorId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_proveedor, null);

        builder.setView(dialogView);

        TextView tvTitulo = dialogView.findViewById(R.id.tvTituloDialogProveedor);
        final EditText etNombre = dialogView.findViewById(R.id.etNombreProveedor);
        final EditText etContacto = dialogView.findViewById(R.id.etContactoProveedor);
        final EditText etTelefono = dialogView.findViewById(R.id.etTelefonoProveedor);
        final EditText etEmail = dialogView.findViewById(R.id.etEmailProveedor);
        final EditText etDireccion = dialogView.findViewById(R.id.etDireccionProveedor);
        Button btnCancelar = dialogView.findViewById(R.id.btnCancelarProveedor);
        Button btnGuardar = dialogView.findViewById(R.id.btnGuardarProveedor);

        // Si es edición, cargar datos del proveedor
        if (proveedorId != null) {
            tvTitulo.setText("Editar Proveedor");
            cargarDatosProveedor(proveedorId, etNombre, etContacto, etTelefono, etEmail, etDireccion);
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
                guardarProveedor(
                        etNombre.getText().toString(),
                        etContacto.getText().toString(),
                        etTelefono.getText().toString(),
                        etEmail.getText().toString(),
                        etDireccion.getText().toString(),
                        proveedorId
                );
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void cargarDatosProveedor(int proveedorId, EditText etNombre, EditText etContacto,
                                      EditText etTelefono, EditText etEmail, EditText etDireccion) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT nombre, contacto, telefono, email, direccion " +
                        "FROM PROVEEDORES WHERE proveedor_id = ?",
                new String[]{String.valueOf(proveedorId)}
        );

        if (cursor.moveToFirst()) {
            etNombre.setText(cursor.getString(0));
            etContacto.setText(cursor.getString(1));
            etTelefono.setText(cursor.getString(2));
            etEmail.setText(cursor.getString(3));
            etDireccion.setText(cursor.getString(4));
        }

        cursor.close();
        db.close();
    }

    private void guardarProveedor(String nombre, String contacto, String telefono,
                                  String email, String direccion, Integer proveedorId) {
        // Validaciones
        if (nombre.isEmpty() || telefono.isEmpty()) {
            Toast.makeText(this, "Nombre y teléfono son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("nombre", nombre);
        values.put("contacto", contacto.isEmpty() ? null : contacto);
        values.put("telefono", telefono);
        values.put("email", email.isEmpty() ? null : email);
        values.put("direccion", direccion.isEmpty() ? null : direccion);

        if (proveedorId == null) {
            // Nuevo proveedor
            long resultado = db.insert("PROVEEDORES", null, values);

            if (resultado != -1) {
                Toast.makeText(this, " Proveedor registrado exitosamente", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, " Error al registrar proveedor", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Editar proveedor existente
            int resultado = db.update("PROVEEDORES", values, "proveedor_id = ?",
                    new String[]{String.valueOf(proveedorId)});

            if (resultado > 0) {
                Toast.makeText(this, " Proveedor actualizado exitosamente", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, " Error al actualizar proveedor", Toast.LENGTH_SHORT).show();
            }
        }

        db.close();
        cargarProveedores(); // Refrescar lista
    }

    private void mostrarMenuOpcionesProveedor(int position) {
        final String proveedorId = listaProveedores.get(position).get("id");
        final String nombreProveedor = listaProveedores.get(position).get("nombre");

        final CharSequence[] opciones = {"Ver Detalles", "Editar", "Eliminar", "Cancelar"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Opciones: " + nombreProveedor);
        builder.setItems(opciones, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // Ver Detalles
                        mostrarDetallesProveedor(Integer.parseInt(proveedorId));
                        break;
                    case 1: // Editar
                        mostrarDialogoProveedor(Integer.parseInt(proveedorId));
                        break;
                    case 2: // Eliminar
                        confirmarEliminarProveedor(Integer.parseInt(proveedorId), nombreProveedor);
                        break;
                    case 3: // Cancelar
                        dialog.dismiss();
                        break;
                }
            }
        });
        builder.show();
    }

    private void mostrarDetallesProveedor(int proveedorId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Obtener información del proveedor
        Cursor cursor = db.rawQuery(
                "SELECT nombre, contacto, telefono, email, direccion " +
                        "FROM PROVEEDORES WHERE proveedor_id = ?",
                new String[]{String.valueOf(proveedorId)}
        );

        if (cursor.moveToFirst()) {
            String nombre = cursor.getString(0);
            String contacto = cursor.getString(1);
            String telefono = cursor.getString(2);
            String email = cursor.getString(3);
            String direccion = cursor.getString(4);

            // Obtener materia prima suministrada por este proveedor
            Cursor cursorMateria = db.rawQuery(
                    "SELECT COUNT(*) as total_materia, GROUP_CONCAT(nombre) as materias " +
                            "FROM MATERIA_PRIMA WHERE proveedor_id = ?",
                    new String[]{String.valueOf(proveedorId)}
            );

            String infoMateria = "";
            if (cursorMateria.moveToFirst()) {
                int totalMateria = cursorMateria.getInt(0);
                String materias = cursorMateria.getString(1);

                if (totalMateria > 0) {
                    infoMateria = "\n\nMateria Prima Suministrada (" + totalMateria + "):\n" +
                            (materias != null ? materias.replace(",", "\n") : "N/A");
                } else {
                    infoMateria = "\n\nEste proveedor no tiene materia prima registrada.";
                }
            }
            cursorMateria.close();

            // Obtener compras realizadas a este proveedor
            Cursor cursorCompras = db.rawQuery(
                    "SELECT COUNT(*) as total_compras, COALESCE(SUM(total_compra), 0) as total_gastado " +
                            "FROM COMPRAS_MP WHERE proveedor_id = ?",
                    new String[]{String.valueOf(proveedorId)}
            );

            String infoCompras = "";
            if (cursorCompras.moveToFirst()) {
                int totalCompras = cursorCompras.getInt(0);
                double totalGastado = cursorCompras.getDouble(1);

                infoCompras = "\n\nHistorial de Compras:\n" +
                        "Total de Compras: " + totalCompras + "\n" +
                        "Total Gastado: $" + String.format("%.2f", totalGastado);
            }
            cursorCompras.close();

            String mensaje = "Detalles del Proveedor:\n\n" +
                    "Nombre: " + nombre + "\n" +
                    "Contacto: " + (contacto != null ? contacto : "No especificado") + "\n" +
                    "Teléfono: " + telefono + "\n" +
                    "Email: " + (email != null ? email : "No especificado") + "\n" +
                    "Dirección: " + (direccion != null ? direccion : "No especificada") +
                    infoMateria + infoCompras;

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Detalles del Proveedor");
            builder.setMessage(mensaje);
            builder.setPositiveButton("Aceptar", null);
            builder.show();
        }

        cursor.close();
        db.close();
    }

    private void confirmarEliminarProveedor(final int proveedorId, String nombreProveedor) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(" Confirmar Eliminación");
        builder.setMessage("¿Estás seguro de eliminar al proveedor:\n\n" +
                nombreProveedor + "\n\n" +
                "Esta acción no se puede deshacer.");

        builder.setPositiveButton("ELIMINAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                eliminarProveedor(proveedorId);
            }
        });

        builder.setNegativeButton("CANCELAR", null);
        builder.show();
    }

    private void eliminarProveedor(int proveedorId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            db.beginTransaction();

            // Verificar si el proveedor tiene materia prima registrada
            Cursor cursorMateria = db.rawQuery(
                    "SELECT COUNT(*) FROM MATERIA_PRIMA WHERE proveedor_id = ?",
                    new String[]{String.valueOf(proveedorId)}
            );

            cursorMateria.moveToFirst();
            int tieneMateria = cursorMateria.getInt(0);
            cursorMateria.close();

            // Verificar si el proveedor tiene compras registradas
            Cursor cursorCompras = db.rawQuery(
                    "SELECT COUNT(*) FROM COMPRAS_MP WHERE proveedor_id = ?",
                    new String[]{String.valueOf(proveedorId)}
            );

            cursorCompras.moveToFirst();
            int tieneCompras = cursorCompras.getInt(0);
            cursorCompras.close();

            if (tieneMateria > 0 || tieneCompras > 0) {
                // No se puede eliminar porque tiene registros relacionados
                String mensaje = "No se puede eliminar este proveedor porque:\n\n";

                if (tieneMateria > 0) {
                    mensaje += "- Tiene " + tieneMateria + " materias primas registradas\n";
                }
                if (tieneCompras > 0) {
                    mensaje += "- Tiene " + tieneCompras + " compras registradas\n";
                }
                mensaje += "\nPrimero elimine o transfiera estos registros.";

                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
            } else {
                // Se puede eliminar
                int resultado = db.delete("PROVEEDORES", "proveedor_id = ?",
                        new String[]{String.valueOf(proveedorId)});

                if (resultado > 0) {
                    Toast.makeText(this, " Proveedor eliminado exitosamente", Toast.LENGTH_SHORT).show();
                    cargarProveedores(); // Refrescar lista
                } else {
                    Toast.makeText(this, " Error al eliminar proveedor", Toast.LENGTH_SHORT).show();
                }
            }

            db.setTransactionSuccessful();

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}