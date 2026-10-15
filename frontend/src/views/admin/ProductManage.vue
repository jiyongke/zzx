<template>
  <div class="pm-page">
    <!-- 顶部总览统计条（总和） -->
    <div class="stat-bar">
      <div class="stat"><span class="stat-label">商品总数</span><b>{{ sum.count }}</b></div>
      <div class="stat"><span class="stat-label">在售</span><b>{{ onSaleCount }}</b></div>
      <div class="stat"><span class="stat-label">总库存</span><b>{{ sum.stock }}</b></div>
      <div class="stat"><span class="stat-label">总销量</span><b>{{ sum.sales }}</b></div>
      <div class="stat highlight"><span class="stat-label">总销售额（估）</span><b>¥{{ sum.amount.toFixed(2) }}</b></div>
    </div>

    <div class="body">
      <!-- 左侧分类导航 -->
      <aside class="cat-side">
        <div class="side-title">商品分类</div>
        <div class="cat-item all" :class="{ active: activeCat === null }" @click="activeCat = null">
          <span class="cat-name">📦 全部商品</span>
          <span class="cnt">{{ total }}</span>
        </div>
        <div
          v-for="c in categories"
          :key="c.id"
          class="cat-item"
          :class="{ active: activeCat === c.id }"
          @click="activeCat = c.id"
        >
          <span class="cat-name">{{ c.icon }} {{ c.name }}</span>
          <span class="cnt">{{ countByCat(c.id) }}</span>
        </div>
        <el-button class="manage-cat" @click="catDialog = true">
          <el-icon><Setting /></el-icon>&nbsp;管理分类
        </el-button>
      </aside>

      <!-- 右侧商品区 -->
      <main class="goods-area">
        <div class="area-head">
          <h3>{{ currentTitle }}</h3>
          <div class="head-right">
            <el-input v-model="keyword" placeholder="搜索商品名称" clearable style="width: 220px" />
            <el-button type="primary" @click="openDialog()">+ 新增商品</el-button>
          </div>
        </div>

        <div class="sub-stats">
          当前{{ activeCat === null ? '全部' : '分类' }}：{{ curProducts.length }} 个商品 · 总销量 {{ curSales }} · 销售额 ¥{{ curAmount.toFixed(2) }}
        </div>

        <!-- 商品卡片网格 -->
        <div class="goods-grid" v-loading="loading">
          <div v-for="p in curProducts" :key="p.id" class="goods-card">
            <div class="img-wrap">
              <el-image :src="p.coverImage" fit="cover" class="g-img" />
              <el-tag v-if="p.traceable" class="trace-tag" type="success" size="small">可溯源</el-tag>
              <span v-if="p.status === 3" class="off-tag pending">待审核</span>
              <span v-else-if="p.status !== 1" class="off-tag">已下架</span>
            </div>
            <div class="g-name" :title="p.name">{{ p.name }}</div>
            <div class="g-meta">
              <span class="g-price">¥{{ p.price.toFixed(2) }}</span>
              <span class="g-stock">库存 {{ p.stock }}</span>
            </div>
            <div class="g-sales">已售 {{ p.sales }} 件 · {{ p.categoryName }}</div>
            <div class="g-ops">
              <template v-if="p.status !== 3">
                <el-switch :model-value="p.status === 1" @change="(v) => toggleStatus(p, v)" size="small" />
              </template>
              <span v-else class="pending-label">待平台审核</span>
              <span class="op-btns">
                <el-button v-if="p.status === 3" text type="success" size="small" @click="reviewProduct(p, true)">通过</el-button>
                <el-button v-if="p.status === 3" text type="danger" size="small" @click="reviewProduct(p, false)">驳回</el-button>
                <el-button text type="primary" size="small" @click="openDialog(p)">编辑</el-button>
                <el-button text type="danger" size="small" @click="removeProduct(p)">删除</el-button>
              </span>
            </div>
          </div>
        </div>
        <el-empty v-if="!loading && !curProducts.length" description="该分类暂无商品" />
      </main>
    </div>

    <!-- 商品弹窗 -->
    <el-dialog v-model="dialog" :title="form.id ? '编辑商品' : '新增商品'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="商品名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联溯源批次">
          <el-select v-model="form.batchId" placeholder="可选，关联后可溯源" clearable style="width: 100%">
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

    <!-- 分类管理弹窗 -->
    <el-dialog v-model="catDialog" title="分类管理" width="600px">
      <div class="cat-toolbar">
        <el-button type="primary" size="small" @click="openCatForm()">+ 新增分类</el-button>
      </div>
      <el-table :data="allCategories" size="small" stripe max-height="380">
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="图标" width="70">
          <template #default="{ row }"><span style="font-size: 20px">{{ row.icon }}</span></template>
        </el-table-column>
        <el-table-column label="分类名称" prop="name" />
        <el-table-column label="排序" prop="sort" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openCatForm(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="removeCategory(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 分类表单弹窗 -->
    <el-dialog v-model="catFormDialog" :title="catForm.id ? '编辑分类' : '新增分类'" width="420px">
      <el-form :model="catForm" label-width="80px">
        <el-form-item label="名称"><el-input v-model="catForm.name" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="catForm.icon" placeholder="emoji 或图片URL" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="catForm.sort" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="catForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="catFormDialog = false">取消</el-button>
        <el-button type="primary" @click="saveCategory">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Setting } from '@element-plus/icons-vue'
import {
  adminProducts, saveProduct as apiSaveProduct, updateProduct, updateProductStatus, deleteProduct,
  getCategories, adminCategories, saveCategory as apiSaveCategory, updateCategory, deleteCategory, adminTraceBatches
} from '../../api'

// ============ 数据 ============
const products = ref([])
const categories = ref([])      // 启用分类（左侧导航）
const allCategories = ref([])   // 全部分类（分类管理）
const batches = ref([])
const keyword = ref('')
const activeCat = ref(null)     // null = 全部
const loading = ref(false)
const dialog = ref(false)
const form = reactive({ id: null, name: '', categoryId: null, batchId: null, price: 1, stock: 0, coverImage: '', description: '' })

// ============ 统计 ============
const total = computed(() => products.value.length)
const onSaleCount = computed(() => products.value.filter((p) => p.status === 1).length)
const sum = computed(() => {
  const all = products.value
  return {
    count: all.length,
    stock: all.reduce((s, p) => s + p.stock, 0),
    sales: all.reduce((s, p) => s + p.sales, 0),
    amount: all.reduce((s, p) => s + p.sales * p.price, 0)
  }
})

const filtered = computed(() => {
  if (!keyword.value.trim()) return products.value
  const kw = keyword.value.trim()
  return products.value.filter((p) => p.name.includes(kw))
})

const curProducts = computed(() => {
  const list = filtered.value
  return activeCat.value === null ? list : list.filter((p) => p.categoryId === activeCat.value)
})

const curSales = computed(() => curProducts.value.reduce((s, p) => s + p.sales, 0))
const curAmount = computed(() => curProducts.value.reduce((s, p) => s + p.sales * p.price, 0))

const countByCat = (id) => products.value.filter((p) => p.categoryId === id).length

const currentTitle = computed(() => {
  if (activeCat.value === null) return '全部商品'
  const c = categories.value.find((c) => c.id === activeCat.value)
  return c ? `${c.icon} ${c.name}` : '商品'
})

// ============ 商品操作 ============
const load = async () => {
  loading.value = true
  try {
    const [p, c, allC, b] = await Promise.all([
      adminProducts({ page: 1, size: 100 }),
      getCategories(),
      adminCategories(),
      adminTraceBatches({ page: 1, size: 100 })
    ])
    products.value = p.data.records || []
    categories.value = c.data || []
    allCategories.value = allC.data || []
    batches.value = b.data.records || b.data
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
  if (form.id) {
    await updateProduct(form.id, { ...form })
  } else {
    await apiSaveProduct({ ...form, sales: 0, status: 1 })
  }
  dialog.value = false
  ElMessage.success('保存成功')
  load()
}

const toggleStatus = async (row, v) => {
  await updateProductStatus(row.id, v ? 1 : 0)
  ElMessage.success(v ? '已上架' : '已下架')
  load()
}

// 商品发布审核：通过 -> 上架(1)；驳回 -> 下架(0)
const reviewProduct = async (row, approve) => {
  if (approve) {
    await ElMessageBox.confirm(`审核通过「${row.name}」并上架销售？`, '审核确认', { type: 'info' })
  } else {
    await ElMessageBox.confirm(`驳回「${row.name}」的上架申请？驳回后该商品保持下架状态。`, '驳回确认', { type: 'warning' })
  }
  await updateProductStatus(row.id, approve ? 1 : 0)
  ElMessage.success(approve ? '已审核通过并上架' : '已驳回上架申请')
  load()
}

const removeProduct = async (row) => {
  await ElMessageBox.confirm(`确定删除「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteProduct(row.id)
  ElMessage.success('已删除')
  load()
}

// ============ 分类操作 ============
const catDialog = ref(false)
const catFormDialog = ref(false)
const catForm = reactive({ id: null, name: '', icon: '', sort: 0, status: 1 })

const openCatForm = (row) => {
  Object.assign(catForm, row || { id: null, name: '', icon: '', sort: 0, status: 1 })
  catFormDialog.value = true
}

const saveCategory = async () => {
  if (!catForm.name) {
    ElMessage.warning('请填写分类名称')
    return
  }
  if (catForm.id) {
    await updateCategory(catForm.id, { ...catForm })
  } else {
    await apiSaveCategory({ ...catForm })
  }
  catFormDialog.value = false
  ElMessage.success('保存成功')
  load()
}

const removeCategory = async (row) => {
  await ElMessageBox.confirm(`确定删除「${row.name}」分类吗？`, '提示', { type: 'warning' })
  await deleteCategory(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.pm-page { max-width: 1280px; }

/* 顶部统计条 */
.stat-bar {
  display: flex;
  background: linear-gradient(120deg, #fff7f0, #ffe8d6);
  border-radius: 14px;
  padding: 16px 24px;
  margin-bottom: 16px;
  box-shadow: 0 2px 10px rgba(210, 105, 30, 0.08);
}
.stat { flex: 1; text-align: center; }
.stat-label { display: block; font-size: 12px; color: #8a5a3b; margin-bottom: 4px; }
.stat b { font-size: 20px; color: #d2691e; }
.stat.highlight b { color: #2e8b57; }

.body { display: flex; gap: 16px; align-items: flex-start; }

/* 左侧分类导航 */
.cat-side {
  width: 210px;
  background: #fff;
  border-radius: 14px;
  padding: 14px 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  position: sticky;
  top: 8px;
}
.side-title { font-size: 13px; color: #999; padding: 4px 10px 10px; font-weight: 600; }
.cat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  margin-bottom: 4px;
  border-radius: 8px;
  cursor: pointer;
  color: #444;
  transition: all 0.15s;
}
.cat-item:hover { background: #fff7f0; }
.cat-item.active { background: #ff8c42; color: #fff; font-weight: 600; }
.cat-item .cnt {
  font-size: 12px;
  background: #f0f0f0;
  border-radius: 10px;
  padding: 1px 8px;
}
.cat-item.active .cnt { background: rgba(255, 255, 255, 0.25); color: #fff; }
.manage-cat { width: 100%; margin-top: 12px; }

/* 右侧商品区 */
.goods-area { flex: 1; min-width: 0; }
.area-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.area-head h3 { font-size: 18px; }
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
.off-tag {
  position: absolute; right: 8px; top: 8px;
  background: rgba(0, 0, 0, 0.55); color: #fff;
  font-size: 12px; padding: 2px 8px; border-radius: 10px;
}
.off-tag.pending { background: #e6a23c; }
.g-name {
  font-size: 13px; font-weight: 600; color: #333;
  margin: 10px 12px 6px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.g-meta { display: flex; justify-content: space-between; align-items: baseline; margin: 0 12px; }
.g-price { color: #e4393c; font-weight: 700; font-size: 16px; }
.g-stock { color: #999; font-size: 12px; }
.g-sales { color: #999; font-size: 12px; margin: 6px 12px 10px; }
.g-ops { display: flex; justify-content: space-between; align-items: center; margin: 0 10px; }
.op-btns { display: flex; }
.pending-label { color: #e6a23c; font-size: 12px; }
</style>
