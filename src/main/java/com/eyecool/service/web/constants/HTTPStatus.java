package com.eyecool.service.web.constants;

/**
 * 描述：常量类
 *
 * @author duancunming
 * @date 2018-10-15 12:05
 **/
public enum HTTPStatus {

    /*
     * 成功响应码
     * */
    OK("0000", "操作成功"),

    /*
     * 错误信息
     * */
    E_400("400", "请求处理异常，请稍后再试"),
    E_404("404", "请求路径不存在"),
    E_500("500", "请求方式有误,请检查 GET/POST"),
    E_501("501", "无法支持其对此资源的请求"),
    E_502("502", "权限不足"),

    E_0001("0001", "维护失败"),
    E_0002("0002", "查无结果"),
    E_0003("0003", "内部错误"),
    E_0004("0004", "参数验证失败"),
    E_0005("0005", "验证码错误"),
    E_0006("0006", "帐户信息不存在"),
    E_0007("0007", "帐户不可用，请联系管理员"),
    E_0008("0008", "用户和密码不匹配"),
    E_0009("0009", "原密码错误"),
    E_0010("0010", "设备ip已存在"),
    E_0011("0011", "设备编码已存在"),

    E_10008("10008", "角色删除失败,尚有用户属于此角色"),
    E_10009("10009", "帐户已存在"),
    E_10010("10010", "商户名称已存在"),
    E_10011("10011", "商户已禁用"),
    E_10012("10012", "商户已过期"),
    E_10013("10013", "商户已无效"),

    E_10014("10014", "证件号已存在"),
    E_10015("10015", "身份证号已存在"),
    E_10016("10016", "组名称已存在"),
    E_10017("10017", "用户未授予角色信息，请联系管理员授予角色"),
    E_10018("10018", "角色名已存在"),
    E_10019("10019", "照片路径不能为空"),

    E_20011("20011", "登录已过期,请重新登录！"),
    E_90003("90003", "缺少必填参数");

    private String responseCode;
    private String responseMsg;

    HTTPStatus() {
    }

    /**
     * 构造函数
     * @param responseCode code
     * @param responseMsg  msg
     */
    HTTPStatus(String responseCode, String responseMsg) {
        this.responseCode = responseCode;
        this.responseMsg = responseMsg;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseMsg() {
        return responseMsg;
    }

    public void setResponseMsg(String responseMsg) {
        this.responseMsg = responseMsg;
    }

}
