<template>
  <div class="auth-wrap">
    <div class="auth-card">
      <h2 class="auth-title">欢迎回来</h2>
      <p class="auth-sub">登录橙心助农溯源电商平台</p>
      <el-form :model="form" :rules="rules" ref="formRef" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-button type="primary" size="large" class="auth-btn" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>
      <p class="auth-link">还没有账号？<router-link to="/register">立即注册</router-link></p>
      <el-divider>演示账号</el-divider>
      <div class="demo-accounts">
        <el-tag size="small" class="demo-tag" @click="fill('admin', '123456')">管理员 admin</el-tag>
        <el-tag size="small" class="demo-tag" type="success" @click="fill('farmer01', '123456')">农户 farmer01</el-tag>
        <el-tag size="small" class="demo-tag" type="info" @click="fill('consumer01', '123456')">消费者 consumer01</el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '../api'
import { useUserStore } from '../store/user'

const store = useUserStore()
const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = ref({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const fill = (username, password) => {
  form.value = { username, password }
}

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const { data } = await login(form.value)
    store.setLogin(data.token, data.user)
    ElMessage.success(`欢迎，${data.user.nickname}`)
    // 登录后进入角色专属工作台（游客手动访问的页面除外）
    router.push(route.query.redirect || '/workspace')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-wrap {
  display: flex;
  justify-content: center;
  padding: 60px 16px;
}
.auth-card {
  width: 400px;
  background: #fff;
  border-radius: 12px;
  padding: 40px 36px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}
.auth-title { font-size: 24px; text-align: center; color: #ff8c42; }
.auth-sub { text-align: center; color: #999; margin: 6px 0 24px; font-size: 13px; }
.auth-btn { width: 100%; }
.auth-link { text-align: center; margin-top: 16px; font-size: 14px; color: #666; }
.auth-link a { color: #ff8c42; text-decoration: none; }
.demo-accounts { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
.demo-tag { cursor: pointer; }
</style>
