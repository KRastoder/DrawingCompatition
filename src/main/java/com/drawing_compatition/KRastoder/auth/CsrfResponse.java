package com.drawing_compatition.KRastoder.auth;

public record CsrfResponse(String cookieName, String headerName, String token) {
}
