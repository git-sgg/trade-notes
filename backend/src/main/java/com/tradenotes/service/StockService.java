package com.tradenotes.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tradenotes.entity.KLinePoint;
import com.tradenotes.entity.StockWatch;
import com.tradenotes.mapper.StockWatchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockWatchMapper stockWatchMapper;
    private final StockPriceService stockPriceService;

    private static final String DEFAULT_USER_ID = "default";

    public List<StockWatch> getWatchList() {
        LambdaQueryWrapper<StockWatch> qw = new LambdaQueryWrapper<>();
        qw.eq(StockWatch::getUserId, DEFAULT_USER_ID)
          .orderByDesc(StockWatch::getCreatedAt);
        return stockWatchMapper.selectList(qw);
    }

    public StockWatch addStock(String symbol) {
        // 搜索股票名称（调用 Alpha Vantage SYMBOL_SEARCH，消耗1次请求）
        String name = stockPriceService.searchStockName(symbol);
        StockWatch stock = new StockWatch();
        stock.setUserId(DEFAULT_USER_ID);
        stock.setSymbol(symbol.toUpperCase());
        stock.setName(name);
        stock.setCreatedAt(LocalDateTime.now());
        stockWatchMapper.insert(stock);
        return stock;
    }

    public void deleteStock(Long id) {
        stockWatchMapper.deleteById(id);
    }

    public List<KLinePoint> getKLineData(Long stockId, int days) {
        StockWatch stock = stockWatchMapper.selectById(stockId);
        if (stock == null) return List.of();
        return stockPriceService.getKLineData(stock.getSymbol(), days);
    }

    public boolean isValidSymbol(String symbol) {
        return stockPriceService.isValidSymbol(symbol);
    }

    public StockWatch getById(Long id) {
        return stockWatchMapper.selectById(id);
    }
}
