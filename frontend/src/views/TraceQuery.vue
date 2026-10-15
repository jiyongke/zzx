<template>
  <div class="trace-page">
    <div class="trace-hero">
      <h1>农产品溯源查询</h1>
      <p>输入溯源批次号，或扫描商品外包装上的溯源二维码，即可查询该批次农产品从种植到物流的全链路信息。</p>
      <div class="search-row">
        <el-input
          v-model="code"
          size="large"
          placeholder="请输入溯源批次号，如 TR20260901001"
          clearable
          style="width: 420px"
          @keyup.enter="handleQuery"
        >
          <template #append>
            <el-button type="primary" :icon="Search" @click="handleQuery">查 询</el-button>
          </template>
        </el-input>
      </div>
    </div>

    <!-- 查询结果 -->
    <div v-if="result" class="result-wrap">
      <div class="result-head">
        <h2>{{ result.batch.productName }}</h2>
        <el-tag type="success">溯源认证</el-tag>
        <el-tag :type="hashTagType" v-if="result.batch.status === 1">{{ hashStatusText }}</el-tag>
        <el-tag v-if="result.firstScan" type="danger" effect="dark">首次扫码提示</el-tag>
        <QrCodeBox :code="result.batch.batchCode" class="head-qr" />
      </div>

      <!-- 防伪校验提示 -->
      <el-alert
        v-if="result.batch.status === 1 && result.hashStatus === 'TAMPERED'"
        type="error" :closable="false" class="trust-tip"
        title="⚠ 哈希校验未通过：当前溯源链摘要与平台存证不一致，该批次信息疑似被篡改，请谨慎购买并联系平台核验"
      />
      <el-alert
        v-else-if="result.batch.status === 1 && result.hashStatus === 'VERIFIED'"
        type="success" :closable="false" class="trust-tip"
        :title="`SHA-256 摘要校验通过：溯源链信息与平台存证完全一致（该批次已被扫码 ${result.scanCount || 0} 次）`"
      />
      <el-alert
        v-else-if="result.batch.status === 1 && result.hashStatus === 'NONE'"
        type="warning" :closable="false" class="trust-tip"
        title="该批次为历史数据，未进行哈希存证，请以平台登记信息为准"
      />
      <el-alert
        v-if="result.highFreqWarning"
        type="warning" :closable="false" class="trust-tip"
        title="检测到短时间内高频查询，已触发防伪预警：请警惕仿冒二维码/批量截图传播"
      />
      <el-alert
        v-else-if="result.firstScan"
        type="info" :closable="false" class="trust-tip"
        title="该溯源二维码为首次被扫码，请核对商品批次号与包装信息一致"
      />

      <el-descriptions :column="3" border class="batch-info">
        <el-descriptions-item label="溯源批次号">{{ result.batch.batchCode }}</el-descriptions-item>
        <el-descriptions-item label="产地">{{ result.batch.origin }}</el-descriptions-item>
        <el-descriptions-item label="生产主体">{{ result.batch.farmerName }}</el-descriptions-item>
        <el-descriptions-item label="种植日期">{{ fmt(result.batch.plantDate) }}</el-descriptions-item>
        <el-descriptions-item label="采收日期">{{ fmt(result.batch.harvestDate) }}</el-descriptions-item>
        <el-descriptions-item label="溯源环节">{{ result.nodes.length }} 个</el-descriptions-item>
      </el-descriptions>
      <p class="batch-desc">{{ result.batch.description }}</p>

      <h3 class="chain-title">溯源环节链</h3>
      <el-timeline class="trace-timeline">
        <el-timeline-item
          v-for="n in result.nodes"
          :key="n.id"
          :timestamp="fmt(n.recordTime)"
          placement="top"
          :type="n.nodeType === 'CORRECTION' ? 'warning' : 'success'"
        >
          <div class="node-card">
            <div class="node-head">
              <el-tag size="small" :type="n.nodeType === 'CORRECTION' ? 'warning' : 'success'">{{ typeName(n.nodeType) }}</el-tag>
              <b>{{ n.title }}</b>
              <span class="node-op">{{ n.operator }}</span>
            </div>
            <p class="node-desc">{{ n.description }}</p>
            <div v-if="n.location" class="node-loc">📍 {{ n.location }}</div>
            <div v-if="n.recorderRole" class="node-loc">录入：{{ n.recorderRole === 'ADMIN' ? '平台管理员' : (n.operator || n.recorderId) }} · {{ fmt(n.recorderTime) }}</div>
          </div>
        </el-timeline-item>
      </el-timeline>

      <div class="query-again">
        <el-button @click="reset">再次查询</el-button>
      </div>
    </div>

    <!-- 快捷查询示例 -->
    <div v-else class="example-wrap">
      <h3>可查询的溯源批次</h3>
      <div class="example-list">
        <div v-for="b in batches" :key="b.id" class="example-item" @click="queryByBatch(b)">
          <span class="ex-name">{{ b.productName }}</span>
          <span class="ex-code">{{ b.batchCode }}</span>
          <el-tag size="small" type="success">可溯源</el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { queryTrace, getTraceBatches } from '../api'
import QrCodeBox from '../components/QrCodeBox.vue'

const route = useRoute()
const code = ref('')
const result = ref(null)
const batches = ref([])

const NODE_TYPES = {
  PLANT: '选种育苗', FERTILIZE: '有机施肥', PEST: '绿色防治', HARVEST: '人工采收',
  PROCESS: '加工分拣', QUALITY: '质检认证', STORAGE: '仓储保鲜', LOGISTICS: '冷链物流'
}
const typeName = (t) => NODE_TYPES[t] || (t === 'CORRECTION' ? '更正记录' : t)
const fmt = (d) => (d ? String(d).replace('T', ' ').slice(0, 16) : '')

const hashStatusText = computed(() => ({
  VERIFIED: '哈希校验通过', TAMPERED: '疑似被篡改', NONE: '未哈希存证'
}[result.value?.hashStatus] || '未校验'))

const hashTagType = computed(() => ({
  VERIFIED: 'success', TAMPERED: 'danger', NONE: 'warning'
}[result.value?.hashStatus] || 'info'))

const handleQuery = async () => {
  if (!code.value.trim()) {
    ElMessage.warning('请输入溯源批次号')
    return
  }
  const { data } = await queryTrace(code.value.trim())
  result.value = data
}

const queryByBatch = (b) => {
  code.value = b.batchCode
  handleQuery()
}

const reset = () => {
  result.value = null
  code.value = ''
}

onMounted(async () => {
  const { data } = await getTraceBatches()
  batches.value = data
  // 支持扫码/链接直达：/trace?code=批次号 自动查询
  if (route.query.code) {
    code.value = String(route.query.code)
    handleQuery()
  }
})
</script>

<style scoped>
.trace-hero {
  background: linear-gradient(120deg, #eafaf0, #d8f3e3);
  border-radius: 16px;
  padding: 48px 40px;
  text-align: center;
}
.trace-hero h1 { font-size: 30px; color: #2e8b57; margin-bottom: 12px; }
.trace-hero p { color: #5a8a70; margin-bottom: 24px; font-size: 14px; }
.search-row { display: flex; justify-content: center; }

.result-wrap { margin-top: 24px; background: #fff; border-radius: 12px; padding: 24px; }
.result-head { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.result-head h2 { font-size: 20px; }
.head-qr { margin-left: auto; }
.batch-info { margin-bottom: 12px; }
.batch-desc { color: #666; font-size: 14px; line-height: 1.8; margin-bottom: 20px; }
.chain-title { margin-bottom: 16px; font-size: 16px; }
.node-card { background: #f9fafb; border-radius: 8px; padding: 12px 16px; }
.node-head { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.node-op { color: #999; font-size: 12px; margin-left: auto; }
.node-desc { color: #666; font-size: 13px; line-height: 1.7; }
.node-loc { color: #999; font-size: 12px; margin-top: 4px; }
.trust-tip { margin-bottom: 14px; }
.query-again { text-align: center; margin-top: 20px; }

.example-wrap { margin-top: 24px; background: #fff; border-radius: 12px; padding: 24px; }
.example-wrap h3 { margin-bottom: 16px; }
.example-list { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.example-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid #eee;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.example-item:hover { border-color: #2e8b57; background: #f4fbf7; }
.ex-name { flex: 1; font-size: 14px; }
.ex-code { color: #999; font-size: 12px; font-family: monospace; }
</style>
