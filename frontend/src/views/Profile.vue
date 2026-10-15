<template>
  <div class="profile-page">
    <h2 class="page-title">个人中心</h2>

    <!-- 农户资质认证（准入可信） -->
    <div v-if="store.role === 'FARMER'" class="section qual-section">
      <div class="section-head">
        <h3>农户资质认证</h3>
        <el-tag :type="qualTagType" size="small">{{ qualStatusName }}</el-tag>
      </div>
      <el-alert
        v-if="qual.auditStatus === 3" type="error" :closable="false" class="qual-alert"
        :title="'资质审核未通过：' + (qual.auditRemark || '请补充材料后重新提交')"
      />
      <el-alert
        v-else-if="qual.auditStatus === 1" type="warning" :closable="false" class="qual-alert"
        title="资质材料已提交，等待平台管理员审核。审核通过后方可上架商品、发布溯源链。"
      />
      <el-alert
        v-else-if="qual.auditStatus === 2" type="success" :closable="false" class="qual-alert"
        title="资质审核已通过，您可以上架商品并发布溯源链。"
      />
      <el-alert
        v-else type="info" :closable="false" class="qual-alert"
        title="请提交实名信息与资质证照、质量安全材料，经平台审核通过后方可开展产销业务（准入可信）。"
      />
      <el-form :model="qual" label-width="110px" style="max-width: 680px" class="qual-form">
        <el-form-item label="真实姓名">
          <el-input v-model="qual.realName" placeholder="与身份证一致的姓名" :disabled="qual.auditStatus === 1 || qual.auditStatus === 2" />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="qual.idCard" placeholder="18位身份证号" :disabled="qual.auditStatus === 1 || qual.auditStatus === 2" />
        </el-form-item>
        <el-form-item label="资质证照URL">
          <el-input v-model="qual.qualification" placeholder="营业执照/食品生产许可证等图片URL" :disabled="qual.auditStatus === 1 || qual.auditStatus === 2" />
        </el-form-item>
        <el-form-item label="质量安全材料">
          <el-input v-model="qual.qualityMaterial" type="textarea" :rows="2" placeholder="检测报告、产地证明等图片URL（多个用逗号分隔）" :disabled="qual.auditStatus === 1 || qual.auditStatus === 2" />
        </el-form-item>
        <el-button v-if="qual.auditStatus !== 1" type="primary" @click="handleSubmitQualification">
          {{ qual.auditStatus === 2 ? '更新资质材料（重新审核）' : '提交资质认证' }}
        </el-button>
        <span v-if="qual.auditTime" class="qual-time">最近审核时间：{{ fmt(qual.auditTime) }}</span>
      </el-form>
    </div>

    <div class="profile-grid">
      <!-- 个人信息 -->
      <div class="section">
        <h3>个人信息</h3>
        <el-form label-width="70px" style="max-width: 420px">
          <el-form-item label="账号"><el-input :model-value="store.user?.username" disabled /></el-form-item>
          <el-form-item label="昵称"><el-input v-model="profile.nickname" /></el-form-item>
          <el-form-item label="手机号"><el-input v-model="profile.phone" /></el-form-item>
          <el-button type="primary" @click="saveProfile">保存资料</el-button>
        </el-form>

        <el-divider>修改密码</el-divider>
        <el-form label-width="70px" style="max-width: 420px">
          <el-form-item label="原密码"><el-input v-model="pwd.oldPassword" type="password" show-password /></el-form-item>
          <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password" show-password /></el-form-item>
          <el-button type="warning" @click="savePassword">修改密码</el-button>
        </el-form>
      </div>

      <!-- 收货地址 -->
      <div class="section">
        <div class="section-head">
          <h3>收货地址</h3>
          <el-button text type="primary" @click="openDialog()">+ 新增</el-button>
        </div>
        <div v-for="a in addresses" :key="a.id" class="address-item">
          <div class="addr-main">
            <div class="addr-receiver">{{ a.receiver }} <span class="addr-phone">{{ a.phone }}</span></div>
            <div class="addr-detail">{{ a.province }}{{ a.city }}{{ a.district }}{{ a.detail }}</div>
          </div>
          <div class="addr-ops">
            <el-tag v-if="a.isDefault === 1" size="small" type="success">默认</el-tag>
            <el-button text type="primary" size="small" @click="openDialog(a)">编辑</el-button>
            <el-button text type="danger" size="small" @click="removeAddress(a)">删除</el-button>
          </div>
        </div>
        <el-empty v-if="addresses.length === 0" description="暂无地址" :image-size="60" />
      </div>
    </div>

    <!-- 地址弹窗 -->
    <el-dialog v-model="dialog" :title="editingId ? '编辑地址' : '新增地址'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="收货人"><el-input v-model="form.receiver" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="省市区">
          <div class="region-row">
            <el-input v-model="form.province" placeholder="省" />
            <el-input v-model="form.city" placeholder="市" />
            <el-input v-model="form.district" placeholder="区/县" />
          </div>
        </el-form-item>
        <el-form-item label="详细地址"><el-input v-model="form.detail" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="设为默认"><el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="saveAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getInfo, updateProfile, changePassword, getAddresses, addAddress, updateAddress, deleteAddress, getQualification, submitQualification } from '../api'
import { useUserStore } from '../store/user'

const store = useUserStore()
const addresses = ref([])
const dialog = ref(false)
const editingId = ref(null)
const profile = reactive({ nickname: '', phone: '' })
const pwd = reactive({ oldPassword: '', newPassword: '' })
const form = reactive({ receiver: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: 0 })

// ---- 农户资质认证 ----
const qual = reactive({ realName: '', idCard: '', qualification: '', qualityMaterial: '', auditStatus: null, auditRemark: '', auditTime: null })

const qualStatusName = computed(() => ({
  0: '未提交', 1: '待审核', 2: '已通过', 3: '已驳回'
}[qual.auditStatus] || '未提交'))

const qualTagType = computed(() => ({
  0: 'info', 1: 'warning', 2: 'success', 3: 'danger'
}[qual.auditStatus] || 'info'))

const fmt = (d) => (d ? String(d).replace('T', ' ').slice(0, 19) : '')

const handleSubmitQualification = async () => {
  if (!qual.realName || !qual.idCard || !qual.qualification || !qual.qualityMaterial) {
    ElMessage.warning('请完整填写真实姓名、身份证号、资质证照与质量安全材料')
    return
  }
  await submitQualification({ ...qual })
  qual.auditStatus = 1
  ElMessage.success('资质认证已提交，等待平台管理员审核')
}

onMounted(async () => {
  const tasks = [getInfo(), getAddresses()]
  if (store.role === 'FARMER') tasks.push(getQualification())
  const [infoRes, addrRes, qualRes] = await Promise.all(tasks)
  profile.nickname = infoRes.data.nickname
  profile.phone = infoRes.data.phone
  addresses.value = addrRes.data
  if (qualRes) {
    Object.assign(qual, {
      realName: qualRes.data.realName || '',
      idCard: qualRes.data.idCard || '',
      qualification: qualRes.data.qualification || '',
      qualityMaterial: qualRes.data.qualityMaterial || '',
      auditStatus: qualRes.data.auditStatus,
      auditRemark: qualRes.data.auditRemark || '',
      auditTime: qualRes.data.auditTime
    })
  }
})

const saveProfile = async () => {
  await updateProfile({ nickname: profile.nickname, phone: profile.phone })
  const { data } = await getInfo()
  store.setUser(data)
  ElMessage.success('资料已保存')
}

const savePassword = async () => {
  if (!pwd.oldPassword || !pwd.newPassword) {
    ElMessage.warning('请填写完整')
    return
  }
  await changePassword({ ...pwd })
  pwd.oldPassword = ''
  pwd.newPassword = ''
  ElMessage.success('密码修改成功')
}

const openDialog = (addr) => {
  editingId.value = addr?.id || null
  Object.assign(form, addr || { receiver: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: 0 })
  dialog.value = true
}

const saveAddress = async () => {
  if (!form.receiver || !form.phone || !form.detail) {
    ElMessage.warning('请填写完整地址')
    return
  }
  if (editingId.value) {
    await updateAddress(editingId.value, { ...form })
  } else {
    await addAddress({ ...form })
  }
  const { data } = await getAddresses()
  addresses.value = data
  dialog.value = false
  ElMessage.success('地址已保存')
}

const removeAddress = async (a) => {
  await ElMessageBox.confirm('确定删除该地址吗？', '提示', { type: 'warning' })
  await deleteAddress(a.id)
  addresses.value = addresses.value.filter((x) => x.id !== a.id)
}
</script>

<style scoped>
.page-title { font-size: 22px; margin-bottom: 16px; }
.profile-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; align-items: start; }
.section { background: #fff; border-radius: 12px; padding: 20px 24px; }
.section-head { display: flex; justify-content: space-between; align-items: center; }
.section h3 { font-size: 16px; margin-bottom: 16px; }
.address-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px dashed #f0f0f0;
}
.address-item:last-child { border-bottom: none; }
.addr-receiver { font-weight: 600; margin-bottom: 4px; }
.addr-phone { color: #999; font-weight: 400; font-size: 12px; }
.addr-detail { color: #666; font-size: 13px; }
.addr-ops { display: flex; align-items: center; gap: 8px; }
.region-row { display: flex; gap: 8px; width: 100%; }
.qual-section { margin-bottom: 16px; }
.qual-alert { margin-bottom: 14px; }
.qual-form { margin-top: 4px; }
.qual-time { margin-left: 12px; color: #999; font-size: 12px; }
</style>
