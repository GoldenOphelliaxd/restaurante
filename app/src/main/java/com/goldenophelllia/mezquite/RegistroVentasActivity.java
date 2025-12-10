package com.goldenophelllia.mezquite;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class RegistroVentasActivity extends AppCompatActivity {

    private ListView listViewVentas;
    private EditText etFechaDesde, etFechaHasta;
    private Button btnHoy, btnAyer, btnMes, btnBuscar, btnVolver;
    private TextView tvTotalVentas, tvMontoTotal;

    private Database dbHelper;
    private SimpleAdapter adapter;
    private ArrayList<HashMap<String, String>> listaVentas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_ventas);

        // 1. INICIALIZAR VISTAS
        listViewVentas = findViewById(R.id.listViewVentas);
        etFechaDesde = findViewById(R.id.etFechaDesde);
        etFechaHasta = findViewById(R.id.etFechaHasta);
        btnHoy = findViewById(R.id.btnHoy);
        btnAyer = findViewById(R.id.btnAyer);
        btnMes = findViewById(R.id.btnMes);
        btnBuscar = findViewById(R.id.btnBuscarVentas);
        btnVolver = findViewById(R.id.btnVolverVentas);
        tvTotalVentas = findViewById(R.id.tvTotalVentas);
        tvMontoTotal = findViewById(R.id.tvMontoTotal);

        // 2. INICIALIZAR BASE DE DATOS
        dbHelper = new Database(this);

        // 3. CONFIGURAR LISTVIEW
        configurarListView();

        // 4. CONFIGURAR BOTONES
        configurarBotones();

        // 5. CARGAR VENTAS DEL DÍA ACTUAL POR DEFECTO
        cargarVentasHoy();
    }

    // MÉTODO 1: Configurar ListView
    private void configurarListView() {
        listaVentas = new ArrayList<>();

        String[] from = {"venta_id", "fecha", "cliente", "total", "productos"};
        int[] to = {
                R.id.tvVentaId,
                R.id.tvVentaFecha,
                R.id.tvVentaCliente,
                R.id.tvVentaTotal,
                R.id.tvVentaProductos
        };

        adapter = new SimpleAdapter(
                this,
                listaVentas,
                R.layout.item_venta,
                from,
                to
        );

        listViewVentas.setAdapter(adapter);

        // Click en item para ver detalles
        listViewVentas.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String ventaId = listaVentas.get(position).get("venta_id");
                mostrarDetalleVenta(Integer.parseInt(ventaId));
            }
        });
    }

    // MÉTODO 2: Configurar botones
    private void configurarBotones() {

        btnHoy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fechaHoy = obtenerFechaActual();
                etFechaDesde.setText(fechaHoy);
                etFechaHasta.setText(fechaHoy);
                buscarVentas();
            }
        });

        btnAyer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fechaAyer = obtenerFechaAyer();
                etFechaDesde.setText(fechaAyer);
                etFechaHasta.setText(fechaAyer);
                buscarVentas();
            }
        });

        btnMes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String primeraDiaMes = obtenerPrimerDiaMes();
                String ultimoDiaMes = obtenerUltimoDiaMes();
                etFechaDesde.setText(primeraDiaMes);
                etFechaHasta.setText(ultimoDiaMes);
                buscarVentas();
            }
        });

        btnBuscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buscarVentas();
            }
        });

        btnVolver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    // MÉTODO 3: Buscar ventas
    private void buscarVentas() {

        String fechaDesde = etFechaDesde.getText().toString().trim();
        String fechaHasta = etFechaHasta.getText().toString().trim();

        // 2. Validar que las fechas no estén vacías
        if (fechaDesde.isEmpty() || fechaHasta.isEmpty()) {
            Toast.makeText(this, "Ingrese las fechas de búsqueda", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Validar formato
        if (!esFormatoFechaValido(fechaDesde) || !esFormatoFechaValido(fechaHasta)) {
            Toast.makeText(this, "Formato de fecha incorrecto. Use AAAA-MM-DD", Toast.LENGTH_SHORT).show();
            return;
        }

        // 4. Validar que fechaDesde <= fechaHasta
        if (fechaDesde.compareTo(fechaHasta) > 0) {
            Toast.makeText(this, "La fecha 'Desde' debe ser menor o igual a 'Hasta'", Toast.LENGTH_SHORT).show();
            return;
        }

        // 5. Ejecutar consulta a la base de datos
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT v.venta_id, v.fecha_venta, v.total_venta, " +
                "COALESCE(c.nombre, 'Cliente General') as cliente_nombre " +
                "FROM VENTAS v " +
                "LEFT JOIN CLIENTES c ON v.cliente_id = c.cliente_id " +
                "WHERE DATE(v.fecha_venta) BETWEEN ? AND ? " +
                "ORDER BY v.fecha_venta DESC";

        Cursor cursor = db.rawQuery(query, new String[]{fechaDesde, fechaHasta});

        // 6. Limpiar lista anterior
        listaVentas.clear();

        int totalVentas = 0;
        double montoTotal = 0.0;

        // 7. Procesar resultados
        while (cursor.moveToNext()) {
            int ventaId = cursor.getInt(0);
            String fecha = cursor.getString(1);
            double total = cursor.getDouble(2);
            String cliente = cursor.getString(3);

            // Obtener productos de esta venta
            String productos = obtenerProductosVenta(ventaId);

            // Formatear fecha
            String fechaFormateada = fecha.split(" ")[0];

            // Crear item para la lista
            HashMap<String, String> venta = new HashMap<>();
            venta.put("venta_id", String.valueOf(ventaId));
            venta.put("fecha", fechaFormateada);
            venta.put("cliente", "Cliente: " + cliente);
            venta.put("total", String.format("$%.2f", total));
            venta.put("productos", productos);

            listaVentas.add(venta);

            // Acumular totales
            totalVentas++;
            montoTotal += total;
        }

        cursor.close();
        db.close();

        // 8. Actualizar resumen
        tvTotalVentas.setText(String.valueOf(totalVentas));
        tvMontoTotal.setText(String.format("$%.2f", montoTotal));

        // 9. Notificar al adaptador
        adapter.notifyDataSetChanged();

        // 10. Mostrar mensaje según resultados
        if (totalVentas == 0) {
            Toast.makeText(this, "No hay ventas en el período seleccionado", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this,
                    String.format("Encontradas %d ventas", totalVentas),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // MÉTODO 4: Validar formato de fecha
    private boolean esFormatoFechaValido(String fecha) {
        // Formato: AAAA-MM-DD
        if (fecha.length() != 10) return false;
        if (fecha.charAt(4) != '-' || fecha.charAt(7) != '-') return false;

        try {
            int año = Integer.parseInt(fecha.substring(0, 4));
            int mes = Integer.parseInt(fecha.substring(5, 7));
            int dia = Integer.parseInt(fecha.substring(8, 10));

            if (año < 2000 || año > 2100) return false;
            if (mes < 1 || mes > 12) return false;
            if (dia < 1 || dia > 31) return false;

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // MÉTODO 5: Obtener productos de una venta
    private String obtenerProductosVenta(int ventaId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT p.nombre, dv.cantidad, dv.peso " +
                "FROM DETALLE_VENTA dv " +
                "JOIN PRODUCTOS p ON dv.producto_id = p.producto_id " +
                "WHERE dv.venta_id = ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(ventaId)});

        StringBuilder productos = new StringBuilder();
        int count = 0;

        while (cursor.moveToNext() && count < 3) {
            String nombre = cursor.getString(0);
            double cantidad = cursor.getDouble(1);
            double peso = cursor.getDouble(2);

            if (count > 0) {
                productos.append(", ");
            }

            if (peso > 0) {
                productos.append(String.format(Locale.US, "%.1f kg %s", peso, nombre));
            } else {
                productos.append(String.format(Locale.US, "%.0f %s", cantidad, nombre));
            }

            count++;
        }

        if (cursor.getCount() > 3) {
            productos.append("...");
        }

        cursor.close();
        db.close();

        return productos.toString();
    }

    // MÉTODO 6: Cargar ventas del día actual
    private void cargarVentasHoy() {
        String fechaHoy = obtenerFechaActual();
        etFechaDesde.setText(fechaHoy);
        etFechaHasta.setText(fechaHoy);
        buscarVentas();
    }

    // MÉTODO 7: Mostrar detalle de venta
    private void mostrarDetalleVenta(final int ventaId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_detalle_venta, null);

        builder.setView(dialogView);

        TextView tvTitulo = dialogView.findViewById(R.id.tvTituloDetalle);
        TextView tvInfo = dialogView.findViewById(R.id.tvInfoVenta);
        ListView listViewDetalles = dialogView.findViewById(R.id.listViewDetalles);
        TextView tvTotal = dialogView.findViewById(R.id.tvDetalleTotal);
        Button btnCerrar = dialogView.findViewById(R.id.btnCerrarDetalle);

        cargarDetalleVenta(ventaId, tvTitulo, tvInfo, tvTotal, listViewDetalles);

        final AlertDialog dialog = builder.create();

        btnCerrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    // MÉTODO 8: Cargar detalle completo de venta
    private void cargarDetalleVenta(int ventaId, TextView tvTitulo, TextView tvInfo,
                                    TextView tvTotal, ListView listViewDetalles) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String queryVenta = "SELECT v.venta_id, v.fecha_venta, v.total_venta, " +
                "COALESCE(c.nombre, 'Cliente General') as cliente, " +
                "v.tipo_venta " +
                "FROM VENTAS v " +
                "LEFT JOIN CLIENTES c ON v.cliente_id = c.cliente_id " +
                "WHERE v.venta_id = ?";

        Cursor cursorVenta = db.rawQuery(queryVenta, new String[]{String.valueOf(ventaId)});

        if (cursorVenta.moveToFirst()) {
            int id = cursorVenta.getInt(0);
            String fecha = cursorVenta.getString(1);
            double total = cursorVenta.getDouble(2);
            String cliente = cursorVenta.getString(3);
            String tipoVenta = cursorVenta.getString(4);

            tvTitulo.setText("Detalle de Venta #" + id);
            tvInfo.setText(String.format("Venta #%d\nFecha: %s\nCliente: %s\nTipo: %s",
                    id, fecha, cliente, tipoVenta));
            tvTotal.setText(String.format("$%.2f", total));

            cargarDetallesProductos(ventaId, listViewDetalles);
        }

        cursorVenta.close();
        db.close();
    }

    // MÉTODO 9: Cargar productos de la venta (para el detalle)
    private void cargarDetallesProductos(int ventaId, ListView listView) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT p.nombre, dv.cantidad, dv.peso, dv.precio_unitario, dv.subtotal " +
                "FROM DETALLE_VENTA dv " +
                "JOIN PRODUCTOS p ON dv.producto_id = p.producto_id " +
                "WHERE dv.venta_id = ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(ventaId)});

        ArrayList<HashMap<String, String>> detalles = new ArrayList<>();

        while (cursor.moveToNext()) {
            String nombre = cursor.getString(0);
            double cantidad = cursor.getDouble(1);
            double peso = cursor.getDouble(2);
            double precio = cursor.getDouble(3);
            double subtotal = cursor.getDouble(4);

            HashMap<String, String> detalle = new HashMap<>();
            detalle.put("nombre", nombre);

            if (peso > 0) {
                detalle.put("detalle", String.format(Locale.US,
                        "%.1f kg x $%.2f = $%.2f", peso, precio, subtotal));
            } else {
                detalle.put("detalle", String.format(Locale.US,
                        "%.0f x $%.2f = $%.2f", cantidad, precio, subtotal));
            }

            detalles.add(detalle);
        }

        cursor.close();
        db.close();

        SimpleAdapter adapterDetalles = new SimpleAdapter(
                this,
                detalles,
                android.R.layout.simple_list_item_2,
                new String[]{"nombre", "detalle"},
                new int[]{android.R.id.text1, android.R.id.text2}
        );

        listView.setAdapter(adapterDetalles);
    }

    // MÉTODOS UTILITARIOS PARA FECHAS

    private String obtenerFechaActual() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date());
    }

    private String obtenerFechaAyer() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(calendar.getTime());
    }

    private String obtenerPrimerDiaMes() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(calendar.getTime());
    }

    private String obtenerUltimoDiaMes() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(calendar.getTime());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}