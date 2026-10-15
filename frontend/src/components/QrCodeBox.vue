<template>
  <div class="qr-box">
    <canvas ref="canvasRef" class="qr-canvas" />
    <div class="qr-code">{{ code }}</div>
    <el-button text type="primary" size="small" @click="download">下载二维码（打印贴包装）</el-button>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import QRCode from 'qrcode'

const props = defineProps({
  code: { type: String, required: true },
  size: { type: Number, default: 150 }
})

const canvasRef = ref(null)

const qrText = () => `${location.origin}/trace?code=${encodeURIComponent(props.code)}`

const render = async () => {
  if (!canvasRef.value || !props.code) return
  await QRCode.toCanvas(canvasRef.value, qrText(), {
    width: props.size,
    margin: 1,
    color: { dark: '#2e5c3a', light: '#ffffff' }
  })
}

const download = () => {
  if (!canvasRef.value) return
  const a = document.createElement('a')
  a.href = canvasRef.value.toDataURL('image/png')
  a.download = `溯源二维码_${props.code}.png`
  a.click()
}

onMounted(render)
watch(() => props.code, render)
</script>

<style scoped>
.qr-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  background: #fff;
  border: 1px dashed #bcd9c6;
  border-radius: 10px;
  padding: 12px;
}
.qr-canvas { width: 150px; height: 150px; }
.qr-code {
  font-family: monospace;
  font-size: 12px;
  color: #4c7a5a;
  background: #f2f9f2;
  padding: 2px 10px;
  border-radius: 6px;
}
</style>
