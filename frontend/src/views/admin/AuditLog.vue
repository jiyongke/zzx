<template>
  <div class="audit-page">
    <h2 class="page-title">操作日志</h2>
    <p class="page-desc">记录平台关键操作（谁 · 何时 · 做了什么），保障数据准确与交易可追溯。</p>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="按用户 / 操作 / 详情搜索" clearable style="width: 260px" @keyup.enter="load(1)" />
      <el-button @click="load(1)">搜索</el-button>
      <el-button @click="clearFilter">重置</el-button>
    </div>

    <el-table :data="logs" v-loading="loading" stripe>
      <el-table-column label="时间" width="170">
        <template #default="{ row }">{{ fmt(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作用户" width="140">
        <template #default="{ row }">
          {{ row.username || 'anonymous' }}
          <el-tag v-if="row.role" size="small" :type="roleType(row.role)" style="margin-left: 6px">{{ roleName(row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作类型" width="130">
        <template #default="{ row }">
          <el-tag size="small" :type="actionType(row.action)">{{ actionName(row.action) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作对象" width="130">
        <template #default="{ row }">{{ row.targetType || '-' }}<span v-if="row.targetId"> #{{ row.targetId }}</span></template>
      </el-table-column>
      <el-table-column label="详情" min-width="280" prop="detail" show-overflow-tooltip />
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAuditLogs } from '../../api'

const logs = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const loading = ref(false)

const ACTION = {
  CREATE: '新增', UPDATE: '修改', DELETE: '删除', PUBLISH: '发布', OFFLINE: '下架',
  ON_SHELF: '上架', OFF_SHELF: '下架', SHIP: '发货', REFUND_APPLY: '申请售后',
  REFUND_HANDLE: '售后处理', TRACE_REVIEW: '溯源审核', QUALIFICATION_REVIEW: '资质审核',
  DELIVERY: '物流更新', QUALIFICATION_SUBMIT: '提交资质'
}
const actionName = (a) => ACTION[a] || a
const actionType = (a) => ({
  CREATE: 'success', DELETE: 'danger', SHIP: 'primary', REFUND_HANDLE: 'warning',
  REFUND_APPLY: 'warning', PUBLISH: 'success', OFFLINE: 'info', ON_SHELF: 'success', OFF_SHELF: 'info',
  TRACE_REVIEW: 'warning', QUALIFICATION_REVIEW: 'warning', DELIVERY: 'primary', QUALIFICATION_SUBMIT: 'info'
}[a] || 'info')

const ROLE = { ADMIN: '管理员', FARMER: '农户', CONSUMER: '消费者' }
const roleName = (r) => ROLE[r] || r
const roleType = (r) => ({ ADMIN: 'danger', FARMER: 'success', CONSUMER: 'info' }[r] || 'info')

const fmt = (d) => (d ? String(d).replace('T', ' ').slice(0, 19) : '')

const load = async (p) => {
  page.value = p || page.value
  loading.value = true
  try {
    const { data } = await getAuditLogs({ page: page.value, size: size.value, keyword: keyword.value })
    logs.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const clearFilter = () => {
  keyword.value = ''
  load(1)
}

onMounted(() => load(1))
</script>

<style scoped>
.page-title { font-size: 22px; margin-bottom: 6px; }
.page-desc { color: #999; font-size: 13px; margin-bottom: 14px; }
.toolbar { margin-bottom: 12px; display: flex; gap: 8px; }
.pager { display: flex; justify-content: center; margin-top: 16px; }
</style>
