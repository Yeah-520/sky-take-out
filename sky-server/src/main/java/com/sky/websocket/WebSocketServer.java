package com.sky.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket服务
 */
@Slf4j
@Component
@ServerEndpoint("/ws/{sid}")
public class WebSocketServer {

    //存放会话对象
    private static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        sessionMap.put(sid, session);
        log.info("新连接，会话ID：{}", sid);
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     * @param sid     会话ID
     */
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        log.info("收到客户端：{} 的消息：{}", sid, message);
        log.info("收到来自客户端：{} 的信息:{}", sid, message);
    }

    /**
     * 连接关闭调用的方法
     *
     * @param sid 会话ID
     */
    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        sessionMap.remove(sid);
        log.info("连接关闭，会话ID：{}", sid);
    }

    /**
     * 群发
     *
     * @param message 消息
     */
    public void sendToAllClient(String message) {
        for (Map.Entry<String, Session> entry : sessionMap.entrySet()) {
            Session session = entry.getValue();
            // 连接已失效,顺手清理
            if (!session.isOpen()) {
                sessionMap.remove(entry.getKey());
                continue;
            }
            try {
                // 异步发送,避免慢客户端阻塞整轮群发
                session.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                log.error("发送消息出错：{}", e.getMessage());
                sessionMap.remove(entry.getKey());
            }
        }
    }

}
