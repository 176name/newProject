import { createRouter, createWebHistory } from 'vue-router';
import Login from '../views/Login.vue';
import Monitor from '../views/Monitor.vue';

const routes = [
    { path: '/', redirect: '/login' },
    { path: '/login', component: Login },
    { path: '/monitor', component: Monitor }
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

// 路由守卫：没登录想进 monitor 会被踢回 login
router.beforeEach((to, from, next) => {
    const isLogin = localStorage.getItem('isLogin');
    if (to.path === '/monitor' && !isLogin) {
        next('/login');
    } else {
        next();
    }
});

export default router;
