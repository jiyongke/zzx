<template>
  <div>
    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-cats">
        <span
          class="cat-chip"
          :class="{ active: !query.categoryId }"
          @click="selectCategory(null)"
        >全部</span>
        <span
          v-for="c in categories"
          :key="c.id"
          class="cat-chip"
          :class="{ active: Number(query.categoryId) === c.id }"
          @click="selectCategory(c.id)"
        >{{ c.icon }} {{ c.name }}</span>
      </div>
      <div class="filter-right">
        <el-input
          v-model="query.keyword"
          placeholder="搜索农产品"
          clearable
          style="width: 220px"
          @keyup.enter="load(1)"
        >
          <template #append>
            <el-button :icon="Search" @click="load(1)" />
          </template>
        </el-input>
        <el-select v-model="query.sort" style="width: 150px" @change="load(1)">
          <el-option label="综合排序" value="" />
          <el-option label="销量优先" value="sales" />
          <el-option label="价格从低到高" value="price_asc" />
          <el-option label="价格从高到低" value="price_desc" />
        </el-select>
      </div>
    </div>

    <!-- 商品列表 -->
    <div v-loading="loading" class="product-grid">
      <div v-for="p in products" :key="p.id" class="product-card" @click="$router.push(`/product/${p.id}`)">
        <el-image class="product-img" :src="p.coverImage" fit="cover" lazy />
        <div class="product-info">
          <div class="product-name">{{ p.name }}</div>
          <div class="product-meta">
            <el-tag v-if="p.traceable" size="small" type="success">可溯源</el-tag>
            <el-tag v-else size="small" type="info">普通</el-tag>
            <span class="product-sales">已售 {{ p.sales }}</span>
          </div>
          <div class="product-price">¥{{ p.price.toFixed(2) }}</div>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && products.length === 0" description="暂无相关商品" />
    <div class="pager">
      <el-pagination
        v-model:current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="prev, pager, next, total"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { getCategories, getProducts } from '../api'

const route = useRoute()
const categories = ref([])
const products = ref([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  page: 1,
  size: 12,
  categoryId: route.query.categoryId ? Number(route.query.categoryId) : null,
  keyword: '',
  sort: ''
})

const load = async (page) => {
  query.page = page || query.page
  loading.value = true
  try {
    const { data } = await getProducts(query)
    products.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const selectCategory = (id) => {
  query.categoryId = id
  load(1)
}

onMounted(async () => {
  const { data } = await getCategories()
  categories.value = data
  load(1)
})
</script>

<style scoped>
.filter-bar {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.filter-cats { display: flex; flex-wrap: wrap; gap: 8px; }
.cat-chip {
  padding: 6px 14px;
  border-radius: 20px;
  background: #f5f6f8;
  cursor: pointer;
  font-size: 13px;
  color: #555;
}
.cat-chip.active { background: #ff8c42; color: #fff; }
.filter-right { display: flex; gap: 10px; }
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
.product-img { width: 100%; height: 190px; display: block; }
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
.pager { display: flex; justify-content: center; margin-top: 24px; }
</style>
