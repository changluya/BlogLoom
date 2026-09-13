package com.changlu.blogloom.model.vo;

import lombok.Data;

import java.util.Map;

/**
 * @author changlu
 * @date 2026-09-13
 */
@Data
public class QqResultVO {
    private String success;

    private String msg;

    private Map<String, Object> data;

    private String time;

    private  String api_vers;
}
