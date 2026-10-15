<template>
  <div class="workspace">
    <!-- 顶部欢迎条 -->
    <div class="welcome-bar">
      <div>
        <h1>{{ welcomeTitle }}</h1>
        <p>{{ welcomeDesc }}</p>
      </div>
      <el-tag :type="roleTagType" size="large">{{ roleName }}</el-tag>
    </div>

    <!-- 角色功能入口卡片 -->
    <div class="card-grid">
      <div
        v-for="card in cards"
        :key="card.path"
        class="func-card"
        @click="$router.push(card.path)"
      >
        <el-icon :size="30" :color="card.color" class="card-icon"><component :is="card.icon" /></el-icon>
        <div class="card-title">{{ card.title }}</div>
        <div class="card-desc">{{ card.desc }}</div>
      </div>
    </div>

    <!-- 角色统计概览 -->
    <div v-if="stats.length" class="stats-row">
      <div v-for="s in stats" :key="s.label" class="stat-box">
        <div class="stat-num">{{ s.value }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </div>
    </div>

    <!-- 消费者：快捷查询示例 -->
    <div v-if="store.role === 'CONSUMER'" class="quick-section">
      <h3>可溯源的农产品批次</h3>
      <div class="example-list">
        <div v-for="b in batches" :key="b.id" class="example-item" @click="$router.push(`/trace/batch/${b.id}`)">
          <span class="ex-name">{{ b.productName }}</span>
          <span class="ex-code">{{ b.batchCode }}</span>
          <el-tag size="small" type="success">可溯源</el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  ShoppingCart, Tickets, Search, DataAnalysis, Goods, Collection, User,
  List, Box, TrendCharts
} from '@element-plus/icons-vue'
import {
  getCart, getMyOrders, adminTraceBatches, getDashboard, getTraceBatches
} from '../api'
import { useUserStore } from '../store/user'

const store = useUserStore()

const roleName = computed(() => ({
  ADMIN: '管理员', FARMER: '农户', CONSUMER: '消费者'
}[store.role] || '消费者'))

const roleTagType = computed(() => ({
  ADMIN: 'danger', FARMER: 'success', CONSUMER: 'info'
}[store.role] || 'info'))

const welcomeTitle = computed(() => ({
  ADMIN: '平台管理控制台',
  FARMER: '我的溯源工作台',
  CONSUMER: '欢迎回来，安心选购'
}[store.role] || '工作台'))

const welcomeDesc = computed(() => ({
  ADMIN: '经营数据总览、商品/订单/用户/溯源全链路管理',
  FARMER: '管理我的溯源批次，录入环节并发布溯源链',
  CONSUMER: '每一份农产品都有来处，扫码查溯源，放心购买'
}[store.role] || ''))

// 角色功能卡片
const cards = computed(() => ({
  ADMIN: [
    { path: '/admin/dashboard', title: '数据看板', desc: '销售趋势 · 分类占比 · 批次状态', icon: TrendCharts, color: '#ff8c42' },
    { path: '/admin/report', title: '数据报表', desc: '日报/周报 · 一键导出 CSV', icon: DataAnalysis, color: '#2e8b57' },
    { path: '/admin/products', title: '商品与分类', desc: '按分类分组管理商品 · 分类维护', icon: Goods, color: '#409eff' },
    { path: '/admin/orders', title: '订单与售后', desc: '全量订单 · 发货 · 退款审核', icon: Tickets, color: '#e6a23c' },
    { path: '/admin/users', title: '用户管理', desc: '账号维护 · 角色分配', icon: User, color: '#909399' },
    { path: '/admin/trace', title: '溯源监管', desc: '全部批次与环节监管', icon: Search, color: '#13c2c2' },
    { path: '/admin/audit', title: '操作日志', desc: '关键操作留痕 · 可追溯', icon: List, color: '#722ed1' }
  ],
  FARMER: [
    { path: '/farmer/products', title: '我的商品', desc: '上新 · 改价 · 库存维护 · 申请上架', icon: Goods, color: '#409eff' },
    { path: '/admin/trace', title: '溯源批次管理', desc: '新建批次 · 录入环节 · 发布溯源链', icon: Box, color: '#67c23a' },
    { path: '/profile', title: '资质认证', desc: '实名与资质证照 · 审核状态', icon: User, color: '#722ed1' },
    { path: '/trace', title: '溯源查询演示', desc: '以消费者视角体验扫码查询', icon: Search, color: '#13c2c2' },
    { path: '/products', title: '商城浏览', desc: '查看平台上架的可溯源商品', icon: Goods, color: '#409eff' }
  ],
  CONSUMER: [
    { path: '/products', title: '全部商品', desc: '时令水果 · 有机蔬菜 · 五谷杂粮', icon: Goods, color: '#409eff' },
    { path: '/cart', title: '我的购物车', desc: '挑选的农产品都在这里', icon: ShoppingCart, color: '#ff8c42' },
    { path: '/orders', title: '我的订单', desc: '下单记录 · 物流跟踪', icon: List, color: '#67c23a' },
    { path: '/trace', title: '溯源查询', desc: '输入批次号/扫码查看全链路', icon: Search, color: '#13c2c2' },
    { path: '/profile', title: '个人中心', desc: '收货地址 · 账户设置', icon: User, color: '#909399' }
  ]
}[store.role] || []))

// 角色统计概览
const stats = ref([])

const loadStats = async () => {
  try {
    if (store.role === 'ADMIN') {
      const { data } = await getDashboard()
      stats.value = [
        { label: '注册用户', value: data.userCount },
        { label: '在售商品', value: data.productCount },
        { label: '溯源批次', value: data.batchCount },
        { label: '累计销售额', value: '¥' + data.totalSales }
      ]
    } else if (store.role === 'FARMER') {
      const { data } = await adminTraceBatches({ page: 1, size: 100 })
      const my = data.records || []
      stats.value = [
        { label: '我的溯源批次', value: data.total },
        { label: '待完善批次', value: my.filter((b) => b.status === 0).length },
        { label: '待审核批次', value: my.filter((b) => b.status === 3).length },
        { label: '可溯源批次', value: my.filter((b) => b.status === 1).length }
      ]
    } else if (store.role === 'CONSUMER') {
      const [cartRes, orderRes] = await Promise.all([
        getCart(),
        getMyOrders({ page: 1, size: 1, status: 'PENDING' })
      ])
      stats.value = [
        { label: '购物车商品', value: cartRes.data.reduce((s, i) => s + i.quantity, 0) },
        { label: '待支付订单', value: orderRes.data.total }
      ]
    }
  } catch (e) {
    // 统计加载失败不阻塞页面
  }
}

const batches = ref([])

onMounted(async () => {
  loadStats()
  if (store.role === 'CONSUMER') {
    try {
      const { data } = await getTraceBatches()
      batches.value = data
    } catch (e) { /* ignore */ }
  }
})
</script>

<style scoped>
.welcome-bar {
  background: linear-gradient(120deg, #fff7f0, #ffe8d6);
  border-radius: 16px;
  padding: 28px 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.welcome-bar h1 { font-size: 24px; color: #d2691e; margin-bottom: 8px; }
.welcome-bar p { color: #8a5a3b; font-size: 14px; }

.card-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.func-card {
  background: #fff;
  border-radius: 12px;
  padding: 22px 18px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: transform 0.2s, box-shadow 0.2s;
}
.func-card:hover { transform: translateY(-4px); box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1); }
.card-icon { margin-bottom: 12px; }
.card-title { font-size: 15px; font-weight: 600; margin-bottom: 6px; }
.card-desc { font-size: 12px; color: #999; line-height: 1.6; }

.stats-row { display: flex; gap: 14px; margin-top: 16px; }
.stat-box { flex: 1; background: #fff; border-radius: 12px; padding: 18px; text-align: center; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04); }
.stat-num { font-size: 22px; font-weight: 700; color: #ff8c42; margin-bottom: 6px; }
.stat-label { font-size: 12px; color: #999; }

.quick-section { background: #fff; border-radius: 12px; padding: 20px 24px; margin-top: 16px; }
.quick-section h3 { font-size: 15px; margin-bottom: 14px; }
.example-list { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.example-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 14px; border: 1px solid #eee; border-radius: 8px; cursor: pointer;
  transition: all 0.2s;
}
.example-item:hover { border-color: #2e8b57; background: #f4fbf7; }
.ex-name { flex: 1; font-size: 13px; }
.ex-code { color: #999; font-size: 12px; font-family: monospace; }
</style>
