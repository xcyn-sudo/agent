import { createRouter, createWebHistory } from 'vue-router'
import { getToken, getUserRoleFromLocalStorage } from '@/utils/token'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { guest: true, layout: 'guest' },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/register/RegisterView.vue'),
      meta: { guest: true, layout: 'guest' },
    },
    {
      path: '/chat',
      name: 'Chat',
      component: () => import('@/views/chat/ChatView.vue'),
      meta: { requiresAuth: true, layout: 'main' },
    },
    {
      path: '/admin/users',
      name: 'UserManage',
      component: () => import('@/views/user/UserManageView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true, layout: 'main', title: '用户管理' },
    },
    {
      path: '/admin/knowledge',
      name: 'Knowledge',
      component: () => import('@/views/knowledge/KnowledgeView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true, layout: 'main', title: '知识库管理' },
    },
    {
      path: '/admin/dashboard',
      name: 'Dashboard',
      component: () => import('@/views/dashboard/DashboardView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true, layout: 'main', title: '数据仪表盘' },
    },
    {
      path: '/403',
      name: 'Forbidden',
      component: () => import('@/views/error/403.vue'),
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/error/404.vue'),
    },
  ],
})

router.beforeEach((to, _from, next) => {
  let token: string | null = null
  let userRole: string | null = null

  try {
    // 尝试使用 auth store 获取状态
    const authStore = useAuthStore()
    token = authStore.token
    userRole = authStore.user?.role ?? null
  } catch {
    // Pinia 未初始化时回退到 localStorage 方式
    token = getToken()
    userRole = getUserRoleFromLocalStorage()
  }

  if (to.path === '/login' || to.path === '/register') {
    // 已登录用户访问登录/注册页 → 重定向到 /chat
    token ? next('/chat') : next()
  } else {
    if (!token) {
      // 未登录 → 重定向到 /login，携带 redirect 参数
      next(`/login?redirect=${to.path}`)
    } else if (to.meta.requiresAdmin) {
      // 需要管理员权限 → 检查角色
      if (userRole !== 'admin') {
        next('/403')
      } else {
        next()
      }
    } else {
      next()
    }
  }
})

export default router
