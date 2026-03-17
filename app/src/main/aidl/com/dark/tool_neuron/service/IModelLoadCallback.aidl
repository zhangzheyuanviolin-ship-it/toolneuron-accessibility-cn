package com.dark.tool_neuron.service;

interface IModelLoadCallback {
    void onSuccess();
    void onError(String message);
}