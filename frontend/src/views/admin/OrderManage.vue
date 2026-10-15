<template>
  <div class="manage-page">
    <h2 class="page-title">订单管理</h2>

    <div class="toolbar">
      <el-select v-model="status" style="width: 140px" @change="load(1)">
        <el-option label="全部状态" value="ALL" />
        <el-option label="待支付" value="PENDING" />
        <el-option label="已支付" value="PAID" />
        <el-option label="已发货" value="SHIPPED" />
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="售后中" value="REFUNDING" />
        <el-option label="已退款" value="REFUNDED" />
        <el-option label="已取消" value="CANCELLED" />
      </el-select>
      <el-input v-model="keyword" placeholder="订单号/收货人" clearable style="width: 220px" @keyup.enter="load(1)" />
      <el-button @click="load(1)">搜索</el-button>
    </div>

    <el-table :data="orders" v-loading="loading" stripe>
      <el-table-column label="订单号" prop="order.orderNo" min-width="170" />
      <el-table-column label="收货人" prop="order.receiver" width="90" />
      <el-table-column label="商品" min-width="160">
        <template #default="{ row }">
          <span v-for="i in row.items" :key="i.id" class="item-text">{{ i.productName }} ×{{ i.quantity }}<br /></span>
        </template>
      </el-table-column>
      <el-table-column label="金额" width="100">
        <template #default="{ row }">¥{{ row.order.totalAmount.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="下单时间" width="150">
        <template #default="{ row }">{{ fmt(row.order.createTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusType(row.order.status)" size="small">{{ statusName(row.order.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="物流" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.order.status === 'SHIPPED'" :type="deliveryTagType(row.order.deliveryStatus)" size="small">{{ deliveryName(row.order.deliveryStatus) }}</el-tag>
          <span v-else class="tip">-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.order.status === 'PAID' && ['ADMIN', 'FARMER'].includes(store.role)"
            type="primary" size="small" @click="openShip(row)"
          >发货</el-button>
          <template v-if="row.order.status === 'SHIPPED' && row.order.deliveryStatus === 'SHIPPED' && ['ADMIN', 'FARMER'].includes(store.role)">
            <el-button type="warning" size="small" @click="handleDelivery(row, 'DELIVERING')">配送中</el-button>
          </template>
          <template v-if="row.order.status === 'SHIPPED' && row.order.deliveryStatus === 'DELIVERING' && ['ADMIN', 'FARMER'].includes(store.role)">
            <el-button type="success" size="small" @click="handleDelivery(row, 'SIGNED')">已签收</el-button>
          </template>
          <template v-if="row.order.status === 'REFUNDING' && store.role === 'ADMIN'">
            <el-button type="danger" size="small" @click="handleRefund(row, true)">同意退款</el-button>
            <el-button type="info" size="small" @click="handleRefund(row, false)">驳回</el-button>
          </template>
          <el-button v-else text type="primary" size="small" @click="$router.push(`/order/${row.order.id}`)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 发货弹窗：快递公司 + 运单号 -->
    <el-dialog v-model="shipDialog" title="订单发货" width="460px">
      <el-form :model="shipForm" label-width="90px">
        <el-form-item label="订单号"><el-input :model-value="shipForm.orderNo" disabled /></el-form-item>
        <el-form-item label="快递公司">
          <el-input v-model="shipForm.courierCompany" placeholder="如：顺丰速运 / 中通快递" />
        </el-form-item>
        <el-form-item label="运单号">
          <el-input v-model="shipForm.trackingNo" placeholder="快递运单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialog = false">取消</el-button>
        <el-button type="primary" @click="submitShip">确认发货</el-button>
      </template>
    </el-dialog>

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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminOrders, shipOrder, updateDelivery, refundHandle } from '../../api'
import { useUserStore } from '../../store/user'

const store = useUserStore()
const orders = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const status = ref('ALL')
const keyword = ref('')
const loading = ref(false)
const shipDialog = ref(false)
const shipForm = reactive({ id: null, orderNo: '', courierCompany: '', trackingNo: '' })

const STATUS = { PENDING: '待支付', PAID: '已支付', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消', REFUNDING: '售后中', REFUNDED: '已退款' }
const statusName = (s) => STATUS[s] || s
const statusType = (s) => ({ PENDING: 'warning', PAID: 'primary', SHIPPED: 'success', COMPLETED: 'info', CANCELLED: 'danger', REFUNDING: 'warning', REFUNDED: 'danger' }[s] || 'info')
const DELIVERY = { PENDING: '待发货', SHIPPED: '已发货', DELIVERING: '配送中', SIGNED: '已签收' }
const deliveryName = (s) => DELIVERY[s] || s || '-'
const deliveryTagType = (s) => ({ PENDING: 'info', SHIPPED: 'primary', DELIVERING: 'warning', SIGNED: 'success' }[s] || 'info')
const fmt = (d) => (d ? String(d).replace('T', ' ').slice(0, 19) : '')

const load = async (p) => {
  page.value = p || page.value
  loading.value = true
  try {
    const { data } = await adminOrders({ page: page.value, size: size.value, status: status.value, keyword: keyword.value })
    orders.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const openShip = (row) => {
  Object.assign(shipForm, { id: row.order.id, orderNo: row.order.orderNo, courierCompany: '', trackingNo: '' })
  shipDialog.value = true
}

const submitShip = async () => {
  if (!shipForm.courierCompany || !shipForm.trackingNo) {
    ElMessage.warning('请填写快递公司与运单号')
    return
  }
  await shipOrder(shipForm.id, { courierCompany: shipForm.courierCompany, trackingNo: shipForm.trackingNo })
  shipDialog.value = false
  ElMessage.success('已发货，物流时间轴已生成')
  load()
}

const handleDelivery = async (row, deliveryStatus) => {
  const action = deliveryStatus === 'DELIVERING' ? '标记为配送中' : '标记为已签收'
  await ElMessageBox.confirm(`确认将订单 ${row.order.orderNo} ${action}？`, '物流更新', { type: 'info' })
  await updateDelivery(row.order.id, deliveryStatus)
  ElMessage.success(action + '成功')
  load()
}

const handleRefund = async (row, approve) => {
  if (approve) {
    await ElMessageBox.confirm(`同意为订单 ${row.order.orderNo} 退款 ¥${row.order.totalAmount.toFixed(2)}？退款后商品销量将回滚。`, '退款确认', { type: 'warning' })
  } else {
    await ElMessageBox.confirm(`驳回订单 ${row.order.orderNo} 的售后申请？`, '驳回确认', { type: 'info' })
  }
  await refundHandle(row.order.id, approve)
  ElMessage.success(approve ? '已同意退款' : '已驳回申请')
  load()
}

onMounted(() => load(1))
</script>

<style scoped>
.page-title { font-size: 22px; margin-bottom: 16px; }
.toolbar { margin-bottom: 12px; display: flex; gap: 8px; }
.item-text { font-size: 13px; line-height: 1.7; }
.tip { color: #999; font-size: 12px; }
.pager { display: flex; justify-content: center; margin-top: 16px; }
</style>
