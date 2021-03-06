package com.hcordeiro.android.InthegraApp.Activities.Rotas;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.equalsp.stransthe.Linha;
import com.equalsp.stransthe.rotas.Rota;
import com.google.android.gms.maps.model.LatLng;
import com.hcordeiro.android.InthegraApp.Activities.MenuPrincipalActivity;
import com.hcordeiro.android.InthegraApp.InthegraAPI.AsyncTasks.InthegraRotasAsync;
import com.hcordeiro.android.InthegraApp.InthegraAPI.AsyncTasks.InthegraRotasAsyncResponse;
import com.hcordeiro.android.InthegraApp.R;
import com.hcordeiro.android.InthegraApp.Util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Activity para exibir as rotas entre uma origem e um destino.
 *
 * Created by hugo on 17/05/16.
 */
public class RotasSelecionarRotaActivity extends AppCompatActivity implements InthegraRotasAsyncResponse {
    private final String TAG = "GerarRota";
    private Set<Rota> rotas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "OnCreate Called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rotas_selecionar_rota_activity);
        /* Recupera a origem e o destino setados no menu anterior */
        Bundle bundle = getIntent().getParcelableExtra("Bundle");
        LatLng origem = bundle.getParcelable("Origem");
        LatLng destino = bundle.getParcelable("Destino");

        /* Carrega as rotas possíves*/
        rotas = new TreeSet<>();
        carregarRotas(origem, destino, Util.DISTANCIA_MAXIMA_A_PE);
    }

    /* Carrega as rotas possíveis através do service de rotas. Esse
     * carregamento é feito de maneira assíncrona e quando a resposta
     * é recebida a função processFinish é chamada */
    private void carregarRotas(LatLng origem, LatLng destino, Double distanciaMaxima) {
        Log.d(TAG, "carregarRotas Called");
        InthegraRotasAsync asyncTask =  new InthegraRotasAsync(RotasSelecionarRotaActivity.this);
        asyncTask.delegate = this;
        asyncTask.execute(origem, destino, distanciaMaxima);
    }

    /**
     * Função que processa o resultado da chamada assíncrona ao service de rotas.
     *
     * @param result List de rotas entre a origem e o destino
     */
    @Override
    public void processFinish(Set<Rota> result) {
        Log.d(TAG, "processFinish Called");
        rotas = result;
        List<Rota> listaRotas = new ArrayList<>();
        listaRotas.addAll(rotas);

        /* Se a lista de rotas retornar vazia,
         * i.e. não há rotas diretas entre a origem e o destino,
         * um alerta é exibido e a activity é finalizada*/
        if (listaRotas.isEmpty()) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(RotasSelecionarRotaActivity.this);
            alertBuilder.setMessage("Não foram encontradas rotas para a origem e o destino selecionados...");
            alertBuilder.setCancelable(false);
            alertBuilder.setNeutralButton("Certo",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            Intent intent = new Intent(RotasSelecionarRotaActivity.this, MenuPrincipalActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });
            AlertDialog alert = alertBuilder.create();
            alert.show();
        }

        /* A lista de rotas retornada pelo service contém várias paradas para uma mesma linha.
         * Por isso, agrupamos as rotas por linha, para a seguir escolher a parada mais próxima. */
        Map<Linha, List<Rota>> rotasPorLinha = new HashMap<>();
        for (Rota r : rotas) {
            Linha l = r.getTrechos().get(1).getLinha();
            if (rotasPorLinha.containsKey(l)) {
                rotasPorLinha.get(l).add(r);
            } else {
                List<Rota> rotasDaLinha = new ArrayList<>();
                rotasDaLinha.add(r);
                rotasPorLinha.put(l, rotasDaLinha);
            }
        }

        /* Seleciona apenas as paradas mais próximas de cada linha para exibir na tela */
        ArrayList<Rota> rotasList = new ArrayList<>();
        for (List<Rota> rotas : rotasPorLinha.values()) {
            Iterator<Rota> iterator = rotas.iterator();
            Rota r = iterator.next();
            Rota melhorRota = r;
            Double melhorDistancia = r.getTrechos().get(0).getDistancia();

            while (iterator.hasNext()) {
                r = iterator.next();
                Double distancia = r.getTrechos().get(0).getDistancia();
                if (distancia.compareTo(melhorDistancia) > 0) {
                    melhorDistancia = distancia;
                    melhorRota = r;
                }
            }

            rotasList.add(melhorRota);
        }

        /* Ordena as rotas pela proximidade da origem para a primeira parada. */
        Collections.sort(rotasList, new Comparator<Rota>() {
            @Override
            public int compare(Rota r1, Rota r2) {
                Double d1 = r1.getTrechos().get(0).getDistancia();
                Double d2 = r2.getTrechos().get(0).getDistancia();
                return d1.compareTo(d2);
            }
        });

        /* Adapter que observará a lista de rotas */
        RotasAdapter adapter = new RotasAdapter(this, rotasList);

        /* Recupera a ListView que irá conter as informações das linhas que passam naquela parada */
        final ListView listView = (ListView) findViewById(R.id.rotasListView);
        if (listView != null) {
            listView.setAdapter(adapter);

            /* Seta a função que será executada ao clicar em algum item da ListView */
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    /* Recupera a linha que foi selecionada na ListView */
                    Rota rota = (Rota) (listView.getItemAtPosition(position));

                     /* Inicia a atividade que exibe o mapa da rota */
                    Intent myIntent = new Intent(RotasSelecionarRotaActivity.this, RotasMapaActivity.class);
                    myIntent.putExtra("Rota", rota);
                    startActivity(myIntent);
                }
            });
        }
    }
}
