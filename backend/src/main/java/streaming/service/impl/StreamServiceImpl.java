package streaming.service.impl;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import streaming.config.ZlmConfig;
import streaming.domain.Result;
import streaming.service.IStreamService;

@Slf4j
@Service
public class StreamServiceImpl implements IStreamService {

    @Autowired
    private ZlmConfig zlmConfig;

    @Value("${camera.username:admin}")
    private String username;

    @Value("${camera.password:123456}")
    private String password;

    @Override
    public Result startPush(String cameraIp) {
        // 1. 构造唯一的流ID（将IP的点换成下划线避免路径解析问题）
        String streamId = "camera_" + cameraIp.replace(".", "_");

        // 2. 拼接带认证的源 RTSP 地址
        String rtspUrl = String.format("rtsp://%s:%s@%s:554/Streaming/Channels/101",
                username, password, cameraIp);

        log.info("准备向 ZLM 发起拉流请求，源地址: {}", rtspUrl);

        // 3. 拼接 ZLM API 地址 (addStreamProxy)
        String url = String.format("http://%s:%d/index/api/addStreamProxy?secret=%s&vhost=__defaultVhost__&app=live&stream=%s&url=%s",
                zlmConfig.getHost(), zlmConfig.getHttpPort(), zlmConfig.getSecret(), streamId, rtspUrl);

        try {
            // 4. 发送 GET 请求给 ZLM
            String response = HttpUtil.get(url);
            JSONObject json = JSON.parseObject(response);

            if (json != null) {
                int code = json.getIntValue("code");
                String msg = json.getString("msg");

                // --- 核心修正逻辑 ---
                // code == 0: 初始拉流成功
                // code == -1 且 msg 包含 "exists": 流已经在拉取中了，直接复用
                if (code == 0 || (code == -1 && msg != null && msg.contains("exists"))) {

                    // 5. 拼接播放地址给前端 (FLV 格式，低延迟首选)
                    String playUrl = String.format("http://%s:%d/live/%s.live.flv",
                            zlmConfig.getHost(), zlmConfig.getHttpPort(), streamId);

                    if (code == 0) {
                        log.info("ZLM 开启新拉流成功，播放地址: {}", playUrl);
                    } else {
                        log.info("ZLM 检测到流已存在，复用地址: {}", playUrl);
                    }

                    return Result.success(playUrl);
                }

                log.error("ZLM 返回业务错误 - Code: {}, Msg: {}", code, msg);
                return Result.error("ZLM拉流失败：" + msg);
            }

            return Result.error("ZLM 返回数据异常");

        } catch (Exception e) {
            log.error("连接 ZLM 服务器异常, 目标 URL: {}", url, e);
            return Result.error("连接ZLM服务器失败，请检查网络、IP端口或虚拟机防火墙");
        }
    }
}
