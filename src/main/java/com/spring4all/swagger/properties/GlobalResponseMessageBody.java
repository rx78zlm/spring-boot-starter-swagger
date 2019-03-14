package com.spring4all.swagger.properties;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by zhangleimin on 2019/3/12.
 */
@Data
@NoArgsConstructor
public class GlobalResponseMessageBody {

    /**
     * 响应码
     **/
    private int code;

    /**
     * 响应消息
     **/
    private String message;

    /**
     * 响应体
     **/
    private String modelRef;
}
