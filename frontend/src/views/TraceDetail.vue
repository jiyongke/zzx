<template>
  <div v-loading="loading" class="trace-detail">
    <el-breadcrumb separator="/" class="crumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/trace' }">溯源查询</el-breadcrumb-item>
      <el-breadcrumb-item>溯源详情</el-breadcrumb-item>
    </el-breadcrumb>

    <div v-if="data" class="result-wrap">
      <div class="result-head">
        <h2>{{ data.batch.productName }}</h2>
        <el-tag type="success">溯源认证</el-tag>
        <el-tag :type="hashTagType" v-if="data.batch.status === 1">{{ hashStatusText }}</el-tag>
        <el-tag v-if="data.firstScan" type="danger" effect="dark">首次扫码提示</el-tag>
        <QrCodeBox :code="data.batch.batchCode" class="head-qr" />
      </div>

      <!-- 防伪校验提示 -->
      <el-alert
        v-if="data.batch.status === 1 && data.hashStatus === 'TAMPERED'"
        type="error" :closable="false" class="trust-tip"
        title="⚠ 哈希校验未通过：当前溯源链摘要与平台存证不一致，该批次信息疑似被篡改，请谨慎购买并联系平台核验"
      />
      <el-alert
        v-else-if="data.batch.status === 1 && data.hashStatus === 'VERIFIED'"
        type="success" :closable="false" class="trust-tip"
        :title="`SHA-256 摘要校验通过：溯源链信息与平台存证完全一致（${data.scanCount || 0} 次扫码）`"
      />
      <el-alert
        v-else-if="data.batch.status === 1 && data.hashStatus === 'NONE'"
        type="warning" :closable="false" class="trust-tip"
        title="该批次为历史数据，未进行哈希存证，请以平台登记信息为准"
      />
      <el-alert
        v-if="data.highFreqWarning"
        type="warning" :closable="false" class="trust-tip"
        title="检测到短时间内高频查询，已触发防伪预警：请警惕仿冒二维码/批量截图传播"
      />
      <el-alert
        v-else-if="data.firstScan"
        type="info" :closable="false" class="trust-tip"
        title="该溯源二维码为首次被扫码，请核对商品批次号与包装信息一致"
      />

      <el-descriptions :column="3" border class="batch-info">
        <el-descriptions-item label="溯源批次号">{{ data.batch.batchCode }}</el-descriptions-item>
        <el-descriptions-item label="产地">{{ data.batch.origin }}</el-descriptions-item>
        <el-descriptions-item label="生产主体">{{ data.batch.farmerName }}</el-descriptions-item>
        <el-descriptions-item label="种植日期">{{ fmt(data.batch.plantDate) }}</el-descriptions-item>
        <el-descriptions-item label="采收日期">{{ fmt(data.batch.harvestDate) }}</el-descriptions-item>
        <el-descriptions-item label="溯源环节">{{ data.nodes.length }} 个</el-descriptions-item>
      </el-descriptions>
      <p class="batch-desc">{{ data.batch.description }}</p>

      <h3 class="chain-title">溯源环节链</h3>
      <el-timeline class="trace-timeline">
        <el-timeline-item
          v-for="n in data.nodes"
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
    </div>
    <el-empty v-else-if="!loading" description="溯源批次不存在" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getTraceBatch } from '../api'
import QrCodeBox from '../components/QrCodeBox.vue'

const route = useRoute()
const loading = ref(false)
const data = ref(null)

const NODE_TYPES = {
  PLANT: '选种育苗', FERTILIZE: '有机施肥', PEST: '绿色防治', HARVEST: '人工采收',
  PROCESS: '加工分拣', QUALITY: '质检认证', STORAGE: '仓储保鲜', LOGISTICS: '冷链物流'
}
const typeName = (t) => NODE_TYPES[t] || (t === 'CORRECTION' ? '更正记录' : t)
const fmt = (d) => (d ? String(d).replace('T', ' ').slice(0, 16) : '')

const hashStatusText = computed(() => ({
  VERIFIED: '哈希校验通过', TAMPERED: '疑似被篡改', NONE: '未哈希存证'
}[data.value?.hashStatus] || '未校验'))

const hashTagType = computed(() => ({
  VERIFIED: 'success', TAMPERED: 'danger', NONE: 'warning'
}[data.value?.hashStatus] || 'info'))

onMounted(async () => {
  loading.value = true
  try {
    const { data: d } = await getTraceBatch(route.params.id)
    data.value = d
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.crumb { margin-bottom: 16px; }
.result-wrap { background: #fff; border-radius: 12px; padding: 24px; }
.result-head { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.result-head h2 { font-size: 20px; }
.head-qr { margin-left: auto; }
.batch-info { margin-bottom: 12px; }
.batch-desc { color: #666; font-size: 14px; line-height: 1.8; margin-bottom: 20px; }
.chain-title { margin-bottom: 16px; }
.node-card { background: #f9fafb; border-radius: 8px; padding: 12px 16px; }
.node-head { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.node-op { color: #999; font-size: 12px; margin-left: auto; }
.node-desc { color: #666; font-size: 13px; line-height: 1.7; }
.node-loc { color: #999; font-size: 12px; margin-top: 4px; }
.trust-tip { margin-bottom: 14px; }
</style>
