<template>
  <div class="app">
    <!-- 顶部导航 -->
    <header class="navbar">
      <div class="nav-inner">
        <router-link to="/" class="logo">
          <el-icon :size="24" color="#ff8c42"><Orange /></el-icon>
          <span class="logo-text">橙心助农</span>
          <span class="logo-sub">农产品溯源电商平台</span>
        </router-link>

        <nav class="nav-menu">
          <!-- 未登录：公共商城导航 -->
          <template v-if="!store.isLogin">
            <router-link to="/" class="nav-item">首页</router-link>
            <router-link to="/products" class="nav-item">全部商品</router-link>
            <router-link to="/cart" class="nav-item" @click="handleCartNav">
              <el-badge :value="cartCount" :hidden="cartCount === 0" :max="99">
                <span class="nav-cart"><el-icon><ShoppingCart /></el-icon> 购物车</span>
              </el-badge>
            </router-link>
            <router-link to="/trace" class="nav-item">
              <el-icon><Search /></el-icon> 溯源查询
            </router-link>
          </template>

          <!-- 已登录：按角色渲染专属菜单 -->
          <template v-else>
            <router-link
              v-for="m in roleMenus[store.role] || []"
              :key="m.path"
              :to="m.path"
              class="nav-item"
            >{{ m.label }}</router-link>
          </template>
        </nav>

        <div class="nav-right">
          <!-- 管理端快捷入口（按角色） -->
          <template v-if="store.isLogin">
            <el-dropdown trigger="click">
              <span class="user-entry">
                <el-avatar :size="30" :src="store.user?.avatar || ''">
                  {{ (store.user?.nickname || 'U').charAt(0) }}
                </el-avatar>
                <span class="user-name">{{ store.user?.nickname }}</span>
                <el-tag size="small" :type="roleTagType" class="role-tag">{{ roleName }}</el-tag>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="$router.push('/workspace')">我的工作台</el-dropdown-item>
                  <el-dropdown-item @click="$router.push('/profile')">个人中心</el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text @click="$router.push('/login')">登录</el-button>
            <el-button type="primary" round @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <!-- 页面主体 -->
    <main class="main">
      <router-view />
    </main>

    <!-- 底部 -->
    <footer class="footer">
      <p>橙心助农 · 农产品溯源电商平台 | 让每一份农产品都有来处</p>
      <p class="footer-sub">Spring Boot + Vue 全栈实训项目</p>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ShoppingCart } from '@element-plus/icons-vue'
import { getCart } from './api'
import { useUserStore } from './store/user'

const store = useUserStore()
const router = useRouter()
const route = useRoute()
const cartCount = ref(0)

const roleName = computed(() => ({
  ADMIN: '管理员', FARMER: '农户', CONSUMER: '消费者'
}[store.role] || '消费者'))

const roleTagType = computed(() => ({
  ADMIN: 'danger', FARMER: 'success', CONSUMER: 'info'
}[store.role] || 'info'))

// 各角色专属导航菜单
const roleMenus = {
  ADMIN: [
    { path: '/workspace', label: '工作台' },
    { path: '/admin/dashboard', label: '数据看板' },
    { path: '/admin/report', label: '数据报表' },
    { path: '/admin/products', label: '商品与分类' },
    { path: '/admin/orders', label: '订单管理' },
    { path: '/admin/users', label: '用户管理' },
    { path: '/admin/trace', label: '溯源监管' },
    { path: '/admin/audit', label: '操作日志' }
  ],
  FARMER: [
    { path: '/workspace', label: '我的工作台' },
    { path: '/admin/trace', label: '溯源管理' },
    { path: '/profile', label: '资质认证' },
    { path: '/products', label: '商城浏览' },
    { path: '/trace', label: '溯源查询' }
  ],
  CONSUMER: [
    { path: '/workspace', label: '工作台' },
    { path: '/products', label: '全部商品' },
    { path: '/cart', label: '购物车' },
    { path: '/orders', label: '我的订单' },
    { path: '/trace', label: '溯源查询' }
  ]
}

// 登录后刷新购物车数量
const refreshCart = async () => {
  if (!store.isLogin) {
    cartCount.value = 0
    return
  }
  try {
    const { data } = await getCart()
    cartCount.value = data.reduce((sum, i) => sum + i.quantity, 0)
  } catch (e) {
    cartCount.value = 0
  }
}

// 未登录点击购物车时引导登录
const handleCartNav = (e) => {
  if (!store.isLogin) {
    e.preventDefault()
    ElMessage.warning('请先登录查看购物车')
    router.push('/login')
  }
}

watch(() => store.isLogin, refreshCart)

// 路由切换时同步购物车数量（加购后徽标实时更新）
watch(() => route.fullPath, refreshCart)

const handleLogout = async () => {
  await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
  store.logout()
  cartCount.value = 0
  router.push('/')
}

onMounted(refreshCart)
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif; background: #f7f8fa; color: #333; }

.navbar {
  background: #fff;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}
.nav-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 0 16px;
}
.logo { display: flex; align-items: center; gap: 6px; text-decoration: none; }
.logo-text { font-size: 20px; font-weight: 700; color: #ff8c42; }
.logo-sub { font-size: 12px; color: #999; margin-top: 6px; }
.nav-menu { display: flex; gap: 6px; flex: 1; }
.nav-item {
  padding: 6px 14px;
  color: #555;
  text-decoration: none;
  font-size: 15px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.nav-item:hover { color: #ff8c42; background: #fff7f0; }
.nav-item.router-link-active { color: #ff8c42; font-weight: 600; }
.nav-cart { display: inline-flex; align-items: center; gap: 4px; }
.nav-right { display: flex; align-items: center; gap: 8px; }
.user-entry { display: flex; align-items: center; gap: 8px; cursor: pointer; outline: none; }
.user-name { font-size: 14px; color: #333; max-width: 90px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.role-tag { border: none; }
.main { max-width: 1200px; margin: 0 auto; min-height: calc(100vh - 160px); padding: 20px 16px; }
.footer { text-align: center; padding: 24px 0; color: #999; font-size: 13px; }
.footer-sub { font-size: 12px; margin-top: 4px; color: #bbb; }
</style>
