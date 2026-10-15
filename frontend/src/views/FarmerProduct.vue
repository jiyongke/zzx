<template>
  <div class="fp-page">
    <!-- 顶部统计条 -->
    <div class="stat-bar">
      <div class="stat"><span class="stat-label">我的商品</span><b>{{ list.length }}</b></div>
      <div class="stat"><span class="stat-label">在售</span><b>{{ statusCount(1) }}</b></div>
      <div class="stat"><span class="stat-label">待审核</span><b>{{ statusCount(3) }}</b></div>
      <div class="stat"><span class="stat-label">已下架</span><b>{{ statusCount(0) }}</b></div>
      <div class="stat highlight"><span class="stat-label">总库存</span><b>{{ totalStock }}</b></div>
    </div>

    <div class="area-head">
      <h3>我的商品</h3>
      <div class="head-right">
        <el-input v-model="keyword" placeholder="搜索商品名称" clearable style="width: 220px" />
        <el-button type="primary" @click="openDialog()">+ 上新商品</el-button>
      </div>
    </div>
    <div class="sub-stats">
      提示：新增或修改后商品进入<b>待审核</b>，由平台管理员审核通过后上架；可关联自己名下的溯源批次实现"一批一码"可溯源。
    </div>

    <div class="goods-grid" v-loading="loading">
      <div v-for="p in filtered" :key="p.id" class="goods-card">
        <div class="img-wrap">
          <el-image :src="p.coverImage" fit="cover" class="g-img" />
          <el-tag v-if="p.traceable" class="trace-tag" type="success" size="small">可溯源</el-tag>
          <span class="status-tag" :class="'st' + p.status">{{ statusText(p.status) }}</span>
        </div>
        <div class="g-name" :title="p.name">{{ p.name }}</div>
        <div class="g-meta">
          <span class="g-price">¥{{ Number(p.price).toFixed(2) }}</span>
          <span class="g-stock">库存 {{ p.stock }}</span>
        </div>
        <div class="g-sales">已售 {{ p.sales || 0 }} 件 · {{ p.categoryName || '未分类' }}</div>
        <div v-if="p.batchCode" class="g-batch">溯源批次：{{ p.batchCode }}</div>
        <div class="g-ops">
          <div class="op-btns">
            <el-button text type="primary" size="small" @click="openDialog(p)">编辑</el-button>
            <el-button v-if="p.status !== 1" text type="success" size="small" @click="applyOnShelf(p)">
              {{ p.status === 3 ? '重新提交' : '申请上架' }}
            </el-button>
          </div>
        </div>
      </div>
    </div>
    <el-empty v-if="!loading && !filtered.length" description="还没有商品，点击右上角「上新商品」开始" />

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialog" :title="form.id ? '编辑商品' : '上新商品'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="商品名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联溯源批次">
          <el-select v-model="form.batchId" placeholder="可选，仅限自己名下的批次" clearable style="width: 100%">
            <el-option v-for="b in batches" :key="b.id" :label="`${b.productName} (${b.batchCode})`" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格(元)"><el-input-number v-model="form.price" :min="0.01" :precision="2" :step="1" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="form.stock" :min="0" :step="10" /></el-form-item>
        <el-form-item label="封面图URL"><el-input v-model="form.coverImage" placeholder="https://..." /></el-form-item>
        <el-form-item label="商品描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="saveProduct">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  farmerProducts, farmerSaveProduct, farmerUpdateProduct, farmerApplyProduct,
  getCategories, adminTraceBatches
} from '../api'

const list = ref([])
const categories = ref([])
const batches = ref([])
const keyword = ref('')
const loading = ref(false)
const dialog = ref(false)
const form = reactive({ id: null, name: '', categoryId: null, batchId: null, price: 1, stock: 0, coverImage: '', description: '' })

const filtered = computed(() => {
  if (!keyword.value.trim()) return list.value
  const kw = keyword.value.trim()
  return list.value.filter((p) => p.name.includes(kw))
})

const totalStock = computed(() => list.value.reduce((s, p) => s + (p.stock || 0), 0))
const statusCount = (st) => list.value.filter((p) => p.status === st).length
const statusText = (st) => ({ 1: '已上架', 0: '已下架', 3: '待审核' }[st] || '未知')

const load = async () => {
  loading.value = true
  try {
    const [p, c, b] = await Promise.all([
      farmerProducts({ page: 1, size: 100 }),
      getCategories(),
      adminTraceBatches({ page: 1, size: 100 })
    ])
    list.value = p.data.records || []
    categories.value = c.data || []
    batches.value = b.data.records || b.data || []
  } finally {
    loading.value = false
  }
}

const openDialog = (row) => {
  Object.assign(form, row || { id: null, name: '', categoryId: null, batchId: null, price: 1, stock: 0, coverImage: '', description: '' })
  dialog.value = true
}

const saveProduct = async () => {
  if (!form.name || !form.price) {
    ElMessage.warning('请填写商品名称和价格')
    return
  }
  const payload = {
    name: form.name,
    categoryId: form.categoryId,
    batchId: form.batchId,
    price: form.price,
    stock: form.stock,
    coverImage: form.coverImage,
    description: form.description
  }
  if (form.id) {
    await farmerUpdateProduct(form.id, payload)
    ElMessage.success('已保存，修改生效（状态由平台审核控制）')
  } else {
    await farmerSaveProduct(payload)
    ElMessage.success('已提交，等待平台审核上架')
  }
  dialog.value = false
  load()
}

const applyOnShelf = async (row) => {
  await farmerApplyProduct(row.id)
  ElMessage.success('已提交上架申请，等待平台审核')
  load()
}

onMounted(load)
</script>

<style scoped>
.fp-page { max-width: 1280px; }

.stat-bar {
  display: flex;
  background: linear-gradient(120deg, #f2f9f2, #e3f0e3);
  border-radius: 14px;
  padding: 16px 24px;
  margin-bottom: 16px;
  box-shadow: 0 2px 10px rgba(74, 124, 90, 0.1);
}
.stat { flex: 1; text-align: center; }
.stat-label { display: block; font-size: 12px; color: #6b8a6e; margin-bottom: 4px; }
.stat b { font-size: 20px; color: #4c7a5a; }
.stat.highlight b { color: #2e8b57; }

.area-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.area-head h3 { font-size: 18px; margin: 0; }
.head-right { display: flex; gap: 8px; }
.sub-stats { color: #888; font-size: 13px; margin-bottom: 12px; }

.goods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(215px, 1fr));
  gap: 14px;
}
.goods-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s, box-shadow 0.2s;
  padding-bottom: 10px;
}
.goods-card:hover { transform: translateY(-4px); box-shadow: 0 10px 24px rgba(0, 0, 0, 0.12); }
.img-wrap { position: relative; height: 150px; }
.g-img { width: 100%; height: 100%; }
.trace-tag { position: absolute; left: 8px; top: 8px; }
.status-tag {
  position: absolute; right: 8px; top: 8px;
  color: #fff; font-size: 12px; padding: 2px 8px; border-radius: 10px;
}
.status-tag.st1 { background: #67c23a; }
.status-tag.st0 { background: rgba(0, 0, 0, 0.55); }
.status-tag.st3 { background: #e6a23c; }
.g-name {
  font-size: 13px; font-weight: 600; color: #333;
  margin: 10px 12px 6px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.g-meta { display: flex; justify-content: space-between; align-items: baseline; margin: 0 12px; }
.g-price { color: #e4393c; font-weight: 700; font-size: 16px; }
.g-stock { color: #999; font-size: 12px; }
.g-sales { color: #999; font-size: 12px; margin: 6px 12px 0; }
.g-batch { color: #4c7a5a; font-size: 12px; margin: 4px 12px 0; }
.g-ops { display: flex; justify-content: flex-end; margin: 8px 10px 0; }
.op-btns { display: flex; }
</style>
