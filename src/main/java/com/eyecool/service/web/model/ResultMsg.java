/*******************************************************************************
 * 系统名称   ： 会刷脸
 * 开发部门   ： 山东眼神智能科技有限公司
 * 文件名称   :  ResultMsg
 ******************************************************************************/
package com.eyecool.service.web.model;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.eyecool.service.web.constants.HTTPStatus;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 功能描述 :返回值信息
 *
 * @author duancunming
 * @date 2018-10-15 12:05
 **/
public class ResultMsg {
    private String code;
    private String msg;
    private String redirect;
    private Object data;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getRedirect() {
        return redirect;
    }

    public void setRedirect(String redirect) {
        this.redirect = redirect;
    }

    public ResultMsg() {
        this.code = HTTPStatus.OK.getResponseCode();
        this.msg = HTTPStatus.OK.getResponseMsg();
    }

    public ResultMsg(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public ResultMsg(String code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public ResultMsg(HTTPStatus enumObj) {
        this.code = enumObj.getResponseCode();
        this.msg = enumObj.getResponseMsg();
    }

    public ResultMsg(HTTPStatus enumObj, Object data) {
        this.code = enumObj.getResponseCode();
        this.msg = enumObj.getResponseMsg();
        this.data = data;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat);
    }

    /**
     * 返回OK，无数据
     *
     * @return ResultMsg
     */
    public static ResultMsg createOkMsg() {
        return new ResultMsg();
    }

    /**
     * 返回OK，有数据
     *
     * @param data data
     * @return ResultMsg
     */
    public static ResultMsg createOkMsg(Object data) {
        return new ResultMsg(HTTPStatus.OK, data);
    }

    /**
     * 返回一个returnData为空对象的成功消息的json
     *
     * @return ResultMsg
     */
    public static JSONObject newJson() {
        return new JSONObject();
    }

    /**
     * 返回错误信息JSON
     *
     * @param errorEnum 错误码的errorEnum
     * @return
     */
    public static JSONObject errorJson(HTTPStatus errorEnum) {
        JSONObject resultJson = new JSONObject();
        resultJson.put("code", errorEnum.getResponseCode());
        resultJson.put("msg", errorEnum.getResponseMsg());
        resultJson.put("data", new JSONObject());
        return resultJson;
    }

    /**
     * 参与验证失败
     *
     * @param msg msg
     * @return ResultMsg
     */
    public static ResultMsg createParamErrorMsg(String msg) {
        if (StringUtils.isEmpty(msg)) {
            msg = HTTPStatus.E_0004.getResponseMsg();
        }
        return new ResultMsg(HTTPStatus.E_0004.getResponseCode(), msg, null);
    }

    /**
     * 错误
     *
     * @param ex 异常
     * @return ResultMsg
     */
    public static ResultMsg createErrorMsg(Exception ex) {
        return new ResultMsg(HTTPStatus.E_0001.getResponseCode(), ex.getMessage(), null);
    }

    /**
     * 错误
     *
     * @param code 编码
     * @param msg  消息
     * @return ResultMsg
     */
    public static ResultMsg createErrorMsg(String code, String msg) {
        return new ResultMsg(code, msg, null);
    }

    /**
     * 错误
     *
     * @param msg 消息
     * @return ResultMsg
     */
    public static ResultMsg createErrorMsg(String msg) {
        return new ResultMsg(HTTPStatus.E_0001.getResponseCode(), msg, null);
    }

    /**
     * 错误
     *
     * @param code 编码
     * @param msg  消息
     * @param data data
     * @return ResultMsg
     */
    public static ResultMsg createErrorMsg(String code, String msg, Object data) {
        return new ResultMsg(code, msg, data);
    }

    /**
     * 查询分页结果后的封装工具方法
     *
     * @param requestJson 请求参数json,此json在之前调用fillPageParam 方法时,已经将pageRow放入
     * @param list        查询分页对象list
     * @param totalCount  查询出记录的总条数
     */
    public static JSONObject successPage(final JSONObject requestJson, List<JSONObject> list, int totalCount) {
        int pageRow = requestJson.getIntValue("pageRow");
        int totalPage = getPageCounts(pageRow, totalCount);
        JSONObject result = newJson();
        JSONObject returnData = new JSONObject();
        returnData.put("list", list);
        returnData.put("totalCount", totalCount);
        returnData.put("totalPage", totalPage);
        result.put("pageData", returnData);
        return result;
    }

    /**
     * 获取总页数
     *
     * @param pageRow   每页行数
     * @param itemCount 结果的总条数
     * @return int
     */
    public static int getPageCounts(int pageRow, int itemCount) {
        if (itemCount == 0) {
            return 1;
        }
        return itemCount % pageRow > 0 ?
                itemCount / pageRow + 1 :
                itemCount / pageRow;
    }


}
