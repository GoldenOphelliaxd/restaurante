package com.goldenophelllia.mezquite;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ReportesActivity extends AppCompatActivity {

    private Database dbHelper;
    private TextView tvFechaSeleccionada;
    private Button btnHoy, btnAyer, btnEsteMes, btnGenerarReporte;
    private LinearLayout layoutResumen, layoutDetalle;
    private Calendar fechaActual;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportes);

        dbHelper = new Database(this);
        fechaActual = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Inicializar vistas
        tvFechaSeleccionada = findViewById(R.id.tvFechaSeleccionada);
        btnHoy = findViewById(R.id.btnHoy);
        btnAyer = findViewById(R.id.btnAyer);
        btnEsteMes = findViewById(R.id.btnEsteMes);
        btnGenerarReporte = findViewById(R.id.btnGenerarReporte);
        layoutResumen = findViewById(R.id.layoutResumen);
        layoutDetalle = findViewById(R.id.layoutDetalle);

        // Configurar fecha inicial
        actualizarFecha(fechaActual);

        // Configurar botones
        btnHoy.setOnClickListener(v -> {
            fechaActual = Calendar.getInstance();
            actualizarFecha(fechaActual);
            generarReporteDiario();
        });

        btnAyer.setOnClickListener(v -> {
            Calendar ayer = Calendar.getInstance();
            ayer.add(Calendar.DAY_OF_MONTH, -1);
            fechaActual = ayer;
            actualizarFecha(fechaActual);
            generarReporteDiario();
        });

        btnEsteMes.setOnClickListener(v -> {
            generarReporteMensual();
        });

        btnGenerarReporte.setOnClickListener(v -> {
            mostrarSelectorFecha();
        });

        // Generar reporte del día actual al iniciar
        generarReporteDiario();
    }

    private void actualizarFecha(Calendar fecha) {
        String fechaStr = dateFormat.format(fecha.getTime());
        tvFechaSeleccionada.setText("Fecha: " + fechaStr);
    }

    private void mostrarSelectorFecha() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    fechaActual.set(year, month, dayOfMonth);
                    actualizarFecha(fechaActual);
                    generarReporteDiario();
                },
                fechaActual.get(Calendar.YEAR),
                fechaActual.get(Calendar.MONTH),
                fechaActual.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void generarReporteDiario() {
        String fecha = obtenerFechaSQL(fechaActual);

        // Limpiar layouts
        layoutResumen.removeAllViews();
        layoutDetalle.removeAllViews();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            // 1. RESUMEN DEL DÍA
            String queryResumen = "SELECT " +
                    "COUNT(*) as total_ventas, " +
                    "SUM(v.total_venta) as total_dinero, " +
                    "SUM(CASE WHEN v.tipo_venta = 'local' THEN 1 ELSE 0 END) as ventas_local, " +
                    "SUM(CASE WHEN v.tipo_venta = 'para_llevar' THEN 1 ELSE 0 END) as ventas_llevar " +
                    "FROM VENTAS v " +
                    "WHERE DATE(v.fecha_venta) = ? " +
                    "AND v.estado = 'pagada'";

            Cursor cursorResumen = db.rawQuery(queryResumen, new String[]{fecha});

            if (cursorResumen.moveToFirst()) {
                int totalVentas = cursorResumen.getInt(0);
                double totalDinero = cursorResumen.getDouble(1);
                int ventasLocal = cursorResumen.getInt(2);
                int ventasLlevar = cursorResumen.getInt(3);

                // Crear card de resumen
                CardView cardResumen = new CardView(this);
                cardResumen.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
                cardResumen.setCardElevation(4);
                cardResumen.setRadius(8);
                cardResumen.setContentPadding(16, 16, 16, 16);

                LinearLayout cardLayout = new LinearLayout(this);
                cardLayout.setOrientation(LinearLayout.VERTICAL);

                TextView tvTitulo = new TextView(this);
                tvTitulo.setText(" RESUMEN DEL DÍA");
                tvTitulo.setTextSize(18);
                tvTitulo.setTypeface(tvTitulo.getTypeface(), android.graphics.Typeface.BOLD);
                tvTitulo.setTextColor(Color.parseColor("#2E7D32"));
                cardLayout.addView(tvTitulo);

                addItemResumen(cardLayout, " Total Vendido:", formatDinero(totalDinero));
                addItemResumen(cardLayout, " Total Ventas:", String.valueOf(totalVentas));
                addItemResumen(cardLayout, " Ventas Local:", String.valueOf(ventasLocal));
                addItemResumen(cardLayout, " Ventas Para Llevar:", String.valueOf(ventasLlevar));

                cardResumen.addView(cardLayout);
                layoutResumen.addView(cardResumen);
            }
            cursorResumen.close();

            // 2. PRODUCTOS MÁS VENDIDOS
            String queryProductos = "SELECT " +
                    "p.nombre, " +
                    "SUM(dv.cantidad) as total_piezas, " +
                    "SUM(dv.peso) as total_peso, " +
                    "SUM(dv.subtotal) as total_venta " +
                    "FROM DETALLE_VENTA dv " +
                    "JOIN PRODUCTOS p ON dv.producto_id = p.producto_id " +
                    "JOIN VENTAS v ON dv.venta_id = v.venta_id " +
                    "WHERE DATE(v.fecha_venta) = ? " +
                    "AND v.estado = 'pagada' " +
                    "GROUP BY dv.producto_id " +
                    "ORDER BY total_venta DESC";

            Cursor cursorProductos = db.rawQuery(queryProductos, new String[]{fecha});

            if (cursorProductos.getCount() > 0) {
                TextView tvTituloProductos = new TextView(this);
                tvTituloProductos.setText("\n PRODUCTOS MÁS VENDIDOS");
                tvTituloProductos.setTextSize(16);
                tvTituloProductos.setTypeface(tvTituloProductos.getTypeface(), android.graphics.Typeface.BOLD);
                tvTituloProductos.setTextColor(Color.parseColor("#1976D2"));
                tvTituloProductos.setPadding(0, 16, 0, 8);
                layoutDetalle.addView(tvTituloProductos);

                int contador = 1;
                while (cursorProductos.moveToNext()) {
                    String nombre = cursorProductos.getString(0);
                    double piezas = cursorProductos.getDouble(1);
                    double peso = cursorProductos.getDouble(2);
                    double venta = cursorProductos.getDouble(3);

                    CardView cardProducto = new CardView(this);
                    cardProducto.setCardBackgroundColor(Color.WHITE);
                    cardProducto.setCardElevation(2);
                    cardProducto.setRadius(4);
                    cardProducto.setContentPadding(12, 12, 12, 12);

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 0, 0, 8);
                    cardProducto.setLayoutParams(params);

                    LinearLayout productoLayout = new LinearLayout(this);
                    productoLayout.setOrientation(LinearLayout.VERTICAL);

                    // Número y nombre
                    TextView tvNumNombre = new TextView(this);
                    tvNumNombre.setText(contador + ". " + nombre);
                    tvNumNombre.setTextSize(14);
                    tvNumNombre.setTypeface(tvNumNombre.getTypeface(), android.graphics.Typeface.BOLD);
                    productoLayout.addView(tvNumNombre);

                    // Detalles
                    LinearLayout detallesLayout = new LinearLayout(this);
                    detallesLayout.setOrientation(LinearLayout.VERTICAL);
                    detallesLayout.setPadding(16, 4, 0, 4);

                    if (peso > 0) {
                        addItemDetalle(detallesLayout, " Peso vendido:", String.format("%.2f kg", peso));
                    }
                    if (piezas > 0) {
                        addItemDetalle(detallesLayout, " Piezas vendidas:", String.format("%.0f", piezas));
                    }
                    addItemDetalle(detallesLayout, " Total venta:", formatDinero(venta));

                    productoLayout.addView(detallesLayout);
                    cardProducto.addView(productoLayout);
                    layoutDetalle.addView(cardProducto);

                    contador++;
                }
            } else {
                TextView tvNoVentas = new TextView(this);
                tvNoVentas.setText("No hay ventas registradas para este día");
                tvNoVentas.setTextSize(14);
                tvNoVentas.setTextColor(Color.GRAY);
                tvNoVentas.setPadding(0, 16, 0, 16);
                layoutDetalle.addView(tvNoVentas);
            }
            cursorProductos.close();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al generar reporte: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            db.close();
        }
    }

    private void generarReporteMensual() {
        Calendar inicioMes = Calendar.getInstance();
        inicioMes.set(Calendar.DAY_OF_MONTH, 1);

        Calendar hoy = Calendar.getInstance();

        String fechaInicio = obtenerFechaSQL(inicioMes);
        String fechaFin = obtenerFechaSQL(hoy);

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT " +
                "DATE(v.fecha_venta) as fecha, " +
                "COUNT(*) as ventas_dia, " +
                "SUM(v.total_venta) as total_dia " +
                "FROM VENTAS v " +
                "WHERE DATE(v.fecha_venta) BETWEEN ? AND ? " +
                "AND v.estado = 'pagada' " +
                "GROUP BY DATE(v.fecha_venta) " +
                "ORDER BY fecha DESC";

        Cursor cursor = db.rawQuery(query, new String[]{fechaInicio, fechaFin});

        // Limpiar layouts
        layoutResumen.removeAllViews();
        layoutDetalle.removeAllViews();

        // Mostrar resumen mensual
        CardView cardMensual = new CardView(this);
        cardMensual.setCardBackgroundColor(Color.parseColor("#E3F2FD"));
        cardMensual.setCardElevation(4);
        cardMensual.setRadius(8);
        cardMensual.setContentPadding(16, 16, 16, 16);

        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitulo = new TextView(this);
        tvTitulo.setText(" RESUMEN MENSUAL");
        tvTitulo.setTextSize(18);
        tvTitulo.setTypeface(tvTitulo.getTypeface(), android.graphics.Typeface.BOLD);
        tvTitulo.setTextColor(Color.parseColor("#1565C0"));
        cardLayout.addView(tvTitulo);

        double totalMes = 0;
        int diasConVentas = cursor.getCount();

        while (cursor.moveToNext()) {
            totalMes += cursor.getDouble(2);
        }
        cursor.close();

        addItemResumen(cardLayout, " Total del Mes:", formatDinero(totalMes));
        addItemResumen(cardLayout, " Días con ventas:", String.valueOf(diasConVentas));
        if (diasConVentas > 0) {
            addItemResumen(cardLayout, " Promedio diario:", formatDinero(totalMes / diasConVentas));
        }

        cardMensual.addView(cardLayout);
        layoutResumen.addView(cardMensual);

        // Mostrar detalle por día
        cursor = db.rawQuery(query, new String[]{fechaInicio, fechaFin});

        if (cursor.getCount() > 0) {
            TextView tvTituloDetalle = new TextView(this);
            tvTituloDetalle.setText("\n VENTAS POR DÍA");
            tvTituloDetalle.setTextSize(16);
            tvTituloDetalle.setTypeface(tvTituloDetalle.getTypeface(), android.graphics.Typeface.BOLD);
            tvTituloDetalle.setTextColor(Color.parseColor("#1976D2"));
            tvTituloDetalle.setPadding(0, 16, 0, 8);
            layoutDetalle.addView(tvTituloDetalle);

            while (cursor.moveToNext()) {
                String fecha = cursor.getString(0);
                int ventasDia = cursor.getInt(1);
                double totalDia = cursor.getDouble(2);

                CardView cardDia = new CardView(this);
                cardDia.setCardBackgroundColor(Color.WHITE);
                cardDia.setCardElevation(2);
                cardDia.setRadius(4);
                cardDia.setContentPadding(12, 12, 12, 12);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 8);
                cardDia.setLayoutParams(params);

                LinearLayout diaLayout = new LinearLayout(this);
                diaLayout.setOrientation(LinearLayout.VERTICAL);

                TextView tvFecha = new TextView(this);
                tvFecha.setText(" " + fecha);
                tvFecha.setTextSize(14);
                tvFecha.setTypeface(tvFecha.getTypeface(), android.graphics.Typeface.BOLD);
                diaLayout.addView(tvFecha);

                LinearLayout detallesLayout = new LinearLayout(this);
                detallesLayout.setOrientation(LinearLayout.VERTICAL);
                detallesLayout.setPadding(16, 4, 0, 4);

                addItemDetalle(detallesLayout, " Ventas:", String.valueOf(ventasDia));
                addItemDetalle(detallesLayout, " Total:", formatDinero(totalDia));

                diaLayout.addView(detallesLayout);
                cardDia.addView(diaLayout);
                layoutDetalle.addView(cardDia);
            }
        }

        cursor.close();
        db.close();
    }

    private void addItemResumen(LinearLayout layout, String label, String value) {
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setPadding(0, 4, 0, 4);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextSize(14);
        tvLabel.setTypeface(tvLabel.getTypeface(), android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        tvLabel.setLayoutParams(labelParams);

        TextView tvValue = new TextView(this);
        tvValue.setText(value);
        tvValue.setTextSize(14);
        tvValue.setTextColor(Color.parseColor("#388E3C"));

        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        valueParams.gravity = android.view.Gravity.END;
        tvValue.setLayoutParams(valueParams);

        itemLayout.addView(tvLabel);
        itemLayout.addView(tvValue);
        layout.addView(itemLayout);
    }

    private void addItemDetalle(LinearLayout layout, String label, String value) {
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setPadding(0, 2, 0, 2);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextSize(12);

        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        tvLabel.setLayoutParams(labelParams);

        TextView tvValue = new TextView(this);
        tvValue.setText(value);
        tvValue.setTextSize(12);
        tvValue.setTypeface(tvValue.getTypeface(), android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        valueParams.gravity = android.view.Gravity.END;
        tvValue.setLayoutParams(valueParams);

        itemLayout.addView(tvLabel);
        itemLayout.addView(tvValue);
        layout.addView(itemLayout);
    }

    private String obtenerFechaSQL(Calendar calendar) {
        SimpleDateFormat sqlFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sqlFormat.format(calendar.getTime());
    }

    private String formatDinero(double cantidad) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));
        return formato.format(cantidad);
    }
    // Método que se ejecuta al hacer clic en el botón
    public void volverAlMenu(View view) {
        finish(); // Esto cierra la actividad actual y vuelve a la anterior
    }
}