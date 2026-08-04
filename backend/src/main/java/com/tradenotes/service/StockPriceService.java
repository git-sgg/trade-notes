package com.tradenotes.service;

import com.tradenotes.entity.KLinePoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockPriceService {

    private final RestTemplate restTemplate;

    @Value("${stock.api-key}")
    private String apiKey;

    @Value("${stock.api-base}")
    private String apiBase;

    private final Map<String, CacheEntry> priceCache = new LinkedHashMap<>(32) {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > 50;
        }
    };

    private static final long CACHE_TTL_MS = 10 * 60 * 1000;

    private static class CacheEntry {
        final List<KLinePoint> data;
        final long timestamp;

        CacheEntry(List<KLinePoint> data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public List<KLinePoint> getKLineData(String symbol, int days) {
        String userSymbol = symbol.toUpperCase().trim();
        String cacheKey = userSymbol + "#" + days;
        String apiSymbol = normalizeSymbol(userSymbol);
        CacheEntry cached = priceCache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_TTL_MS) {
            return cached.data;
        }

        List<KLinePoint> data;
        boolean isAStock = symbol.matches("\\d{6}");

        if (isAStock) {
            // A股优先用腾讯历史K线（免费、含今日数据、无频率限制）
            data = fetchHistoryFromTencent(symbol, days);
            if (data == null || data.isEmpty()) {
                log.warn("腾讯历史K线失败，Alpha Vantage 兜底: {}", symbol);
                data = fetchFromAlphaVantage(apiSymbol);
            }
        } else {
            // 美股用 Alpha Vantage
            data = fetchFromAlphaVantage(apiSymbol);
        }

        if (!data.isEmpty()) {
            priceCache.put(cacheKey, new CacheEntry(data));
        }
        return data;
    }

    /**
     * 腾讯历史K线（90天，含今日）
     * 格式: https://web.ifzq.gtimg.cn/appstock/app/fqkline/get?_var=kline_dayqfq&param=sh600519,day,,,90,qfq
     * 返回: [date, open, close, high, low, vol]
     */
    private List<KLinePoint> fetchHistoryFromTencent(String symbol, int days) {
        String prefix = symbol.startsWith("6") ? "sh" : "sz";
        String url = String.format(
                "https://web.ifzq.gtimg.cn/appstock/app/fqkline/get?_var=kline_dayqfq&param=%s%s,day,,,%d,qfq",
                prefix, symbol, days);
        try {
            String raw = restTemplate.getForObject(url, String.class);
            if (raw == null || raw.isBlank()) return null;

            // 去掉 var= 前缀: kline_dayqfq={...}
            int eq = raw.indexOf('=');
            if (eq < 0) return null;
            String jsonStr = raw.substring(eq + 1);

            // 用正则提取 qfqday 数组（避免 Jackson 泛型类型推断问题）
            // 格式: "qfqday":[["2026-03-11","90.680","91.490",...],["2026-03-12",...],...]
            int qfqIdx = jsonStr.indexOf("\"qfqday\"");
            int dayIdx = jsonStr.indexOf("\"day\"");
            int arrStart = -1;
            if (qfqIdx >= 0) arrStart = jsonStr.indexOf('[', qfqIdx);
            else if (dayIdx >= 0) arrStart = jsonStr.indexOf('[', dayIdx);
            if (arrStart < 0) return null;

            // 找到对应的结束 ]
            int depth = 0;
            int arrEnd = arrStart;
            for (int i = arrStart; i < jsonStr.length(); i++) {
                char c = jsonStr.charAt(i);
                if (c == '[') depth++;
                else if (c == ']') { depth--; if (depth == 0) { arrEnd = i; break; } }
            }
            String arrStr = jsonStr.substring(arrStart + 1, arrEnd);

            List<KLinePoint> points = new ArrayList<>();
            java.util.regex.Pattern BAR = java.util.regex.Pattern.compile(
                    "\\[\\\"([^\\\"]+)\\\",\\\"([^\\\"]+)\\\",\\\"([^\\\"]+)\\\",\\\"([^\\\"]+)\\\",\\\"([^\\\"]+)\\\",[^\\]]+\\]");
            java.util.regex.Matcher m = BAR.matcher(arrStr);
            while (m.find()) {
                String date  = m.group(1);
                float open   = parseF(m.group(2));
                float close  = parseF(m.group(3));
                float high   = parseF(m.group(4));
                float low    = parseF(m.group(5));
                points.add(new KLinePoint(date, close, open, high, low));
            }
            log.info("腾讯历史K线 {}: {} 条", symbol, points.size());
            return points;
        } catch (Exception e) {
            log.warn("腾讯历史K线异常 ({}): {}", symbol, e.getMessage());
            return null;
        }
    }

    private float parseF(String s) {
        try { return Float.parseFloat(s.trim()); } catch (Exception e) { return 0f; }
    }

    /**
     * 从腾讯接口拉今日实时行情，构造一个 KLinePoint
     * 字段含义（经实际数据验证）：
     *   [3]  = 昨收
     *   [4]  = 今开
     *   [5]  = 当前价（实时变动的盘口价，非收盘）
     *   [33] = 今高
     *   [34] = 今低
     *   [35] = "今收盘/成交量/成交额"（收盘从这里取！）
     *   [30] = YYYYMMDDHHMMSS 时间戳
     */
    private KLinePoint fetchTodayFromTencent(String symbol) {
        String prefix = symbol.startsWith("6") ? "sh" : "sz";
        String url = "http://qt.gtimg.cn/q=" + prefix + symbol;
        try (InputStream is = new URL(url).openStream()) {
            byte[] bytes = is.readAllBytes();
            String raw = new String(bytes, Charset.forName("GBK"));

            int eq = raw.indexOf('=');
            if (eq < 0) return null;
            int q1 = raw.indexOf('"', eq);
            int q2 = raw.indexOf('"', q1 + 1);
            if (q1 < 0 || q2 < 0) return null;
            String content = raw.substring(q1 + 1, q2);
            String[] parts = content.split("~");
            if (parts.length < 36) return null;

            double prevClose = parseOrZero(parts[3]);  // 昨收
            double open      = parseOrZero(parts[4]); // 今开
            double current   = parseOrZero(parts[5]); // 当前价
            double high      = parseOrZero(parts[33]);// 今高
            double low       = parseOrZero(parts[34]);// 今低

            // 今日收盘从 [35] 取，格式 "price/vol/amount"
            double close = prevClose; // 默认用昨收兜底
            try {
                String[] cv = parts[35].split("/");
                close = Double.parseDouble(cv[0]);
            } catch (Exception ignored) {}

            // 校验今高今低：必须与昨收+今日涨跌幅度接近，否则用昨收兜底
            double ref = Math.max(prevClose, close);
            if (high <= 0 || high > ref * 1.5 || high < ref * 0.5) {
                high = ref;
            }
            if (low <= 0 || low < ref * 0.5 || low > ref * 1.5) {
                low = ref;
            }

            // 无当日实际交易（停牌）：close ≈ prevClose
            if (Math.abs(close - prevClose) < 0.001 * prevClose) {
                return null;
            }

            // 日期从 [30] 时间戳提取
            String today;
            if (parts[30].length() >= 8) {
                String ts = parts[30];
                today = ts.substring(0, 4) + "-" + ts.substring(4, 6) + "-" + ts.substring(6, 8);
            } else {
                today = java.time.LocalDate.now().toString();
            }

            log.debug("腾讯实时 {}: 开={} 收={} 今高={} 今低={}",
                    symbol, open, close, high, low);
            return new KLinePoint(today, (float) close, (float) open, (float) high, (float) low);
        } catch (Exception e) {
            log.warn("腾讯实时行情获取失败 ({}): {}", symbol, e.getMessage());
            return null;
        }
    }

    private double parseOrZero(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }

    private List<KLinePoint> fetchFromAlphaVantage(String apiSymbol) {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("未配置 Alpha Vantage API Key！");
            return Collections.emptyList();
        }

        String url = String.format("%s?function=TIME_SERIES_DAILY&symbol=%s&apikey=%s&outputsize=compact",
                apiBase, apiSymbol, apiKey);

        try {
            long start = System.currentTimeMillis();
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return Collections.emptyList();
            if (response.containsKey("Error Message")) {
                log.warn("Alpha Vantage API 错误: {}", response.get("Error Message"));
                return Collections.emptyList();
            }
            if (response.containsKey("Note")) {
                log.warn("Alpha Vantage 频率超限: {}", response.get("Note"));
                return Collections.emptyList();
            }
            // Alpha Vantage 免费配额耗尽时返回 {"1. Information": "..."}
            if (response.containsKey("1. Information")) {
                log.warn("Alpha Vantage 配额耗尽: {}", response.get("1. Information"));
                return Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            Map<String, Map<String, String>> timeSeries =
                    (Map<String, Map<String, String>>) response.get("Time Series (Daily)");
            if (timeSeries == null || timeSeries.isEmpty()) {
                return Collections.emptyList();
            }

            List<KLinePoint> points = new ArrayList<>();
            timeSeries.entrySet().stream()
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .forEach(entry -> {
                        String date = entry.getKey();
                        Map<String, String> bar = entry.getValue();
                        try {
                            double open = Double.parseDouble(bar.get("1. open"));
                            double high = Double.parseDouble(bar.get("2. high"));
                            double low = Double.parseDouble(bar.get("3. low"));
                            double close = Double.parseDouble(bar.get("4. close"));
                            points.add(new KLinePoint(date, close, open, high, low));
                        } catch (NumberFormatException ignored) {}
                    });

            log.info("Alpha Vantage 获取 {} K线 {} 条", apiSymbol, points.size());
            return points;
        } catch (Exception e) {
            log.error("调用 Alpha Vantage 失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 规范化股票代码
     */
    public String normalizeSymbol(String symbol) {
        String s = symbol.toUpperCase().trim();
        if (s.matches("\\d{6}")) {
            return s.startsWith("6") ? s + ".SHH" : s + ".SHE";
        }
        return s;
    }

    /**
     * 搜索股票名称
     * 优先级：A股 → 腾讯接口（中文名） → Alpha Vantage SYMBOL_SEARCH（英文名）
     *       美股 → Alpha Vantage SYMBOL_SEARCH
     */
    public String searchStockName(String symbol) {
        String s = symbol.trim();

        // A股走腾讯接口（免费、中文名、无频率限制）
        if (s.matches("\\d{6}")) {
            String cnName = getChineseNameFromTencent(s);
            if (cnName != null && !cnName.isEmpty()) {
                return cnName;
            }
        }

        // fallback 到 Alpha Vantage
        return searchFromAlphaVantage(s);
    }

    /**
     * 腾讯股票接口：A股免费查中文名
     * 格式: http://qt.gtimg.cn/q=sh600519
     * 返回: v_sh600519="1~贵州茅台~600519~..."
     */
    private String getChineseNameFromTencent(String symbol) {
        String prefix = symbol.startsWith("6") ? "sh" : "sz";
        String url = "http://qt.gtimg.cn/q=" + prefix + symbol;
        try {
            // 手动处理 GBK 编码
            try (InputStream is = new URL(url).openStream()) {
                byte[] bytes = is.readAllBytes();
                String raw = new String(bytes, Charset.forName("GBK"));
                log.debug("腾讯返回: {}", raw);

                // 解析: v_sh600519="1~贵州茅台~600519~..."
                int eq = raw.indexOf('=');
                if (eq < 0) return null;
                int q1 = raw.indexOf('"', eq);
                int q2 = raw.indexOf('"', q1 + 1);
                if (q1 < 0 || q2 < 0) return null;
                String content = raw.substring(q1 + 1, q2);
                String[] parts = content.split("~");
                if (parts.length >= 2) {
                    return parts[1];
                }
            }
        } catch (Exception e) {
            log.warn("腾讯接口获取名称失败 ({}): {}", symbol, e.getMessage());
        }
        return null;
    }

    /**
     * Alpha Vantage SYMBOL_SEARCH（英文名）
     */
    private String searchFromAlphaVantage(String keyword) {
        if (apiKey == null || apiKey.isBlank()) {
            return keyword.toUpperCase().trim();
        }
        String url = String.format("%s?function=SYMBOL_SEARCH&keywords=%s&apikey=%s",
                apiBase, keyword.trim(), apiKey);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return keyword.trim().toUpperCase();
            if (response.containsKey("Note")) {
                log.warn("Alpha Vantage 频率超限");
                return keyword.trim().toUpperCase();
            }
            @SuppressWarnings("unchecked")
            List<Map<String, String>> matches =
                    (List<Map<String, String>>) response.get("bestMatches");
            if (matches != null && !matches.isEmpty()) {
                return matches.get(0).get("2. name");
            }
        } catch (Exception e) {
            log.warn("Alpha Vantage 名称搜索失败: {}", e.getMessage());
        }
        return keyword.trim().toUpperCase();
    }

    public boolean isValidSymbol(String symbol) {
        if (apiKey == null || apiKey.isBlank()) return false;
        String apiSymbol = normalizeSymbol(symbol);
        String url = String.format("%s?function=TIME_SERIES_DAILY&symbol=%s&apikey=%s&outputsize=compact",
                apiBase, apiSymbol, apiKey);
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return false;
            if (response.containsKey("Error Message")) return false;
            if (response.containsKey("Note")) return false;
            @SuppressWarnings("unchecked")
            Map<String, ?> ts = (Map<String, ?>) response.get("Time Series (Daily)");
            return ts != null && !ts.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public void invalidateCache(String symbol) {
        String prefix = symbol.toUpperCase().trim() + "#";
        priceCache.keySet().removeIf(k -> k.startsWith(prefix));
    }
}
