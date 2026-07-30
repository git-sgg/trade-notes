package com.tradenotes.controller;

import com.tradenotes.entity.Store;
import com.tradenotes.service.StoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stores")
@CrossOrigin(origins = "*")
public class StoreController {

    @Autowired
    private StoreService storeService;

    @GetMapping
    public Map<String, Object> getAll(@RequestParam(required = false) String brand) {
        List<Store> stores = storeService.getStores(brand);
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

    @PutMapping("/{id}/coordinates")
    public Map<String, Object> updateCoords(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Map<String, Object> r = new HashMap<>();
        try {
            BigDecimal lng = new BigDecimal(body.get("longitude").toString());
            BigDecimal lat = new BigDecimal(body.get("latitude").toString());
            int updated = storeService.updateCoordinates(id, lng, lat);
            r.put("success", updated > 0);
            r.put("message", updated > 0 ? "坐标已更新" : "未找到该门店");
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", e.getMessage());
        }
        return r;
    }
}
