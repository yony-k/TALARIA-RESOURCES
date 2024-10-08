package com.yonyk.talaria.resources.common.security.grpc;

import org.springframework.stereotype.Component;

import io.grpc.*;

@Component
public class AuthenticationClientInterceptor implements ClientInterceptor {
  private Metadata metadata;

  public void setInterceptor(String accessTokenHeader, String accessToken) {
    this.metadata = new Metadata();
    Metadata.Key<String> key = Metadata.Key.of(accessTokenHeader, Metadata.ASCII_STRING_MARSHALLER);
    metadata.put(key, accessToken);
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {
    return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
        channel.newCall(methodDescriptor, callOptions)) {
      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        // 헤더에 accessToken이 들어간 메타데이터 등록
        headers.merge(metadata);
        // 서비스 클래스로 넘어가기
        super.start(responseListener, headers);
      }
    };
  }
}
