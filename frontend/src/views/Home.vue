<template>
  <div>
    <!-- 主视觉 Banner -->
    <section class="banner">
      <div class="banner-inner">
        <div class="banner-text">
          <h1>每一份农产品，都有来处</h1>
          <p>从田间到餐桌，种植、施肥、采收、质检全链路可溯源<br/>扫码即查，吃得放心，买得安心</p>
          <div class="banner-actions">
            <el-button type="primary" size="large" round @click="$router.push('/products')">立即选购</el-button>
            <el-button size="large" round @click="$router.push('/trace')">查溯源</el-button>
          </div>
        </div>
        <el-image
          class="banner-img"
          src="https://picsum.photos/seed/farm/560/420"
          fit="cover"
        />
      </div>
    </section>

    <!-- 分类入口 -->
    <section class="section">
      <h2 class="section-title">农产品分类</h2>
      <div class="category-grid">
        <div v-for="c in categories" :key="c.id" class="category-item" @click="goCategory(c.id)">
          <span class="category-icon">{{ c.icon }}</span>
          <span>{{ c.name }}</span>
        </div>
      </div>
    </section>

    <!-- 溯源流程 -->
    <section class="section trace-flow">
      <h2 class="section-title">全程可追溯 · 八大环节</h2>
      <div class="flow-steps">
        <div v-for="(s, i) in flowSteps" :key="i" class="flow-step">
          <span class="flow-num">{{ i + 1 }}</span>
          <span class="flow-name">{{ s }}</span>
        </div>
      </div>
    </section>

    <!-- 热销推荐 -->
    <section class="section">
      <div class="section-head">
        <h2 class="section-title">热销推荐</h2>
        <el-link type="primary" @click="$router.push('/products')">查看更多 ></el-link>
      </div>
      <div class="product-grid">
        <div v-for="p in hotProducts" :key="p.id" class="product-card" @click="$router.push(`/product/${p.id}`)">
          <el-image class="product-img" :src="p.coverImage" fit="cover" lazy />
          <div class="product-info">
            <div class="product-name">{{ p.name }}</div>
            <div class="product-meta">
              <el-tag v-if="p.traceable" size="small" type="success">可溯源</el-tag>
              <span class="product-sales">已售 {{ p.sales }}</span>
            </div>
            <div class="product-price">¥{{ p.price.toFixed(2) }}</div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCategories, getHotProducts } from '../api'

const router = useRouter()
const categories = ref([])
const hotProducts = ref([])
const flowSteps = ['选种育苗', '种植管理', '有机施肥', '绿色防治', '人工采收', '加工分拣', '质检认证', '冷链物流']

onMounted(async () => {
  const [c, h] = await Promise.all([getCategories(), getHotProducts(8)])
  categories.value = c.data
  hotProducts.value = h.data
})

const goCategory = (id) => router.push({ path: '/products', query: { categoryId: id } })
</script>

<style scoped>
.banner {
  background: linear-gradient(120deg, #fff7f0 0%, #ffe8d6 60%, #ffd9b8 100%);
  border-radius: 16px;
  overflow: hidden;
}
.banner-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 48px 40px;
  gap: 40px;
}
.banner-text h1 { font-size: 36px; color: #d2691e; margin-bottom: 16px; }
.banner-text p { color: #8a5a3b; font-size: 15px; line-height: 1.8; margin-bottom: 24px; }
.banner-img { width: 400px; height: 300px; border-radius: 12px; box-shadow: 0 8px 24px rgba(210, 105, 30, 0.25); }

.section { margin-top: 32px; }
.section-title { font-size: 22px; margin-bottom: 16px; color: #333; }
.section-head { display: flex; justify-content: space-between; align-items: center; }

.category-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 16px; }
.category-item {
  background: #fff;
  border-radius: 12px;
  padding: 24px 0;
  text-align: center;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: transform 0.2s;
}
.category-item:hover { transform: translateY(-4px); box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08); }
.category-icon { font-size: 36px; }

.trace-flow { background: #fff; border-radius: 12px; padding: 24px; }
.flow-steps { display: flex; justify-content: space-between; flex-wrap: wrap; gap: 8px; }
.flow-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  width: 12%;
  min-width: 90px;
}
.flow-num {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffb347, #ff8c42);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}
.flow-name { font-size: 13px; color: #666; }

.product-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.product-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.product-card:hover { transform: translateY(-4px); box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1); }
.product-img { width: 100%; height: 200px; display: block; }
.product-info { padding: 12px 14px 14px; }
.product-name {
  font-size: 14px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 8px;
}
.product-meta { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.product-sales { font-size: 12px; color: #999; }
.product-price { color: #e4393c; font-size: 18px; font-weight: 700; }
</style>
