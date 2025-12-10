package com.goldenophelllia.mezquite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class NuevaVentaActivity extends AppCompatActivity {

    private Spinner spinnerClientes;
    private Spinner spinnerProductos;
    private EditText etCantidad, etPeso;
    private TextView tvTotal;
    private Button btnAgregar, btnFinalizar;
    private ListView listViewProductos;

    private ArrayList<String> listaProductosVenta = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private double total = 0.0;

    private ArrayList<Integer> productosIds = new ArrayList<>();
    private ArrayList<Double> cantidades = new ArrayList<>();
    private ArrayList<Double> pesos = new ArrayList<>();
    private ArrayList<Double> preciosUnitarios = new ArrayList<>();
    private ArrayList<Double> subtotales = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nueva_venta);

        // Inicializar vistas
        spinnerClientes = findViewById(R.id.spinnerClientes);
        spinnerProductos = findViewById(R.id.spinnerProductos);
        etCantidad = findViewById(R.id.etCantidad);
        etPeso = findViewById(R.id.etPeso);
        tvTotal = findViewById(R.id.tvTotal);
        btnAgregar = findViewById(R.id.btnAgregarProducto);
        btnFinalizar = findViewById(R.id.btnFinalizarVenta);
        listViewProductos = findViewById(R.id.listViewProductos);

        // Configurar adaptador para la lista
        adapter = new ArrayAdapter<String>(
                NuevaVentaActivity.this,
                android.R.layout.simple_list_item_1,
                listaProductosVenta
        );
        listViewProductos.setAdapter(adapter);

        // Cargar datos en los spinners
        cargarClientesEnSpinner();
        cargarProductosEnSpinner();

        // Listener para cuando se selecciona un producto
        spinnerProductos.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Mostrar campo de peso solo para mojarra
                String productoSeleccionado = spinnerProductos.getSelectedItem().toString();
                if (productoSeleccionado.contains("Mojarra")) {
                    etPeso.setVisibility(View.VISIBLE);
                } else {
                    etPeso.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Botón agregar producto
        btnAgregar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                agregarProductoAVenta();
            }
        });

        // Botón finalizar venta
        btnFinalizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finalizarVenta();
            }
        });
    }

    // ========== MÉTODO PARA CARGAR CLIENTES ==========
    private void cargarClientesEnSpinner() {
        Database dbHelper = new Database(NuevaVentaActivity.this);
        Cursor cursor = dbHelper.obtenerClientes();

        ArrayList<String> clientes = new ArrayList<>();
        clientes.add("Seleccione un cliente");
        clientes.add("Cliente local (Mostrador)");  // Opción por defecto

        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Ajusta según las columnas de tu tabla clientes
                String nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
                String telefono = cursor.getString(cursor.getColumnIndexOrThrow("telefono"));
                clientes.add(nombre + " - " + telefono);
            } while (cursor.moveToNext());
            cursor.close();
        }

        ArrayAdapter<String> adapterClientes = new ArrayAdapter<>(
                NuevaVentaActivity.this,
                android.R.layout.simple_spinner_item,
                clientes
        );
        adapterClientes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerClientes.setAdapter(adapterClientes);
    }

    // ========== MÉTODO PARA CARGAR PRODUCTOS ==========
    private void cargarProductosEnSpinner() {
        Database dbHelper = new Database(NuevaVentaActivity.this);
        Cursor cursor = dbHelper.obtenerProductos();

        ArrayList<String> productos = new ArrayList<>();
        productos.add("Seleccione un producto");

        while (cursor.moveToNext()) {
            String nombre = cursor.getString(1); // columna nombre
            double precio = cursor.getDouble(2); // columna precio_base
            productos.add(nombre + " - $" + precio);
        }

        cursor.close();

        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<String>(
                NuevaVentaActivity.this,
                android.R.layout.simple_spinner_item,
                productos
        );
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProductos.setAdapter(adapterSpinner);
    }

    // ========== MÉTODO PARA AGREGAR PRODUCTO A VENTA ==========
    private void agregarProductoAVenta() {
        String productoSeleccionado = spinnerProductos.getSelectedItem().toString();

        if (productoSeleccionado.equals("Seleccione un producto")) {
            Toast.makeText(NuevaVentaActivity.this, "Seleccione un producto", Toast.LENGTH_SHORT).show();
            return;
        }

        String cantidadStr = etCantidad.getText().toString();
        if (cantidadStr.isEmpty()) {
            Toast.makeText(NuevaVentaActivity.this, "Ingrese la cantidad", Toast.LENGTH_SHORT).show();
            return;
        }

        double cantidad = Double.parseDouble(cantidadStr);

        // Obtener ID y precio del producto
        Database dbHelper = new Database(NuevaVentaActivity.this);

        // Extraer solo el nombre del producto (sin el precio)
        String nombreProducto = productoSeleccionado.split(" - ")[0];

        Cursor cursor = dbHelper.obtenerProductos();
        int productoId = 0;
        double precioBase = 0;
        String unidadMedida = "";

        while (cursor.moveToNext()) {
            String nombre = cursor.getString(1);
            if (nombre.equals(nombreProducto)) {
                productoId = cursor.getInt(0);
                precioBase = cursor.getDouble(2);
                unidadMedida = cursor.getString(3);
                break;
            }
        }
        cursor.close();

        if (productoId == 0) {
            Toast.makeText(NuevaVentaActivity.this, "Error: Producto no encontrado", Toast.LENGTH_SHORT).show();
            return;
        }

        double subtotal = 0;
        double peso = 0;

        // Calcular subtotal
        if (nombreProducto.contains("Mojarra")) {
            String pesoStr = etPeso.getText().toString();
            if (pesoStr.isEmpty()) {
                Toast.makeText(NuevaVentaActivity.this, "Ingrese el peso", Toast.LENGTH_SHORT).show();
                return;
            }
            peso = Double.parseDouble(pesoStr);
            subtotal = precioBase * peso; // Precio por kg

            // Agregar a listas temporales
            productosIds.add(productoId);
            cantidades.add(cantidad);
            pesos.add(peso);
            preciosUnitarios.add(precioBase);
            subtotales.add(subtotal);

            // Mostrar en lista
            String item = String.format(Locale.US,
                    "Mojarra: %.1f kg x $%.2f = $%.2f",
                    peso, precioBase, subtotal);
            listaProductosVenta.add(item);

        } else {
            subtotal = precioBase * cantidad;

            // Agregar a listas temporales
            productosIds.add(productoId);
            cantidades.add(cantidad);
            pesos.add(0.0); // Sin peso
            preciosUnitarios.add(precioBase);
            subtotales.add(subtotal);

            // Mostrar en lista
            String item = String.format(Locale.US,
                    "%s: %.0f x $%.2f = $%.2f",
                    nombreProducto, cantidad, precioBase, subtotal);
            listaProductosVenta.add(item);
        }

        total += subtotal;

        // Actualizar interfaz
        tvTotal.setText(String.format(Locale.US, "Total: $%.2f", total));
        adapter.notifyDataSetChanged();

        // Limpiar campos
        etCantidad.setText("");
        etPeso.setText("");
        spinnerProductos.setSelection(0);
    }

    // ========== MÉTODO PARA FINALIZAR VENTA ==========
    private void finalizarVenta() {
        if (productosIds.isEmpty()) {
            Toast.makeText(NuevaVentaActivity.this, "Agregue al menos un producto", Toast.LENGTH_SHORT).show();
            return;
        }

        Database dbHelper = new Database(NuevaVentaActivity.this);

        // Obtener cliente seleccionado (si existe)
        int clienteId = 0; // 0 = cliente local
        String clienteSeleccionado = spinnerClientes.getSelectedItem().toString();

        if (!clienteSeleccionado.equals("Seleccione un cliente") &&
                !clienteSeleccionado.equals("Cliente local (Mostrador)")) {

            // Extraer ID del cliente si está seleccionado uno real
            // Necesitarías implementar esta lógica según cómo guardas los IDs
        }

        // Insertar venta
        long ventaId = dbHelper.insertarVenta(clienteId, total, "local");

        if (ventaId == -1) {
            Toast.makeText(NuevaVentaActivity.this, "Error al crear la venta", Toast.LENGTH_SHORT).show();
            return;
        }

        // Insertar detalles de venta
        boolean detallesExitosos = true;
        for (int i = 0; i < productosIds.size(); i++) {
            long detalleId = dbHelper.insertarDetalleVenta(
                    ventaId,
                    productosIds.get(i),
                    cantidades.get(i),
                    pesos.get(i),
                    preciosUnitarios.get(i),
                    subtotales.get(i)
            );

            if (detalleId == -1) {
                detallesExitosos = false;
            }
        }

        if (detallesExitosos) {
            Toast.makeText(NuevaVentaActivity.this,
                    "Venta registrada exitosamente! ID: " + ventaId,
                    Toast.LENGTH_LONG).show();

            // Limpiar todo
            limpiarCampos();
        } else {
            Toast.makeText(NuevaVentaActivity.this,
                    "Error al registrar algunos detalles",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ========== METODO PARA LIMPIAR CAMPOS ==========
    private void limpiarCampos() {
        listaProductosVenta.clear();
        productosIds.clear();
        cantidades.clear();
        pesos.clear();
        preciosUnitarios.clear();
        subtotales.clear();
        total = 0;

        tvTotal.setText("Total: $0.00");
        adapter.notifyDataSetChanged();

        etCantidad.setText("");
        etPeso.setText("");
        spinnerProductos.setSelection(0);
        spinnerClientes.setSelection(0); // También resetear cliente
    }

    // ========== MÉTODO PARA VOLVER AL MENÚ ==========
    public void volverAlMenu(View view) {
        finish(); // Esto cierra la actividad actual y vuelve a la anterior
    }
}