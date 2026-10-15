<template>
  <div class="checkout-page">
    <h2 class="page-title">确认订单</h2>

    <el-empty v-if="items.length === 0" description="没有待结算的商品" />

    <template v-else>
      <!-- 收货地址 -->
      <div class="section">
        <div class="section-head">
          <h3>收货地址</h3>
          <el-button text type="primary" @click="openAddressDialog">+ 新增地址</el-button>
        </div>
        <div class="address-grid">
          <div
            v-for="a in addresses"
            :key="a.id"
            class="address-card"
            :class="{ active: selectedAddress === a.id }"
            @click="selectedAddress = a.id"
          >
            <div class="addr-receiver">{{ a.receiver }} <span class="addr-phone">{{ a.phone }}</span></div>
            <div class="addr-detail">{{ a.province }}{{ a.city }}{{ a.district }}{{ a.detail }}</div>
            <el-tag v-if="a.isDefault === 1" size="small" type="success">默认</el-tag>
          </div>
        </div>
      </div>

      <!-- 商品清单 -->
      <div class="section">
        <h3>商品清单</h3>
        <div v-for="item in items" :key="item.productId" class="order-item">
          <el-image class="item-img" :src="item.coverImage" fit="cover" />
          <div class="item-name">{{ item.name }}</div>
          <div class="item-price">¥{{ item.price.toFixed(2) }} × {{ item.quantity }}</div>
          <div class="item-amount">¥{{ (item.price * item.quantity).toFixed(2) }}</div>
        </div>
      </div>

      <!-- 备注 -->
      <div class="section">
        <h3>订单备注</h3>
        <el-input v-model="remark" placeholder="选填，给配送员留言（如：请放门口）" maxlength="100" />
      </div>

      <div class="submit-bar">
        <div class="total">应付金额：<span class="total-price">¥{{ totalAmount.toFixed(2) }}</span></div>
        <el-button type="primary" size="large" round :loading="submitting" @click="submitOrder">提交订单</el-button>
      </div>
    </template>

    <!-- 新增地址弹窗 -->
    <el-dialog v-model="addressDialog" title="新增收货地址" width="480px">
      <el-form :model="addrForm" label-width="80px">
        <el-form-item label="收货人"><el-input v-model="addrForm.receiver" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="addrForm.phone" /></el-form-item>
        <el-form-item label="省市区"><el-input v-model="addrForm.province" placeholder="省" style="width: 32%" /> <el-input v-model="addrForm.city" placeholder="市" style="width: 32%" /> <el-input v-model="addrForm.district" placeholder="区/县" style="width: 32%" /></el-form-item>
        <el-form-item label="详细地址"><el-input v-model="addrForm.detail" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="设为默认"><el-switch v-model="addrForm.isDefault" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addressDialog = false">取消</el-button>
        <el-button type="primary" @click="saveAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAddresses, addAddress, createOrder } from '../api'

const route = useRoute()
const router = useRouter()
const addresses = ref([])
const selectedAddress = ref(null)
const remark = ref('')
const submitting = ref(false)
const addressDialog = ref(false)
const addrForm = ref({ receiver: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: 0 })

// 待结算商品：购物车结算 or 立即购买
const items = ref([])

const totalAmount = computed(() =>
  items.value.reduce((sum, i) => sum + (i.price || 0) * i.quantity, 0)
)

onMounted(async () => {
  const [addrRes] = await Promise.all([getAddresses()])
  addresses.value = addrRes.data
  const def = addresses.value.find((a) => a.isDefault === 1) || addresses.value[0]
  if (def) selectedAddress.value = def.id

  // 立即购买模式：从路由参数构造商品
  if (route.query.buyNow) {
    const detail = await import('../api').then((m) => m.getProductDetail(route.query.productId))
    const p = detail.data
    items.value = [{
      productId: p.id,
      name: p.name,
      price: p.price,
      coverImage: p.coverImage,
      quantity: Number(route.query.quantity) || 1
    }]
  } else {
    const cartRes = await import('../api').then((m) => m.getCart())
    items.value = cartRes.data.map((c) => ({
      productId: c.productId,
      name: c.name,
      price: c.price,
      coverImage: c.coverImage,
      quantity: c.quantity
    }))
  }
})

const openAddressDialog = () => {
  addrForm.value = { receiver: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: 0 }
  addressDialog.value = true
}

const saveAddress = async () => {
  const f = addrForm.value
  if (!f.receiver || !f.phone || !f.detail) {
    ElMessage.warning('请填写完整的地址信息')
    return
  }
  await addAddress(f)
  const { data } = await getAddresses()
  addresses.value = data
  selectedAddress.value = data[0]?.id
  addressDialog.value = false
  ElMessage.success('地址已保存')
}

const submitOrder = async () => {
  if (!selectedAddress.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  submitting.value = true
  try {
    const fromCart = !route.query.buyNow
    const payload = {
      addressId: selectedAddress.value,
      remark: remark.value,
      fromCart
    }
    if (!fromCart) {
      payload.items = items.value.map((i) => ({ productId: i.productId, quantity: i.quantity }))
    }
    const { data } = await createOrder(payload)
    await ElMessageBox.confirm(
      `订单已创建，应付 ¥${data.totalAmount.toFixed(2)}。现在模拟支付？`,
      '支付确认',
      { confirmButtonText: '立即支付', cancelButtonText: '稍后支付', type: 'info' }
    )
    // 模拟支付
    const pay = await import('../api').then((m) => m.payOrder(data.id))
    ElMessage.success('支付成功')
    router.push(`/order/${data.id}`)
  } catch (e) {
    // 用户取消支付：停留本页；其他失败：请求拦截器已提示，也停留本页
    if (e === 'cancel') return
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.page-title { font-size: 22px; margin-bottom: 16px; }
.section { background: #fff; border-radius: 12px; padding: 20px 24px; margin-bottom: 16px; }
.section-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.section h3 { font-size: 16px; margin-bottom: 12px; }
.address-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.address-card {
  border: 1.5px solid #e5e6eb;
  border-radius: 10px;
  padding: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.address-card.active { border-color: #ff8c42; background: #fff7f0; }
.addr-receiver { font-weight: 600; margin-bottom: 6px; }
.addr-phone { color: #999; font-weight: 400; font-size: 13px; }
.addr-detail { color: #666; font-size: 13px; line-height: 1.6; margin-bottom: 8px; }
.order-item { display: flex; align-items: center; gap: 16px; padding: 10px 0; border-bottom: 1px dashed #f0f0f0; }
.order-item:last-child { border-bottom: none; }
.item-img { width: 56px; height: 56px; border-radius: 8px; }
.item-name { flex: 1; font-size: 14px; }
.item-price { color: #666; font-size: 13px; width: 160px; }
.item-amount { color: #e4393c; font-weight: 700; }
.submit-bar {
  background: #fff;
  border-radius: 12px;
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 24px;
}
.total-price { color: #e4393c; font-size: 22px; font-weight: 700; }
</style>
