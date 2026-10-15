<template>
  <div class="orders-page">
    <h2 class="page-title">我的订单</h2>

    <el-tabs v-model="activeStatus" @tab-change="load(1)">
      <el-tab-pane label="全部" name="ALL" />
      <el-tab-pane label="待支付" name="PENDING" />
      <el-tab-pane label="已支付" name="PAID" />
      <el-tab-pane label="已发货" name="SHIPPED" />
      <el-tab-pane label="已完成" name="COMPLETED" />
      <el-tab-pane label="已取消" name="CANCELLED" />
    </el-tabs>

    <el-empty v-if="!loading && orders.length === 0" description="暂无相关订单" />

    <div v-loading="loading" class="order-list">
      <div v-for="o in orders" :key="o.order.id" class="order-card" @click="$router.push(`/order/${o.order.id}`)">
        <div class="order-head">
          <span class="order-no">订单号：{{ o.order.orderNo }}</span>
          <span class="order-time">{{ fmt(o.order.createTime) }}</span>
          <el-tag :type="statusType(o.order.status)" size="small">{{ statusName(o.order.status) }}</el-tag>
        </div>
        <div class="order-body">
          <div v-for="item in o.items" :key="item.id" class="order-item">
            <el-image class="item-img" :src="item.productImage" fit="cover" />
            <div class="item-name">{{ item.productName }}</div>
            <div class="item-spec">¥{{ item.price.toFixed(2) }} × {{ item.quantity }}</div>
          </div>
          <div class="order-total">
            <div>共 {{ o.items.length }} 种商品</div>
            <div>合计：<span class="total-price">¥{{ o.order.totalAmount.toFixed(2) }}</span></div>
          </div>
        </div>
      </div>
    </div>

    <div class="pager">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next, total"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyOrders } from '../api'

const orders = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const activeStatus = ref('ALL')
const loading = ref(false)

const STATUS = {
  PENDING: '待支付', PAID: '已支付', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消'
}
const statusName = (s) => STATUS[s] || s
const statusType = (s) => ({
  PENDING: 'warning', PAID: 'primary', SHIPPED: 'success', COMPLETED: 'info', CANCELLED: 'danger'
}[s] || 'info')
const fmt = (d) => (d ? String(d).replace('T', ' ').slice(0, 19) : '')

const load = async (p) => {
  page.value = p || page.value
  loading.value = true
  try {
    const { data } = await getMyOrders({ page: page.value, size: size.value, status: activeStatus.value })
    orders.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

onMounted(() => load(1))
</script>

<style scoped>
.page-title { font-size: 22px; margin-bottom: 16px; }
.order-list { display: flex; flex-direction: column; gap: 12px; }
.order-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.order-card:hover { box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08); }
.order-head { display: flex; align-items: center; gap: 16px; padding-bottom: 12px; border-bottom: 1px solid #f5f5f5; }
.order-no { font-size: 13px; color: #666; font-family: monospace; }
.order-time { font-size: 12px; color: #999; }
.order-body { display: flex; align-items: center; padding-top: 12px; gap: 12px; }
.order-item { display: flex; align-items: center; gap: 10px; flex: 1; }
.item-img { width: 56px; height: 56px; border-radius: 8px; }
.item-name { font-size: 14px; }
.item-spec { color: #999; font-size: 13px; }
.order-total { text-align: right; font-size: 13px; color: #666; }
.total-price { color: #e4393c; font-size: 18px; font-weight: 700; }
.pager { display: flex; justify-content: center; margin-top: 20px; }
</style>
