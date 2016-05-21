package com.hcordeiro.android.InthegraApp.Activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.equalsp.stransthe.Linha;
import com.equalsp.stransthe.Parada;
import com.hcordeiro.android.InthegraApp.InthegraAPI.InthegraServiceSingleton;
import com.hcordeiro.android.InthegraApp.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity de detalhe de parada, exibe as informações básicas e a lista de linhas que passam pela
 * parada, além de direcionar o usuário para a visualização da localização da parada no mapa.
 *
 * Created by hugo on 17/05/16.
 */
public class DetailParadaActivity extends AppCompatActivity {
    private final String TAG = "DetailParada";
    private Parada parada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "OnCreate Called");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_parada);
        parada = (Parada) getIntent().getSerializableExtra("Parada");

        TextView denominacaoParadaTxt = (TextView) findViewById(R.id.denominacaoParadaTxt);
        if (denominacaoParadaTxt != null) {
            denominacaoParadaTxt.setText(parada.getDenomicao());
        }

        TextView codigoParadaTxt = (TextView) findViewById(R.id.codigoParadaTxt);
        if (codigoParadaTxt != null) {
            codigoParadaTxt.setText(parada.getCodigoParada());
        }

        TextView enderecoParadaTxt = (TextView) findViewById(R.id.enderecoParadaTxt);
        if (enderecoParadaTxt != null) {
            enderecoParadaTxt.setText(parada.getEndereco());
        }


        List<Linha> linhas = new ArrayList<>();
        try {
            Log.d(TAG, "Carregando linhas...");
            linhas = InthegraServiceSingleton.getLinhas(parada);
        } catch (IOException e) {
            Log.e(TAG, "Não foi possível recuperar linhas, motivo: " + e.getMessage());
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(DetailParadaActivity.this);
            alertBuilder.setMessage("Não foi possível recuperar recuperar a lista de Linhas da Parada informada");
            alertBuilder.setCancelable(false);
            alertBuilder.setNeutralButton("Certo",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            Intent intent = new Intent(DetailParadaActivity.this, DisplayMenuParadasActivity.class);
                            startActivity(intent);
                        }
                    });
            AlertDialog alert = alertBuilder.create();
            alert.show();
        }

        ArrayAdapter<Linha> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, linhas);

        final ListView listView = (ListView) findViewById(R.id.linhasListView);
        if (listView != null) {
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent myIntent = new Intent(DetailParadaActivity.this, DetailLinhaActivity.class);
                    Linha linha = (Linha) (listView.getItemAtPosition(position));
                    myIntent.putExtra("Linha", linha);
                    startActivity(myIntent);
                }
            });
        }
    }

    public void displayMapActivity(View view) {
        Log.i(TAG, "DisplayMapActivity Called");
        Intent intent = new Intent(this, DisplayMapaParadaActivity.class);
        intent.putExtra("Parada", parada);
        startActivity(intent);
    }
}