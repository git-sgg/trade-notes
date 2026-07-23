import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// 添加股票
export function addStock(symbol) {
  return api.post('/stocks', { symbol }).then(r => r.data)
}

// 获取自选股列表
export function getStocks() {
  return api.get('/stocks').then(r => r.data)
}

// 删除自选股
export function deleteStock(id) {
  return api.delete(`/stocks/${id}`).then(r => r.data)
}

// 获取K线数据
export function getPrices(stockId, days = 90) {
  return api.get(`/stocks/${stockId}/prices`, { params: { days } }).then(r => r.data)
}

// 获取交易记录
export function getRecords(stockId) {
  return api.get(`/stocks/${stockId}/records`).then(r => r.data)
}

// 添加交易记录
export function addRecord(stockId, data) {
  return api.post(`/stocks/${stockId}/records`, data).then(r => r.data)
}

// 删除交易记录
export function deleteRecord(id) {
  return api.delete(`/stocks/records/${id}`).then(r => r.data)
}

// 更新交易记录
export function updateRecord(id, data) {
  return api.put(`/stocks/records/${id}`, data).then(r => r.data)
}
