/*
 * Copyright 2016-2019 新商态（北京）科技有限公司
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xst.golddata

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import groovyx.net.http.ApacheHttpBuilder
import groovyx.net.http.HttpBuilder
import org.apache.http.HttpException
import org.apache.http.HttpHost
import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
import org.apache.http.client.HttpRequestRetryHandler
import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.apache.http.config.RegistryBuilder
import org.apache.http.config.SocketConfig
import org.apache.http.conn.ConnectTimeoutException
import org.apache.http.conn.DnsResolver
import org.apache.http.conn.socket.ConnectionSocketFactory
import org.apache.http.conn.socket.PlainConnectionSocketFactory
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.protocol.HttpContext
import org.apache.http.protocol.HttpCoreContext
import org.apache.http.ssl.SSLContexts
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSession
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

/**
 * Created by wdg on 2018/8/2.
 */
class HttpConfig {
    static Logger logger=LoggerFactory.getLogger(HttpConfig)
    static  HttpBuilder builder=null;
    static SSLContext sslContext = SSLContexts.custom()
            .loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
//    static PoolingHttpClientConnectionManager connectionManager=new PoolingHttpClientConnectionManager()

    static NoopHostnameVerifier hostnameVerifier= new NoopHostnameVerifier(){
        @Override
        public boolean verify(final String s, final SSLSession sslSession) {
            return true;
        }

    }
    static SSLConnectionSocketFactory sslConnectionSocketFactory=new SSLConnectionSocketFactory(sslContext,hostnameVerifier){
        @Override
        public Socket createSocket(final HttpContext context) throws IOException {
            Proxy proxy=context.getAttribute('proxy')
            if(proxy!=null) {
                return new Socket(proxy);
            }
            return super.createSocket(context)
        }

        @Override
        public Socket connectSocket(int connectTimeout, Socket socket, HttpHost host, InetSocketAddress remoteAddress,
                                    InetSocketAddress localAddress, HttpContext context) throws IOException {

            if(context.getAttribute('proxy')){
                // Convert address to unresolved
                InetSocketAddress unresolvedRemote = InetSocketAddress
                        .createUnresolved(host.getHostName(), remoteAddress.getPort());
                return super.connectSocket(connectTimeout, socket, host, unresolvedRemote, localAddress, context);
            }
            return super.connectSocket(connectTimeout,socket,host,remoteAddress,localAddress,context);
        }
    }
    static     PoolingHttpClientConnectionManager connectionManager=           new PoolingHttpClientConnectionManager(
                            RegistryBuilder.<ConnectionSocketFactory>create()
                                    .register("http", new MyConnectionSocketFactory())
                                    .register("https", sslConnectionSocketFactory) .build(),new FakeDnsResolver())
     static     PoolingHttpClientConnectionManager defaultDnsConnectonManager=   new PoolingHttpClientConnectionManager(
                    RegistryBuilder.<ConnectionSocketFactory>create()
                            .register("http", new MyConnectionSocketFactory())
                            .register("https",sslConnectionSocketFactory ) .build())

    static class FakeDnsResolver implements DnsResolver {
        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {
            // Return some fake DNS record for every request, we won't be using it
            byte[] aa=new byte[4]
            aa[0]=(byte)1
            aa[1]=(byte)1
            aa[2]=(byte)1
            aa[3]=(byte)1

            return [ InetAddress.getByAddress(aa ) ].toArray(new InetAddress[1]);
        }
    }
    static class MyConnectionSocketFactory extends PlainConnectionSocketFactory {
        @Override
        public Socket createSocket(final HttpContext context) throws IOException {

            Proxy proxy=context.getAttribute('proxy')
            if(proxy!=null) {
                return new Socket(proxy);
            }
            return super.createSocket(context)

        }

        @Override
        public Socket connectSocket(int connectTimeout, Socket socket, HttpHost host, InetSocketAddress remoteAddress,
                                    InetSocketAddress localAddress, HttpContext context) throws IOException {
            if(context.getAttribute('proxy')){
                // Convert address to unresolved
                InetSocketAddress unresolvedRemote = InetSocketAddress
                        .createUnresolved(host.getHostName(), remoteAddress.getPort());
                return super.connectSocket(connectTimeout, socket, host, unresolvedRemote, localAddress, context);
            }
            return super.connectSocket(connectTimeout,socket,host,remoteAddress,localAddress,context);
        }
    }
    static Closure noProxyClosure=null
    public  static HttpBuilder get(){
        if(!noProxyClosure){
            noProxyClosure={
               client.clientCustomizer( HttpConfig.createHttpConfig(null))
            }
        }
        builder=ApacheHttpBuilder.configure (noProxyClosure)
        return  builder;
    }

    static LoadingCache<String,Closure> cache=CacheBuilder.newBuilder().expireAfterWrite(3,TimeUnit.MINUTES).build(new CacheLoader<String, Closure>() {
        @Override
        Closure load(String proxy) throws Exception {

            def proxyType=null;
            def proxyHost=null;
            def proxyPort=null;

            if(proxy.startsWith("socks")){
                String tempStr=proxy.replace('socks://','');
                if(tempStr.indexOf(":")>0){
                    proxyType=Proxy.Type.SOCKS
                    def xx= tempStr.split(':')
                    proxyHost=xx[0]
                    proxyPort=xx[1]
                }
            }
            if(proxy.startsWith("http")){
                proxyType=Proxy.Type.HTTP
                URL proxyUrl= new URL(proxy);
                proxyHost=proxyUrl.host
                proxyPort=proxyUrl.port
            }

            Closure c={
                Integer intProxyPort=new Integer(proxyPort)
                execution.proxy proxyHost, intProxyPort, proxyType, false
                client.clientCustomizer( HttpConfig.createHttpConfig(execution.getProxyInfo().proxy))
            }


            return c;
        }
    })

    public  static HttpBuilder get(String key){

        ApacheHttpBuilder builder2=ApacheHttpBuilder.configure (cache.get(key))
       return builder2
    }

    private static Consumer createHttpConfig(Proxy proxy){
        Consumer aa={ HttpClientBuilder builder ->
            RequestConfig.Builder requestBuilder=RequestConfig.custom()

            requestBuilder.setCookieSpec(CookieSpecs.STANDARD)
            SocketConfig socketConfig = SocketConfig.custom()
                    .setRcvBufSize(1024*1024)
                    .setSoKeepAlive(true)
                    .setTcpNoDelay(false)
                    .build();
            builder.setDefaultSocketConfig(socketConfig)
            String connTimeout=System.getProperty('http.connectTimeout');
            String socketTimeout=System.getProperty('http.socketTimeout');

            requestBuilder.connectTimeout=connTimeout?new Integer(connTimeout):10000

            requestBuilder.socketTimeout=socketTimeout?new Integer(socketTimeout):10000
            requestBuilder.circularRedirectsAllowed=true
            builder.setRedirectStrategy(new PreservedRedirectStrategy())
            RequestConfig rc=requestBuilder.build();

            builder.defaultRequestConfig=rc

            builder.setRetryHandler(retryHandler())


            connectionManager.setMaxTotal(200)
            connectionManager.setDefaultMaxPerRoute(10)
            defaultDnsConnectonManager.setMaxTotal(200)
            defaultDnsConnectonManager.setDefaultMaxPerRoute(10)
            if(proxy!=null){
                builder.addInterceptorFirst(new HttpRequestInterceptor() {
                    @Override
                    void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
                        context.setAttribute("proxy",proxy);
                    }
                })
                builder.setConnectionManager(connectionManager)
            }else{
                builder.setConnectionManager(defaultDnsConnectonManager)
            }


        };

        return aa
    }
    private static   HttpRequestRetryHandler retryHandler(){
        int retryTime=3;
        HttpRequestRetryHandler myRetryHandler = new HttpRequestRetryHandler(){
            @Override
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context)
            {
                HttpRequest request = (HttpRequest) context.getAttribute(HttpCoreContext.HTTP_REQUEST);

                logger.info('retry:{},ex:{},uri:{}',executionCount,exception.getClass().simpleName,request.getRequestLine().uri)
                if (executionCount >= retryTime)
                {
                    // Do not retry if over max retry count
                    return false;
                }

                if (exception instanceof InterruptedIOException)
                {
                    // Timeout
//                    return false;
                }

                if (exception instanceof UnknownHostException)
                {
                    // Unknown host
                    return false;
                }
                if (exception instanceof ConnectException)
                {
                    // Connection refused
                    return false;
                }

                if (exception instanceof SSLException)
                {
                    // SSL handshake exception
                    return false;
                }
                if (exception instanceof ConnectTimeoutException)
                {
                    // Timeout
//                    return false;
                    return  true;
                }
                if(exception instanceof SocketTimeoutException){

                    return true;
                }

                return true;
            }

        };
        return  myRetryHandler;
    }
}

