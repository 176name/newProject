import request from './request';

export function login(username, password) {
    return request.post('/api/auth/login', { username, password });
}

export function verifyToken() {
    return request.get('/api/auth/verify');
}
