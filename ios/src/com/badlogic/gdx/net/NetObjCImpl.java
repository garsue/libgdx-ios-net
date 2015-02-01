package com.badlogic.gdx.net;

import com.badlogic.gdx.Net;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.StreamUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.robovm.apple.foundation.NSData;
import org.robovm.apple.foundation.NSError;
import org.robovm.apple.foundation.NSHTTPURLResponse;
import org.robovm.apple.foundation.NSInputStream;
import org.robovm.apple.foundation.NSMutableURLRequest;
import org.robovm.apple.foundation.NSOperationQueue;
import org.robovm.apple.foundation.NSURL;
import org.robovm.apple.foundation.NSURLConnection;
import org.robovm.apple.foundation.NSURLRequest;
import org.robovm.apple.foundation.NSURLRequestCachePolicy;
import org.robovm.apple.foundation.NSURLResponse;
import org.robovm.objc.block.VoidBlock3;

/**
 *
 * @author ohsuga
 */
public class NetObjCImpl {

    static class HttpClientResponse implements Net.HttpResponse {

        private final NSHTTPURLResponse response;
        private final NSData data;
        private final HttpStatus status;

        HttpClientResponse(NSURLResponse response, NSData data) throws IOException {
            this.response = (NSHTTPURLResponse) response;
            this.data = data;
            this.status = new HttpStatus((int) this.response.getStatusCode());
        }

        @Override
        public byte[] getResult() {
            return data.getBytes();
        }

        @Override
        public String getResultAsString() {
            return new String(data.getBytes());
        }

        @Override
        public InputStream getResultAsStream() {
            return getInputStream();
        }

        @Override
        public HttpStatus getStatus() {
            return status;
        }

        @Override
        public String getHeader(String name) {
            return response.getAllHeaderFields().get(name);
        }

        @Override
        public Map<String, List<String>> getHeaders() {
            Map<String, String> headers = response.getAllHeaderFields();
            Map<String, List<String>> ret = new HashMap<>();
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                ArrayList<String> value = new ArrayList<>();
                value.add(entry.getValue());
                ret.put(entry.getKey(), value);
            }
            return ret;
        }

        private InputStream getInputStream() {
            return new ByteArrayInputStream(data.getBytes());
        }
    }

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final NSOperationQueue queue = new NSOperationQueue();
    final ObjectMap<Net.HttpRequest, NSURLRequest> requests;
    final ObjectMap<Net.HttpRequest, Net.HttpResponseListener> listeners;
    final Lock lock;

    public NetObjCImpl() {
        requests = new ObjectMap<>();
        listeners = new ObjectMap<>();
        lock = new ReentrantLock();
    }

    public void sendHttpRequest(final Net.HttpRequest request, final Net.HttpResponseListener httpResponseListener) {
        if (request.getUrl() == null) {
            httpResponseListener.failed(new GdxRuntimeException("can't process a HTTP request without URL set"));
            return;
        }

        try {
            final String method = request.getMethod();
            NSURL url;

            if (method.equalsIgnoreCase(Net.HttpMethods.GET)) {
                String queryString = "";
                String value = request.getContent();
                if (value != null && !"".equals(value)) {
                    queryString = "?" + value;
                }
                url = new NSURL(request.getUrl() + queryString);
            } else {
                url = new NSURL(request.getUrl());
            }

            final NSMutableURLRequest nuRequest = new NSMutableURLRequest();
            nuRequest.setURL(url);
            nuRequest.setCachePolicy(NSURLRequestCachePolicy.ReloadIgnoringLocalCacheData);
            nuRequest.setShouldHandleHTTPCookies(false);
            nuRequest.setTimeoutInterval(request.getTimeOut());
            nuRequest.setHTTPMethod(method);

            final boolean doingOutPut = method.equalsIgnoreCase(Net.HttpMethods.POST) || method.equalsIgnoreCase(Net.HttpMethods.PUT);

            lock.lock();
            try {
                requests.put(request, nuRequest);
                listeners.put(request, httpResponseListener);
            } finally {
                lock.unlock();
            }

            // Headers get set regardless of the method
            nuRequest.setAllHTTPHeaderFields(request.getHeaders());

            executorService.submit(new Runnable() {

                @Override
                public void run() {
                    try {
                        // Set the content for POST and PUT (GET has the information embedded in the URL)
                        if (doingOutPut) {
                            // we probably need to use the content as stream here instead of using it as a string.
                            String contentAsString = request.getContent();
                            if (contentAsString != null) {
                                nuRequest.setHTTPBody(new NSData(contentAsString.getBytes()));
                            } else {
                                InputStream contentAsStream = request.getContentStream();
                                if (contentAsStream != null) {
                                    nuRequest.setHTTPBodyStream(toNSInputStream(contentAsStream));
                                }
                            }
                        }

                        NSURLConnection.sendAsynchronousRequest(nuRequest, queue, new VoidBlock3<NSURLResponse, NSData, NSError>() {

                            @Override
                            public void invoke(NSURLResponse nuResponse, NSData data, NSError error) {
                                if (error != null) {
                                    removeOnFailure(request, httpResponseListener, new Exception(error.description()));
                                    return;
                                }

                                final HttpClientResponse clientResponse;
                                try {
                                    clientResponse = new HttpClientResponse(nuResponse, data);
                                } catch (IOException e) {
                                    removeOnFailure(request, httpResponseListener, e);
                                    return;
                                }
                                lock.lock();
                                try {
                                    Net.HttpResponseListener listener = listeners.get(request);
                                    if (listener != null) {
                                        listener.handleHttpResponse(clientResponse);
                                        listeners.remove(request);
                                    }
                                } finally {
                                    lock.unlock();
                                }

                            }
                        });

                    } catch (final Exception e) {
                        removeOnFailure(request, httpResponseListener, e);
                    }
                }

            });

        } catch (Exception e) {
            removeOnFailure(request, httpResponseListener, e);
        }
    }

    private void removeOnFailure(Net.HttpRequest request, Net.HttpResponseListener httpResponseListener, Exception e) {
        lock.lock();
        try {
            httpResponseListener.failed(e);
        } finally {
            requests.remove(request);
            listeners.remove(request);
            lock.unlock();
        }
    }

    private static NSInputStream toNSInputStream(InputStream in) throws IOException {
        final int BUFSIZE = 4096;
        ByteArrayOutputStream out;
        out = new ByteArrayOutputStream(BUFSIZE);
        try {
            byte[] tmp = new byte[BUFSIZE];
            while (true) {
                int r = in.read(tmp);
                if (r == -1) {
                    break;
                }
                out.write(tmp, 0, r);
            }
            ByteBuffer bytes = ByteBuffer.wrap(out.toByteArray());
            return new NSInputStream(new NSData(bytes));
        } finally {
            StreamUtils.closeQuietly(out);
        }
    }

    public void cancelHttpRequest(Net.HttpRequest httpRequest) {
        try {
            lock.lock();
            Net.HttpResponseListener httpResponseListener = listeners.get(httpRequest);

            if (httpResponseListener != null) {
                httpResponseListener.cancelled();
                requests.remove(httpRequest);
                listeners.remove(httpRequest);
            }
        } finally {
            lock.unlock();
        }
    }

}
