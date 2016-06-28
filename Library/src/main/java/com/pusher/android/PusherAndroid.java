package com.pusher.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.pusher.client.Client;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.ChannelEventListener;
import com.pusher.client.channel.PresenceChannel;
import com.pusher.client.channel.PresenceChannelEventListener;
import com.pusher.client.channel.PrivateChannel;
import com.pusher.client.channel.PrivateChannelEventListener;
import com.pusher.client.connection.Connection;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;

/**
 * Created by jamiepatel on 09/06/2016.
 */
public class PusherAndroid implements Client {

    private final Pusher pusher;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";

    public PusherAndroid(final String apiKey) {
        this(apiKey, new PusherOptions());
    }

    PusherAndroid(final String apiKey, final PusherOptions pusherOptions) {
        this.pusher = new Pusher(apiKey, pusherOptions);
        PusherPushNotificationRegistration.getInstance().setApiKey(apiKey);
    }

    @Override
    public Connection getConnection() {
        return this.pusher.getConnection();
    }

    @Override
    public void connect() {
        this.pusher.connect();
    }

    @Override
    public void connect(ConnectionEventListener eventListener, ConnectionState... connectionStates) {
        this.pusher.connect(eventListener, connectionStates);
    }

    @Override
    public void disconnect() {
        this.pusher.disconnect();
    }

    @Override
    public Channel subscribe(String channelName) {
        return this.pusher.subscribe(channelName);
    }

    @Override
    public Channel subscribe(String channelName, ChannelEventListener listener, String... eventNames) {
        return this.pusher.subscribe(channelName, listener, eventNames);
    }

    @Override
    public PrivateChannel subscribePrivate(String channelName) {
        return this.pusher.subscribePrivate(channelName);
    }

    @Override
    public PrivateChannel subscribePrivate(String channelName, PrivateChannelEventListener listener, String... eventNames) {
        return this.pusher.subscribePrivate(channelName, listener, eventNames);
    }

    @Override
    public PresenceChannel subscribePresence(String channelName) {
        return this.pusher.subscribePresence(channelName);
    }

    @Override
    public PresenceChannel subscribePresence(String channelName, PresenceChannelEventListener listener, String... eventNames) {
        return this.pusher.subscribePresence(channelName, listener, eventNames);
    }

    @Override
    public void unsubscribe(String channelName) {
        this.pusher.unsubscribe(channelName);
    }

    @Override
    public Channel getChannel(String channelName) {
        return this.pusher.getChannel(channelName);
    }

    @Override
    public PrivateChannel getPrivateChannel(String channelName) {
        return this.pusher.getPrivateChannel(channelName);
    }

    @Override
    public PresenceChannel getPresenceChannel(String channelName) {
        return this.pusher.getPresenceChannel(channelName);
    }

    public PusherPushNotificationRegistration nativePusher() {
        return PusherPushNotificationRegistration.getInstance();
    }

}
