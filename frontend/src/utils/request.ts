import axios from 'axios';
import { ElMessage } from 'element-plus';

const service = axios.create({
    baseURL: '/api',
    timeout: 5000
});

// 响应拦截器
service.interceptors.response.use(
    (response) => {
        return response;
    },
    (error) => {
        console.warn('后端连接失败或异常');
        ElMessage.warning('请求失败');
        return Promise.reject(error);
    }
);

export default service;
