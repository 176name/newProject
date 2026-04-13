package streaming.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import streaming.domain.Result;
import streaming.service.IStreamService;

@RestController
@RequestMapping("/api/video")
@CrossOrigin // 必须加！解决 Vue 跨域访问 backend 的问题
public class StreamController {

    @Autowired
    private IStreamService streamService;

    @GetMapping("/play")
    public Result play(@RequestParam String cameraIp) {
        return streamService.startPush(cameraIp);
    }
}
