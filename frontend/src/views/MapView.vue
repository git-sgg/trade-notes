<template>
  <div class="map-page">
    <div class="map-header">
      <h2>地图</h2>
      <input
        v-model="keyword"
        class="search-input"
        placeholder="搜索地点"
        @keyup.enter="searchPlace"
      />
      <button class="btn-search" @click="searchPlace">搜索</button>
    </div>
    <div id="tencent-map" class="map-container"></div>
  </div>
</template>

<script setup>
import { onMounted, ref, onUnmounted } from 'vue'

const keyword = ref('')
let mapInstance = null
let markers = []

const TMap_KEY = '5RMBZ-DNO6V-CUQPE-5UYKT-RZNM3-EGBUJ'

onMounted(() => {
  loadTencentMap()
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
    document.getElementById('tencent-map').innerHTML = '<p style="text-align:center;padding:20px;">地图加载失败，请检查网络</p>'
  }
  document.head.appendChild(script)
}

function initMap() {
  // 默认中心点：上海
  mapInstance = new TMap.Map(document.getElementById('tencent-map'), {
    center: new TMap.LatLng(31.2304, 121.4737),
    zoom: 11,
    mapStyleId: 'style1',
  })
}

function searchPlace() {
  if (!keyword.value.trim() || !mapInstance) return

  const search = new TMap.service.Search({
    pageSize: 10,
  })

  search.searchRegion({
    keyword: keyword.value,
    regionName: '全国',
    autoExtend: true,
    pageIndex: 0,
    pageSize: 10,
  }).then((result) => {
    // 清除旧标记
    mapInstance.removeOverlays(markers)
    markers = []

    if (!result.data || result.data.length === 0) {
      alert('未找到结果')
      return
    }

    // 第一个结果移动到中心
    const first = result.data[0]
    if (first.location) {
      mapInstance.moveToCenter(first.location, 14)
    }

    // 添加标记
    result.data.forEach((item) => {
      if (item.location) {
        const marker = new TMap.MultiMarker({
          id: 'marker-' + Math.random(),
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
</script>

<style scoped>
.map-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.map-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: #1a1a2e;
  border-bottom: 1px solid #2a2a4a;
}

.map-header h2 {
  margin: 0;
  font-size: 16px;
  color: #e0e0e0;
  white-space: nowrap;
}

.search-input {
  flex: 1;
  padding: 6px 12px;
  border: 1px solid #3a3a5a;
  border-radius: 4px;
  background: #0f0f1a;
  color: #e0e0e0;
  font-size: 14px;
}

.search-input:focus {
  outline: none;
  border-color: #4a9eff;
}

.btn-search {
  padding: 6px 16px;
  background: #4a9eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  white-space: nowrap;
}

.btn-search:hover {
  background: #3a8eef;
}

.map-container {
  flex: 1;
  width: 100%;
  min-height: 400px;
}
</style>
