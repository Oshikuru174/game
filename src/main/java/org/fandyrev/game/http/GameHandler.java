package org.fandyrev.game.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.AsciiString;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.fandyrev.game.engine.Command;
import org.fandyrev.game.engine.GameEngine;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameHandler extends SimpleChannelInboundHandler<HttpObject> {

    private static final AsciiString CONTENT_TYPE = AsciiString.cached("Content-Type");
    private static final AsciiString CONTENT_LENGTH = AsciiString.cached("Content-Length");
    private static final AsciiString CONNECTION = AsciiString.cached("Connection");
    private static final AsciiString KEEP_ALIVE = AsciiString.cached("keep-alive");
    private static final AsciiString LOCATION = AsciiString.cached("Location");
    private static final AsciiString SET_COOKIE = AsciiString.cached("Set-Cookie");
    private static final AsciiString COOKIE = AsciiString.cached("Cookie");
    private Pattern messageBodyPattern, sessionIdPattern;

    public GameHandler() {
        super();
        messageBodyPattern = Pattern.compile("^([^&^=]+=[^&^=]*)?((&([^&^=]+=[^&^=]*))*)$");
        sessionIdPattern = Pattern.compile("^[\\w;= ]*GAME_SESSION_ID=([\\da-f]{8}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{12})[\\w;= ]*$");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {

        if (msg instanceof FullHttpRequest) {
            FullHttpRequest req = (FullHttpRequest) msg;
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(req.uri());
            String uri = queryStringDecoder.path();
            HttpMethod method = req.method();
            HashMap<String, String> content = getContent(req.content().toString(StandardCharsets.UTF_8));
            String sessionId = getSessionId(req.headers().get(COOKIE));

            FullHttpResponse response = null;


            if("/".equals(uri)) {
                response = redirect("/login");
            } else if(!ResourceMapper.isGameResourse(uri)) {
                response = notFound();
            } else if("/authorisationError".equals(uri)) {
                response = getGameResourse("/authorisationError", null);
            }

            if(response == null) {
                Command requestCommand = createCommand(uri, method, sessionId, content);
                System.out.println("requestCommand=" + requestCommand);
                Command responseCommand = GameEngine.getInstance().executeCommand(requestCommand);
                System.out.println("responseCommand=" + responseCommand);
                response = executeCommand(responseCommand);
                System.out.println("response=" + response == null);
                System.out.println();

            }

            response.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
            response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());

            //System.out.println(response);
            boolean keepAlive = HttpUtil.isKeepAlive(req);

            if (!keepAlive) {
                ctx.write(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                response.headers().set(CONNECTION, KEEP_ALIVE);
                ctx.write(response);
            }
        }
    }

    private Command createCommand(String uri, HttpMethod method, String sessionId, HashMap<String, String> content){
        Command requestCommand = null;

        switch (uri){
            case "/":
                requestCommand = new Command(Command.LOGIN, sessionId);
                break;
            case "/login":
                requestCommand = new Command(Command.LOGIN, sessionId);
                break;
            case "/main":
                if(method.equals(HttpMethod.POST)){
                    requestCommand = new Command(Command.AUTHORIZATION, sessionId, content);
                } else {
                    requestCommand = new Command(Command.MAIN, sessionId);
                }
                break;
            case "/duels":
                if(method.equals(HttpMethod.POST)){
                    if(content != null && "start".equals(content.get(Command.PARAM_MATCHING))) {
                        requestCommand = new Command(Command.START_MATCHING, sessionId, content);
                    } else if (content != null && "stop".equals(content.get(Command.PARAM_MATCHING))){
                        requestCommand = new Command(Command.STOP_MATCHING, sessionId, content);
                    }
                }

                if(requestCommand == null){
                    requestCommand = new Command(Command.DUELS, sessionId);
                }
                break;
            case "/combat":
                if(method.equals(HttpMethod.POST)){
                    requestCommand = new Command(Command.HIT, sessionId, content);
                } else {
                    requestCommand = new Command(Command.COMBAT, sessionId);
                }
                break;
            case "/logout":
                requestCommand = new Command(Command.LOGOUT, sessionId);
                break;
        }

        return requestCommand;
    }

    private FullHttpResponse executeCommand(Command command){
        FullHttpResponse response = null;
        switch(command.getCommand()){
            case Command.LOGIN:
                response = getGameResourse("/login", command);
                break;
            case Command.AUTHORIZATION:
                response = authorization(command);
                break;
            case Command.AUTHORIZATION_ERROR:
                response = redirect("/authorisationError");
                break;
            case Command.REDIRECT:
                response = redirect(command.getParam(Command.PARAM_URL));
                break;
            case Command.DUELS:
                response = getGameResourse("/duels", command);
                break;
            case Command.MAIN:
                response = getGameResourse("/main", command);
                break;
            case Command.WAITING_DUEL:
                response = getGameResourse("/duels", command);
                break;
            case Command.COMBAT:
                response = getGameResourse("/combat", command);
                break;
        }

        return response;
    }

    private FullHttpResponse getGameResourse(String url, Command command) {
        byte[] content = getResource(url, command);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(content));

        return response;
    }

    private FullHttpResponse authorization(Command command) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(getResource("/main", command)));
        response.headers().set(SET_COOKIE, "GAME_SESSION_ID=" + command.getSessionId());

        return response;
    }

    private FullHttpResponse redirect(String url){
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.FOUND);
        response.headers().set(LOCATION, url);

        return response;
    }

    private FullHttpResponse notFound(){
        FullHttpResponse response = response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.NOT_FOUND);

        return response;
    }

    private String getSessionId(String cookie) {
        if(cookie == null)
            return null;

        Matcher matcher = sessionIdPattern.matcher(cookie);
        if(matcher.matches())
            return matcher.group(1);

        return null;
    }

    private HashMap<String, String> getContent(String content) throws IOException {
        if(content == null)
            return null;

        Matcher matcher = messageBodyPattern.matcher(content);
        if(!matcher.matches())
            return null;

        HashMap body = new HashMap<String, String>();
        String[] param = content.split("&");
        for(int i = 0; i < param.length; i++){
            String[] keyValue = param[i].split("=");
            if(keyValue.length > 1) {
                body.put(keyValue[0], keyValue[1]);
            } else {
                body.put(keyValue[0], null);
            }
        }

        return body;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private byte[] getResource(String url, Command command){
        Template main = Velocity.getTemplate("/src/main/resources/vm" + url + ".vm", "UTF8");
        VelocityContext vc = new VelocityContext();

        if(command.getParam() != null && command.getParam().size() > 0) {
            for (Map.Entry<String, String> param : command.getParam().entrySet()) {
                vc.put(param.getKey(), param.getValue());
            }
        }

        StringWriter sw = new StringWriter();
        vc.put(Command.PARAM_PAGE_GEN, "page: " + (System.nanoTime() - command.getStartTime()) / 1000000 + "ms");
        main.merge(vc, sw);

        return sw.toString().getBytes();
    }
}
