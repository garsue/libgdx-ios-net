package com.badlogic.gdx.backends.iosrobovm;

import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.NetJavaServerSocketImpl;
import com.badlogic.gdx.net.NetJavaSocketImpl;
import com.badlogic.gdx.net.NetObjCImpl;
import com.badlogic.gdx.net.ServerSocket;
import com.badlogic.gdx.net.ServerSocketHints;
import com.badlogic.gdx.net.Socket;
import com.badlogic.gdx.net.SocketHints;
import org.robovm.apple.foundation.NSURL;
import org.robovm.apple.uikit.UIApplication;

/**
 * Objective-C Framework backend version of
 * {@link com.badlogic.gdx.backends.iosrobovm.IOSNet}
 *
 * @author ohsuga
 */
public class ObjCNet implements Net {

    NetObjCImpl netObjCImpl = new NetObjCImpl();
    final UIApplication uiApp;

    public ObjCNet(IOSApplication app) {
        uiApp = app.uiApp;
    }

    @Override
    public void sendHttpRequest(HttpRequest httpRequest, HttpResponseListener httpResponseListener) {
        netObjCImpl.sendHttpRequest(httpRequest, httpResponseListener);
    }

    @Override
    public void cancelHttpRequest(HttpRequest httpRequest) {
        netObjCImpl.cancelHttpRequest(httpRequest);
    }

    @Override
    public ServerSocket newServerSocket(Protocol protocol, int port, ServerSocketHints hints) {
        return new NetJavaServerSocketImpl(protocol, port, hints);
    }

    @Override
    public Socket newClientSocket(Protocol protocol, String host, int port, SocketHints hints) {
        return new NetJavaSocketImpl(protocol, host, port, hints);
    }

    @Override
    public void openURI(String URI) {
        uiApp.openURL(new NSURL(URI));
    }
}
