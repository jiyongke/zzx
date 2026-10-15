<template>
  <div class="dashboard">
    <h2 class="page-title">数据看板</h2>

    <!-- 指标卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-num">{{ dashboard.userCount ?? '-' }}</div>
        <div class="stat-label">注册用户</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ dashboard.productCount ?? '-' }}</div>
        <div class="stat-label">在售商品</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ dashboard.batchCount ?? '-' }}</div>
        <div class="stat-label">溯源批次</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ dashboard.orderCount ?? '-' }}</div>
        <div class="stat-label">订单总数</div>
      </div>
      <div class="stat-card highlight">
        <div class="stat-num">¥{{ dashboard.totalSales ?? 0 }}</div>
        <div class="stat-label">累计销售额</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ dashboard.todayOrders ?? '-' }}</div>
        <div class="stat-label">今日订单</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ dashboard.pendingShip ?? '-' }}</div>
        <div class="stat-label">待发货</div>
      </div>
    </div>

    <!-- 图表 -->
    <div class="chart-grid">
      <div class="chart-box">
        <h3>近7日销售趋势</h3>
        <div ref="salesChartRef" class="chart"></div>
      </div>
      <div class="chart-box">
        <h3>分类销售占比</h3>
        <div ref="categoryChartRef" class="chart"></div>
      </div>
      <div class="chart-box">
        <h3>溯源批次状态</h3>
        <div ref="batchChartRef" class="chart"></div>
      </div>
      <div class="chart-box">
        <h3>最新订单</h3>
        <el-table :data="dashboard.recentOrders || []" size="small" max-height="300">
          <el-table-column label="订单号" prop="order.orderNo" min-width="150" />
          <el-table-column label="金额" width="90">
            <template #default="{ row }">¥{{ row.order.totalAmount.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="statusType(row.order.status)">{{ statusName(row.order.status) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { getDashboard } from '../../api'

const dashboard = ref({})
const salesChartRef = ref()
const categoryChartRef = ref()
const batchChartRef = ref()
let charts = []

const STATUS = { PENDING: '待支付', PAID: '已支付', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消' }
const statusName = (s) => STATUS[s] || s
const statusType = (s) => ({ PENDING: 'warning', PAID: 'primary', SHIPPED: 'success', COMPLETED: 'info', CANCELLED: 'danger' }[s] || 'info')

const BATCH_STATUS = { 0: '待完善', 1: '可溯源', 2: '已下架' }

const renderCharts = () => {
  charts.forEach((c) => c.dispose())
  charts = []

  // 1. 销售趋势
  const daily = dashboard.value.dailySales || []
  const chart1 = echarts.init(salesChartRef.value)
  chart1.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: daily.map((d) => String(d.day).slice(5)) },
    yAxis: { type: 'value', name: '销售额(元)' },
    series: [{
      name: '销售额', type: 'bar', barWidth: 28,
      itemStyle: { color: '#ff8c42', borderRadius: [6, 6, 0, 0] },
      data: daily.map((d) => d.amount)
    }]
  })
  charts.push(chart1)

  // 2. 分类销售占比
  const cats = dashboard.value.categorySales || []
  const chart2 = echarts.init(categoryChartRef.value)
  chart2.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie',
      radius: ['38%', '62%'],
      center: ['50%', '45%'],
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { formatter: '{b}: {c}元' },
      data: cats.length ? cats : [{ name: '暂无数据', value: 0 }]
    }]
  })
  charts.push(chart2)

  // 3. 批次状态
  const batchStatus = dashboard.value.batchStatus || []
  const chart3 = echarts.init(batchChartRef.value)
  chart3.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie',
      radius: '60%',
      center: ['50%', '45%'],
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { formatter: (p) => `${BATCH_STATUS[p.name] || p.name}: ${p.value}` },
      data: batchStatus.length
        ? batchStatus.map((b) => ({ name: String(b.name), value: b.value }))
        : [{ name: '暂无数据', value: 0 }]
    }]
  })
  charts.push(chart3)
}

const handleResize = () => charts.forEach((c) => c.resize())

onMounted(async () => {
  const { data } = await getDashboard()
  dashboard.value = data
  renderCharts()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach((c) => c.dispose())
})
</script>

<style scoped>
.page-title { font-size: 22px; margin-bottom: 16px; }
.stat-cards { display: grid; grid-template-columns: repeat(7, 1fr); gap: 12px; margin-bottom: 16px; }
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px 12px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.stat-card.highlight { background: linear-gradient(135deg, #ffb347, #ff8c42); color: #fff; }
.stat-num { font-size: 22px; font-weight: 700; margin-bottom: 6px; }
.stat-label { font-size: 12px; color: #999; }
.stat-card.highlight .stat-label { color: #fff; opacity: 0.9; }
.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart-box { background: #fff; border-radius: 12px; padding: 20px; }
.chart-box h3 { font-size: 15px; margin-bottom: 12px; }
.chart { height: 280px; }
</style>
