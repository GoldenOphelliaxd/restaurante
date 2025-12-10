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
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.content.Context;

public class ClientesActivity extends AppCompatActivity {

    private ListView listViewClientes;
    private EditText etBuscarCliente;
    private Button btnNuevoCliente, btnVolver;

    private Database dbHelper;
    private SimpleCursorAdapter adapter;
    private Cursor cursorClientes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clientes);

        // Inicializar vistas
        listViewClientes = findViewById(R.id.listViewClientes);
        etBuscarCliente = findViewById(R.id.etBuscarCliente);
        btnNuevoCliente = findViewById(R.id.btnNuevoCliente);
        btnVolver = findViewById(R.id.btnVolver);

        // Inicializar base de datos
        dbHelper = new Database(this);

        // Configurar botones
        btnNuevoCliente.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialogoCliente(null);
            }
        });

        btnVolver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Volver a la actividad anterior
            }
        });

        // Configurar búsqueda
        etBuscarCliente.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarClientes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Configurar lista
        configurarListView();

        // Cargar clientes iniciales
        cargarClientes();

        // Configurar clic largo para editar/eliminar
        listViewClientes.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mostrarMenuOpcionesCliente(id);
                return true;
            }
        });
    }

    private void configurarListView() {
        // Definir columnas y views para el adaptador
        String[] from = new String[]{
                "nombre",
                "telefono",
                "email",
                "fecha_registro"
        };

        int[] to = new int[]{
                android.R.id.text1,
                android.R.id.text2
        };

        // Crear adaptador personalizado
        adapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_list_item_2,
                null,
                from,
                to,
                0
        ) {
            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                super.bindView(view, context, cursor);

                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                String nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
                String telefono = cursor.getString(cursor.getColumnIndexOrThrow("telefono"));
                String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
                String fecha = cursor.getString(cursor.getColumnIndexOrThrow("fecha_registro"));

                text1.setText(nombre);
                text2.setText("Tel: " + telefono + " | Email: " + (email != null ? email : "N/A") +
                        "\nRegistro: " + fecha);
            }
        };

        listViewClientes.setAdapter(adapter);
    }

    private void cargarClientes() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        cursorClientes = db.rawQuery(
                "SELECT cliente_id as _id, nombre, telefono, email, fecha_registro " +
                        "FROM CLIENTES ORDER BY nombre ASC",
                null
        );

        adapter.changeCursor(cursorClientes);
    }

    private void filtrarClientes(String texto) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT cliente_id as _id, nombre, telefono, email, fecha_registro " +
                "FROM CLIENTES WHERE nombre LIKE ? OR telefono LIKE ? " +
                "ORDER BY nombre ASC";

        String likeText = "%" + texto + "%";

        if (cursorClientes != null && !cursorClientes.isClosed()) {
            cursorClientes.close();
        }

        cursorClientes = db.rawQuery(query, new String[]{likeText, likeText});
        adapter.changeCursor(cursorClientes);
    }

    private void mostrarDialogoCliente(final Integer clienteId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_cliente, null);

        builder.setView(dialogView);

        // Obtener referencias de los elementos del diálogo
        TextView tvTitulo = dialogView.findViewById(R.id.tvTituloDialog);
        final EditText etNombre = dialogView.findViewById(R.id.etNombreCliente);
        final EditText etTelefono = dialogView.findViewById(R.id.etTelefonoCliente);
        final EditText etEmail = dialogView.findViewById(R.id.etEmailCliente);
        Button btnCancelar = dialogView.findViewById(R.id.btnCancelarCliente);
        Button btnGuardar = dialogView.findViewById(R.id.btnGuardarCliente);

        // Si es edición, cargar datos del cliente
        if (clienteId != null) {
            tvTitulo.setText("Editar Cliente");
            cargarDatosCliente(clienteId, etNombre, etTelefono, etEmail);
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
                guardarCliente(etNombre.getText().toString(),
                        etTelefono.getText().toString(),
                        etEmail.getText().toString(),
                        clienteId);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void cargarDatosCliente(int clienteId, EditText etNombre, EditText etTelefono, EditText etEmail) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT nombre, telefono, email FROM CLIENTES WHERE cliente_id = ?",
                new String[]{String.valueOf(clienteId)}
        );

        if (cursor.moveToFirst()) {
            etNombre.setText(cursor.getString(0));
            etTelefono.setText(cursor.getString(1));
            etEmail.setText(cursor.getString(2));
        }

        cursor.close();
        db.close();
    }

    private void guardarCliente(String nombre, String telefono, String email, Integer clienteId) {
        // Validaciones
        if (nombre.isEmpty() || telefono.isEmpty()) {
            Toast.makeText(this, "Nombre y teléfono son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("nombre", nombre);
        values.put("telefono", telefono);
        values.put("email", email.isEmpty() ? null : email);

        if (clienteId == null) {
            // Nuevo cliente
            values.put("fecha_registro", getFechaActual());
            long resultado = db.insert("CLIENTES", null, values);

            if (resultado != -1) {
                Toast.makeText(this, "Cliente registrado exitosamente", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error al registrar cliente", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Editar cliente existente
            int resultado = db.update("CLIENTES", values, "cliente_id = ?",
                    new String[]{String.valueOf(clienteId)});

            if (resultado > 0) {
                Toast.makeText(this, "Cliente actualizado exitosamente", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error al actualizar cliente", Toast.LENGTH_SHORT).show();
            }
        }

        db.close();
        cargarClientes(); // Refrescar lista
    }

    private void mostrarMenuOpcionesCliente(final long clienteId) {
        final CharSequence[] opciones = {"Ver Detalles", "Editar", "Eliminar", "Cancelar"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Opciones del Cliente");
        builder.setItems(opciones, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // Ver Detalles
                        mostrarDetallesCliente((int) clienteId);
                        break;
                    case 1: // Editar
                        mostrarDialogoCliente((int) clienteId);
                        break;
                    case 2: // Eliminar
                        confirmarEliminarCliente((int) clienteId);
                        break;
                    case 3: // Cancelar
                        dialog.dismiss();
                        break;
                }
            }
        });
        builder.show();
    }

    private void mostrarDetallesCliente(int clienteId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT nombre, telefono, email, fecha_registro " +
                        "FROM CLIENTES WHERE cliente_id = ?",
                new String[]{String.valueOf(clienteId)}
        );

        if (cursor.moveToFirst()) {
            String nombre = cursor.getString(0);
            String telefono = cursor.getString(1);
            String email = cursor.getString(2);
            String fecha = cursor.getString(3);

            // Obtener historial de ventas
            Cursor cursorVentas = db.rawQuery(
                    "SELECT COUNT(*) as total_ventas, SUM(total_venta) as total_gastado " +
                            "FROM VENTAS WHERE cliente_id = ?",
                    new String[]{String.valueOf(clienteId)}
            );

            int totalVentas = 0;
            double totalGastado = 0;

            if (cursorVentas.moveToFirst()) {
                totalVentas = cursorVentas.getInt(0);
                totalGastado = cursorVentas.getDouble(1);
            }

            String mensaje = "Detalles del Cliente:\n\n" +
                    "Nombre: " + nombre + "\n" +
                    "Teléfono: " + telefono + "\n" +
                    "Email: " + (email != null ? email : "No registrado") + "\n" +
                    "Fecha de Registro: " + fecha + "\n\n" +
                    "Historial de Compras:\n" +
                    "Total de Compras: " + totalVentas + "\n" +
                    "Total Gastado: $" + String.format("%.2f", totalGastado);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Detalles del Cliente");
            builder.setMessage(mensaje);
            builder.setPositiveButton("Aceptar", null);
            builder.show();

            cursorVentas.close();
        }

        cursor.close();
        db.close();
    }

    private void confirmarEliminarCliente(final int clienteId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmar Eliminación");
        builder.setMessage("¿Estás seguro de eliminar este cliente?");

        builder.setPositiveButton("Sí, Eliminar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                eliminarCliente(clienteId);
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void eliminarCliente(int clienteId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Primero verificar si el cliente tiene ventas registradas
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM VENTAS WHERE cliente_id = ?",
                new String[]{String.valueOf(clienteId)}
        );

        cursor.moveToFirst();
        int tieneVentas = cursor.getInt(0);
        cursor.close();

        if (tieneVentas > 0) {
            // No eliminar, solo marcar como inactivo (si tuviéramos ese campo)
            // O mostrar mensaje de que no se puede eliminar
            Toast.makeText(this,
                    "No se puede eliminar el cliente porque tiene ventas registradas",
                    Toast.LENGTH_LONG).show();
        } else {
            int resultado = db.delete("CLIENTES", "cliente_id = ?",
                    new String[]{String.valueOf(clienteId)});

            if (resultado > 0) {
                Toast.makeText(this, "Cliente eliminado exitosamente", Toast.LENGTH_SHORT).show();
                cargarClientes(); // Refrescar lista
            } else {
                Toast.makeText(this, "Error al eliminar cliente", Toast.LENGTH_SHORT).show();
            }
        }

        db.close();
    }

    private String getFechaActual() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cursorClientes != null && !cursorClientes.isClosed()) {
            cursorClientes.close();
        }
        dbHelper.close();
    }
}