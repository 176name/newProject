package streaming.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import streaming.domain.Result;
import streaming.service.IStreamService;

@RestController
@RequestMapping("/api/video")
public class StreamController {

    @Autowired
    private IStreamService streamService;

    @GetMapping("/play")
    public Result getPlayUrl(@RequestParam String cameraIp) {
        return streamService.startPush(cameraIp);
    }
}
