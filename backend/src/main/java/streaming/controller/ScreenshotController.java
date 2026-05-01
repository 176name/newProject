package streaming.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import streaming.domain.Result;

import java.io.File;
import java.util.*;

@RestController
@RequestMapping("/api/screenshots")
public class ScreenshotController {

    @Value("${screenshot.dir:./screenshots}")
    private String screenshotDir;

    @GetMapping("/list")
    public Result listScreenshots() {
        File dir = new File(screenshotDir);
        List<Map<String, Object>> files = new ArrayList<>();

        if (dir.exists() && dir.isDirectory()) {
            File[] fileList = dir.listFiles((d, name) ->
                    name.endsWith(".jpg") || name.endsWith(".png"));

            if (fileList != null) {
                for (File file : fileList) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", file.getName());
                    item.put("url", "/screenshots/" + file.getName());
                    item.put("type", file.getName().startsWith("stranger") ? "stranger" : "fall");
                    item.put("time", new Date(file.lastModified()).toString());
                    files.add(item);
                }
            }
        }

        // 按时间倒序
        files.sort((a, b) -> b.get("name").toString().compareTo(a.get("name").toString()));

        return Result.success(files);
    }
}
