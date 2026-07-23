package com.tradenotes.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        return new Result<>(true, "ok", data);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(false, msg, null);
    }
}
