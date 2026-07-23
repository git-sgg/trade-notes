package com.tradenotes.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tradenotes.entity.TradeRecord;
import com.tradenotes.mapper.TradeRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeRecordService {

    private final TradeRecordMapper tradeRecordMapper;

    public List<TradeRecord> getByStockId(Long stockId) {
        LambdaQueryWrapper<TradeRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(TradeRecord::getStockId, stockId)
          .orderByAsc(TradeRecord::getTradeDate);
        return tradeRecordMapper.selectList(qw);
    }

    public TradeRecord addRecord(Long stockId, String tradeType, String price,
                                  String tradeDate, String reason, Integer quantity) {
        TradeRecord record = new TradeRecord();
        record.setStockId(stockId);
        record.setTradeType(tradeType.toUpperCase());
        record.setPrice(new java.math.BigDecimal(price));
        record.setQuantity(quantity != null ? quantity : 100);
        record.setTradeDate(LocalDate.parse(tradeDate));
        record.setReason(reason);
        record.setCreatedAt(LocalDateTime.now());
        tradeRecordMapper.insert(record);
        return record;
    }

    public void deleteRecord(Long id) {
        tradeRecordMapper.deleteById(id);
    }

    public TradeRecord updateRecord(Long id, String tradeType, String price,
                                    String tradeDate, String reason, Integer quantity) {
        TradeRecord record = tradeRecordMapper.selectById(id);
        if (record == null) return null;
        record.setTradeType(tradeType.toUpperCase());
        record.setPrice(new java.math.BigDecimal(price));
        record.setQuantity(quantity != null ? quantity : 100);
        record.setTradeDate(LocalDate.parse(tradeDate));
        record.setReason(reason);
        tradeRecordMapper.updateById(record);
        return record;
    }
}
