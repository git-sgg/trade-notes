package com.tradenotes.controller;

import com.tradenotes.entity.KLinePoint;
import com.tradenotes.entity.StockWatch;
import com.tradenotes.service.StockService;
import com.tradenotes.service.TradeRecordService;
import com.tradenotes.entity.TradeRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@CrossOrigin
public class StockController {

    private final StockService stockService;
    private final TradeRecordService tradeRecordService;

    // ========== 自选股 ==========

    @GetMapping
    public Result<List<StockWatch>> getWatchList() {
        return Result.ok(stockService.getWatchList());
    }

    @PostMapping
    public Result<StockWatch> addStock(@RequestBody Map<String, String> body) {
        String symbol = body.get("symbol");
        if (symbol == null || symbol.trim().isEmpty()) {
            return Result.fail("股票代码不能为空");
        }
        symbol = symbol.trim();

        // Yahoo Finance 数据源验证（允许添加，即使数据源暂时不可用）
        // 数据获取失败时 K线图会为空，但用户仍可手动记录交易
        StockWatch stock = stockService.addStock(symbol);
        return Result.ok(stock);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteStock(@PathVariable Long id) {
        stockService.deleteStock(id);
        return Result.ok(null);
    }

    // ========== K线数据 ==========

    @GetMapping("/{id}/prices")
    public Result<List<KLinePoint>> getPrices(
            @PathVariable Long id,
            @RequestParam(defaultValue = "90") int days) {
        return Result.ok(stockService.getKLineData(id, days));
    }

    // ========== 交易记录 ==========

    @GetMapping("/{id}/records")
    public Result<List<TradeRecord>> getRecords(@PathVariable Long id) {
        return Result.ok(tradeRecordService.getByStockId(id));
    }

    @PostMapping("/{id}/records")
    public Result<TradeRecord> addRecord(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String type = (String) body.get("type");
        String price = String.valueOf(body.get("price"));
        String date = (String) body.get("date");
        String reason = (String) body.get("reason");
        Integer quantity = body.get("quantity") != null
                ? Integer.parseInt(String.valueOf(body.get("quantity"))) : 100;

        if (type == null || price == null || date == null) {
            return Result.fail("参数不完整");
        }
        if (!type.equalsIgnoreCase("BUY") && !type.equalsIgnoreCase("SELL")) {
            return Result.fail("type 必须是 BUY 或 SELL");
        }

        TradeRecord record = tradeRecordService.addRecord(id, type, price, date, reason, quantity);
        return Result.ok(record);
    }

    @DeleteMapping("/records/{id}")
    public Result<Void> deleteRecord(@PathVariable Long id) {
        tradeRecordService.deleteRecord(id);
        return Result.ok(null);
    }

    @PutMapping("/records/{id}")
    public Result<TradeRecord> updateRecord(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String type = (String) body.get("type");
        String price = String.valueOf(body.get("price"));
        String date = (String) body.get("date");
        String reason = (String) body.get("reason");
        Integer quantity = body.get("quantity") != null
                ? Integer.parseInt(String.valueOf(body.get("quantity"))) : 100;

        if (type == null || price == null || date == null) {
            return Result.fail("参数不完整");
        }
        if (!type.equalsIgnoreCase("BUY") && !type.equalsIgnoreCase("SELL")) {
            return Result.fail("type 必须是 BUY 或 SELL");
        }

        TradeRecord record = tradeRecordService.updateRecord(id, type, price, date, reason, quantity);
        if (record == null) {
            return Result.fail("记录不存在");
        }
        return Result.ok(record);
    }
}
