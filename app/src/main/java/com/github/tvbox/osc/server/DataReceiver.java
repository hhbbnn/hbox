package com.github.tvbox.osc.server;

/**
 * @author pj567
 * @date :2021/1/5
 * @description:
 */
public interface DataReceiver {

    /**
     * @param text
     */
    void onTextReceived(String text);

    void onApiReceived(String name,String url);

    void onLiveReceived(String url);

    void onEpgReceived(String url);

    void onPushReceived(String url);

    void onRepoReceived(String url);
}