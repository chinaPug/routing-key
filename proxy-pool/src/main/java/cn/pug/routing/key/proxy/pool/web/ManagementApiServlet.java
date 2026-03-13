package cn.pug.routing.key.proxy.pool.web;

import cn.pug.routing.key.proxy.pool.component.ServerContext;
import cn.pug.routing.key.proxy.pool.metrics.ProxyMetrics;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理 API Servlet
 * 
 * 提供 RESTful API 用于管理代理池
 * 
 * @author pug
 * @since 1.0.0
 */
@Slf4j
public class ManagementApiServlet extends HttpServlet {

    private final ServerContext serverContext;
    private final ProxyMetrics metrics;

    public ManagementApiServlet(ServerContext serverContext, ProxyMetrics metrics) {
        this.serverContext = serverContext;
        this.metrics = metrics;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        PrintWriter out = resp.getWriter();
        JSONObject result = new JSONObject();

        try {
            switch (path) {
                case "/status":
                    result = getStatus();
                    break;
                case "/units":
                    result = getUnits();
                    break;
                case "/metrics":
                    result = getMetrics();
                    break;
                case "/connections":
                    result = getConnections();
                    break;
                default:
                    resp.setStatus(404);
                    result.put("error", "Unknown endpoint: " + path);
            }
        } catch (Exception e) {
            log.error("API 请求处理失败: {}", e.getMessage());
            resp.setStatus(500);
            result.put("error", e.getMessage());
        }

        out.print(result.toJSONString());
        out.flush();
    }

    /**
     * 获取系统状态
     */
    private JSONObject getStatus() {
        JSONObject status = new JSONObject();
        status.put("running", true);
        status.put("proxyPort", serverContext.getProxyPort());
        status.put("servletPort", serverContext.getServletPort());
        status.put("registeredUnits", serverContext.getUnitCount());
        status.put("availablePorts", serverContext.getAvailablePorts());
        return status;
    }

    /**
     * 获取代理节点列表
     */
    private JSONObject getUnits() {
        JSONObject result = new JSONObject();
        result.put("units", serverContext.getAllUnits());
        return result;
    }

    /**
     * 获取监控指标
     */
    private JSONObject getMetrics() {
        JSONObject result = new JSONObject();
        ProxyMetrics.PoolStats stats = metrics.getStats();
        
        result.put("activeConnections", stats.getActive());
        result.put("totalConnections", stats.getTotal());
        result.put("availableConnections", stats.getAvailable());
        result.put("maxConnections", stats.getMax());
        result.put("prometheus", metrics.scrape());
        
        return result;
    }

    /**
     * 获取连接信息
     */
    private JSONObject getConnections() {
        JSONObject result = new JSONObject();
        result.put("connections", serverContext.getConnectionDetails());
        return result;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        PrintWriter out = resp.getWriter();
        JSONObject result = new JSONObject();

        try {
            switch (path) {
                case "/units/kick":
                    result = kickUnit(req);
                    break;
                default:
                    resp.setStatus(404);
                    result.put("error", "Unknown endpoint: " + path);
            }
        } catch (Exception e) {
            log.error("API 请求处理失败: {}", e.getMessage());
            resp.setStatus(500);
            result.put("error", e.getMessage());
        }

        out.print(result.toJSONString());
        out.flush();
    }

    /**
     * 踢出代理节点
     */
    private JSONObject kickUnit(HttpServletRequest req) {
        String unitId = req.getParameter("unitId");
        JSONObject result = new JSONObject();
        
        if (unitId == null || unitId.isEmpty()) {
            result.put("success", false);
            result.put("error", "Missing unitId parameter");
            return result;
        }

        boolean success = serverContext.kickUnit(unitId);
        result.put("success", success);
        result.put("message", success ? "Unit kicked successfully" : "Unit not found");
        
        return result;
    }
}
