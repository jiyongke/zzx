<template>
  <div class="manage-page">
    <div class="page-head">
      <h2 class="page-title">溯源管理</h2>
      <el-button type="primary" @click="openBatchDialog()">+ 新建溯源批次</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="产品名/批次号" clearable style="width: 220px" @keyup.enter="load(1)" />
      <el-button @click="load(1)">搜索</el-button>
      <span class="tip">农户仅管理本人批次，管理员可管理全部批次；发布需先经管理员审核</span>
    </div>

    <!-- 批次列表 -->
    <el-table :data="batches" v-loading="loading" stripe>
      <el-table-column label="批次号" prop="batchCode" width="150" />
      <el-table-column label="产品名称" prop="productName" min-width="140" />
      <el-table-column label="产地" prop="origin" min-width="140" show-overflow-tooltip />
      <el-table-column label="生产主体" prop="farmerName" width="130" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="batchStatusType(row.status)" size="small">{{ batchStatusName(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="扫码" width="80" align="center">
        <template #default="{ row }">
          <span v-if="row.status === 1">{{ row.scanCount || 0 }} 次</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="290" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="openDetail(row)">管理环节</el-button>
          <el-button
            v-if="row.status === 0"
            text type="success" size="small" @click="publish(row)"
          >{{ isAdmin ? '直接发布' : '提交审核' }}</el-button>
          <template v-if="row.status === 3 && isAdmin">
            <el-button text type="success" size="small" @click="review(row, true)">审核通过</el-button>
            <el-button text type="danger" size="small" @click="review(row, false)">驳回</el-button>
          </template>
          <el-button
            v-if="row.status === 1"
            text type="warning" size="small" @click="offline(row)"
          >下架</el-button>
          <el-button text type="danger" size="small" @click="removeBatch(row)">删除</el-button>
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

    <!-- 新建批次弹窗 -->
    <el-dialog v-model="batchDialog" title="新建溯源批次" width="520px">
      <el-form :model="batchForm" label-width="90px">
        <el-form-item label="产品名称"><el-input v-model="batchForm.productName" /></el-form-item>
        <el-form-item label="批次号">
          <el-input v-model="batchForm.batchCode" placeholder="留空自动生成（TR+日期）" />
        </el-form-item>
        <el-form-item label="产地"><el-input v-model="batchForm.origin" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="batchForm.categoryId" placeholder="选择分类" clearable style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="种植日期"><el-date-picker v-model="batchForm.plantDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="采收日期"><el-date-picker v-model="batchForm.harvestDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="批次说明"><el-input v-model="batchForm.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialog = false">取消</el-button>
        <el-button type="primary" @click="saveBatch">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批次详情抽屉：环节管理 -->
    <el-drawer v-model="detailDrawer" :title="`溯源环节 · ${current?.productName || ''}`" size="560px">
      <div v-if="detail" class="drawer-body">
        <el-descriptions :column="2" border size="small" class="batch-meta">
          <el-descriptions-item label="批次号">{{ detail.batch.batchCode }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ batchStatusName(detail.batch.status) }}</el-descriptions-item>
          <el-descriptions-item label="产地" :span="2">{{ detail.batch.origin }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.batch.status === 1" label="哈希校验">
            <el-tag :type="hashTagType" size="small">{{ hashStatusText }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.batch.status === 1" label="扫码次数">{{ detail.scanCount || 0 }} 次</el-descriptions-item>
          <el-descriptions-item v-if="detail.batch.hashValue" label="摘要哈希" :span="2">
            <span class="hash-text">{{ detail.batch.hashValue }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div class="qr-row">
          <QrCodeBox :code="detail.batch.batchCode" />
        </div>

        <!-- 可信提示 -->
        <el-alert
          v-if="detail.batch.status === 0 && !isAdmin && detail.nextNodeType === 'QUALITY'"
          type="warning" :closable="false" class="trust-tip"
          title="选种到加工环节已录完，请关闭抽屉后点该批次行的「提交审核」；质检、仓储、物流由管理员审核时补录"
        />
        <el-alert
          v-else-if="detail.batch.status === 3 && isAdmin && !detail.allComplete"
          type="warning" :closable="false" class="trust-tip"
          title="农户已提交审核，请在下方按顺序补录「质检认证→仓储保鲜→冷链物流」，录全后点列表行「审核通过」"
        />
        <el-alert
          v-else-if="detail.batch.status === 3 && isAdmin && detail.allComplete"
          type="success" :closable="false" class="trust-tip"
          title="八大环节已补齐，请关闭抽屉后点该批次行的「审核通过」发布并生成 SHA-256 哈希存证"
        />
        <el-alert
          v-else-if="detail.batch.status === 3"
          type="warning" :closable="false" class="trust-tip"
          title="该批次已提交发布审核，溯源记录已锁定，等待平台管理员审核"
        />
        <el-alert
          v-else-if="detail.batch.status === 1 && !correctionMode"
          type="success" :closable="false" class="trust-tip"
          title="批次已发布：记录已锁定（存证可信），仅平台管理员可追加更正记录；消费者扫码时将自动校验 SHA-256 摘要哈希"
        />

        <div v-if="isAdmin && detail.batch.status === 1 && !correctionMode" class="correction-bar">
          <el-button type="warning" plain size="small" @click="startCorrection">+ 追加更正记录</el-button>
          <span class="tip">批次已锁定，仅可追加更正记录（全程留痕）</span>
        </div>

        <el-timeline class="node-timeline">
          <el-timeline-item
            v-for="n in detail.nodes"
            :key="n.id"
            :timestamp="fmt(n.recordTime)"
            placement="top"
            :type="n.nodeType === 'CORRECTION' ? 'warning' : 'success'"
          >
            <div class="node-card">
              <div class="node-head">
                <el-tag size="small" :type="n.nodeType === 'CORRECTION' ? 'warning' : 'success'">{{ typeName(n.nodeType) }}</el-tag>
                <b>{{ n.title }}</b>
                <el-button
                  v-if="detail.batch.status === 0"
                  text type="danger" size="small" style="margin-left: auto" @click="removeNode(n)"
                >删除</el-button>
              </div>
              <p class="node-desc">{{ n.description }}</p>
              <div class="node-recorder" v-if="n.recorderRole">
                录入：{{ recorderName(n) }}（{{ n.recorderRole === 'ADMIN' ? '平台管理员' : '农户' }}）{{ fmt(n.recorderTime) }}
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-if="detail.nodes.length === 0" description="尚未录入溯源环节" :image-size="60" />

        <!-- 环节录入 / 更正 -->
        <template v-if="canEditNode">
          <el-divider>{{ correctionMode ? '追加更正记录' : '录入溯源环节' }}</el-divider>
          <el-form :model="nodeForm" label-width="90px">
            <el-form-item label="环节类型">
              <el-select v-model="nodeForm.nodeType" disabled style="width: 100%">
                <el-option
                  :label="correctionMode ? '更正记录（CORRECTION）' : typeName(detail.nextNodeType)"
                  :value="nodeForm.nodeType"
                />
              </el-select>
              <div v-if="!correctionMode" class="next-hint">
                按业务顺序录入（选种→施肥→防治→采收→加工→质检→仓储→物流），下一步：{{ detail.nextNodeType ? typeName(detail.nextNodeType) : '八大环节已录全，请提交审核' }}
              </div>
              <div v-else class="next-hint">批次已发布，溯源记录锁定，更正记录将追加到环节链末尾并全程留痕</div>
            </el-form-item>
            <el-form-item label="标题"><el-input v-model="nodeForm.title" :placeholder="correctionMode ? '如：质检报告更新说明' : '如：有机施肥'" /></el-form-item>
            <el-form-item label="描述"><el-input v-model="nodeForm.description" type="textarea" :rows="2" /></el-form-item>
            <el-form-item label="地点"><el-input v-model="nodeForm.location" /></el-form-item>
            <el-form-item label="操作人"><el-input v-model="nodeForm.operator" :placeholder="store.user?.nickname" /></el-form-item>
            <el-form-item label="时间">
              <el-date-picker
                v-model="nodeForm.recordTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
            <el-button type="primary" style="width: 100%" @click="saveNode">
              {{ correctionMode ? '追加更正记录' : '保存环节' }}
            </el-button>
            <el-button v-if="correctionMode" text type="info" style="width: 100%; margin-top: 6px" @click="cancelCorrection">取消更正</el-button>
          </el-form>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminTraceBatches, adminTraceBatch, createBatch, publishBatch, reviewTraceBatch,
  offlineBatch, deleteBatch, addTraceNode, deleteTraceNode, getCategories
} from '../../api'
import { useUserStore } from '../../store/user'
import QrCodeBox from '../../components/QrCodeBox.vue'

const store = useUserStore()
const isAdmin = computed(() => store.role === 'ADMIN')
const batches = ref([])
const categories = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const loading = ref(false)

const batchDialog = ref(false)
const detailDrawer = ref(false)
const current = ref(null)
const detail = ref(null)
const correctionMode = ref(false)

const NODE_TYPES = {
  PLANT: '选种育苗', FERTILIZE: '有机施肥', PEST: '绿色防治', HARVEST: '人工采收',
  PROCESS: '加工分拣', QUALITY: '质检认证', STORAGE: '仓储保鲜', LOGISTICS: '冷链物流'
}
const typeName = (t) => NODE_TYPES[t] || (t === 'CORRECTION' ? '更正记录' : t)
const fmt = (d) => (d ? String(d).replace('T', ' ').slice(0, 19) : '')
const batchStatusName = (s) => ({ 0: '待完善', 1: '可溯源', 2: '已下架', 3: '待审核' }[s] || '未知')
const batchStatusType = (s) => ({ 0: 'info', 1: 'success', 2: 'info', 3: 'warning' }[s] || 'info')

const hashStatusText = computed(() => ({
  VERIFIED: '校验通过（未被篡改）', TAMPERED: '疑似被篡改！', NONE: '未存证（历史批次）'
}[detail.value?.hashStatus] || '未校验'))

const hashTagType = computed(() => ({
  VERIFIED: 'success', TAMPERED: 'danger', NONE: 'info'
}[detail.value?.hashStatus] || 'info'))

const recorderName = (n) => (n.recorderRole === 'ADMIN' ? '平台管理员' : n.operator || n.recorderId || '-')

// 质检环节须由平台管理员核验录入（避免单一主体自证）
const qualBlocked = computed(() =>
  detail.value && detail.value.batch.status === 0 && !isAdmin.value && detail.value.nextNodeType === 'QUALITY'
)

// 环节是否可录入：
// 待完善(status=0)可录入（质检对农户锁定）；待审核(status=3)管理员补录质检/仓储/物流；已发布仅更正模式
const canEditNode = computed(() => {
  if (!detail.value) return false
  const st = detail.value.batch.status
  if (st === 0) return !qualBlocked.value
  if (st === 3 && isAdmin.value) return !detail.value.allComplete
  if (isAdmin.value && correctionMode.value) return true
  return false
})

const batchForm = reactive({ productName: '', batchCode: '', origin: '', categoryId: null, plantDate: '', harvestDate: '', description: '' })
const nodeForm = reactive({ batchId: null, nodeType: '', title: '', description: '', location: '', operator: '', recordTime: '' })

const load = async (p) => {
  page.value = p || page.value
  loading.value = true
  try {
    const { data } = await adminTraceBatches({ page: page.value, size: size.value, keyword: keyword.value })
    batches.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const openBatchDialog = () => {
  Object.assign(batchForm, { productName: '', batchCode: '', origin: '', categoryId: null, plantDate: '', harvestDate: '', description: '' })
  batchDialog.value = true
}

const saveBatch = async () => {
  if (!batchForm.productName) {
    ElMessage.warning('请填写产品名称')
    return
  }
  await createBatch({ ...batchForm })
  batchDialog.value = false
  ElMessage.success('批次已创建，请按业务顺序录入八大环节')
  load()
}

const openDetail = async (row) => {
  current.value = row
  const { data } = await adminTraceBatch(row.id)
  detail.value = data
  correctionMode.value = false
  resetNodeForm(row.id)
  detailDrawer.value = true
}

const resetNodeForm = (batchId) => {
  Object.assign(nodeForm, {
    batchId,
    nodeType: correctionMode.value ? 'CORRECTION' : (detail.value?.nextNodeType || 'PLANT'),
    title: '', description: '', location: '', operator: '', recordTime: ''
  })
}

const startCorrection = () => {
  correctionMode.value = true
  Object.assign(nodeForm, { batchId: nodeForm.batchId, nodeType: 'CORRECTION', title: '', description: '', location: '', operator: '', recordTime: '' })
}

const cancelCorrection = () => {
  correctionMode.value = false
  resetNodeForm(nodeForm.batchId)
}

const saveNode = async () => {
  if (!nodeForm.title) {
    ElMessage.warning('请填写环节标题')
    return
  }
  if (correctionMode.value && !nodeForm.description) {
    ElMessage.warning('更正记录请填写更正说明')
    return
  }
  await addTraceNode({ ...nodeForm })
  ElMessage.success(correctionMode.value ? '更正记录已追加并留痕' : '环节已录入')
  const { data } = await adminTraceBatch(nodeForm.batchId)
  detail.value = data
  resetNodeForm(nodeForm.batchId)
}

const removeNode = async (n) => {
  await ElMessageBox.confirm(`删除环节「${n.title}」？删除后该批次将需重新发布。`, '提示', { type: 'warning' })
  await deleteTraceNode(n.id)
  const { data } = await adminTraceBatch(nodeForm.batchId)
  detail.value = data
}

const publish = async (row) => {
  if (isAdmin.value) {
    await ElMessageBox.confirm(`直接发布「${row.productName}」批次并计算溯源链 SHA-256 摘要哈希存证？`, '发布确认', { type: 'info' })
  } else {
    await ElMessageBox.confirm(`提交「${row.productName}」的溯源链发布申请？管理员审核通过后消费者方可扫码查询。`, '提交审核', { type: 'info' })
  }
  await publishBatch(row.id)
  ElMessage.success(isAdmin.value ? '已发布，可溯源（已哈希存证）' : '已提交审核，等待平台管理员审核')
  load()
}

const review = async (row, approve) => {
  if (approve) {
    await ElMessageBox.confirm(`审核通过「${row.productName}」的溯源链发布申请？通过后将计算摘要哈希存证并对外可查。`, '审核确认', { type: 'info' })
    await reviewTraceBatch(row.id, true, '')
  } else {
    const { value } = await ElMessageBox.prompt('请填写驳回原因', '驳回发布申请', {
      confirmButtonText: '确定驳回',
      cancelButtonText: '取消',
      inputPattern: /.+/,
      inputErrorMessage: '驳回原因不能为空'
    })
    await reviewTraceBatch(row.id, false, value)
  }
  ElMessage.success(approve ? '已审核通过并发布' : '已驳回发布申请')
  load()
}

const offline = async (row) => {
  await ElMessageBox.confirm('下架后消费者将无法查询该批次溯源信息，确定？', '提示', { type: 'warning' })
  await offlineBatch(row.id)
  ElMessage.success('已下架')
  load()
}

const removeBatch = async (row) => {
  await ElMessageBox.confirm(`确定删除批次「${row.productName}」及其全部溯源环节？已发布批次农户不可删除。`, '警告', { type: 'warning' })
  await deleteBatch(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  const { data } = await getCategories()
  categories.value = data
  load(1)
})
</script>

<style scoped>
.page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { font-size: 22px; }
.toolbar { margin-bottom: 12px; display: flex; gap: 8px; align-items: center; }
.tip { color: #999; font-size: 12px; }
.pager { display: flex; justify-content: center; margin-top: 16px; }
.batch-meta { margin-bottom: 16px; }
.qr-row { display: flex; justify-content: center; margin-bottom: 16px; }
.node-timeline { margin-top: 8px; }
.node-card { background: #f9fafb; border-radius: 8px; padding: 10px 14px; }
.node-head { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.node-desc { color: #666; font-size: 13px; line-height: 1.7; }
.node-recorder { color: #999; font-size: 12px; margin-top: 6px; }
.hash-text { font-family: monospace; font-size: 11px; color: #666; word-break: break-all; }
.trust-tip { margin-bottom: 14px; }
.correction-bar { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.next-hint { color: #999; font-size: 12px; line-height: 1.6; margin-top: 4px; }
</style>
