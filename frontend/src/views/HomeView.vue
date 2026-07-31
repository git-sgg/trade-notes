<template>
  <div class="app">

    <!-- 顶部：添加股票 -->
    <div class="topbar">
      <div class="add-stock">
        <input
          v-model="newSymbol"
          class="symbol-input"
          placeholder="输入股票代码，如 AAPL / 600519"
          @keyup.enter="handleAddStock"
        />
        <button class="btn-add" @click="handleAddStock" :disabled="adding">
          {{ adding ? '加载中…' : '添加' }}
        </button>
      </div>
      <div v-if="addError" class="error-msg">{{ addError }}</div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading">加载中…</div>

    <!-- 股票卡片列表 -->
    <div v-else class="stock-list">
      <div v-if="stocks.length === 0" class="empty-hint">
        还没有添加自选股，请在上方输入股票代码添加
      </div>

      <div
        v-for="stock in stocks"
        :key="stock.id"
        class="stock-card"
      >
        <!-- 卡片头部 -->
        <div class="card-header">
          <div class="stock-info">
            <div class="stock-name-row">
              <span class="stock-name">{{ stock.name || stock.symbol }}</span>
              <span class="stock-symbol">{{ stock.symbol }}</span>
            </div>
            <div v-if="calcStockPnL(stock)" class="stock-pnl" :class="calcStockPnL(stock).isProfit ? 'profit' : 'loss'">
              <div class="pnl-row1">
                <span class="pnl-price">${{ calcStockPnL(stock).currentPrice }}</span>
                <span class="pnl-sep">｜成本 ${{ calcStockPnL(stock).avgBuyPrice }}</span>
                <span class="pnl-sep">｜剩 {{ calcStockPnL(stock).totalQty }} 股</span>
                <span class="pnl-pct">{{ calcStockPnL(stock).abs }}</span>
              </div>
              <div class="pnl-row2">
                <span class="pnl-realized">已实现 {{ calcStockPnL(stock).realized }}</span>
                <span class="pnl-sep">｜</span>
                <span class="pnl-unrealized">浮动 {{ calcStockPnL(stock).unrealized }}</span>
                <span class="pnl-sep">｜</span>
                <span class="pnl-pct2">{{ calcStockPnL(stock).pct }}</span>
              </div>
            </div>
            <div v-else-if="stock.chartData && stock.chartData.length > 0" class="stock-pnl no-trade">
              <span class="pnl-price">${{ stock.chartData[stock.chartData.length - 1].close }}</span>
            </div>
          </div>
          <div class="card-actions">
            <button class="btn-buy" @click="openTradeModal(stock, 'BUY')">买入</button>
            <button class="btn-sell" @click="openTradeModal(stock, 'SELL')">卖出</button>
            <button class="btn-delete" @click="handleDeleteStock(stock.id)">删除</button>
          </div>
        </div>

        <!-- K线图 -->
        <div class="chart-wrapper">
          <div v-if="stock.priceLoading" class="chart-loading">正在获取K线数据…</div>
          <div v-else-if="stock.priceError" class="chart-error">{{ stock.priceError }}</div>
          <div v-else-if="stock.chartData.length === 0" class="chart-error">暂无K线数据</div>
          <div
            v-else
            ref="chartRefs"
            class="chart"
            :data-stock-id="stock.id"
          ></div>
        </div>

        <!-- 交易记录列表（备用/摘要） -->
        <div v-if="stock.records && stock.records.length > 0" class="records-list">
          <div
            v-for="rec in stock.records"
            :key="rec.id"
            class="record-item"
            :class="rec.tradeType.toLowerCase()"
          >
            <span class="rec-type">{{ rec.tradeType === 'BUY' ? '买入' : '卖出' }}</span>
            <span class="rec-price">${{ rec.price }}</span>
            <span class="rec-qty">{{ rec.quantity }}股</span>
            <span class="rec-date">{{ rec.tradeDate }}</span>
            <span class="rec-reason">{{ rec.reason }}</span>
            <button class="rec-edit" @click="openEditTradeModal(stock, rec)">✎</button>
            <button class="rec-del" @click="handleDeleteRecord(stock.id, rec.id)">×</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 交易弹窗 -->
    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="modal">
        <div class="modal-title">
          {{ editingRecord ? '编辑' : '记录' }} {{ modalType === 'BUY' ? '买入' : '卖出' }}：{{ currentStock?.name }} ({{ currentStock?.symbol }})
        </div>
        <div class="modal-body">
          <div class="form-row">
            <label>价格</label>
            <input v-model="form.price" type="number" step="0.01" placeholder="例如 150.00" />
          </div>
          <div class="form-row">
            <label>数量（股）</label>
            <input v-model.number="form.quantity" type="number" step="100" min="100" placeholder="100" />
          </div>
          <div class="form-row">
            <label>日期</label>
            <input v-model="form.date" type="date" />
          </div>
          <div class="form-row">
            <label>理由</label>
            <textarea v-model="form.reason" placeholder="为什么买入/卖出？" rows="3"></textarea>
          </div>
          <div v-if="modalError" class="error-msg">{{ modalError }}</div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showModal = false">取消</button>
          <button class="btn-confirm" @click="handleConfirmTrade">
            {{ confirming ? '保存中…' : (editingRecord ? '保存' : '确认') }}
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, reactive, nextTick, onMounted } from 'vue'
import * as echarts from 'echarts'
import { getStocks, addStock, deleteStock, getPrices, getRecords, addRecord, deleteRecord, updateRecord } from '../api/index.js'

// ========== 状态 ==========
const stocks = ref([])
const newSymbol = ref('')
const adding = ref(false)
const addError = ref('')
const loading = ref(true)

// 弹窗状态
const showModal = ref(false)
const modalType = ref('BUY')   // BUY / SELL
const currentStock = ref(null)
const editingRecord = ref(null)  // null = 新增模式；有值 = 编辑模式
const modalError = ref('')
const confirming = ref(false)
const form = reactive({ price: '', date: '', reason: '', quantity: 100 })

// chart refs map
const chartInstances = {}

// ========== 生命周期 ==========
onMounted(async () => {
  await loadStocks()
  loading.value = false
})

// ========== 加载自选股 ==========
async function loadStocks() {
  const res = await getStocks()
  if (res.success) {
    // 先初始化每个股票的默认值，避免模板访问 undefined 字段崩溃
    stocks.value = res.data.map(s => ({
      ...s,
      priceLoading: false,
      priceError: '',
      chartData: [],
      records: [],
    }))
    await nextTick()
    for (const stock of stocks.value) {
      loadStockData(stock)  // 并发加载，不要 await
    }
  }
}

// 加载单个股票的数据（K线 + 交易记录）
async function loadStockData(stock) {
  stock.priceLoading = true
  stock.priceError = ''
  stock.chartData = []
  stock.records = []

  // 并行获取K线和交易记录
  const [priceRes, recordRes] = await Promise.all([
    getPrices(stock.id, 180).catch(() => ({ success: false, data: [] })),
    getRecords(stock.id).catch(() => ({ success: false, data: [] })),
  ])

  if (priceRes.success) {
    stock.chartData = priceRes.data
  } else {
    stock.priceError = '无法获取K线数据，请检查网络或股票代码'
  }

  if (recordRes.success) {
    stock.records = recordRes.data
  }

  stock.priceLoading = false

  await nextTick()
  renderChart(stock)
}

// ========== 添加股票 ==========
async function handleAddStock() {
  const symbol = newSymbol.value.trim()
  if (!symbol) {
    addError.value = '请输入股票代码'
    return
  }
  addError.value = ''
  adding.value = true
  try {
    const res = await addStock(symbol)
    if (res.success) {
      newSymbol.value = ''
      await loadStocks()
    } else {
      addError.value = res.message || '添加失败'
    }
  } catch (e) {
    addError.value = '网络错误，请检查后端服务是否启动'
  } finally {
    adding.value = false
  }
}

// ========== 删除股票 ==========
async function handleDeleteStock(id) {
  await deleteStock(id)
  // 销毁图表实例
  if (chartInstances[id]) {
    chartInstances[id].dispose()
    delete chartInstances[id]
  }
  await loadStocks()
}

// ========== 交易弹窗 ==========
function openTradeModal(stock, type) {
  editingRecord.value = null
  currentStock.value = stock
  modalType.value = type
  form.price = ''
  form.date = new Date().toISOString().slice(0, 10)
  form.reason = ''
  form.quantity = 100
  modalError.value = ''
  showModal.value = true
}

function openEditTradeModal(stock, record) {
  editingRecord.value = record
  currentStock.value = stock
  modalType.value = record.tradeType
  form.price = record.price
  form.date = record.tradeDate
  form.reason = record.reason || ''
  form.quantity = record.quantity || 100
  modalError.value = ''
  showModal.value = true
}

async function handleConfirmTrade() {
  if (!form.price || !form.date) {
    modalError.value = '请填写价格和日期'
    return
  }
  modalError.value = ''
  confirming.value = true
  try {
    const payload = {
      type: modalType.value,
      price: form.price,
      date: form.date,
      reason: form.reason,
      quantity: form.quantity,
    }
    const res = editingRecord.value
      ? await updateRecord(editingRecord.value.id, payload)
      : await addRecord(currentStock.value.id, payload)
    if (res.success) {
      showModal.value = false
      editingRecord.value = null
      await loadStockData(currentStock.value)
    } else {
      modalError.value = res.message
    }
  } catch (e) {
    modalError.value = '网络错误'
  } finally {
    confirming.value = false
  }
}

// ========== 删除交易记录 ==========
async function handleDeleteRecord(stockId, recordId) {
  await deleteRecord(recordId)
  const stock = stocks.value.find(s => s.id === stockId)
  if (stock) {
    await loadStockData(stock)
  }
}

// ========== 盈亏计算 ==========
/**
 * 返回 { pct, currentPrice, avgBuyPrice, abs, realized, unrealized, remainingQty, totalInvested }
 * abs: 总盈亏 = 已实现 + 浮动盈亏
 * realized: 已实现盈亏（卖出落袋）
 * unrealized: 浮动盈亏（剩余持股）
 * remainingQty: 剩余持股数
 * totalInvested: 总买入成本
 */
function calcStockPnL(stock) {
  const prices = stock.chartData
  const records = stock.records || []
  if (!prices || prices.length === 0 || records.length === 0) return null

  const currentPrice = parseFloat(prices[prices.length - 1].close)

  // 全部买入记录，按时间排序（FIFO）
  const buys = records
    .filter(r => r.tradeType === 'BUY')
    .sort((a, b) => a.tradeDate.localeCompare(b.tradeDate))
  if (buys.length === 0) return null

  // 构建 FIFO 买入队列，每条记录 { price, qty }
  const queue = []
  buys.forEach(r => {
    const qty = parseInt(r.quantity) || 100
    queue.push({ price: parseFloat(r.price), qty })
  })

  // 全部卖出记录，按时间排序
  const sells = records
    .filter(r => r.tradeType === 'SELL')
    .sort((a, b) => a.tradeDate.localeCompare(b.tradeDate))

  // FIFO 匹配：每笔卖出从队列头部扣减
  let realized = 0.0 // 已实现盈亏
  sells.forEach(r => {
    let qty = parseInt(r.quantity) || 100
    const sellPrice = parseFloat(r.price)
    while (qty > 0 && queue.length > 0) {
      const head = queue[0]
      const matched = Math.min(qty, head.qty)
      realized += (sellPrice - head.price) * matched
      head.qty -= matched
      qty -= matched
      if (head.qty <= 0) queue.shift()
    }
  })

  // 剩余持股（队列中累计）
  let remainingQty = 0, remainingCost = 0
  queue.forEach(b => {
    remainingQty += b.qty
    remainingCost += b.price * b.qty
  })

  // 浮动盈亏 = (当前价 - 均价) × 剩余股数
  const unrealized = remainingQty > 0 ? (currentPrice - remainingCost / remainingQty) * remainingQty : 0

  // 总买入成本（用于计算 pct）
  const totalInvested = buys.reduce((sum, r) => sum + parseFloat(r.price) * (parseInt(r.quantity) || 100), 0)
  // 总盈亏
  const abs = realized + unrealized
  // 盈亏百分比 = 总盈亏 / 总成本
  const pct = totalInvested > 0 ? (abs / totalInvested) * 100 : 0
  // 加权均价
  const avgBuyPrice = remainingQty > 0 ? remainingCost / remainingQty : 0

  return {
    currentPrice: currentPrice.toFixed(2),
    avgBuyPrice: avgBuyPrice.toFixed(2),
    abs: abs >= 0 ? '+' + abs.toFixed(2) : abs.toFixed(2),
    realized: realized >= 0 ? '+' + realized.toFixed(2) : realized.toFixed(2),
    unrealized: unrealized >= 0 ? '+' + unrealized.toFixed(2) : unrealized.toFixed(2),
    pct: (pct >= 0 ? '+' : '') + pct.toFixed(2) + '%',
    isProfit: abs >= 0,
    totalQty: remainingQty,
    totalInvested: totalInvested.toFixed(2),
  }
}

// ========== ECharts 渲染 ==========
function renderChart(stock) {
  const el = document.querySelector(`.chart[data-stock-id="${stock.id}"]`)
  if (!el) return

  if (chartInstances[stock.id]) {
    chartInstances[stock.id].dispose()
  }

  const chart = echarts.init(el, null, { renderer: 'canvas' })
  chartInstances[stock.id] = chart

  const data = stock.chartData || []

  // 构建ECharts数据
  const dates = data.map(d => d.date)
  const closes = data.map(d => d.close)

  // 买卖点标注
  const buyPoints = []
  const sellPoints = []
  const records = stock.records || []
  records.forEach(rec => {
    const idx = dates.indexOf(rec.tradeDate)
    if (idx >= 0) {
      const item = { coord: [rec.tradeDate, rec.price] }
      if (rec.tradeType === 'BUY') {
        buyPoints.push(item)
      } else {
        sellPoints.push(item)
      }
    }
  })

  // 工具提示格式化（包含交易记录理由）
  const recordMap = {}
  records.forEach(rec => {
    recordMap[rec.tradeDate] = recordMap[rec.tradeDate] || []
    recordMap[rec.tradeDate].push(rec)
  })

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter: function (params) {
        if (!params || params.length === 0) return ''
        const date = params[0].axisValue
        const price = params[0].value
        let html = `<div style="font-size:12px;line-height:1.6">`
        html += `<b>${date}</b><br/>`
        html += `收盘价: <b>$${price}</b><br/>`
        // 附上当天交易记录
        const recs = recordMap[date]
        if (recs) {
          recs.forEach(r => {
            const label = r.tradeType === 'BUY' ? '买入' : '卖出'
            const color = r.tradeType === 'BUY' ? '#f56c6c' : '#67c23a'
            html += `<span style="color:${color}">◆ ${label}</span> $${r.price}`
            if (r.reason) html += `<br/><span style="color:#888;font-size:11px">理由: ${r.reason}</span>`
            html += '<br/>'
          })
        }
        html += '</div>'
        return html
      }
    },
    grid: {
      left: '60px',
      right: '20px',
      top: '20px',
      bottom: '60px',
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#2a3a5a' } },
      axisLabel: { color: '#7a8ba8', fontSize: 10 },
      splitLine: { show: false },
    },
    yAxis: {
      type: 'value',
      scale: true,
      axisLine: { show: false },
      axisLabel: { color: '#7a8ba8', fontSize: 10, formatter: v => '$' + v.toFixed(2) },
      splitLine: { lineStyle: { color: '#1a2540', type: 'dashed' } },
    },
    dataZoom: [
      {
        type: 'inside',
        xAxisIndex: 0,
        start: 0,
        end: 100,
        zoomOnMouseWheel: true,
      },
      {
        type: 'slider',
        xAxisIndex: 0,
        start: 0,
        end: 100,
        height: 20,
        bottom: 5,
        borderColor: '#2a3a5a',
        backgroundColor: '#0d1225',
        fillerColor: 'rgba(60, 120, 216, 0.2)',
        handleStyle: { color: '#3c78d8' },
        textStyle: { color: '#7a8ba8', fontSize: 10 },
        moveHandleStyle: { color: '#3c78d8' },
      }
    ],
    series: [
      {
        name: '收盘价',
        type: 'line',
        data: closes,
        smooth: true,
        symbol: 'none',
        lineStyle: { color: '#3c78d8', width: 1.5 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(60, 120, 216, 0.3)' },
            { offset: 1, color: 'rgba(60, 120, 216, 0)' },
          ])
        },
        markPoint: {
          symbol: 'circle',
          symbolSize: 12,
          label: {
            formatter: '{b}',
            color: '#fff',
            fontSize: 11,
            position: 'top',
            distance: 4,
          },
          data: [
            ...buyPoints.map(p => ({
              ...p,
              name: 'B',
              itemStyle: { color: '#f56c6c', borderColor: '#fff', borderWidth: 1 },
            })),
            ...sellPoints.map(p => ({
              ...p,
              name: 'S',
              itemStyle: { color: '#67c23a', borderColor: '#fff', borderWidth: 1 },
            })),
          ],
        },
      }
    ],
  }

  chart.setOption(option, true)

  // 响应窗口变化
  window.addEventListener('resize', () => chart.resize())
}
</script>

<style>
/* ========== 全局 ========== */
.app {
  min-height: 100vh;
  background: #0a0e1a;
  padding: 16px;
}

/* ========== 顶部 ========== */
.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  background: #0d1425;
  padding: 12px 0 10px;
  border-bottom: 1px solid #1a2540;
  margin-bottom: 16px;
}

.add-stock {
  display: flex;
  gap: 10px;
  align-items: center;
}

.symbol-input {
  flex: 1;
  background: #111827;
  border: 1px solid #2a3a5a;
  color: #e0e6ed;
  padding: 8px 14px;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}
.symbol-input:focus { border-color: #3c78d8; }
.symbol-input::placeholder { color: #4a5a7a; }

.btn-add {
  background: #1d4ed8;
  color: #fff;
  border: none;
  padding: 8px 20px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
  white-space: nowrap;
}
.btn-add:hover:not(:disabled) { background: #2563eb; }
.btn-add:disabled { opacity: 0.5; cursor: not-allowed; }

/* ========== 错误信息 ========== */
.error-msg {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 6px;
  padding-left: 2px;
}

/* ========== 加载 ========== */
.loading, .empty-hint {
  text-align: center;
  color: #4a5a7a;
  padding: 60px 0;
  font-size: 14px;
}

/* ========== 股票卡片 ========== */
.stock-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stock-card {
  background: #0d1425;
  border: 1px solid #1a2540;
  border-radius: 10px;
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 10px;
  border-bottom: 1px solid #1a2540;
}

.stock-info { display: flex; flex-direction: column; gap: 3px; }
.stock-name-row { display: flex; align-items: baseline; gap: 8px; }
.stock-name { font-size: 16px; font-weight: 600; color: #e0e6ed; }
.stock-symbol { font-size: 12px; color: #4a5a7a; }

/* 盈亏展示 */
.stock-pnl {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
}
.stock-pnl.profit { color: #67c23a; }
.stock-pnl.loss   { color: #f56c6c; }
.stock-pnl.no-trade { color: #4a5a7a; font-size: 12px; }
.pnl-row1, .pnl-row2 { display: flex; align-items: center; gap: 6px; }
.pnl-price { font-size: 13px; font-weight: 600; }
.pnl-sep  { color: #5a7a9a; }
.pnl-pct  { font-weight: 700; font-size: 13px; }
.pnl-realized { font-size: 11px; opacity: 0.8; }
.pnl-unrealized { font-size: 11px; opacity: 0.8; }
.pnl-pct2 { font-size: 11px; font-weight: 600; }
.stock-pnl.profit .pnl-pct { color: #67c23a; }
.stock-pnl.loss   .pnl-pct { color: #f56c6c; }

.card-actions { display: flex; gap: 8px; align-items: center; }

.btn-buy, .btn-sell {
  border: none;
  padding: 5px 16px;
  border-radius: 5px;
  font-size: 13px;
  cursor: pointer;
  font-weight: 500;
  transition: opacity 0.2s;
}
.btn-buy { background: #f56c6c; color: #fff; }
.btn-sell { background: #67c23a; color: #fff; }
.btn-buy:hover, .btn-sell:hover { opacity: 0.85; }

.btn-delete {
  background: transparent;
  border: 1px solid #2a3a5a;
  color: #4a5a7a;
  padding: 5px 12px;
  border-radius: 5px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-delete:hover { border-color: #f56c6c; color: #f56c6c; }

/* ========== 图表 ========== */
.chart-wrapper {
  position: relative;
  height: 280px;
  padding: 4px 0;
}

.chart {
  width: 100%;
  height: 100%;
}

.chart-loading, .chart-error {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #4a5a7a;
  font-size: 13px;
}
.chart-error { color: #f56c6c; }

/* ========== 交易记录列表 ========== */
.records-list {
  padding: 8px 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.record-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px;
  border-radius: 5px;
  font-size: 12px;
  background: #111827;
  position: relative;
}
.record-item.buy { border-left: 3px solid #f56c6c; }
.record-item.sell { border-left: 3px solid #67c23a; }

.rec-type { font-weight: 600; min-width: 28px; }
.record-item.buy .rec-type { color: #f56c6c; }
.record-item.sell .rec-type { color: #67c23a; }

.rec-price { color: #e0e6ed; font-weight: 500; }
.rec-qty  { color: #4a5a7a; font-size: 12px; }
.rec-date { color: #4a5a7a; }
.rec-reason { color: #7a8ba8; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.rec-del {
  background: none;
  border: none;
  color: #4a5a7a;
  cursor: pointer;
  font-size: 14px;
  padding: 0 4px;
  line-height: 1;
}
.rec-del:hover { color: #f56c6c; }

.rec-edit {
  background: none;
  border: none;
  color: #4a5a7a;
  cursor: pointer;
  font-size: 14px;
  padding: 0 4px;
  line-height: 1;
}
.rec-edit:hover { color: #74c0fc; }

/* ========== 弹窗 ========== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  backdrop-filter: blur(2px);
}

.modal {
  background: #111827;
  border: 1px solid #2a3a5a;
  border-radius: 12px;
  width: 360px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.5);
  overflow: hidden;
}

.modal-title {
  padding: 16px 20px;
  font-size: 16px;
  font-weight: 600;
  color: #e0e6ed;
  border-bottom: 1px solid #1a2540;
}

.modal-body { padding: 16px 20px; }

.form-row {
  margin-bottom: 14px;
}
.form-row label {
  display: block;
  font-size: 12px;
  color: #7a8ba8;
  margin-bottom: 6px;
}
.form-row input,
.form-row textarea {
  width: 100%;
  background: #0d1425;
  border: 1px solid #2a3a5a;
  color: #e0e6ed;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  font-family: inherit;
  transition: border-color 0.2s;
  box-sizing: border-box;
}
.form-row input:focus,
.form-row textarea:focus { border-color: #3c78d8; }
.form-row textarea { resize: vertical; }

.modal-footer {
  padding: 12px 20px 16px;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.btn-cancel {
  background: transparent;
  border: 1px solid #2a3a5a;
  color: #7a8ba8;
  padding: 8px 20px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}
.btn-cancel:hover { border-color: #4a5a7a; color: #e0e6ed; }

.btn-confirm {
  background: #1d4ed8;
  color: #fff;
  border: none;
  padding: 8px 24px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s;
}
.btn-confirm:hover:not(:disabled) { background: #2563eb; }
.btn-confirm:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
