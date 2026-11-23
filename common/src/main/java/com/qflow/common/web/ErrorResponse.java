package com.qflow.common.web;

public record ErrorResponse(int status, String message, long timestamp) {
}