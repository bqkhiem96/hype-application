//
// MIT License
//
// Copyright (C) 2018 HypeLabs Inc.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//

package com.hypelabs.hypechatdemo;

import android.app.Application;
import android.util.Log;

import com.hypelabs.hype.Error;
import com.hypelabs.hype.Hype;
import com.hypelabs.hype.Instance;
import com.hypelabs.hype.Message;
import com.hypelabs.hype.MessageInfo;
import com.hypelabs.hype.MessageObserver;
import com.hypelabs.hype.NetworkObserver;
import com.hypelabs.hype.StateObserver;

import java.util.HashMap;
import java.util.Map;

public class ChatApplication extends BaseApplication implements StateObserver, NetworkObserver, MessageObserver, BaseApplication.LifecycleDelegate {

    private static final String TAG = ChatApplication.class.getName();

    // The stores object keeps track of message storage associated with each instance (peer)
    private Map<String, Store> stores;

    @Override
    public void onApplicationStart(Application app) {

        requestHypeToStart();
    }

    @Override
    public void onApplicationStop(Application app) {

        requestHypeToStop();
    }

    protected void requestHypeToStart() {

        Hype.setContext(getApplicationContext());

        // Add this as an Hype observer
        Hype.addStateObserver(this);
        Hype.addNetworkObserver(this);
        Hype.addMessageObserver(this);

        // Generate an app identifier in the HypeLabs dashboard (https://hypelabs.io/apps/),
        // by creating a new app. Copy the given identifier here.
        Hype.setAppIdentifier("f0e3f9c5");

        Hype.start();
    }

    protected void requestHypeToStop()
    {
        // The current release has a known issue with Bluetooth Low Energy that causes all
        // connections to drop when the SDK is stopped. This is an Android issue.
        Hype.stop();
    }

    @Override
    public void onHypeStart() {
        Log.i(TAG, "Hype started!");
    }

    @Override
    public void onHypeStop(Error error) {

        String description = "";

        if (error != null) {
            description = String.format("[%s]", error.getDescription());
        }

        Log.i(TAG, String.format("Hype stopped [%s]", description));
    }

    public void onHypeFailedStarting(Error error) {

        Log.i(TAG, String.format("Hype failed starting [%s]", error.getDescription()));
    }

    @Override
    public void onHypeReady() {

        Log.i(TAG, String.format("Hype is ready"));

        requestHypeToStart();
    }

    @Override
    public void onHypeStateChange() {

        Log.i(TAG, String.format("Hype changed state to [%d] (Idle=0, Starting=1, Running=2, Stopping=3)", Hype.getState().getValue()));
    }

    boolean shouldResolveInstance(Instance instance)
    {
        // This method can be used to decide whether an instance is interesting
        return true;
    }

    @Override
    public void onHypeInstanceFound(Instance instance) {

        Log.i(TAG, String.format("Hype found instance: %s", instance.getStringIdentifier()));

        if(shouldResolveInstance(instance)){
            Hype.resolve(instance);
        }
    }

    @Override
    public void onHypeInstanceLost(Instance instance, Error error) {

        Log.i(TAG, String.format("Hype lost instance: %s [%s]", instance.getStringIdentifier(), error.getDescription()));
        removeFromResolvedInstancesMap(instance);
    }

    @Override
    public void onHypeInstanceResolved(Instance instance) {

        Log.i(TAG, String.format("Hype resolved instance: %s", instance.getStringIdentifier()));

        // This device is now capable of communicating
        addToResolvedInstancesMap(instance);
    }

    @Override
    public void onHypeInstanceFailResolving(Instance instance, Error error) {

        Log.i(TAG, String.format("Hype failed resolving instance: %s [%s]", instance.getStringIdentifier(), error.getDescription()));
    }

    @Override
    public void onHypeMessageReceived(Message message, Instance instance) {
        String receivedMessage = new String(message.getData());
        Log.i("WTF", "message = " + receivedMessage);
        Log.i(TAG, String.format("Hype got a message from: %s", instance.getStringIdentifier()));

        Store store = getStores().get(instance.getStringIdentifier());

        // Storing the message triggers a reload update in the ChatActivity
        store.add(message);

        // Update the UI for the ContactActivity as well
        ContactActivity contactActivity = ContactActivity.getDefaultInstance();

        if (contactActivity != null) {
            contactActivity.notifyAddedMessage();
        }
    }

    @Override
    public void onHypeMessageFailedSending(MessageInfo messageInfo, Instance instance, Error error) {

        Log.i(TAG, String.format("Hype failed to send message: %d [%s]", messageInfo.getIdentifier(), error.getDescription()));
    }

    @Override
    public void onHypeMessageSent(MessageInfo messageInfo, Instance instance, float progress, boolean done) {

        Log.i(TAG, String.format("Hype is sending a message: %f", progress));
    }

    @Override
    public void onHypeMessageDelivered(MessageInfo messageInfo, Instance instance, float progress, boolean done) {

        Log.i(TAG, String.format("Hype delivered a message: %f", progress));
    }

    @Override
    public String onHypeRequestAccessToken(int i) {
        // Access the app settings (https://hypelabs.io/apps/) to find an access token to use here.
        return "558cc6d448bd5c647fea181813b636";
    }

    @Override
    public void onCreate() {

        super.onCreate();

        // See BaseApplication.java
        setLifecyleDelegate(this);
    }

    public Map<String, Store> getStores() {

        if (stores == null) {
            stores = new HashMap<>();
        }

        return stores;
    }

    public void addToResolvedInstancesMap(Instance instance) {
        // Instances should be strongly kept by some data structure. Their identifiers
        // are useful for keeping track of which instances are ready to communicate.
        getStores().put(instance.getStringIdentifier(), new Store(instance));

        // Notify the contact activity to refresh the UI
        ContactActivity contactActivity = ContactActivity.getDefaultInstance();

        if (contactActivity != null) {
            contactActivity.notifyContactsChanged();
        }
    }

    public void removeFromResolvedInstancesMap(Instance instance) {
        // Cleaning up is always a good idea. It's not possible to communicate with instances
        // that were previously lost.
        getStores().remove(instance.getStringIdentifier());

        // Notify the contact activity to refresh the UI
        ContactActivity contactActivity = ContactActivity.getDefaultInstance();

        if (contactActivity != null) {
            contactActivity.notifyContactsChanged();
        }
    }
}