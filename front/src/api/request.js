import axios from 'axios';

const request = axios.create({
    baseURL: 'http://localhost:8081'
});

// 请求拦截器：自动附加 Bearer token
request.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// 响应拦截器：401 时清除认证状态，跳转登录
request.interceptors.response.use(
    (response) => {
        return response;
    },
    (error) => {
        if (error.response && error.response.status === 401) {
            localStorage.removeItem('token');
            localStorage.removeItem('userName');
            localStorage.removeItem('role');
            localStorage.removeItem('isLogin');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

export default request;
