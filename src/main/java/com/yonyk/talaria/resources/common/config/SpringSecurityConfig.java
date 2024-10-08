package com.yonyk.talaria.resources.common.config;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import com.yonyk.talaria.resources.common.security.filter.AuthorizationFilter;
import com.yonyk.talaria.resources.common.security.grpc.GrpcClientService;
import com.yonyk.talaria.resources.common.security.handler.*;
import com.yonyk.talaria.resources.common.security.util.AuthorizationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SpringSecurityConfig {
  // 인가 관련 서비스
  private final AuthorizationService authorizationService;
  // gRPC 클라이언트 서비스
  private final GrpcClientService grpcClientService;

  // 예외 처리 핸들러 설정
  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
  private final CustomAccessDeniedHandler customAccessDeniedHandler;

  // accessToken 헤더 이름
  @Value("${jwt.access-token-header}")
  public String accessTokenHeader;

  // 비밀번호 암호화
  @Bean
  public static BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  // 특정 요청 경로에 대해서는 시큐리티 검사 무시
  // resources는 이미지, css, javascript 파일등에 대한 요청을 의미함
  @Bean
  WebSecurityCustomizer webSecurityCustomizer() {
    return (webSecurity) -> webSecurity.ignoring().requestMatchers("/resources/**");
  }

  // 인가 관련 필터
  @Bean
  public AuthorizationFilter authorizationFilter() throws Exception {
    return new AuthorizationFilter(authorizationService, grpcClientService);
  }

  // 지정된 출처(주소)에서 오는 요청 관련 설정
  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    return request -> {
      CorsConfiguration config = new CorsConfiguration();
      // 서버에서 내려보내는 헤더의 특정 내용을 볼 수 있도록 허용
      config.addExposedHeader(accessTokenHeader);
      // 클라이언트에서 보내오는 헤더와 메서드 허용
      config.setAllowedHeaders(Collections.singletonList("*"));
      config.setAllowedMethods(Collections.singletonList("*"));
      // 모든 출처에서 오는 요청 허용
      config.setAllowedOrigins(Collections.singletonList("*"));
      // 쿠키 등 자격증명이 포함된 요청 허용
      config.setAllowCredentials(true);
      return config;
    };
  }

  // 필터체인 설정
  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // 사용자가 의도하지 않은 요청 방지 설정
        .csrf(csrfConf -> csrfConf.disable())
        // 위에서 적은 Cors 설정 적용
        .cors(c -> c.configurationSource(corsConfigurationSource()))
        // 기본 폼 로그인 기능 비활성화
        .formLogin(loginConf -> loginConf.disable())
        // 특정 경로에 대한 인가 설정
        .authorizeHttpRequests(
            authz ->
                authz
                    // Swagger 설정
                    .requestMatchers("/v3/**", "/swagger-ui/**")
                    .permitAll()
                    // 제품 등록, 수정, 삭제는 ADMIN 만 가능
                    .requestMatchers(HttpMethod.POST, "/api/product")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/product")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/product/*")
                    .hasRole("ADMIN")
                    // 제품 조회는 ADMIN 및 USER 가능
                    .requestMatchers(HttpMethod.GET, "/api/product/*")
                    .hasAnyRole("ADMIN", "USER")
                    // 주문 생성, 조회, 수정, 삭제는 ADMIN 및 USER 가능
                    .requestMatchers("/api/order", "/api/order/*")
                    .hasAnyRole("ADMIN", "USER")
                    // 이외 모든 요청 인증 필요
                    .anyRequest()
                    .authenticated())
        // 예외 처리 핸들러 설정
        .exceptionHandling(
            exceptionHandling ->
                exceptionHandling
                    .authenticationEntryPoint(customAuthenticationEntryPoint)
                    .accessDeniedHandler(customAccessDeniedHandler));
    // 커스텀 필터 설정
    http.addFilterBefore(authorizationFilter(), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
