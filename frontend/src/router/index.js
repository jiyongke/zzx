import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

const routes = [
  { path: '/', name: 'Home', component: () => import('../views/Home.vue') },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { public: true } },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue'), meta: { public: true } },
  { path: '/products', name: 'ProductList', component: () => import('../views/ProductList.vue'), meta: { public: true } },
  { path: '/product/:id', name: 'ProductDetail', component: () => import('../views/ProductDetail.vue'), meta: { public: true } },
  { path: '/trace', name: 'TraceQuery', component: () => import('../views/TraceQuery.vue'), meta: { public: true } },
  { path: '/trace/batch/:id', name: 'TraceDetail', component: () => import('../views/TraceDetail.vue'), meta: { public: true } },
  { path: '/cart', name: 'Cart', component: () => import('../views/Cart.vue'), meta: { requiresAuth: true } },
  { path: '/checkout', name: 'Checkout', component: () => import('../views/Checkout.vue'), meta: { requiresAuth: true } },
  { path: '/orders', name: 'OrderList', component: () => import('../views/OrderList.vue'), meta: { requiresAuth: true } },
  { path: '/order/:id', name: 'OrderDetail', component: () => import('../views/OrderDetail.vue'), meta: { requiresAuth: true } },
  { path: '/profile', name: 'Profile', component: () => import('../views/Profile.vue'), meta: { requiresAuth: true } },
  { path: '/workspace', name: 'Workspace', component: () => import('../views/Workspace.vue'), meta: { requiresAuth: true } },
  { path: '/farmer/products', name: 'FarmerProduct', component: () => import('../views/FarmerProduct.vue'), meta: { requiresAuth: true, roles: ['FARMER'] } },
  { path: '/admin/dashboard', name: 'Dashboard', component: () => import('../views/admin/Dashboard.vue'), meta: { requiresAuth: true, roles: ['ADMIN'] } },
  { path: '/admin/report', name: 'Report', component: () => import('../views/admin/Report.vue'), meta: { requiresAuth: true, roles: ['ADMIN'] } },
  { path: '/admin/audit', name: 'AuditLog', component: () => import('../views/admin/AuditLog.vue'), meta: { requiresAuth: true, roles: ['ADMIN'] } },
  { path: '/admin/products', name: 'ProductManage', component: () => import('../views/admin/ProductManage.vue'), meta: { requiresAuth: true, roles: ['ADMIN'] } },
  { path: '/admin/categories', name: 'CategoryManage', component: () => import('../views/admin/CategoryManage.vue'), meta: { requiresAuth: true, roles: ['ADMIN'] } },
  { path: '/admin/orders', name: 'OrderManage', component: () => import('../views/admin/OrderManage.vue'), meta: { requiresAuth: true, roles: ['ADMIN', 'FARMER'] } },
  { path: '/admin/users', name: 'UserManage', component: () => import('../views/admin/UserManage.vue'), meta: { requiresAuth: true, roles: ['ADMIN'] } },
  { path: '/admin/trace', name: 'TraceManage', component: () => import('../views/admin/TraceManage.vue'), meta: { requiresAuth: true, roles: ['ADMIN', 'FARMER'] } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：登录校验 + 角色校验
router.beforeEach((to) => {
  const store = useUserStore()
  if (to.meta.public) return true
  if (to.meta.requiresAuth && !store.isLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.roles && !to.meta.roles.includes(store.role)) {
    return { path: '/' }
  }
  return true
})

export default router
