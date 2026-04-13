import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// 必须引入这个，否则播放器 UI 就是一堆乱码字
import 'xgplayer/dist/index.min.css';

const app = createApp(App)
app.use(router)
app.mount('#app')
