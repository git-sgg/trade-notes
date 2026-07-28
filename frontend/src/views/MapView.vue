<template>
  <div class="map-page">
    <div class="map-header">
      <h2>七鲜超市地图</h2>
      <input
        v-model="keyword"
        class="search-input"
        placeholder="搜索地点"
        @keyup.enter="searchPlace"
      />
      <button class="btn-search" @click="searchPlace">搜索</button>
      <button class="btn-reset" @click="resetView">重置视图</button>
    </div>
    <div id="tencent-map" class="map-container"></div>
  </div>
</template>

<script setup>
import { onMounted, ref, onUnmounted } from 'vue'
import axios from 'axios'

const keyword = ref('')
let mapInstance = null
let markers = []
let storeMarkers = []
let infoWindow = null

const TMap_KEY = '5RMBZ-DNO6V-CUQPE-5UYKT-RZNM3-EGBUJ'
const API_BASE = import.meta.env.VITE_API_BASE || ''

onMounted(() => {
  loadTencentMap()
  loadStores()
})

onUnmounted(() => {
  if (mapInstance) {
    mapInstance = null
  }
})

function loadTencentMap() {
  const callbackName = '__tmap_callback_' + Date.now()
  window[callbackName] = () => {
    initMap()
    delete window[callbackName]
  }

  const script = document.createElement('script')
  script.src = `https://map.qq.com/api/gljs?v=1.exp&libraries=service&key=${TMap_KEY}&callback=${callbackName}`
  script.onerror = () => {
    document.getElementById('tencent-map').innerHTML =
      '<p style="text-align:center;padding:40px;color:#f56c6c;">地图加载失败，请检查网络</p>'
  }
  document.head.appendChild(script)
}

function initMap() {
  mapInstance = new TMap.Map(document.getElementById('tencent-map'), {
    center: new TMap.LatLng(39.08, 117.20),
    zoom: 11,
    mapStyleId: 'style1',
  })

  // 信息窗口
  infoWindow = new TMap.InfoWindow({
    map: mapInstance,
    visible: false,
    offset: { x: 0, y: -25 },
  })
}

// 加载门店数据
async function loadStores() {
  try {
    const res = await axios.get(`${API_BASE}/api/stores`)
    if (res.data.success && res.data.data.length > 0) {
      renderStoreMarkers(res.data.data)
    }
  } catch (e) {
    console.error('加载门店失败:', e)
  }
}

// 渲染门店标注
function renderStoreMarkers(stores) {
  if (!mapInstance) return

  // 清除旧标注
  if (storeMarkers.length > 0) {
    mapInstance.removeOverlays(storeMarkers)
    storeMarkers = []
  }

  const geometries = stores.map((s, i) => ({
    id: s.id,
    styleId: 'store-marker',
    position: new TMap.LatLng(s.latitude, s.longitude),
  }))

  const markerLayer = new TMap.MultiMarker({
    map: mapInstance,
    styles: {
      'store-marker': new TMap.MarkerStyle({
        width: 32,
        height: 40,
        src: 'https://mapapi.qq.com/web/lbsGL/images/marker.png',
        anchor: { x: 16, y: 40 },
        color: '#1d4ed8',
        size: 24,
      }),
    },
    geometries,
  })

  markerLayer.on('click', (e) => {
    const store = stores.find(s => s.id === e.geometry.id)
    if (store) {
      showInfoWindow(store, e.geometry.position)
    }
  })

  storeMarkers.push(markerLayer)
}

// 显示信息窗口
function showInfoWindow(store, position) {
  if (!infoWindow || !store) return

  const onlineTime = `${store.startTimeOnline || ''} - ${store.endTimeOnline || ''}`
  const offlineTime = `${store.startTimeOffline || ''} - ${store.endTimeOffline || ''}`

  infoWindow.open()
  infoWindow.setContent(`
    <div style="padding:4px 8px;min-width:200px;">
      <div style="font-size:15px;font-weight:600;color:#1d4ed8;margin-bottom:6px;">${store.name}</div>
      <div style="font-size:12px;color:#666;margin-bottom:4px;">📍 ${store.address}</div>
      <div style="font-size:12px;color:#888;margin-bottom:4px;">🕐 线上: ${onlineTime}</div>
      <div style="font-size:12px;color:#888;">🏪 线下: ${offlineTime}</div>
    </div>
  `)
  infoWindow.setPosition(position)
}

// 搜索地点
function searchPlace() {
  if (!keyword.value.trim() || !mapInstance) return

  const search = new TMap.service.Search({ pageSize: 10 })
  search.searchRegion({
    keyword: keyword.value,
    regionName: '全国',
    autoExtend: true,
    pageIndex: 0,
    pageSize: 10,
  }).then((result) => {
    mapInstance.removeOverlays(markers)
    markers = []

    if (!result.data || result.data.length === 0) {
      alert('未找到结果')
      return
    }

    const first = result.data[0]
    if (first.location) {
      mapInstance.moveToCenter(first.location, 14)
    }

    result.data.forEach((item) => {
      if (item.location) {
        const marker = new TMap.MultiMarker({
          id: item.id || Math.random().toString(),
          map: mapInstance,
          styles: {
            'marker': new TMap.MarkerStyle({
              width: 24,
              height: 35,
              anchor: { x: 12, y: 35 },
              src: 'https://mapapi.qq.com/web/mapView/img/poi-marker.png',
            }),
          },
          geometries: [{
            id: item.id || Math.random().toString(),
            styleId: 'marker',
            position: item.location,
          }],
        })
        marker.on('click', () => {
          mapInstance.moveToCenter(item.location, 15)
        })
        markers.push(marker)
      }
    })
  }).catch((err) => {
    console.error('搜索失败:', err)
    alert('搜索失败，请重试')
  })
}

// 重置视图到天津
function resetView() {
  if (mapInstance) {
    mapInstance.moveToCenter(new TMap.LatLng(39.08, 117.20), 11)
  }
}
</script>

<style scoped>
.map-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 45px);
}

.map-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #0d1425;
  border-bottom: 1px solid #1a2540;
  flex-shrink: 0;
}

.map-header h2 {
  margin: 0;
  font-size: 15px;
  color: #e0e6ed;
  white-space: nowrap;
  font-weight: 600;
}

.search-input {
  flex: 1;
  padding: 6px 12px;
  border: 1px solid #2a3a5a;
  border-radius: 4px;
  background: #0f0f1a;
  color: #e0e0e0;
  font-size: 13px;
  outline: none;
}

.search-input:focus {
  border-color: #4a9eff;
}

.btn-search {
  padding: 6px 14px;
  background: #1d4ed8;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  white-space: nowrap;
}

.btn-search:hover {
  background: #2563eb;
}

.btn-reset {
  padding: 6px 12px;
  background: transparent;
  color: #4a5a7a;
  border: 1px solid #2a3a5a;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  white-space: nowrap;
}

.btn-reset:hover {
  border-color: #4a5a7a;
  color: #e0e0e0;
}

.map-container {
  flex: 1;
  width: 100%;
}
</style>
