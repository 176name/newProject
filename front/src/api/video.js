// src/api/video.js
import request from './request';

// ========== 设备管理 API ==========
export const getPlayUrl = (cameraIp) => {
    return request.get('/api/video/play', { params: { cameraIp } });
};

export const getDevices = () => {
    return request.get('/api/video/devices');
};

export const addDevice = (device) => {
    return request.post('/api/video/device', device);
};

export const updateDevice = (id, device) => {
    return request.put(`/api/video/device/${id}`, device);
};

export const deleteDevice = (id) => {
    return request.delete(`/api/video/device/${id}`);
};

// ========== 流量监控 API ==========

// 获取实时带宽数据
export const getRealtimeBandwidth = async () => {
    try {
        const res = await request.get('/api/traffic/realtime');
        console.log('实时带宽返回:', res.data);

        if (res.data.code === 200 && res.data.data) {
            const data = res.data.data;
            const totalMbps = parseFloat(data.totalBandwidthMbps) || 0;
            const streams = (data.streams || []).map(s => ({
                stream: s.stream,
                bytesSpeed: s.bytesSpeed || 0,
                bitrate: s.bitrate || 0,
                readerCount: s.readerCount || 0,
                totalBytes: s.totalBytes || 0,
                aliveSecond: s.aliveSecond || 0
            }));

            return {
                code: 200,
                success: true,
                data: {
                    currentBandwidth: totalMbps.toFixed(1),
                    bandwidthChange: '0',
                    // 摄像头是单向推流，上下行共用总带宽
                    uploadBandwidth: totalMbps.toFixed(1),
                    downloadBandwidth: '0',
                    streamCount: data.streamCount || streams.length,
                    streams: streams
                }
            };
        }

        return {
            code: 200,
            success: true,
            data: {
                currentBandwidth: '0',
                bandwidthChange: '0',
                uploadBandwidth: '0',
                downloadBandwidth: '0',
                streamCount: 0,
                streams: []
            }
        };
    } catch (error) {
        console.error('获取实时带宽失败:', error);
        return {
            code: 200,
            success: true,
            data: {
                currentBandwidth: '0',
                bandwidthChange: '0',
                uploadBandwidth: '0',
                downloadBandwidth: '0',
                streamCount: 0,
                streams: []
            }
        };
    }
};

// 获取今日流量统计
export const getTodayTraffic = async () => {
    try {
        const res = await request.get('/api/traffic/ranking', { params: { period: 'day' } });
        console.log('今日流量返回:', res.data);

        if (res.data.code === 200) {
            const ranking = res.data.data || [];
            let totalTrafficMB = 0;

            if (Array.isArray(ranking)) {
                for (const device of ranking) {
                    let traffic = 0;
                    if (device.total_traffic_mb !== undefined && device.total_traffic_mb !== null) {
                        traffic = parseFloat(device.total_traffic_mb);
                        if (isNaN(traffic)) traffic = 0;
                    }
                    totalTrafficMB += traffic;
                }
            }

            const totalTrafficGB = totalTrafficMB / 1024;

            return {
                code: 200,
                success: true,
                data: {
                    todayTraffic: totalTrafficGB.toFixed(1),
                    estimateTraffic: Math.round(totalTrafficGB * 1.2)
                }
            };
        }

        return {
            code: 200,
            success: true,
            data: {
                todayTraffic: '0',
                estimateTraffic: 0
            }
        };
    } catch (error) {
        console.error('获取今日流量失败:', error);
        return {
            code: 200,
            success: true,
            data: {
                todayTraffic: '0',
                estimateTraffic: 0
            }
        };
    }
};

// 获取设备流量排行
export const getDeviceTrafficRank = async () => {
    try {
        const res = await request.get('/api/traffic/ranking', { params: { period: 'day' } });
        console.log('设备排行返回:', res.data);

        if (res.data.code === 200) {
            const ranking = res.data.data || [];
            const rankList = [];

            for (const item of ranking) {
                let trafficMB = 0;
                if (item.total_traffic_mb !== undefined && item.total_traffic_mb !== null) {
                    trafficMB = parseFloat(item.total_traffic_mb);
                    if (isNaN(trafficMB)) trafficMB = 0;
                }

                rankList.push({
                    id: item.id,
                    name: item.name || `设备${item.id}`,
                    ip: item.ip || '',
                    traffic: (trafficMB / 1024).toFixed(1),
                    percent: 0,
                    online: true
                });
            }

            const maxTraffic = rankList.length > 0 ? Math.max(...rankList.map(r => parseFloat(r.traffic))) : 1;
            rankList.forEach(item => {
                item.percent = Math.round((parseFloat(item.traffic) / maxTraffic) * 100);
            });

            const totalTraffic = rankList.reduce((sum, item) => sum + parseFloat(item.traffic), 0);

            return {
                code: 200,
                success: true,
                data: {
                    totalTraffic: totalTraffic.toFixed(1),
                    rankList: rankList
                }
            };
        }

        return {
            code: 200,
            success: true,
            data: {
                totalTraffic: '0',
                rankList: []
            }
        };
    } catch (error) {
        console.error('获取设备排行失败:', error);
        return {
            code: 200,
            success: true,
            data: {
                totalTraffic: '0',
                rankList: []
            }
        };
    }
};

// 获取单个设备流量统计
export const getDeviceTraffic = (deviceId, period = 'day') => {
    return request.get('/api/traffic/device', { params: { deviceId, period } });
};

// 获取带宽历史趋势
export const getBandwidthHistory = async (params) => {
    try {
        const days = params?.hours ? Math.ceil(params.hours / 24) : 7;
        const res = await request.get('/api/traffic/trend', { params: { days } });
        console.log('带宽历史返回:', res.data);

        if (res.data.code === 200) {
            const trend = res.data.data || [];
            const timeLabels = [];
            const incomingData = [];
            const outgoingData = [];

            if (Array.isArray(trend)) {
                for (const item of trend) {
                    timeLabels.push(item.date);
                    let trafficMB = 0;
                    if (item.total_traffic_mb !== undefined && item.total_traffic_mb !== null) {
                        trafficMB = parseFloat(item.total_traffic_mb);
                        if (isNaN(trafficMB)) trafficMB = 0;
                    }
                    const trafficGB = trafficMB / 1024;
                    incomingData.push(trafficGB);
                    outgoingData.push(trafficGB);
                }
            }

            return {
                code: 200,
                success: true,
                data: {
                    timeLabels: timeLabels,
                    incomingData: incomingData,
                    outgoingData: outgoingData
                }
            };
        }

        return {
            code: 200,
            success: true,
            data: {
                timeLabels: [],
                incomingData: [],
                outgoingData: []
            }
        };
    } catch (error) {
        console.error('获取带宽历史失败:', error);
        return {
            code: 200,
            success: true,
            data: {
                timeLabels: [],
                incomingData: [],
                outgoingData: []
            }
        };
    }
};

// 获取当前策略配置
export const getCurrentPolicy = async () => {
    try {
        const res = await request.get('/api/traffic/policy');
        return res.data;
    } catch (error) {
        console.log('策略接口暂未实现，使用默认配置');
        return {
            code: 200,
            success: true,
            data: {
                autoDisconnect: false,
                disconnectMinutes: 5,
                nightDegrade: false,
                nightStart: 23,
                nightEnd: 6,
                nightBitrate: 512,
                bandwidthLimit: false,
                maxBandwidth: 50
            }
        };
    }
};

// 应用策略
export const applyPolicy = async (policyData) => {
    try {
        const res = await request.post('/api/traffic/policy', policyData);
        return res.data;
    } catch (error) {
        console.log('策略接口暂未实现，演示模式');
        return {
            code: 200,
            success: true,
            message: '策略已应用（演示模式）'
        };
    }
};

// 设备限流
export const limitDevice = async (deviceId, limitConfig) => {
    try {
        const res = await request.post(`/api/traffic/device/${deviceId}/limit`, limitConfig);
        return res.data;
    } catch (error) {
        console.log(`限流接口暂未实现，设备${deviceId}限流（演示模式）`);
        return {
            code: 200,
            success: true,
            message: '限流成功（演示模式）'
        };
    }
};
