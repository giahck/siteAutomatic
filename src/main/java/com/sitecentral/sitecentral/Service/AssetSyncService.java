package com.sitecentral.sitecentral.Service;

import com.fasterxml.jackson.annotation.JsonFormat;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AssetSyncService {

    @Autowired
    @Qualifier("mysqlJdbcTemplate")
    private JdbcTemplate mysqlJdbc;

    @Autowired
    @Qualifier("postgresJdbcTemplate")
    private JdbcTemplate postgresJdbc;

    @GrpcClient("vector-service")
    private com.sitecentral.grpc.VectorServiceGrpc.VectorServiceBlockingStub vectorStub;

    public void syncAssetsToVectorDb(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;

        // Creiamo i parametri per la query (es: ?, ?, ?)
        String inSql = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = String.format("SELECT id, name, description FROM asset WHERE id IN (%s)", inSql);

        // 1. Prendi solo gli Asset selezionati da MySQL (via SSH)
        List<Map<String, Object>> assets = mysqlJdbc.queryForList(sql, ids.toArray());

        for (Map<String, Object> asset : assets) {
            System.out.println("DEBUG Mappa MySQL: " + asset);
            Integer id = (Integer) asset.get("id");
            String nome = (String) asset.get("name");
            String description = (String) asset.get("description");
            System.out.println("descrizione"+description);
            try {
                // 2. Chiama Python su Windows via gRPC
                com.sitecentral.grpc.TextRequest request = com.sitecentral.grpc.TextRequest.newBuilder()
                        .setText(nome != null ? nome : "" + " " + (description != null ? description : ""))
                        .build();
                com.sitecentral.grpc.VectorResponse response = vectorStub.getVector(request);
                List<Float> embedding = response.getEmbeddingList();

// 2. LOG: Vedi cosa è arrivato nella console di IntelliJ
                System.out.println("--- DATI RICEVUTI DA PYTHON ---");
                System.out.println("Numero di dimensioni: " + embedding.size()); // Dovrebbe essere 384
                System.out.println("Vettore completo: " + embedding);
                // 3. Salva su PostgreSQL locale (vettore di float)
                String vectorStr = response.getEmbeddingList().toString();
                postgresJdbc.update(
                        "INSERT INTO asset_vectors (id_asset, embedding) VALUES (?, ?::vector) " +
                                "ON CONFLICT (id_asset) DO UPDATE SET embedding = EXCLUDED.embedding",
                        id, vectorStr
                );
                System.out.println("Sincronizzato asset ID: " + id);
            } catch (Exception e) {
                System.err.println("Errore sincronizzazione asset " + id + ": " + e.getMessage());
            }
        }
        System.out.println("Sincronizzazione completata per " + assets.size() + " asset selezionati!");
    }
}