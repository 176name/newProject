package streaming.domain;

import lombok.Data;

@Data
public class Result {
    private int code;
    private String msg;
    private Object data;

    public static Result success(Object data) {
        Result r = new Result();
        r.code = 200;
        r.msg = "操作成功";
        r.data = data;
        return r;
    }
    // 新增：带自定义消息的成功返回
    public static Result success(Object data, String msg) {
        Result r = new Result();
        r.code = 200;
        r.msg = msg;
        r.data = data;
        return r;
    }

    public static Result error(String msg) {
        Result r = new Result();
        r.code = 500;
        r.msg = msg;
        return r;
    }
}
