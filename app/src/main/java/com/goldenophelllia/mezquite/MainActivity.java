package com.goldenophelllia.mezquite;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar base de datos
        Database dbHelper = new Database(this);
        dbHelper.getWritableDatabase(); // Esto crea la BD si no existe

        // Inicializar botones
        Button btnNuevaVenta = findViewById(R.id.btnNuevaVenta);
        Button btnRegistroVentas = findViewById(R.id.btnRegistroVentas);
        Button btnClientes = findViewById(R.id.btnClientes);
        Button btnProveedores = findViewById(R.id.btnProveedores);
        Button btnMateriaPrima = findViewById(R.id.btnMateriaPrima);
        Button btnPlatillos = findViewById(R.id.btnPlatillos);
        Button btnReportes = findViewById(R.id.btnReportes); // ← DECLARADO UNA VEZ

        // Configurar listeners
        btnNuevaVenta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NuevaVentaActivity.class);
                startActivity(intent);
            }
        });

        btnClientes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ClientesActivity.class);
                startActivity(intent);
            }
        });

        btnRegistroVentas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RegistroVentasActivity.class);
                startActivity(intent);
            }
        });

        btnPlatillos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PlatillosActivity.class);
                startActivity(intent);
            }
        });

        btnProveedores.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ProveedoresActivity.class);
                startActivity(intent);
            }
        });

        btnMateriaPrima.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MateriaPrimaActivity.class);
                startActivity(intent);
            }
        });

        // SOLO ESTE LISTENER PARA REPORTES (elimina el anterior)
        btnReportes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ReportesActivity.class);
                startActivity(intent);
            }
        });


    }
}