package com.sky.controller.admin;

import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

/**
 * 通用控制器类
 */
@RestController
@RequestMapping("/admin/common")
@Slf4j
public class CommonController {
    /**
     * 上传本地目录
     */
    private static final String UPLOAD_DIR = "D:/Project/upload/";

    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) {
        log.info("文件上传：{}", file);
        try {
            // 1. 判断文件是否为空
            if (file.isEmpty()) {
                return Result.error("上传文件不能为空");
            }
            // 2. 创建目录，如果不存在
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            // 3. 获取原始文件名 + 后缀
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            // 4. UUID生成新文件名，防止重名覆盖
            String newFileName = UUID.randomUUID() + suffix;
            // 5. 拼接完整文件路径
            File saveFile = new File(UPLOAD_DIR + newFileName);
            // 6. 保存文件到本地
            file.transferTo(saveFile);

            // 返回访问地址（如果需要通过web访问这张图片，后续要配置静态资源映射）
            String url = "http://127.0.0.1:8080" + "/upload/" + newFileName;
            return Result.success(url);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败");
        }
    }
}
