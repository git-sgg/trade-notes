package com.tradenotes.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tradenotes.entity.Store;
import com.tradenotes.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreMapper storeMapper;

    /** 按品牌获取门店列表，不传 brand 则返回全部 */
    public List<Store> getStores(String brand) {
        if (brand == null || brand.isBlank()) {
            return storeMapper.selectList(null);
        }
        return storeMapper.selectList(new QueryWrapper<Store>().eq("brand", brand));
    }

    public void saveStore(Store store) {
        Store exist = storeMapper.selectById(store.getId());
        if (exist != null) {
            storeMapper.updateById(store);
        } else {
            storeMapper.insert(store);
        }
    }

    public int updateCoordinates(String id, BigDecimal lng, BigDecimal lat) {
        Store r = new Store();
        r.setId(id);
        r.setLongitude(lng);
        r.setLatitude(lat);
        return storeMapper.updateById(r);
    }
}
