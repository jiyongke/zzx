<template>
  <div class="cart-page">
    <h2 class="page-title">购物车</h2>

    <el-empty v-if="items.length === 0" description="购物车还是空的，去挑选些可溯源的农产品吧">
      <el-button type="primary" round @click="$router.push('/products')">去逛逛</el-button>
    </el-empty>

    <template v-else>
      <div class="cart-list">
        <div v-for="item in items" :key="item.id" class="cart-item">
          <el-image class="cart-img" :src="item.coverImage" fit="cover" @click="$router.push(`/product/${item.productId}`)" />
          <div class="cart-info">
            <div class="cart-name" @click="$router.push(`/product/${item.productId}`)">{{ item.name }}</div>
            <el-tag v-if="item.traceable" size="small" type="success">可溯源</el-tag>
            <div class="cart-stock" v-if="item.quantity > item.stock" style="color:#e4393c">库存不足</div>
          </div>
          <div class="cart-price">¥{{ item.price.toFixed(2) }}</div>
          <el-input-number
            v-model="item.quantity"
            :min="1"
            :max="Math.max(item.stock, 1)"
            size="small"
            @change="(v) => changeQuantity(item, v)"
          />
          <div class="cart-amount">¥{{ (item.price * item.quantity).toFixed(2) }}</div>
          <el-button text type="danger" :icon="Delete" @click="removeItem(item)" />
        </div>
      </div>

      <div class="cart-footer">
        <div class="total">
          合计：<span class="total-price">¥{{ totalAmount.toFixed(2) }}</span>
        </div>
        <el-button type="primary" size="large" round @click="goCheckout">去结算</el-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { getCart, updateCart, deleteCart } from '../api'

const router = useRouter()
const items = ref([])

const totalAmount = computed(() =>
  items.value.reduce((sum, i) => sum + (i.price || 0) * i.quantity, 0)
)

onMounted(async () => {
  const { data } = await getCart()
  items.value = data
})

const changeQuantity = async (item, v) => {
  if (v > item.stock) {
    ElMessage.warning(`库存仅剩 ${item.stock} 件`)
  }
  await updateCart(item.id, v)
}

const removeItem = async (item) => {
  await ElMessageBox.confirm('确定将该商品移出购物车吗？', '提示', { type: 'warning' })
  await deleteCart(item.id)
  items.value = items.value.filter((i) => i.id !== item.id)
}

const goCheckout = () => {
  if (items.value.some((i) => i.quantity > i.stock)) {
    ElMessage.warning('存在超出库存的商品，请调整数量')
    return
  }
  router.push('/checkout')
}
</script>

<style scoped>
.page-title { font-size: 22px; margin-bottom: 16px; }
.cart-list { background: #fff; border-radius: 12px; }
.cart-item {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}
.cart-item:last-child { border-bottom: none; }
.cart-img { width: 80px; height: 80px; border-radius: 8px; cursor: pointer; }
.cart-info { flex: 1; display: flex; flex-direction: column; gap: 6px; }
.cart-name { font-size: 14px; cursor: pointer; }
.cart-name:hover { color: #ff8c42; }
.cart-price { color: #e4393c; font-weight: 700; width: 90px; }
.cart-amount { color: #e4393c; font-weight: 700; width: 90px; text-align: right; }
.cart-footer {
  margin-top: 16px;
  background: #fff;
  border-radius: 12px;
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 24px;
}
.total { font-size: 15px; }
.total-price { color: #e4393c; font-size: 22px; font-weight: 700; }
</style>
