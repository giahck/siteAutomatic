package com.sitecentral.sitecentral.Controller;

import com.sitecentral.sitecentral.Service.AssetSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AssetController {
    @Autowired
    private AssetSyncService assetSyncService;

    @PostMapping("/sync-selected")
    public ResponseEntity<Map<String, Object>> syncSelected(@RequestBody List<Integer> ids, BindingResult validation) {
        System.out.println("--- COLLAUDO: SONO DENTRO IL CONTROLLER ---");
        if (validation.hasErrors()) {
            String errors = validation.getAllErrors().stream()
                    .map(e -> e.getDefaultMessage())
                    .collect(Collectors.joining("\n"));

            throw new RuntimeException("Richiesta non valida: " + errors);
        }

        new Thread(() -> assetSyncService.syncAssetsToVectorDb(ids)).start();

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Sincronizzazione avviata per " + ids.size() + " asset"
        ));
    }

}
