<template>
  <div class="auth-wrap">
    <div class="auth-card">
      <h2 class="auth-title">加入橙心助农</h2>
      <p class="auth-sub">注册即可购买可溯源的优质农产品</p>
      <el-form :model="form" :rules="rules" ref="formRef" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="设置账号（3-20位）" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="设置密码（6-20位）" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="confirm">
          <el-input v-model="form.confirm" type="password" placeholder="确认密码" :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称" :prefix-icon="UserFilled" />
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="手机号（选填）" :prefix-icon="Iphone" />
        </el-form-item>
        <el-form-item>
          <el-radio-group v-model="form.role" class="role-radio">
            <el-radio value="CONSUMER">我是消费者</el-radio>
            <el-radio value="FARMER">我是农户（需资质认证）</el-radio>
          </el-radio-group>
          <div v-if="form.role === 'FARMER'" class="role-tip">
            注册后将进入农户工作台，需在个人中心提交实名与资质认证及质量安全材料，经平台管理员审核通过后方可上架商品、发布溯源链。
          </div>
        </el-form-item>
        <el-button type="primary" size="large" class="auth-btn" :loading="loading" @click="handleRegister">
          注 册
        </el-button>
      </el-form>
      <p class="auth-link">已有账号？<router-link to="/login">去登录</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, UserFilled, Iphone } from '@element-plus/icons-vue'
import { register } from '../api'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = ref({ username: '', password: '', confirm: '', nickname: '', phone: '', role: 'CONSUMER' })

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度3-20位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, cb) =>
        value === form.value.password ? cb() : cb(new Error('两次密码不一致')),
      trigger: 'blur'
    }
  ]
}

const handleRegister = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    await register({
      username: form.value.username,
      password: form.value.password,
      nickname: form.value.nickname,
      phone: form.value.phone,
      role: form.value.role
    })
    ElMessage.success(form.value.role === 'FARMER' ? '农户账号注册成功，请登录后提交资质认证' : '注册成功，请登录')
    router.push('/login')
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
.role-radio { display: flex; }
.role-tip {
  margin-top: 8px;
  padding: 8px 10px;
  background: #fff8f0;
  border: 1px dashed #ffc8a3;
  border-radius: 6px;
  color: #a66a3d;
  font-size: 12px;
  line-height: 1.6;
}
</style>
