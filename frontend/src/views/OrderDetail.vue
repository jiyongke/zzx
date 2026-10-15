<template>
  <div v-loading="loading" class="order-detail">
    <el-breadcrumb separator="/" class="crumb">
      <el-breadcrumb-item :to="{ path: '/orders' }">我的订单</el-breadcrumb-item>
      <el-breadcrumb-item>订单详情</el-breadcrumb-item>
    </el-breadcrumb>

    <div v-if="detail" class="detail-wrap">
      <!-- 状态与操作 -->
      <div class="status-bar">
        <div class="status-left">
          <el-tag :type="statusType(detail.order.status)" size="large">{{ statusName(detail.order.status) }}</el-tag>
          <span class="status-desc">{{ statusDesc }}</span>
        </div>
        <div class="status-actions">
          <el-button v-if="isOwner && detail.order.status === 'PENDING'" type="primary" @click="handlePay">立即支付</el-button>
          <el-button v-if="isOwner && detail.order.status === 'PENDING'" @click="handleCancel">取消订单</el-button>
          <el-button
            v-if="isOwner && detail.order.status === 'SHIPPED'"
            type="success"
            :disabled="detail.order.deliveryStatus !== 'SIGNED'"
            @click="handleConfirm"
          >确认收货</el-button>
          <span v-if="isOwner && detail.order.status === 'SHIPPED' && detail.order.deliveryStatus !== 'SIGNED'" class="confirm-tip">商品签收后可确认收货</span>
          <el-button v-if="isOwner && ['PAID', 'SHIPPED'].includes(detail.order.status)" type="danger" plain @click="openRefund">申请售后</el-button>
          <el-button @click="goBack">返回列表</el-button>
        </div>
      </div>

      <!-- 售后信息 -->
      <div v-if="['REFUNDING', 'REFUNDED'].includes(detail.order.status)" class="section refund-box">
        <h3>售后信息</h3>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="售后状态">
            <el-tag :type="detail.order.status === 'REFUNDED' ? 'danger' : 'warning'" size="small">
              {{ detail.order.status === 'REFUNDED' ? '已退款' : '售后处理中' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.order.refundReason" label="申请原因">{{ detail.order.refundReason }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.order.refundTime" label="退款时间">{{ fmt(detail.order.refundTime) }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 申请售后弹窗 -->
      <el-dialog v-model="refundDialog" title="申请售后" width="460px">
        <el-form label-width="80px">
          <el-form-item label="申请原因">
            <el-input v-model="refundReason" type="textarea" :rows="3" maxlength="100" show-word-limit placeholder="请描述售后原因，例如：商品质量问题 / 发错货 / 不想要了" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="refundDialog = false">取消</el-button>
          <el-button type="danger" @click="handleRefund">提交申请</el-button>
        </template>
      </el-dialog>

      <!-- 地址 -->
      <div class="section">
        <h3>收货信息</h3>
        <p>{{ detail.order.receiver }}　{{ detail.order.phone }}</p>
        <p class="addr">{{ detail.order.address }}</p>
        <p v-if="detail.order.remark" class="addr">备注：{{ detail.order.remark }}</p>
      </div>

      <!-- 物流跟踪 -->
      <div v-if="detail.order.status === 'SHIPPED' || detail.logs?.length" class="section">
        <h3>物流跟踪</h3>
        <el-descriptions v-if="detail.order.courierCompany || detail.order.trackingNo" :column="2" border class="log-meta">
          <el-descriptions-item label="快递公司">{{ detail.order.courierCompany || '-' }}</el-descriptions-item>
          <el-descriptions-item label="运单号">
            <span class="tracking-no">{{ detail.order.trackingNo || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="物流状态" :span="2">
            <el-tag :type="deliveryTagType(detail.order.deliveryStatus)" size="small">{{ deliveryName(detail.order.deliveryStatus) }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <el-timeline class="log-timeline">
          <el-timeline-item
            v-for="l in detail.logs"
            :key="l.id"
            :timestamp="fmt(l.nodeTime)"
            placement="top"
            type="success"
          >
            <div class="log-card">
              <b>{{ l.title }}</b>
              <p>{{ l.description }}</p>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>

      <!-- 商品 -->
      <div class="section">
        <h3>商品清单</h3>
        <div v-for="item in detail.items" :key="item.id" class="order-item">
          <el-image class="item-img" :src="item.productImage" fit="cover" />
          <div class="item-name">{{ item.productName }}</div>
          <div class="item-price">¥{{ item.price.toFixed(2) }} × {{ item.quantity }}</div>
          <div class="item-amount">¥{{ item.amount.toFixed(2) }}</div>
        </div>
      </div>

      <!-- 金额与时间线 -->
      <div class="section">
        <h3>订单信息</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单编号">{{ detail.order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ fmt(detail.order.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ fmt(detail.order.payTime) }}</el-descriptions-item>
          <el-descriptions-item label="发货时间">{{ fmt(detail.order.shipTime) }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ fmt(detail.order.completeTime) }}</el-descriptions-item>
          <el-descriptions-item label="实付金额">
            <span class="total-price">¥{{ detail.order.totalAmount.toFixed(2) }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderDetail, payOrder, cancelOrder, confirmOrder, refundApply } from '../api'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const loading = ref(false)
const detail = ref(null)
const refundDialog = ref(false)
const refundReason = ref('')

// 是否订单本人（本人可执行支付/取消/确认收货操作；管理员/农户查看他人订单为只读）
const isOwner = computed(() => detail.value?.order && detail.value.order.userId === store.user?.id)

// 返回列表：管理员/农户回订单管理，消费者回我的订单
const goBack = () => {
  router.push(['ADMIN', 'FARMER'].includes(store.role) ? '/admin/orders' : '/orders')
}

const STATUS = {
  PENDING: '待支付', PAID: '已支付', SHIPPED: '已发货', COMPLETED: '已完成',
  CANCELLED: '已取消', REFUNDING: '售后处理中', REFUNDED: '已退款'
}
const statusName = (s) => STATUS[s] || s
const statusType = (s) => ({
  PENDING: 'warning', PAID: 'primary', SHIPPED: 'success', COMPLETED: 'info',
  CANCELLED: 'danger', REFUNDING: 'warning', REFUNDED: 'danger'
}[s] || 'info')

const DELIVERY = { PENDING: '待发货', SHIPPED: '已发货', DELIVERING: '配送中', SIGNED: '已签收' }
const deliveryName = (s) => DELIVERY[s] || s || '-'
const deliveryTagType = (s) => ({ PENDING: 'info', SHIPPED: 'primary', DELIVERING: 'warning', SIGNED: 'success' }[s] || 'info')
const fmt = (d) => (d ? String(d).replace('T', ' ').slice(0, 19) : '')

const statusDesc = computed(() => ({
  PENDING: '订单待支付，请尽快完成支付', PAID: '订单已支付，等待商家发货',
  SHIPPED: `订单已发货（${deliveryName(detail.value?.order.deliveryStatus)}），请留意物流信息`, COMPLETED: '交易已完成，感谢您的信任',
  CANCELLED: '订单已取消', REFUNDING: '售后申请处理中，请耐心等待',
  REFUNDED: '订单已退款，款项将原路退回'
}[detail.value?.order.status] || ''))

const load = async () => {
  loading.value = true
  try {
    const { data } = await getOrderDetail(route.params.id)
    detail.value = data
  } finally {
    loading.value = false
  }
}

const handlePay = async () => {
  await payOrder(detail.value.order.id)
  ElMessage.success('支付成功')
  load()
}

const handleCancel = async () => {
  await ElMessageBox.confirm('确定取消该订单吗？', '提示', { type: 'warning' })
  await cancelOrder(detail.value.order.id)
  ElMessage.success('订单已取消')
  load()
}

const handleConfirm = async () => {
  await ElMessageBox.confirm('确认已收到商品？', '提示', { type: 'warning' })
  await confirmOrder(detail.value.order.id)
  ElMessage.success('确认收货成功')
  load()
}

const openRefund = () => {
  refundReason.value = ''
  refundDialog.value = true
}

const handleRefund = async () => {
  if (!refundReason.value.trim()) {
    ElMessage.warning('请填写售后原因')
    return
  }
  await refundApply(detail.value.order.id, refundReason.value.trim())
  refundDialog.value = false
  ElMessage.success('售后申请已提交，等待平台审核')
  load()
}

onMounted(load)
</script>

<style scoped>
.crumb { margin-bottom: 16px; }
.status-bar {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.status-left { display: flex; align-items: center; gap: 16px; }
.status-desc { color: #666; font-size: 14px; }
.section { background: #fff; border-radius: 12px; padding: 20px 24px; margin-bottom: 16px; }
.section h3 { font-size: 15px; margin-bottom: 14px; }
.section p { color: #444; font-size: 14px; margin-bottom: 6px; }
.addr { color: #666 !important; font-size: 13px !important; }
.order-item { display: flex; align-items: center; gap: 16px; padding: 10px 0; border-bottom: 1px dashed #f0f0f0; }
.order-item:last-child { border-bottom: none; }
.item-img { width: 56px; height: 56px; border-radius: 8px; }
.item-name { flex: 1; font-size: 14px; }
.item-price { color: #666; font-size: 13px; width: 140px; }
.item-amount { color: #e4393c; font-weight: 700; }
.total-price { color: #e4393c; font-size: 18px; font-weight: 700; }
.refund-box { border: 1px solid #fde2e2; background: #fffafa; }
.confirm-tip { color: #999; font-size: 12px; }
.log-meta { margin-bottom: 14px; }
.tracking-no { font-family: monospace; }
.log-card { background: #f9fafb; border-radius: 8px; padding: 10px 14px; }
.log-card p { color: #666; font-size: 13px; margin-top: 4px; }
.log-timeline { padding-left: 4px; }
</style>
