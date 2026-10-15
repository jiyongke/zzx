<template>
  <div class="report-page">
    <h2 class="page-title">数据报表</h2>

    <!-- 统计口径说明 -->
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="caliber-tip"
      title="统计口径说明"
      description="日报 = 所选日期当日创建的订单；周报 = 截止日期前 7 天内创建的订单（均含全部状态，销售额只计已支付及以上）。订单管理列表 = 全量订单（不分时间）。"
    />

    <!-- 查询条件 -->
    <div class="toolbar">
      <el-radio-group v-model="type">
        <el-radio-button value="daily">日报</el-radio-button>
        <el-radio-button value="weekly">周报（近7天）</el-radio-button>
      </el-radio-group>
      <el-date-picker
        v-if="type === 'daily'"
        v-model="date"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="选择日期（默认今天）"
        @change="load"
      />
      <el-date-picker
        v-else
        v-model="endDate"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="结束日期（默认今天）"
        @change="load"
      />
      <el-button type="primary" :loading="loading" @click="load">生成报表</el-button>
      <el-button :disabled="!report" @click="exportCsv">导出 CSV</el-button>
    </div>

    <!-- 指标卡片 -->
    <div v-if="report" class="stat-cards">
      <div class="stat-card">
        <div class="stat-num">{{ report.orderCount }}</div>
        <div class="stat-label">{{ type === 'daily' ? '当日' : '近7天' }}订单数</div>
      </div>
      <div class="stat-card highlight">
        <div class="stat-num">¥{{ Number(report.sales).toFixed(2) }}</div>
        <div class="stat-label">销售额（已支付）</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">¥{{ Number(report.avgOrderAmount).toFixed(2) }}</div>
        <div class="stat-label">客单价</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ report.refundCount }}</div>
        <div class="stat-label">售后单数</div>
      </div>
    </div>

    <!-- 图表 -->
    <div v-if="report" class="chart-grid">
      <div v-if="type === 'weekly'" class="chart-box wide">
        <h3>近7天销售趋势</h3>
        <div ref="trendRef" class="chart"></div>
      </div>
      <div class="chart-box">
        <h3>商品销量 TOP{{ Math.min(5, report.topProducts.length) }}</h3>
        <div ref="topRef" class="chart"></div>
      </div>
      <div class="chart-box">
        <h3>分类销售额占比</h3>
        <div ref="catRef" class="chart"></div>
      </div>
    </div>

    <!-- TOP5 明细表 -->
    <div v-if="report && report.topProducts.length" class="detail-table">
      <h3>商品销量明细</h3>
      <el-table :data="report.topProducts" size="small" stripe>
        <el-table-column label="商品" prop="name" min-width="200" />
        <el-table-column label="销量" prop="quantity" width="100" />
        <el-table-column label="销售额" width="140">
          <template #default="{ row }">¥{{ Number(row.amount).toFixed(2) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getDailyReport, getWeeklyReport } from '../../api'

const type = ref('daily')
const date = ref('')
const endDate = ref('')
const report = ref(null)
const loading = ref(false)

const trendRef = ref(null)
const topRef = ref(null)
const catRef = ref(null)

const load = async () => {
  loading.value = true
  try {
    const { data } = type.value === 'daily'
      ? await getDailyReport(date.value || '')
      : await getWeeklyReport(endDate.value || '')
    report.value = data
    await nextTick()
    renderCharts()
  } catch (e) {
    ElMessage.error('报表加载失败：' + (e?.message || '网络错误'))
  } finally {
    loading.value = false
  }
}

// 统一管理图表实例，重新生成前销毁旧实例（避免图表不刷新）
const charts = []
const renderCharts = () => {
  charts.forEach((c) => c.dispose())
  charts.length = 0

  // 趋势图（周报）
  if (type.value === 'weekly' && report.value.dailyTrend && trendRef.value) {
    const chart = echarts.init(trendRef.value)
    charts.push(chart)
    const trend = report.value.dailyTrend
    chart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['销售额', '订单数'] },
      xAxis: { type: 'category', data: trend.map((t) => t.day.slice(5)) },
      yAxis: [
        { type: 'value', name: '销售额' },
        { type: 'value', name: '订单数', minInterval: 1 }
      ],
      series: [
        { name: '销售额', type: 'line', smooth: true, areaStyle: {}, data: trend.map((t) => Number(t.amount)) },
        { name: '订单数', type: 'bar', yAxisIndex: 1, barWidth: 14, data: trend.map((t) => t.cnt) }
      ]
    })
  }

  // 商品销量 TOP5 条形图
  if (topRef.value) {
    const chart = echarts.init(topRef.value)
    charts.push(chart)
    const list = report.value.topProducts || []
    chart.setOption({
      tooltip: {},
      grid: { left: 90, right: 30, top: 10, bottom: 30 },
      xAxis: { type: 'value', minInterval: 1 },
      yAxis: { type: 'category', inverse: true, data: list.map((p) => p.name) },
      series: [{
        type: 'bar', barMaxWidth: 22,
        itemStyle: { color: '#2e8b57', borderRadius: [0, 4, 4, 0] },
        data: list.map((p) => p.quantity)
      }]
    })
  }

  // 分类占比饼图
  if (catRef.value) {
    const chart = echarts.init(catRef.value)
    charts.push(chart)
    const cats = report.value.categorySales || []
    chart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
      series: [{
        type: 'pie', radius: ['42%', '68%'],
        label: { formatter: '{b}\n{d}%' },
        data: cats.map((c) => ({ name: c.name, value: Number(c.amount) }))
      }]
    })
  }
}

// 导出 CSV（浏览器下载到"下载"目录）
const exportCsv = () => {
  const r = report.value
  if (!r) return
  const rows = [
    ['报表类型', type.value === 'daily' ? '日报 ' + (date.value || '') : '周报(近7天) 截止 ' + (endDate.value || '')],
    ['订单数', r.orderCount],
    ['销售额(元)', Number(r.sales).toFixed(2)],
    ['客单价(元)', Number(r.avgOrderAmount).toFixed(2)],
    ['售后单数', r.refundCount],
    [],
    ['商品', '销量', '销售额(元)']
  ]
  for (const p of r.topProducts || []) {
    rows.push([p.name, p.quantity, Number(p.amount).toFixed(2)])
  }
  const csv = '\uFEFF' + rows.map((row) => row.map((c) => `"${String(c).replace(/"/g, '""')}"`).join(',')).join('\r\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = (type.value === 'daily' ? '日报' : '周报') + '_' + (type.value === 'daily' ? (date.value || 'today') : (endDate.value || 'week')) + '.csv'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  // 延迟释放，避免部分浏览器下载被中断
  setTimeout(() => URL.revokeObjectURL(url), 1000)
  ElMessage.success(`已开始下载 ${a.download}，请查看浏览器"下载"目录`)
}

onMounted(load)
</script>

<style scoped>
.page-title { font-size: 22px; margin-bottom: 6px; }
.caliber-tip { margin-bottom: 14px; }
.toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
.stat-cards { display: flex; gap: 14px; margin-bottom: 16px; }
.stat-card { flex: 1; background: #fff; border-radius: 12px; padding: 18px; text-align: center; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04); }
.stat-num { font-size: 22px; font-weight: 700; color: #ff8c42; margin-bottom: 6px; }
.stat-label { font-size: 12px; color: #999; }
.stat-card.highlight .stat-num { color: #2e8b57; }
.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 16px; }
.chart-box { background: #fff; border-radius: 12px; padding: 18px; }
.chart-box.wide { grid-column: 1 / -1; }
.chart-box h3 { font-size: 14px; margin-bottom: 12px; }
.chart { height: 280px; }
.detail-table { background: #fff; border-radius: 12px; padding: 18px; }
.detail-table h3 { font-size: 14px; margin-bottom: 12px; }
</style>
