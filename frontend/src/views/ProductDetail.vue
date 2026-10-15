<template>
  <div v-loading="loading">
    <el-breadcrumb separator="/" class="crumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/products' }">全部商品</el-breadcrumb-item>
      <el-breadcrumb-item>{{ product?.name }}</el-breadcrumb-item>
    </el-breadcrumb>

    <div v-if="product" class="detail-wrap">
      <!-- 左：图片 -->
      <div class="gallery">
        <el-image class="main-img" :src="product.coverImage" fit="cover" />
      </div>

      <!-- 右：信息 -->
      <div class="info">
        <h1 class="name">{{ product.name }}</h1>
        <div class="tags">
          <el-tag v-if="product.traceable" type="success">✓ 溯源认证</el-tag>
          <el-tag v-else type="info">普通商品</el-tag>
          <el-tag v-if="product.batchCode" @click="$router.push(`/trace/batch/${product.batchId}`)">
            批次号 {{ product.batchCode }} 查看溯源 >
          </el-tag>
        </div>
        <div class="price-row">
          <span class="price">¥{{ product.price.toFixed(2) }}</span>
          <span class="sales">已售 {{ product.sales }} 件</span>
          <span class="stock">库存 {{ product.stock }} 件</span>
        </div>
        <div class="cat-row">分类：{{ product.categoryName }}</div>
        <div class="qty-row">
          <span>购买数量</span>
          <el-input-number v-model="quantity" :min="1" :max="Math.max(product.stock, 1)" />
        </div>
        <div class="actions">
          <el-button type="primary" size="large" :disabled="product.stock <= 0" @click="handleBuy">
            立即购买
          </el-button>
          <el-button size="large" :disabled="product.stock <= 0" @click="handleAddCart">
            <el-icon><ShoppingCart /></el-icon> 加入购物车
          </el-button>
        </div>
      </div>
    </div>

    <!-- 溯源信息 -->
    <div v-if="product?.traceable && traceData" class="trace-section">
      <h2 class="section-title">溯源信息 · {{ traceData.batch.productName }}</h2>
      <el-descriptions :column="3" border class="batch-info">
        <el-descriptions-item label="产地">{{ traceData.batch.origin }}</el-descriptions-item>
        <el-descriptions-item label="种植日期">{{ formatDate(traceData.batch.plantDate) }}</el-descriptions-item>
        <el-descriptions-item label="采收日期">{{ formatDate(traceData.batch.harvestDate) }}</el-descriptions-item>
        <el-descriptions-item label="生产主体">{{ traceData.batch.farmerName }}</el-descriptions-item>
        <el-descriptions-item label="溯源批次号">{{ traceData.batch.batchCode }}</el-descriptions-item>
        <el-descriptions-item label="批次状态">
          <el-tag size="small" type="success">可溯源</el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-timeline class="trace-timeline">
        <el-timeline-item
          v-for="n in traceData.nodes"
          :key="n.id"
          :timestamp="formatDate(n.recordTime)"
          placement="top"
          :type="'success'"
          :hollow="false"
        >
          <div class="node-card">
            <div class="node-head">
              <el-tag size="small" type="success">{{ typeName(n.nodeType) }}</el-tag>
              <b>{{ n.title }}</b>
              <span class="node-op">{{ n.operator }} · {{ n.location }}</span>
            </div>
            <p class="node-desc">{{ n.description }}</p>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>

    <!-- 商品详情 -->
    <div v-if="product" class="desc-section">
      <h2 class="section-title">商品详情</h2>
      <div class="desc-text">{{ product.description }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ShoppingCart } from '@element-plus/icons-vue'
import { getProductDetail, getTraceBatch, addCart } from '../api'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const loading = ref(false)
const product = ref(null)
const traceData = ref(null)
const quantity = ref(1)

const NODE_TYPES = {
  PLANT: '选种育苗', FERTILIZE: '有机施肥', PEST: '绿色防治', HARVEST: '人工采收',
  PROCESS: '加工分拣', QUALITY: '质检认证', STORAGE: '仓储保鲜', LOGISTICS: '冷链物流'
}
const typeName = (t) => NODE_TYPES[t] || t
const formatDate = (d) => (d ? String(d).replace('T', ' ').slice(0, 16) : '')

onMounted(async () => {
  loading.value = true
  try {
    const { data } = await getProductDetail(route.params.id)
    product.value = data
    if (data.traceable && data.batchId) {
      try {
        const t = await getTraceBatch(data.batchId)
        traceData.value = t.data
      } catch (e) { /* 批次不可见时忽略 */ }
    }
  } finally {
    loading.value = false
  }
})

const requireLogin = () => {
  if (!store.isLogin) {
    ElMessage.warning('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return false
  }
  return true
}

const handleAddCart = async () => {
  if (!requireLogin()) return
  await addCart({ productId: product.value.id, quantity: quantity.value })
  ElMessage.success('已加入购物车')
}

const handleBuy = async () => {
  if (!requireLogin()) return
  // 跳转结算页选择收货地址（携带立即购买参数）
  router.push({
    path: '/checkout',
    query: { buyNow: 1, productId: product.value.id, quantity: quantity.value }
  })
}
</script>

<style scoped>
.crumb { margin-bottom: 16px; }
.detail-wrap { display: flex; gap: 32px; background: #fff; border-radius: 12px; padding: 24px; }
.gallery .main-img { width: 420px; height: 420px; border-radius: 12px; }
.info { flex: 1; }
.name { font-size: 22px; color: #333; margin-bottom: 12px; }
.tags { display: flex; gap: 8px; margin-bottom: 16px; }
.price-row { background: #fff7f0; border-radius: 8px; padding: 14px 16px; display: flex; align-items: baseline; gap: 16px; margin-bottom: 16px; }
.price { color: #e4393c; font-size: 26px; font-weight: 700; }
.sales, .stock { color: #999; font-size: 13px; }
.cat-row { color: #666; font-size: 14px; margin-bottom: 16px; }
.qty-row { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; font-size: 14px; color: #666; }
.actions { display: flex; gap: 12px; }
.trace-section, .desc-section { background: #fff; border-radius: 12px; padding: 24px; margin-top: 20px; }
.section-title { font-size: 18px; margin-bottom: 16px; }
.batch-info { margin-bottom: 20px; }
.trace-timeline { padding-left: 6px; }
.node-card { background: #f9fafb; border-radius: 8px; padding: 12px 16px; }
.node-head { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.node-op { color: #999; font-size: 12px; margin-left: auto; }
.node-desc { color: #666; font-size: 13px; line-height: 1.7; }
.desc-text { color: #555; line-height: 1.9; white-space: pre-wrap; }
</style>
