<template>
  <div class="map-page">
    <div class="map-header">
      <h2>天津超市地图</h2>
      <input
        v-model="keyword"
        class="search-input"
        placeholder="搜索地点"
        @keyup.enter="searchPlace"
      />
      <button class="btn-search" @click="searchPlace">搜索</button>
      <button class="btn-reset" @click="resetView">重置视图</button>
    </div>
    <div id="baidu-map" class="map-container"></div>
  </div>
</template>

<script setup>
import { onMounted, ref, onUnmounted } from 'vue'
import axios from 'axios'

const BAIDU_AK = 'BnQWoGsmr7dbp7bEjc6VYfJ1BTw8aORk'
const API_BASE = import.meta.env.VITE_API_BASE || ''

const keyword = ref('')
let activeInfoPanel = null
const adjustMode = ref(false)
const adjustTarget = ref(null)
const adjustTargetBrand = ref(null)

let mapInstance = null
let infoWindow = null

// 门店标注数据（原始 WGS-84 坐标）
let storeMarkers = []

// 搜索结果标注
let searchMarkers = []

// 七鲜图标
const SEVEN_FRESH_ICON = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="32" height="40" viewBox="0 0 32 40">' +
  '<defs><filter id="s1" x="-20%" y="-10%" width="140%" height="140%">' +
  '<feDropShadow dx="0" dy="2" stdDeviation="2" flood-opacity="0.3"/></filter></defs>' +
  '<path filter="url(#s1)" d="M16 0 C7 0 0 7 0 16 C0 24 16 40 16 40 C16 40 32 24 32 16 C32 7 25 0 16 0 Z" fill="#1DC269"/>' +
  '<text x="16" y="22" font-size="13" font-weight="bold" fill="#fff" text-anchor="middle" font-family="Arial">鲜</text>' +
  '</svg>'
)

// 盒马图标
const HEMA_ICON = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="32" height="40" viewBox="0 0 32 40">' +
  '<defs><filter id="h1" x="-20%" y="-10%" width="140%" height="140%">' +
  '<feDropShadow dx="0" dy="2" stdDeviation="2" flood-opacity="0.3"/></filter></defs>' +
  '<path filter="url(#h1)" d="M16 0 C7 0 0 7 0 16 C0 24 16 40 16 40 C16 40 32 24 32 16 C32 7 25 0 16 0 Z" fill="#2D5FC7"/>' +
  '<text x="16" y="22" font-size="13" font-weight="bold" fill="#fff" text-anchor="middle" font-family="Arial">盒</text>' +
  '</svg>'
)

// 山姆图标（营业中）
const SAM_ICON = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="32" height="40" viewBox="0 0 32 40">' +
  '<defs><filter id="sa1" x="-20%" y="-10%" width="140%" height="140%">' +
  '<feDropShadow dx="0" dy="2" stdDeviation="2" flood-opacity="0.3"/></filter></defs>' +
  '<path filter="url(#sa1)" d="M16 0 C7 0 0 7 0 16 C0 24 16 40 16 40 C16 40 32 24 32 16 C32 7 25 0 16 0 Z" fill="#8B5CF6"/>' +
  '<text x="16" y="22" font-size="13" font-weight="bold" fill="#fff" text-anchor="middle" font-family="Arial">姆</text>' +
  '</svg>'
)

// 山姆图标（筹建中）
const SAM_ICON_GRAY = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="32" height="40" viewBox="0 0 32 40">' +
  '<defs><filter id="sa2" x="-20%" y="-10%" width="140%" height="140%">' +
  '<feDropShadow dx="0" dy="2" stdDeviation="2" flood-opacity="0.2"/></filter></defs>' +
  '<path filter="url(#sa2)" d="M16 0 C7 0 0 7 0 16 C0 24 16 40 16 40 C16 40 32 24 32 16 C32 7 25 0 16 0 Z" fill="#9CA3AF"/>' +
  '<text x="16" y="22" font-size="13" font-weight="bold" fill="#fff" text-anchor="middle" font-family="Arial">建</text>' +
  '</svg>'
)

// 搜索图标
const SEARCH_ICON = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="32" height="40" viewBox="0 0 32 40">' +
  '<defs><filter id="s2" x="-20%" y="-10%" width="140%" height="140%">' +
  '<feDropShadow dx="0" dy="2" stdDeviation="2" flood-opacity="0.3"/></filter></defs>' +
  '<path filter="url(#s2)" d="M16 0 C7 0 0 7 0 16 C0 24 16 40 16 40 C16 40 32 24 32 16 C32 7 25 0 16 0 Z" fill="#FF6B35"/>' +
  '<circle cx="16" cy="15" r="7" fill="#fff"/>' +
  '<text x="16" y="19" font-size="11" font-weight="bold" fill="#FF6B35" text-anchor="middle" font-family="Arial">搜</text>' +
  '</svg>'
)

// WGS-84 转百度坐标（GCJ-02 → BD-09）
function wgsToBaidu(lat, lng) {
  const x = lng, y = lat
  const z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * Math.PI)
  const t = Math.atan2(y, x) + 0.000003 * Math.cos(x * Math.PI)
  return { lng: z * Math.cos(t) + 0.0065, lat: z * Math.sin(t) + 0.006 }
}

onMounted(() => {
  loadBaiduMap()
})

function loadBaiduMap() {
  const cbName = 'BMapGL_callback_' + Date.now()
  window[cbName] = () => {
    console.log('[Map] BMapGL loaded, typeof BMapGL:', typeof BMapGL)
    initBaiduMap()
    delete window[cbName]
  }
  const script = document.createElement('script')
  const src = `https://api.map.baidu.com/api?v=1.0&type=webgl&ak=${BAIDU_AK}&callback=${cbName}`
  console.log('[Map] loading:', src)
  script.src = src
  script.onload = () => console.log('[Map] script loaded OK')
  script.onerror = () => {
    console.error('[Map] script load failed')
    document.getElementById('baidu-map').innerHTML =
      '<p style="text-align:center;padding:40px;color:#f56c6c;">地图加载失败，请检查网络</p>'
  }
  document.head.appendChild(script)
}

function initBaiduMap() {
  console.log('[Map] initBaiduMap called, typeof BMapGL:', typeof BMapGL)
  if (typeof BMapGL === 'undefined') {
    console.error('[Map] BMapGL not defined!')
    document.getElementById('baidu-map').innerHTML =
      '<p style="text-align:center;padding:40px;color:#f56c6c;">BMapGL 未定义，请检查控制台错误</p>'
    return
  }
  // 天津中心点（WGS-84 转百度坐标）
  const tjCenter = wgsToBaidu(39.08, 117.20)
  mapInstance = new BMapGL.Map('baidu-map')
  mapInstance.centerAndZoom(new BMapGL.Point(tjCenter.lng, tjCenter.lat), 11)
  mapInstance.enableScrollWheelZoom(true)
  console.log('[Map] map created, container size:', document.getElementById('baidu-map').offsetWidth, 'x', document.getElementById('baidu-map').offsetHeight)

  // 创建统一的 InfoWindow（首次点击后创建）
  // 百度地图的 InfoWindow 跟随 marker，这里用全局变量

  // 加载门店数据
  loadStores()

  // 地图点击事件（校准 / 逆向地理编码）
  mapInstance.addEventListener('click', function(e) {
    if (adjustMode.value && adjustTarget.value) {
      // 校准模式：记录新坐标
      const newLat = e.latlng.lat, newLng = e.latlng.lng
      finishAdjust(newLat, newLng, adjustTargetBrand.value)
    } else {
      // 逆向地理编码：显示地址
      reverseGeocode(e.latlng.lng, e.latlng.lat)
    }
  })
}

// 逆向地理编码（用百度免费 API）
function reverseGeocode(lng, lat) {
  const geoc = new BMapGL.Geocoder()
  geoc.getLocation(new BMapGL.Point(lng, lat), (res) => {
    // res 可能是数组（POI 列表）也可能没结果
    const first = Array.isArray(res) ? res[0] : res
    const title = first?.address || '未知地址'
    const iw = new BMapGL.InfoWindow(`<div style="padding:6px 10px;max-width:240px">
      <b>${title}</b>
      <div style="margin-top:4px;font-size:11px;color:#aaa">经度 ${lng.toFixed(5)} | 纬度 ${lat.toFixed(5)}</div>
      <div style="margin-top:6px;color:#888;font-size:11px">点击地图空白处关闭</div></div>`, {
      width: 240, height: 0, offset: new BMapGL.Size(0, -20),
    })
    mapInstance.openInfoWindow(iw, new BMapGL.Point(lng, lat))
  })
}

// 加载门店数据
async function loadStores() {
  try {
    const res = await axios.get(`${API_BASE}/api/stores`)
    if (!res.data.success || !res.data.data.length) return
    const stores = res.data.data

    // 七鲜：brand='seven'
    const sevenStores = stores.filter(s => s.brand === 'seven')
    if (sevenStores.length) renderStores(sevenStores, 'seven', SEVEN_FRESH_ICON)

    // 盒马：brand='hema'
    const hemaStores = stores.filter(s => s.brand === 'hema')
    if (hemaStores.length) renderStores(hemaStores, 'hema', HEMA_ICON)

    // 山姆：brand='sam'，按筹建状态分颜色
    const samStores = stores.filter(s => s.brand === 'sam')
    if (samStores.length) {
      const open = samStores.filter(s => s.status !== 1)
      const closed = samStores.filter(s => s.status === 1)
      if (open.length)   renderStores(open, 'sam', SAM_ICON)
      if (closed.length) renderStores(closed, 'sam-gray', SAM_ICON_GRAY)
    }
  } catch (e) {
    console.error('加载门店失败:', e)
  }
}

// 渲染门店标注
function renderStores(stores, brand, iconSrc) {
  if (!mapInstance) return
  stores.forEach((s) => {
    // 七鲜数据是 GCJ-02（腾讯地图原始坐标），需要转换一次为 BD-09
    // 盒马/山姆数据已是 BD-09，直接使用
    const isGCJ = (brand === 'seven')
    const bd = isGCJ ? wgsToBaidu(s.latitude, s.longitude) : { lng: s.longitude, lat: s.latitude }
    const pt = new BMapGL.Point(bd.lng, bd.lat)
    const icon = new BMapGL.Icon(iconSrc, new BMapGL.Size(32, 40), {
      anchor: new BMapGL.Size(16, 40),
      imageOffset: new BMapGL.Size(0, 0),
    })
    const marker = new BMapGL.Marker(pt, { icon })
    marker.storeData = { ...s, brand }
    mapInstance.addOverlay(marker)
    storeMarkers.push(marker)

    marker.addEventListener('click', (e) => {
      // BMapGL 中 marker click 会传播到 map click，阻止双重弹窗
      if (e.domEvent) e.domEvent.stopPropagation()
      showStoreInfo(s, brand, pt)
    })
  })
}

// 显示门店 InfoWindow — 用自定义 DOM 覆盖层（百度 InfoWindow 在 iframe 沙箱里，window 全局方法失效）
function showStoreInfo(store, brand, pt) {
  closeStoreInfo()
  const statusText = brand === 'sam-gray' ? '<span style="color:#9CA3AF">（筹建中）</span>' : ''
  const el = document.createElement('div')
  el.className = 'store-info-panel'
  el.innerHTML = `
    <div class="sip-header">
      <b>${store.name}</b> ${statusText}
      <span class="sip-close">×</span>
    </div>
    <div class="sip-addr">${store.address || ''}</div>
    <button class="sip-adjust">📍 调整位置</button>
  `
  el.style.cssText = `
    position: absolute; z-index: 1000;
    background: #fff; border-radius: 8px; padding: 10px;
    box-shadow: 0 2px 12px rgba(0,0,0,.25);
    min-width: 200px; max-width: 260px;
    font-size: 13px; color: #333;
  `
  el.querySelector('.sip-close').onclick = closeStoreInfo
  el.querySelector('.sip-adjust').onclick = () => {
    closeStoreInfo()
    enterAdjustMode(store, brand)
  }
  document.body.appendChild(el)
  activeInfoPanel = el
  activeInfoPanelPt = pt
  // 等一帧让浏览器 layout 完成，再定位（否则 offsetHeight = 0）
  requestAnimationFrame(() => updateInfoPanelPos())
  mapInstance.addEventListener('moveend', updateInfoPanelPos)
  mapInstance.addEventListener('zoomend', updateInfoPanelPos)

}

let activeInfoPanelPt = null
function updateInfoPanelPos() {
  const el = activeInfoPanel
  if (!el || !el.isConnected || !activeInfoPanelPt || !mapInstance) return
  const px = mapInstance.pointToOverlayPixel(activeInfoPanelPt)
  const mapDiv = document.getElementById('baidu-map')
  const rect = mapDiv.getBoundingClientRect()
  const w = el.offsetWidth || 220
  const h = el.offsetHeight || 80
  let left = rect.left + px.x - w / 2
  let top = rect.top + px.y - h - 30   // 30px 偏移，避免压住 marker
  // 防止溢出
  if (top < rect.top + 4) top = rect.top + px.y + 30  // 上方没空间就放下面
  if (left < rect.left + 4) left = rect.left + 4
  if (left + w > rect.right - 4) left = rect.right - w - 4
  el.style.left = left + 'px'
  el.style.top = top + 'px'
}

function closeStoreInfo() {
  if (activeInfoPanel && activeInfoPanel.isConnected) {
    activeInfoPanel.remove()
  }
  activeInfoPanel = null
  activeInfoPanelPt = null
  if (mapInstance) {
    mapInstance.removeEventListener('moveend', updateInfoPanelPos)
    mapInstance.removeEventListener('zoomend', updateInfoPanelPos)
  }
  // 正常模式（非校准）下恢复 pointer，否则 BMapGL 拖拽后 restore 为 default 导致标注无法点击
  if (mapInstance && !adjustMode.value) {
    document.getElementById('baidu-map').style.cursor = 'pointer'
  }
}

// 进入校准模式
function enterAdjustMode(store, brand) {
  adjustMode.value = true
  adjustTarget.value = store
  adjustTargetBrand.value = brand
  document.getElementById('baidu-map').style.cursor = 'crosshair !important'
  showAdjustBanner(store.name)
}

function showAdjustBanner(storeName) {
  let banner = document.getElementById('adjust-banner')
  if (!banner) {
    banner = document.createElement('div')
    banner.id = 'adjust-banner'
    banner.style.cssText = `
      position: fixed; top: 70px; left: 50%; transform: translateX(-50%);
      z-index: 2000; background: #2D5FC7; color: #fff;
      padding: 10px 18px; border-radius: 24px;
      box-shadow: 0 4px 12px rgba(0,0,0,.25);
      font-size: 14px;
    `
    document.body.appendChild(banner)
  }
  banner.innerHTML = `📍 校准模式：点击地图上正确位置来调整 <b>${storeName}</b> <button id="adjust-cancel" style="margin-left:10px;background:#fff;color:#2D5FC7;border:none;border-radius:12px;padding:2px 10px;cursor:pointer">取消</button>`
  document.getElementById('adjust-cancel').onclick = cancelAdjust
}

function hideAdjustBanner() {
  const banner = document.getElementById('adjust-banner')
  if (banner) banner.remove()
}

function cancelAdjust() {
  adjustMode.value = false
  adjustTarget.value = null
  adjustTargetBrand.value = null
  document.getElementById('baidu-map').style.cursor = ''
  hideAdjustBanner()
}

// 完成校准
async function finishAdjust(newLat, newLng, brand) {
  console.log('[adjust] finishAdjust called, lat=', newLat, 'lng=', newLng, 'brand=', brand)
  if (!confirm('确认将 [' + adjustTarget.value.name + '] 坐标更新为\n经度: ' + newLat.toFixed(6) + '\n纬度: ' + newLng.toFixed(6) + '？')) {
    adjustMode.value = false; adjustTarget.value = null; adjustTargetBrand.value = null
    document.getElementById('baidu-map').style.cursor = ''
    return
  }
  const id = adjustTarget.value.id
  const api = `${API_BASE}/api/stores/${id}/coordinates`
  try {
    const res = await axios.put(api, { latitude: newLat, longitude: newLng })
    if (res.data.success) {
      adjustTarget.value.latitude = newLat
      adjustTarget.value.longitude = newLng
      // 更新标注位置：点击坐标已是 BD-09，直接用
      const newPt = new BMapGL.Point(newLng, newLat)
      storeMarkers.forEach(m => {
        const sid = m.storeData && (m.storeData.id || m.storeData.uuid || '')
        if (sid === id) m.setPosition(newPt)
      })
      alert('✅ 坐标已保存！')
    } else {
      alert('❌ 保存失败：' + (res.data.message || ''))
    }
  } catch(e) { alert('❌ ' + e.message) }
  adjustMode.value = false; adjustTarget.value = null; adjustTargetBrand.value = null
  document.getElementById('baidu-map').style.cursor = ''
  hideAdjustBanner()
}

// 搜索地点（百度 LocalSearch）
function searchPlace() {
  const kw = keyword.value.trim()
  if (!kw || !mapInstance) return
  clearSearchMarkers()

  const search = new BMapGL.LocalSearch(mapInstance, {
    renderOptions: { map: mapInstance, panel: null },
    pageCapacity: 8,
  })

  search.setMarkersSet = (pois) => {
    searchMarkers = pois.map((p) => {
      const pt = p.point
      const icon = new BMapGL.Icon(SEARCH_ICON, new BMapGL.Size(32, 40), { anchor: new BMapGL.Size(16, 40) })
      const m = new BMapGL.Marker(pt, { icon })
      m.title = p.title
      mapInstance.addOverlay(m)
      m.addEventListener('click', (e) => {
        if (e.domEvent) e.domEvent.stopPropagation()
        const iw = new BMapGL.InfoWindow(`<div style="padding:6px 10px;max-width:200px">
          <b>${p.title}</b><br><span style="font-size:12px;color:#666">${p.address || ''}</span></div>`,
          { width: 200, height: 0, offset: new BMapGL.Size(0, -20) })
        mapInstance.openInfoWindow(iw, pt)
      })
      return m
    })
    if (searchMarkers.length > 0) {
      mapInstance.setViewport(searchMarkers.map(m => m.getPosition()), { margins: [80, 20, 20, 20] })
    }
  }

  search.search(kw)
}

// 清空搜索标注
function clearSearchMarkers() {
  searchMarkers.forEach(m => mapInstance.removeOverlay(m))
  searchMarkers = []
}

// 重置视图
function resetView() {
  if (!mapInstance) return
  clearSearchMarkers()
  const tj = wgsToBaidu(39.08, 117.20)
  mapInstance.centerAndZoom(new BMapGL.Point(tj.lng, tj.lat), 11)
}

onUnmounted(() => {
  storeMarkers = []
  searchMarkers = []
  mapInstance = null
})
</script>

<style>
/* 门店信息面板（动态创建，非 scoped） */
.store-info-panel .sip-header {
  display: flex; align-items: center; justify-content: space-between;
  font-size: 14px; font-weight: bold; color: #222;
  margin-bottom: 6px;
}
.store-info-panel .sip-close {
  cursor: pointer; color: #999; font-size: 18px; line-height: 1;
  padding: 0 4px; border-radius: 4px; transition: background .15s;
}
.store-info-panel .sip-close:hover { background: #f0f0f0; color: #555; }
.store-info-panel .sip-addr {
  font-size: 12px; color: #666; margin-bottom: 8px; line-height: 1.4;
}
.store-info-panel .sip-adjust {
  background: #2D5FC7; color: #fff; border: none; border-radius: 6px;
  padding: 5px 12px; font-size: 13px; cursor: pointer; width: 100%;
  transition: background .15s;
}
.store-info-panel .sip-adjust:hover { background: #1a4bc9; }
#baidu-map { cursor: pointer !important; }
</style>

<style scoped>
.map-page {
  display: flex; flex-direction: column;
  height: 100vh;   /* 撑满整个视口高度 */
  overflow: hidden;
}
.map-header {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 16px; background: #fff;
  box-shadow: 0 2px 6px rgba(0,0,0,.08); flex-shrink: 0;
}
.map-header h2 { margin: 0; font-size: 18px; color: #333; white-space: nowrap; }
.search-input {
  flex: 1; padding: 8px 12px; border: 1px solid #ddd;
  border-radius: 20px; font-size: 14px; outline: none;
  transition: border-color .2s;
}
.search-input:focus { border-color: #2D5FC7; }
.btn-search {
  padding: 8px 16px; background: #2D5FC7; color: #fff;
  border: none; border-radius: 20px; cursor: pointer; font-size: 14px;
}
.btn-search:hover { background: #1a4bc9; }
.btn-reset {
  padding: 8px 14px; background: #f5f5f5; color: #666;
  border: 1px solid #ddd; border-radius: 20px; cursor: pointer; font-size: 14px;
}
.btn-reset:hover { background: #eee; }
.map-container { flex: 1; min-height: 400px; }
:deep(.info-adjust-btn) {
  background: #2D5FC7; color: #fff; border: none; border-radius: 6px;
  padding: 4px 10px; font-size: 12px; cursor: pointer; width: 100%;
}
:deep(.info-adjust-btn:hover) { background: #1a4bc9; }
</style>
