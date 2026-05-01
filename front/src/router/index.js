import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue'
import Monitor from '../views/Monitor.vue'
import DeviceManage from '../views/DeviceManage.vue'
import Playback from '../views/Playback.vue'
import Traffic from '../views/Traffic.vue'
import AIAnalysis from '../views/AIAnalysis.vue'
import UserCenter from '../views/UserCenter.vue'
import UserManage from '../views/UserManage.vue'
import FaceManage from '../views/FaceManage.vue'
import Screenshots from '../views/Screenshots.vue'

// 路由守卫
const checkAuth = (to, from, next) => {
    const isLogin = localStorage.getItem('isLogin')
    if (to.path !== '/login' && isLogin !== 'true') {
        next('/login')
    } else {
        next()
    }
}

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: Login
    },
    {
        path: '/',
        redirect: '/monitor'
    },
    {
        path: '/monitor',
        component: Layout,
        beforeEnter: checkAuth,
        children: [{ path: '', component: Monitor, meta: { title: '监控大屏', icon: '📹' } }]
    },
    {
        path: '/devices',
        component: Layout,
        beforeEnter: checkAuth,
        children: [{ path: '', component: DeviceManage, meta: { title: '设备管理', icon: '📷' } }]
    },
    {
        path: '/playback',
        component: Layout,
        beforeEnter: checkAuth,
        children: [{ path: '', component: Playback, meta: { title: '录像回放', icon: '🎬' } }]
    },
    {
        path: '/traffic',
        component: Layout,
        beforeEnter: checkAuth,
        children: [{ path: '', component: Traffic, meta: { title: '流量监控', icon: '📊' } }]
    },
    {
        path: '/ai-analysis',
        component: Layout,
        beforeEnter: checkAuth,
        children: [{ path: '', component: AIAnalysis, meta: { title: 'AI分析', icon: '🤖' } }]
    },
    {
        path: '/user-center',
        component: Layout,
        beforeEnter: checkAuth,
        children: [{ path: '', component: UserCenter, meta: { title: '个人中心', icon: '👤' } }]
    },
    {
        path: '/users',
        component: Layout,
        beforeEnter: checkAuth,
        children: [{ path: '', component: UserManage, meta: { title: '用户管理', icon: '👥' } }]
    },
    // ========== 新增路由（不使用 Layout，独立页面）==========
    {
        path: '/face-manage',
        name: 'FaceManage',
        component: FaceManage,
        beforeEnter: checkAuth,
        meta: { title: '人脸管理', icon: '😀' }
    },
    {
        path: '/screenshots',
        name: 'Screenshots',
        component: Screenshots,
        beforeEnter: checkAuth,
        meta: { title: '告警截图', icon: '📸' }
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
