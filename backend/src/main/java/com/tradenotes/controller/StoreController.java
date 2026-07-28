package com.tradenotes.controller;

import com.tradenotes.entity.Store;
import com.tradenotes.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StoreController {

    private final StoreService storeService;

    @GetMapping
    public Map<String, Object> getAll() {
        List<Store> stores = storeService.getAllStores();
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("data", stores);
        return res;
    }

    @PostMapping
    public Map<String, Object> saveStore(@RequestBody Store store) {
        storeService.saveStore(store);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return res;
    }
}
