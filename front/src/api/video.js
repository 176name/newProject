import axios from 'axios';

const request = axios.create({
    baseURL: 'http://localhost:8081'
});

export const getPlayUrl = (cameraIp) => {
    return request.get('/api/video/play', { params: { cameraIp } });
};
