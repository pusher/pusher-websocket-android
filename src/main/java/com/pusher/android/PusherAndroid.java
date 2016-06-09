package com.pusher.android;


import com.pusher.client.Client;
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
    @Override
    public Connection getConnection() {
        return null;
    }

    @Override
    public void connect() {

    }

    @Override
    public void connect(ConnectionEventListener eventListener, ConnectionState... connectionStates) {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public Channel subscribe(String channelName) {
        return null;
    }

    @Override
    public Channel subscribe(String channelName, ChannelEventListener listener, String... eventNames) {
        return null;
    }

    @Override
    public PrivateChannel subscribePrivate(String channelName) {
        return null;
    }

    @Override
    public PrivateChannel subscribePrivate(String channelName, PrivateChannelEventListener listener, String... eventNames) {
        return null;
    }

    @Override
    public PresenceChannel subscribePresence(String channelName) {
        return null;
    }

    @Override
    public PresenceChannel subscribePresence(String channelName, PresenceChannelEventListener listener, String... eventNames) {
        return null;
    }

    @Override
    public void unsubscribe(String channelName) {

    }

    @Override
    public Channel getChannel(String channelName) {
        return null;
    }

    @Override
    public PrivateChannel getPrivateChannel(String channelName) {
        return null;
    }

    @Override
    public PresenceChannel getPresenceChannel(String channelName) {
        return null;
    }
}
