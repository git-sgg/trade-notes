package com.tradenotes.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tradenotes.entity.Store;
import com.tradenotes.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreMapper storeMapper;

    public List<Store> getAllStores() {
        return storeMapper.selectList(new QueryWrapper<>());
    }

    public void saveStores(List<Store> stores) {
        for (Store store : stores) {
            // 先按 id 查，有则更新，无则插入
            Store exist = storeMapper.selectById(store.getId());
            if (exist != null) {
                storeMapper.updateById(store);
            } else {
                storeMapper.insert(store);
            }
        }
    }

    public void saveStore(Store store) {
        Store exist = storeMapper.selectById(store.getId());
        if (exist != null) {
            storeMapper.updateById(store);
        } else {
            storeMapper.insert(store);
        }
    }
}
