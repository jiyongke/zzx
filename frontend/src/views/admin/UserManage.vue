<template>
  <div class="manage-page">
    <el-tabs v-model="activeTab" class="user-tabs">
      <!-- ================= 用户管理 ================= -->
      <el-tab-pane label="用户管理" name="users">
    <div class="page-head">
      <h2 class="page-title">用户管理</h2>
      <el-button type="primary" @click="openDialog()">+ 新增用户</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="账号/昵称" clearable style="width: 200px" @keyup.enter="load(1)" />
      <el-select v-model="roleFilter" style="width: 130px" @change="load(1)">
        <el-option label="全部角色" value="" />
        <el-option label="管理员" value="ADMIN" />
        <el-option label="农户" value="FARMER" />
        <el-option label="消费者" value="CONSUMER" />
      </el-select>
      <el-button @click="load(1)">搜索</el-button>
    </div>

    <el-table :data="users" v-loading="loading" stripe>
      <el-table-column label="ID" prop="id" width="60" />
      <el-table-column label="账号" prop="username" width="130" />
      <el-table-column label="昵称" prop="nickname" width="130" />
      <el-table-column label="手机号" prop="phone" width="130" />
      <el-table-column label="角色" width="130">
        <template #default="{ row }">
          <el-select
            :model-value="row.role"
            size="small"
            @change="(v) => changeRole(row, v)"
          >
            <el-option label="管理员" value="ADMIN" />
            <el-option label="农户" value="FARMER" />
            <el-option label="消费者" value="CONSUMER" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="150">
        <template #default="{ row }">{{ fmt(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            :disabled="row.id === store.user?.id"
            @change="(v) => toggleStatus(row, v)"
          />
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next, total"
        @current-change="load"
      />
    </div>

    <el-dialog v-model="dialog" title="新增用户" width="440px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="账号"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password /></el-form-item>
        <el-form-item label="昵称"><el-input v-model="form.nickname" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="农户" value="FARMER" />
            <el-option label="消费者" value="CONSUMER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="createUser">保存</el-button>
      </template>
    </el-dialog>
      </el-tab-pane>

      <!-- ================= 资质审核（准入可信） ================= -->
      <el-tab-pane label="农户资质审核" name="qualification">
        <div class="page-head">
          <h2 class="page-title">农户资质审核</h2>
        </div>
        <div class="toolbar">
          <el-select v-model="qualStatus" style="width: 150px" @change="loadQual(1)">
            <el-option label="待审核" :value="1" />
            <el-option label="已通过" :value="2" />
            <el-option label="已驳回" :value="3" />
            <el-option label="未提交" :value="0" />
            <el-option label="全部" value="" />
          </el-select>
          <el-input v-model="qualKeyword" placeholder="账号/实名/昵称" clearable style="width: 200px" @keyup.enter="loadQual(1)" />
          <el-button @click="loadQual(1)">搜索</el-button>
          <span class="tip">审核农户实名、资质证照与质量安全材料，通过后农户方可上架商品、发布溯源链（准入可信）</span>
        </div>

        <el-table :data="quals" v-loading="qualLoading" stripe>
          <el-table-column label="ID" prop="id" width="60" />
          <el-table-column label="账号" prop="username" width="120" />
          <el-table-column label="昵称" prop="nickname" width="120" />
          <el-table-column label="实名" prop="realName" width="100" />
          <el-table-column label="身份证号" prop="idCard" min-width="150" show-overflow-tooltip />
          <el-table-column label="资质证照" min-width="90">
            <template #default="{ row }">
              <el-link v-if="row.qualification" type="primary" :href="row.qualification" target="_blank">查看</el-link>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="质量安全材料" min-width="100">
            <template #default="{ row }">
              <el-link v-if="row.qualityMaterial" type="primary" :href="row.qualityMaterial" target="_blank">查看</el-link>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="审核状态" width="90">
            <template #default="{ row }">
              <el-tag :type="qualStatusType(row.auditStatus)" size="small">{{ qualStatusName(row.auditStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="审核意见" min-width="130" show-overflow-tooltip>
            <template #default="{ row }">{{ row.auditRemark || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <template v-if="row.auditStatus === 1">
                <el-button type="success" size="small" @click="handleReview(row, true)">通过</el-button>
                <el-button type="danger" size="small" @click="handleReview(row, false)">驳回</el-button>
              </template>
              <span v-else class="tip">已处理</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="pager">
          <el-pagination
            v-model:current-page="qualPage"
            :page-size="qualSize"
            :total="qualTotal"
            layout="prev, pager, next, total"
            @current-change="loadQual"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminUsers, updateUserStatus, updateUserRole, adminCreateUser, adminQualifications, reviewQualification } from '../../api'
import { useUserStore } from '../../store/user'

const store = useUserStore()
const activeTab = ref('users')
const users = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const roleFilter = ref('')
const loading = ref(false)
const dialog = ref(false)
const form = reactive({ username: '', password: '', nickname: '', phone: '', role: 'CONSUMER' })

// ---- 资质审核 ----
const quals = ref([])
const qualTotal = ref(0)
const qualPage = ref(1)
const qualSize = ref(10)
const qualStatus = ref(1)
const qualKeyword = ref('')
const qualLoading = ref(false)

const fmt = (d) => (d ? String(d).replace('T', ' ').slice(0, 19) : '')
const qualStatusName = (s) => ({ 0: '未提交', 1: '待审核', 2: '已通过', 3: '已驳回' }[s] || '未知')
const qualStatusType = (s) => ({ 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }[s] || 'info')

const load = async (p) => {
  page.value = p || page.value
  loading.value = true
  try {
    const { data } = await adminUsers({ page: page.value, size: size.value, keyword: keyword.value, role: roleFilter.value })
    users.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const loadQual = async (p) => {
  qualPage.value = p || qualPage.value
  qualLoading.value = true
  try {
    const { data } = await adminQualifications({ page: qualPage.value, size: qualSize.value, status: qualStatus.value, keyword: qualKeyword.value })
    quals.value = data.records
    qualTotal.value = data.total
  } finally {
    qualLoading.value = false
  }
}

const handleReview = async (row, approve) => {
  let remark = ''
  if (approve) {
    await ElMessageBox.confirm(`确认通过农户「${row.nickname || row.username}」的资质认证？通过后其可上架商品并发布溯源链。`, '审核确认', { type: 'info' })
  } else {
    const { value } = await ElMessageBox.prompt('请填写驳回原因（将展示给农户）', '驳回资质', {
      confirmButtonText: '确定驳回',
      cancelButtonText: '取消',
      inputPattern: /.+/,
      inputErrorMessage: '驳回原因不能为空'
    })
    remark = value
  }
  await reviewQualification(row.id, approve, remark)
  ElMessage.success(approve ? '已通过资质审核' : '已驳回资质申请')
  loadQual(qualPage.value)
}

const toggleStatus = async (row, v) => {
  await updateUserStatus(row.id, v ? 1 : 0)
  ElMessage.success(v ? '已启用' : '已禁用')
  load()
}

const changeRole = async (row, v) => {
  if (row.id === store.user?.id) {
    ElMessage.warning('不能修改自己的角色')
    load()
    return
  }
  await updateUserRole(row.id, v)
  ElMessage.success('角色已更新')
  load()
}

const openDialog = () => {
  Object.assign(form, { username: '', password: '', nickname: '', phone: '', role: 'CONSUMER' })
  dialog.value = true
}

const createUser = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写账号和密码')
    return
  }
  await adminCreateUser({ ...form }, form.role)
  dialog.value = false
  ElMessage.success('用户创建成功')
  load()
}

onMounted(() => {
  load(1)
  loadQual(1)
})
</script>

<style scoped>
.page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { font-size: 22px; }
.toolbar { margin-bottom: 12px; display: flex; gap: 8px; }
.tip { color: #999; font-size: 12px; }
.pager { display: flex; justify-content: center; margin-top: 16px; }
.user-tabs { background: #fff; border-radius: 12px; padding: 16px 20px; }
</style>
